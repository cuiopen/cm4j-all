package com.cm4j.test.designpattern.visitor;

public interface IVisitor {

	public void visit(CommonEmployee commonEmployee);

	public void visit(Manager manager);
}
