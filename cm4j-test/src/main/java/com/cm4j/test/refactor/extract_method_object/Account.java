package com.cm4j.test.refactor.extract_method_object;

class Account {

    int b(int val1, int c, int a) {
        int c1 = 2;
        //some computations
        c1 *= 2;
        a += 10;
        return 0;
    }
}