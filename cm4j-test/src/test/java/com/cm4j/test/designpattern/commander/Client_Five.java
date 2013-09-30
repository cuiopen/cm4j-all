package com.cm4j.test.designpattern.commander;

import com.cm4j.test.designpattern.commander.command.AddRequirementCommandExtend;
import com.cm4j.test.designpattern.commander.command.Command;

/**
 * redo undo
 * 
 * @author yanghao
 *
 */
public class Client_Five {

    public static void main(String[] args) {
        Invoker invoker = new Invoker();
        Command cmd = new AddRequirementCommandExtend();
        invoker.setCommand(cmd);
        invoker.doAction();
        System.out.println("================================");
        invoker.redo();
        System.out.println("================================");
        invoker.undo();
    }
}
