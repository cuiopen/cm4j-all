package com.cm4j.test.guava.consist;

import java.util.HashMap;
import java.util.Map;

public class MapReference<K, V extends CacheEntry> extends AbsReference {

	private Map<K, V> map = new HashMap<K, V>();

	@Override
	public Map<K, V> get() {
		return map;
	}

	@Override
	public boolean isAllPersist() {
		return false;
	}

}
