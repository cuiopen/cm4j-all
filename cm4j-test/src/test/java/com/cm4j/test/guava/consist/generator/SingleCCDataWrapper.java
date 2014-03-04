package com.cm4j.test.guava.consist.generator;

import java.util.Map;

/**
 * Created by yanghao on 14-3-2.
 */
public class SingleCCDataWrapper extends AbsCCDataWarpper {

    protected SingleCCDataWrapper(Class pojo, Map params, String query) {
        super(pojo, params, query);
        setFileName(getPojo().getSimpleName() + "SingleCache");
        setFtlName("single.ftl");
    }
}
