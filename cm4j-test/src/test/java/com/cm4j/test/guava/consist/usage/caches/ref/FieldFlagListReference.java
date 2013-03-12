package com.cm4j.test.guava.consist.usage.caches.ref;

import java.util.ArrayList;
import java.util.List;

import com.cm4j.test.guava.consist.AbsReference;
import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.DBState;
import com.cm4j.test.guava.consist.entity.FieldFlagSituation;
import com.cm4j.test.guava.consist.usage.caches.single.FieldFlagSingleCache;
import com.google.common.base.Preconditions;

public class FieldFlagListReference extends AbsReference {

	private List<FieldFlagSituation> all_objects = new ArrayList<FieldFlagSituation>();

	@SuppressWarnings("unchecked")
	@Override
	public List<FieldFlagSituation> get() {
		return all_objects;
	}

	public void delete(FieldFlagSituation situation) {
		Preconditions.checkState(!get().contains(situation), "ListValue中不包含此对象，无法删除");
		// 注意顺序，先remove再change
		new FieldFlagSingleCache(situation.getNPlayerId()).reference().delete();
		all_objects.remove(situation);
	}

	public void update(FieldFlagSituation situation) {
		if (!get().contains(situation)) {
			situation.setAttachedKey(getAttachedKey());
			all_objects.add(situation);
		}
		new FieldFlagSingleCache(situation.getNPlayerId()).reference().update(situation);
	}

	@Override
	protected boolean isAllPersist() {
		for (FieldFlagSituation e : all_objects) {
			if (DBState.P != e.getDbState()) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void persistDB() {
		for (FieldFlagSituation situation : all_objects) {
			// 死锁？
			new FieldFlagSingleCache(situation.getNPlayerId()).reference().persist();
		}
	}

	@Override
	protected boolean changeDbState(CacheEntry entry, DBState dbState) {
		throw new RuntimeException("不应该调用到此方法");
	}

	@Override
	protected void attachedKey(String attachedKey) {
		// do nothing
	}
}
