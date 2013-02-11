package com.cm4j.test.designpattern.visitor;

public class Visitor implements IVisitor {

	@Override
	public void visit(CommonEmployee commonEmployee) {
		System.out.println(commonEmployee.getName()+ "-" +commonEmployee.getJob());
	}

	@Override
	public void visit(Manager manager) {
		System.out.println(manager.getName() + "-" + manager.getPerformance());
	}

}
