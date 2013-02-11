package com.cm4j.test.designpattern.visitor;

public abstract class Employee {

	private int salary;
	private String name;
	private int sex;

	public void report() {

	}

	public abstract void accept(IVisitor visitor);

	public int getSalary() {
		return salary;
	}

	public void setSalary(int salary) {
		this.salary = salary;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}
}
