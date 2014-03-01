package com.cm4j.test.refactor.remove_middle_man;

public class Client {
    Foo a;
    Foo impValue = a.getImpValue();
}