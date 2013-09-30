package com.cm4j.test.designpattern.commander;

import org.junit.Test;

import com.cm4j.test.designpattern.commander.command.AddRequirementCommand;
import com.cm4j.test.designpattern.commander.command.ChangeCodeCommand;
import com.cm4j.test.designpattern.commander.command.Command;

/**
 * <pre>
 * 正确命令模式调用
 * 
 * 原理：把所有流程放入command里面，外层由invoker对象setAndExec(SetCommand And DoAction)
 * </pre>
 * 
 * @author yanghao
 *
 */
public class Clinet_Three {
    
    @Test
    public void stepThree (){
        Command cmd = new AddRequirementCommand();
        Invoker invoker = new Invoker();
        invoker.setCommand(cmd);
        invoker.doAction();
        
        Command cmd2 = new ChangeCodeCommand();
        invoker.setCommand(cmd2);
        invoker.doAction();
    }
}
