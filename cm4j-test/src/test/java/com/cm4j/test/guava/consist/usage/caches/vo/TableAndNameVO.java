package com.cm4j.test.guava.consist.usage.caches.vo;

import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.entity.TestTable;

public class TableAndNameVO extends CacheEntry {

	private int id;
	private long value;
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public IEntity parseEntity() {
		return new TestTable(id, value);
	}
}
