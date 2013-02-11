package com.cm4j.test.designpattern.visitor;

import java.util.ArrayList;
import java.util.List;

public class Client {

	public static void main(String[] args) {
		List<Employee> empList = new ArrayList<Employee>();
		CommonEmployee zhangsan = new CommonEmployee();
		zhangsan.setJob("张三工作");
		zhangsan.setName("张三");
		zhangsan.setSalary(1800);
		zhangsan.setSex(1);

		empList.add(zhangsan);

		CommonEmployee lisi = new CommonEmployee();
		lisi.setJob("李四工作");
		lisi.setName("李四");
		lisi.setSalary(2000);
		lisi.setSex(0);
		empList.add(lisi);

		Manager wangwu = new Manager();
		wangwu.setName("王五");
		wangwu.setPerformance("王五工作职责");
		wangwu.setSalary(8000);
		wangwu.setSex(0);
		empList.add(lisi);

		for (Employee employee : empList) {
			employee.accept(new Visitor());
		}
	}
}
