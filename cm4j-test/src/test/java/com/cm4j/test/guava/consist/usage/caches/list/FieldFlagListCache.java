package com.cm4j.test.guava.consist.usage.caches.list;

import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.consist.usage.caches.ref.FieldFlagListReference;

public class FieldFlagListCache extends CacheDescriptor<FieldFlagListReference> {

	public FieldFlagListCache(Object... params) {
		super(params);
	}

	@Override
	public FieldFlagListReference load(String... params) {
		return null;
	}
}
