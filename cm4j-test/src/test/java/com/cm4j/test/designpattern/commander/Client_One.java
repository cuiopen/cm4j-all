package com.cm4j.test.designpattern.commander;

import org.junit.Test;

import com.cm4j.test.designpattern.commander.group.CodeGroup;
import com.cm4j.test.designpattern.commander.group.RequirementGroup;

// 场景类 - 不可取
public class Client_One {

    @Test
    public void stepOne() {
        // 第一天
        RequirementGroup requirementGroup = new RequirementGroup();
        requirementGroup.find();
        requirementGroup.add();
        requirementGroup.plan();

        // 第二天
        CodeGroup codeGroup = new CodeGroup();
        codeGroup.find();
        codeGroup.change();
        codeGroup.plan();
        
        // 第三天？ 其他组？
        // ...... 无尽的维护、无尽的痛苦
    }
}
