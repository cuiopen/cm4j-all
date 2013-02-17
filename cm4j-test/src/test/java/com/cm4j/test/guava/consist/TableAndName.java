package com.cm4j.test.guava.consist;

import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.value.SingleValue;

public class TableAndName extends SingleValue {

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
	protected IEntity parseEntity() {
		return new TestTable(id, value);
	}
}
