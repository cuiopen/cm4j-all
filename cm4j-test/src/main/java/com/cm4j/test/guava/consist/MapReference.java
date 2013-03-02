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

	@Override
	public void persistDB() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean changeDbState(CacheEntry entry, DBState dbState) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void attachedKey(String attachedKey) {
		// TODO Auto-generated method stub
		
	}

}
