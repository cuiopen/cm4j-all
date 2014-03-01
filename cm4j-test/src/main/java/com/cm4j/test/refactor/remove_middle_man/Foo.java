package com.cm4j.test.refactor.remove_middle_man;

public class Foo {
    Bar bar;

    public Foo getImpValue() {
        return bar.getImpValue();
    }
}