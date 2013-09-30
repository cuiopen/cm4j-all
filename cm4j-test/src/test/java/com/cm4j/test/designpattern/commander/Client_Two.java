package com.cm4j.test.designpattern.commander;

import org.junit.Test;

import com.cm4j.test.designpattern.commander.command.AddRequirementCommand;
import com.cm4j.test.designpattern.commander.command.ChangeCodeCommand;
import com.cm4j.test.designpattern.commander.command.Command;

public class Client_Two {
    
    @Test
    public void stepTwo() {
        Command cmd = new AddRequirementCommand();
        cmd.execute();
        
        Command cmd2 = new ChangeCodeCommand();
        cmd2.execute();
    }
}
