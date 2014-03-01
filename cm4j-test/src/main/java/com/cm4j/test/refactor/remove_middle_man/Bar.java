package com.cm4j.test.refactor.remove_middle_man;

public class Bar {
    private Foo impValue1;

    public Bar(Foo impValue) {
        impValue1 = impValue;
    }

    public Foo getImpValue() {
        return impValue1;
    }
}