package com.cm4j.test.guava.consist;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.value.IValue;
import com.google.common.collect.AbstractSequentialIterator;

/**
 * 提供put、get、expire操作
 * 
 * @author Yang.hao
 * @since 2013-1-30 上午11:25:47
 * 
 * @param <K>
 * @param <V>
 */
public class ConcurrentCache<K, V> implements Serializable {
	private static final long serialVersionUID = 7249069246763182397L;

	/* ---------------- Constants -------------- */

	static final int DEFAULT_INITIAL_CAPACITY = 16;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	static final int MAXIMUM_CAPACITY = 1 << 30;
	static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
	static final int RETRIES_BEFORE_LOCK = 2;

	/* ---------------- Fields -------------- */

	final CacheLoader<K, V> loader;
	final int segmentMask;
	final int segmentShift;
	final Segment<K, V>[] segments;

	transient Set<K> keySet;
	transient Set<Map.Entry<K, V>> entrySet;
	transient Collection<V> values;

	// TODO 默认过期纳秒
	final long expireAfterAccessNanos = TimeUnit.SECONDS.toNanos(3);

	/* ---------------- Small Utilities -------------- */

	private static int hash(int h) {
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
		h += (h << 15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h << 3);
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
		return h ^ (h >>> 16);
	}

	final Segment<K, V> segmentFor(int hash) {
		return segments[(hash >>> segmentShift) & segmentMask];
	}

	/* ---------------- Inner Classes -------------- */

	static class HashEntry<K, V> implements ReferenceEntry<K, V> {
		final K key;
		final int hash;
		volatile V value;
		final HashEntry<K, V> next;

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public int getHash() {
			return hash;
		}

		@Override
		public ReferenceEntry<K, V> getNext() {
			return next;
		}

		HashEntry(K key, int hash, HashEntry<K, V> next, V value) {
			this.key = key;
			this.hash = hash;
			this.next = next;
			this.value = value;
		}

		static final <K, V> AtomicReferenceArray<HashEntry<K, V>> newArray(int i) {
			return new AtomicReferenceArray<HashEntry<K, V>>(i);
		}

		volatile long accessTime = Long.MAX_VALUE;

		@Override
		public long getAccessTime() {
			return accessTime;
		}

		@Override
		public void setAccessTime(long time) {
			this.accessTime = time;
		}

		ReferenceEntry<K, V> nextAccess = nullEntry();

		@Override
		public ReferenceEntry<K, V> getNextInAccessQueue() {
			return nextAccess;
		}

		@Override
		public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
			this.nextAccess = next;
		}

		ReferenceEntry<K, V> previousAccess = nullEntry();

		@Override
		public ReferenceEntry<K, V> getPreviousInAccessQueue() {
			return previousAccess;
		}

		@Override
		public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
			this.previousAccess = previous;
		}
	}

	static final class Segment<K, V> extends ReentrantLock implements Serializable {

		private static final long serialVersionUID = 2249069246763182397L;

		// 作为位操作的mask，必须是(2^n)-1
		static final int DRAIN_THRESHOLD = 0x3F;

		final ConcurrentCache<K, V> map;

		transient volatile int count;
		transient int modCount;
		transient int threshold;
		transient volatile AtomicReferenceArray<HashEntry<K, V>> table;
		final float loadFactor;

		final AccessQueue<K, V> accessQueue;
		final AtomicInteger readCount = new AtomicInteger();

		Segment(ConcurrentCache<K, V> map, int initialCapacity, float lf) {
			this.map = map;
			loadFactor = lf;
			setTable(HashEntry.<K, V> newArray(initialCapacity));
			accessQueue = new AccessQueue<K, V>();
		}

		@SuppressWarnings("unchecked")
		static final <K, V> Segment<K, V>[] newArray(int i) {
			return new Segment[i];
		}

		void setTable(AtomicReferenceArray<HashEntry<K, V>> newTable) {
			threshold = (int) (newTable.length() * loadFactor);
			table = newTable;
		}

		HashEntry<K, V> getFirst(int hash) {
			AtomicReferenceArray<HashEntry<K, V>> tab = table;
			return tab.get(hash & (tab.length() - 1));
		}

		HashEntry<K, V> getEntry(K key, int hash) {
			for (HashEntry<K, V> e = getFirst(hash); e != null; e = e.next) {
				if (e.hash == hash && key.equals(e.key)) {
					return e;
				}
			}
			return null;
		}

		HashEntry<K, V> getLiveEntry(K key, int hash, long now) {
			HashEntry<K, V> e = getEntry(key, hash);
			if (e == null) {
				return null;
			} else if (map.isExpired(e, now)) {
				tryExpireEntries(now);
				if (isValueAllPersist(e.getValue())) {
					return null;
				}
			}
			return e;
		}

		V getLiveValue(HashEntry<K, V> entry, long now) {
			if (entry.key == null) {
				return null;
			}
			if (entry.value == null) {
				return null;
			}
			if (map.isExpired(entry, now)) {
				// 如果状态不是P，则会延迟生命周期
				tryExpireEntries(now);
				if (isValueAllPersist(entry.getValue())) {
					return null;
				}
			}
			return entry.value;
		}

		V lockedGetOrLoad(K key, int hash, CacheLoader<K, V> loader) {
			lock();
			try {
				V value = null;
				// 重新读取entry的值
				// 防止2个线程同时load，都从db获取数据，导致加载了2份数据
				HashEntry<K, V> e = getEntry(key, hash);
				if (e != null) {
					value = getLiveValue(e, now());
				}
				if (value == null) {
					value = loader.load(key);
				}

				if (value != null) {
					put(key, hash, value, false);
				}
				return value;
			} finally {
				unlock();
			}
		}

		V get(K key, int hash, CacheLoader<K, V> loader, boolean isLoad) {
			try {
				if (count != 0) { // read-volatile
					HashEntry<K, V> e = getEntry(key, hash);
					if (e != null) {
						V value = getLiveValue(e, now());
						if (value != null) {
							recordAccess(e);
							return value;
						}
					}
				}
				if (isLoad) {
					// at this point e is either null or expired;
					return lockedGetOrLoad(key, hash, loader);
				}
			} finally {
				postReadCleanup();
			}
			return null;
		}

		boolean containsKey(K key, int hash) {
			if (count != 0) { // read-volatile
				long now = now();
				HashEntry<K, V> e = getLiveEntry(key, hash, now);
				return e != null;
			}
			return false;
		}

		boolean replace(K key, int hash, V oldValue, V newValue) {
			lock();
			preWriteCleanup(now());
			try {
				HashEntry<K, V> e = getFirst(hash);
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				boolean replaced = false;
				if (e != null && oldValue.equals(e.value)) {
					replaced = true;
					e.value = newValue;
				}
				return replaced;
			} finally {
				unlock();
				postWriteCleanup();
			}
		}

		V replace(K key, int hash, V newValue) {
			lock();
			preWriteCleanup(now());
			try {
				HashEntry<K, V> e = getFirst(hash);
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue = null;
				if (e != null) {
					oldValue = e.value;
					e.value = newValue;
				}
				return oldValue;
			} finally {
				unlock();
				postWriteCleanup();
			}
		}

		V put(K key, int hash, V value, boolean onlyIfAbsent) {
			lock();
			try {
				preWriteCleanup(now());

				int c = count;
				if (c++ > threshold) { // ensure capacity
					rehash();
				}
				AtomicReferenceArray<HashEntry<K, V>> tab = table;
				int index = hash & (tab.length() - 1);
				HashEntry<K, V> first = tab.get(index);
				HashEntry<K, V> e = first;
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue;
				if (e != null) {
					oldValue = e.value;
					if (!onlyIfAbsent) {
						e.value = value;
						recordAccess(e);
					}
				} else {
					oldValue = null;
					++modCount;
					e = new HashEntry<K, V>(key, hash, first, value);
					recordAccess(e);
					tab.set(index, e);
					count = c; // write-volatile
				}

				// TODO 在put的时候对value设置所属key
				// if (value instanceof SingleValue) {
				// ((SingleValue) value).setAttachedKey((String) key);
				// } else if (value instanceof ListValue<?>) {
				// List<? extends CacheEntry> all = ((ListValue<?>)
				// value).getAll_objects();
				// for (CacheEntry cacheEntry : all) {
				// cacheEntry.setAttachedKey((String) key);
				// }
				// }

				return oldValue;
			} finally {
				unlock();
				postWriteCleanup();
			}
		}

		void rehash() {
			AtomicReferenceArray<HashEntry<K, V>> oldTable = table;
			int oldCapacity = oldTable.length();
			if (oldCapacity >= MAXIMUM_CAPACITY)
				return;

			AtomicReferenceArray<HashEntry<K, V>> newTable = HashEntry.newArray(oldCapacity << 1);
			threshold = (int) (newTable.length() * loadFactor);
			int sizeMask = newTable.length() - 1;
			for (int i = 0; i < oldCapacity; i++) {
				// We need to guarantee that any existing reads of old Map can
				// proceed. So we cannot yet null out each bin.
				HashEntry<K, V> e = oldTable.get(i);

				if (e != null) {
					HashEntry<K, V> next = e.next;
					int idx = e.hash & sizeMask;

					// Single node on list
					if (next == null)
						newTable.set(idx, e);

					else {
						// Reuse trailing consecutive sequence at same slot
						HashEntry<K, V> lastRun = e;
						int lastIdx = idx;
						for (HashEntry<K, V> last = next; last != null; last = last.next) {
							int k = last.hash & sizeMask;
							if (k != lastIdx) {
								lastIdx = k;
								lastRun = last;
							}
						}
						newTable.set(lastIdx, lastRun);

						// Clone all remaining nodes
						for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
							int k = p.hash & sizeMask;
							HashEntry<K, V> n = newTable.get(k);
							newTable.set(k, new HashEntry<K, V>(p.key, p.hash, n, p.value));
						}
					}
				}
			}
			table = newTable;
		}

		/**
		 * Remove; match on key only if value null, else match both.
		 */
		V remove(K key, int hash, V value) {
			lock();
			try {
				preWriteCleanup(now());

				int c = count - 1;
				AtomicReferenceArray<HashEntry<K, V>> tab = table;
				int index = hash & (tab.length() - 1);
				HashEntry<K, V> first = tab.get(index);
				HashEntry<K, V> e = first;
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue = null;
				if (e != null) {
					V v = e.value;
					if (value == null || value.equals(v)) {
						oldValue = v;
						// All entries following removed node can stay
						// in list, but all preceding ones need to be
						// cloned.
						++modCount;
						HashEntry<K, V> newFirst = e.next;
						for (HashEntry<K, V> p = first; p != e; p = p.next)
							newFirst = new HashEntry<K, V>(p.key, p.hash, newFirst, p.value);
						tab.set(index, newFirst);

						// 从队列移除
						accessQueue.remove(e);

						count = c; // write-volatile
					}
				}
				return oldValue;
			} finally {
				unlock();
				postWriteCleanup();
			}
		}

		void removeEntry(HashEntry<K, V> e, int hash) {
			int c = count - 1;
			AtomicReferenceArray<HashEntry<K, V>> tab = table;
			int index = hash & (tab.length() - 1);
			HashEntry<K, V> first = tab.get(index);

			++modCount;
			HashEntry<K, V> newFirst = e.next;
			for (HashEntry<K, V> p = first; p != e; p = p.next) {
				newFirst = new HashEntry<K, V>(p.key, p.hash, newFirst, p.value);
			}
			tab.set(index, newFirst);
			// 从队列移除
			accessQueue.remove(e);
			count = c; // write-volatile
		}

		void clear() {
			if (count != 0) {
				lock();
				try {
					AtomicReferenceArray<HashEntry<K, V>> tab = table;
					for (int i = 0; i < tab.length(); i++)
						tab.set(i, null);

					accessQueue.clear();
					++modCount;
					count = 0; // write-volatile
				} finally {
					unlock();
				}
			}
		}

		// expiration，过期相关业务

		/**
		 * Cleanup expired entries when the lock is available.
		 */
		void tryExpireEntries(long now) {
			if (tryLock()) {
				try {
					expireEntries(now);
				} finally {
					unlock();
					// don't call postWriteCleanup as we're in a read
				}
			}
		}

		void expireEntries(long now) {
			ReferenceEntry<K, V> e;
			while ((e = accessQueue.peek()) != null && map.isExpired(e, now)) {
				// 移除的时候锁定
				lock();
				try {
					// TODO 并发问题 ：简单说：对象过期被删，此时有另一线程修改，还有一个从db加载则有问题
					// 1.isValueAllPersist()通过
					// 2.对象修改
					// 3.removeEntry()成功
					// 4.另一个线程从db加载
					// 5.对象修改状态成功，写库与4的读取非同一对象
					// 强引用解决？
					// or 判断对象在缓存是否存在，区分是首次还是过期了
					if (isValueAllPersist(e.getValue())) {
						removeEntry((HashEntry<K, V>) e, e.getHash());
					} else {
						recordAccess(e);
					}
				} finally {
					unlock();
				}
			}
		}

		boolean isValueAllPersist(V value) {
			if (value instanceof IValue) {
				IValue _v = (IValue) value;
				return _v.isAllPersist();
			}
			return false;
		}

		/**
		 * Performs routine cleanup following a read. Normally cleanup happens
		 * during writes. If cleanup is not observed after a sufficient number
		 * of reads, try cleaning up from the read thread.
		 */
		void postReadCleanup() {
			if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
				cleanUp();
			}
		}

		/**
		 * Performs routine cleanup prior to executing a write. This should be
		 * called every time a write thread acquires the segment lock,
		 * immediately after acquiring the lock.
		 * 
		 * <p>
		 * Post-condition: expireEntries has been run.
		 */
		void preWriteCleanup(long now) {
			runLockedCleanup(now);
		}

		/**
		 * Performs routine cleanup following a write.
		 */
		void postWriteCleanup() {
			runUnlockedCleanup();
		}

		void cleanUp() {
			runLockedCleanup(now());
			runUnlockedCleanup();
		}

		void runLockedCleanup(long now) {
			if (tryLock()) {
				try {
					// 过期，且移除
					expireEntries(now); // calls drainRecencyQueue
				} finally {
					unlock();
				}
			}
		}

		void runUnlockedCleanup() {
			// REMOVE 通知。。。
			if (!isHeldByCurrentThread()) {
				// map.processPendingNotifications();
			}
		}

		/**
		 * 1.记录访问时间<br>
		 * 2.增加到访问对列的尾部
		 */
		void recordAccess(ReferenceEntry<K, V> e) {
			e.setAccessTime(now());
			accessQueue.add(e);
		}

		long now() {
			return System.nanoTime();
		}
	}

	/* ---------------- Public operations -------------- */

	public ConcurrentCache(CacheLoader<K, V> loader) {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, loader);
	}

	public ConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel, CacheLoader<K, V> loader) {
		if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0 || loader == null)
			throw new IllegalArgumentException();

		this.loader = loader;

		if (concurrencyLevel > MAX_SEGMENTS)
			concurrencyLevel = MAX_SEGMENTS;

		// Find power-of-two sizes best matching arguments
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel) {
			++sshift;
			ssize <<= 1;
		}
		segmentShift = 32 - sshift;
		segmentMask = ssize - 1;
		this.segments = Segment.newArray(ssize);

		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		int c = initialCapacity / ssize;
		if (c * ssize < initialCapacity)
			++c;
		int cap = 1;
		while (cap < c)
			cap <<= 1;

		for (int i = 0; i < this.segments.length; ++i)
			this.segments[i] = new Segment<K, V>(this, cap, loadFactor);
	}

	public boolean isEmpty() {
		final Segment<K, V>[] segments = this.segments;
		int[] mc = new int[segments.length];
		int mcsum = 0;
		for (int i = 0; i < segments.length; ++i) {
			if (segments[i].count != 0)
				return false;
			else
				mcsum += mc[i] = segments[i].modCount;
		}
		if (mcsum != 0) {
			for (int i = 0; i < segments.length; ++i) {
				if (segments[i].count != 0 || mc[i] != segments[i].modCount)
					return false;
			}
		}
		return true;
	}

	public int size() {
		final Segment<K, V>[] segments = this.segments;
		long sum = 0;
		long check = 0;
		int[] mc = new int[segments.length];
		// Try a few times to get accurate count. On failure due to
		// continuous async changes in table, resort to locking.
		for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
			check = 0;
			sum = 0;
			int mcsum = 0;
			for (int i = 0; i < segments.length; ++i) {
				sum += segments[i].count;
				mcsum += mc[i] = segments[i].modCount;
			}
			if (mcsum != 0) {
				for (int i = 0; i < segments.length; ++i) {
					check += segments[i].count;
					if (mc[i] != segments[i].modCount) {
						check = -1; // force retry
						break;
					}
				}
			}
			if (check == sum)
				break;
		}
		if (check != sum) { // Resort to locking all segments
			sum = 0;
			for (int i = 0; i < segments.length; ++i)
				segments[i].lock();
			for (int i = 0; i < segments.length; ++i)
				sum += segments[i].count;
			for (int i = 0; i < segments.length; ++i)
				segments[i].unlock();
		}
		if (sum > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		else
			return (int) sum;
	}

	public V get(K key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).get(key, hash, loader, true);
	}

	/**
	 * 存在即获取
	 * 
	 * @param key
	 * @return
	 */
	public V getIfPresent(K key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).get(key, hash, loader, false);
	}

	public boolean containsKey(K key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).containsKey(key, hash);
	}

	public V put(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, false);
	}

	/**
	 * 不存在时放入缓存
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public V putIfAbsent(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, true);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	public V remove(K key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).remove(key, hash, null);
	}

	public boolean remove(K key, V value) {
		int hash = hash(key.hashCode());
		if (value == null)
			return false;
		return segmentFor(hash).remove(key, hash, value) != null;
	}

	public boolean replace(K key, V oldValue, V newValue) {
		if (oldValue == null || newValue == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, oldValue, newValue);
	}

	public V replace(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, value);
	}

	public void clear() {
		for (int i = 0; i < segments.length; ++i)
			segments[i].clear();
	}

	/* ---------------- expiration ---------------- */

	boolean isExpired(ReferenceEntry<K, V> entry, long now) {
		if (now - entry.getAccessTime() > expireAfterAccessNanos) {
			return true;
		}
		return false;
	}

	/* ---------------- Queue -------------- */

	interface ReferenceEntry<K, V> {
		/*
		 * Used by entries that use access order. Access entries are maintained
		 * in a doubly-linked list. New entries are added at the tail of the
		 * list at write time; stale entries are expired from the head of the
		 * list.
		 * 
		 * 插入到尾部，过期从首部
		 */

		K getKey();

		V getValue();

		int getHash();

		ReferenceEntry<K, V> getNext();

		/**
		 * Returns the time that this entry was last accessed, in ns.
		 */
		long getAccessTime();

		/**
		 * Sets the entry access time in ns.
		 */
		void setAccessTime(long time);

		/**
		 * Returns the next entry in the access queue.
		 */
		ReferenceEntry<K, V> getNextInAccessQueue();

		/**
		 * Sets the next entry in the access queue.
		 */
		void setNextInAccessQueue(ReferenceEntry<K, V> next);

		/**
		 * Returns the previous entry in the access queue.
		 */
		ReferenceEntry<K, V> getPreviousInAccessQueue();

		/**
		 * Sets the previous entry in the access queue.
		 */
		void setPreviousInAccessQueue(ReferenceEntry<K, V> previous);

	}

	private enum NullEntry implements ReferenceEntry<Object, Object> {
		INSTANCE;

		@Override
		public Object getKey() {
			return null;
		}

		@Override
		public Object getValue() {
			return null;
		}

		@Override
		public ReferenceEntry<Object, Object> getNext() {
			return null;
		}

		@Override
		public int getHash() {
			return 0;
		};

		@Override
		public long getAccessTime() {
			return 0;
		}

		@Override
		public void setAccessTime(long time) {
		}

		@Override
		public ReferenceEntry<Object, Object> getNextInAccessQueue() {
			return this;
		}

		@Override
		public void setNextInAccessQueue(ReferenceEntry<Object, Object> next) {
		}

		@Override
		public ReferenceEntry<Object, Object> getPreviousInAccessQueue() {
			return this;
		}

		@Override
		public void setPreviousInAccessQueue(ReferenceEntry<Object, Object> previous) {
		}

	}

	@SuppressWarnings("unchecked")
	// impl never uses a parameter or returns any non-null value
	static <K, V> ReferenceEntry<K, V> nullEntry() {
		return (ReferenceEntry<K, V>) NullEntry.INSTANCE;
	}

	static final class AccessQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
		final ReferenceEntry<K, V> head = new ReferenceEntry<K, V>() {

			@Override
			public K getKey() {
				return null;
			}

			@Override
			public V getValue() {
				return null;
			}

			@Override
			public ReferenceEntry<K, V> getNext() {
				return nullEntry();
			}

			public int getHash() {
				return 0;
			};

			@Override
			public long getAccessTime() {
				return Long.MAX_VALUE;
			}

			@Override
			public void setAccessTime(long time) {
			}

			ReferenceEntry<K, V> nextAccess = this;

			@Override
			public ReferenceEntry<K, V> getNextInAccessQueue() {
				return nextAccess;
			}

			@Override
			public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
				this.nextAccess = next;
			}

			ReferenceEntry<K, V> previousAccess = this;

			@Override
			public ReferenceEntry<K, V> getPreviousInAccessQueue() {
				return previousAccess;
			}

			@Override
			public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
				this.previousAccess = previous;
			}
		};

		// implements Queue

		@Override
		public boolean offer(ReferenceEntry<K, V> entry) {
			// unlink
			connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());

			// add to tail
			connectAccessOrder(head.getPreviousInAccessQueue(), entry);
			connectAccessOrder(entry, head);

			return true;
		}

		@Override
		public ReferenceEntry<K, V> peek() {
			ReferenceEntry<K, V> next = head.getNextInAccessQueue();
			return (next == head) ? null : next;
		}

		@Override
		public ReferenceEntry<K, V> poll() {
			ReferenceEntry<K, V> next = head.getNextInAccessQueue();
			if (next == head) {
				return null;
			}

			remove(next);
			return next;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public boolean remove(Object o) {
			ReferenceEntry<K, V> e = (ReferenceEntry) o;
			ReferenceEntry<K, V> previous = e.getPreviousInAccessQueue();
			ReferenceEntry<K, V> next = e.getNextInAccessQueue();
			connectAccessOrder(previous, next);
			nullifyAccessOrder(e);

			return next != NullEntry.INSTANCE;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public boolean contains(Object o) {
			ReferenceEntry<K, V> e = (ReferenceEntry) o;
			return e.getNextInAccessQueue() != NullEntry.INSTANCE;
		}

		@Override
		public boolean isEmpty() {
			return head.getNextInAccessQueue() == head;
		}

		@Override
		public int size() {
			int size = 0;
			for (ReferenceEntry<K, V> e = head.getNextInAccessQueue(); e != head; e = e.getNextInAccessQueue()) {
				size++;
			}
			return size;
		}

		@Override
		public void clear() {
			ReferenceEntry<K, V> e = head.getNextInAccessQueue();
			while (e != head) {
				ReferenceEntry<K, V> next = e.getNextInAccessQueue();
				nullifyAccessOrder(e);
				e = next;
			}

			head.setNextInAccessQueue(head);
			head.setPreviousInAccessQueue(head);
		}

		@Override
		public Iterator<ReferenceEntry<K, V>> iterator() {
			return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
				@Override
				protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
					ReferenceEntry<K, V> next = previous.getNextInAccessQueue();
					return (next == head) ? null : next;
				}
			};
		}
	}

	static <K, V> void connectAccessOrder(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
		previous.setNextInAccessQueue(next);
		next.setPreviousInAccessQueue(previous);
	}

	static <K, V> void nullifyAccessOrder(ReferenceEntry<K, V> nulled) {
		ReferenceEntry<K, V> nullEntry = nullEntry();
		nulled.setNextInAccessQueue(nullEntry);
		nulled.setPreviousInAccessQueue(nullEntry);
	}
}
