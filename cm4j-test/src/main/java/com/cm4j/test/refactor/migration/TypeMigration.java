package com.cm4j.test.refactor.migration;

import java.util.ArrayList;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-11-2 下午3:16
 */
public class TypeMigration {
    String f;
    Integer b;
    private ArrayList<String> myResult;

    public ArrayList<String> getMyResult() {
        return myResult;
    }

    public void setMyResult(ArrayList<String> myResult) {
        this.myResult = myResult;
    }

    void bar(String i) {
    }

    void foo() {
        bar(f);
    }

    public static void main(String[] args) {
        TypeMigration migration = new TypeMigration();
        migration.setMyResult(new ArrayList<String>());
    }
}
