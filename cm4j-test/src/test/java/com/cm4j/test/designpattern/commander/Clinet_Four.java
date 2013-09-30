package com.cm4j.test.designpattern.commander;

import org.junit.Test;

import com.cm4j.test.designpattern.commander.command.AddRequirementCommandExtend;
import com.cm4j.test.designpattern.commander.command.Command;

/**
 * 命令模式扩展 - 多任务联调
 * 
 * @author yanghao
 *
 */
public class Clinet_Four {
    
    @Test
    public void stepThree (){
        Invoker invoker = new Invoker();
        Command cmd = new AddRequirementCommandExtend();
        invoker.setCommand(cmd);
        invoker.doAction();
    }
}
