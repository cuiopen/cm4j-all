package com.cm4j.test.guava.consist.generator;

import java.util.Map;

/**
 * Created by yanghao on 14-3-2.
 */
public class ListCCDataWarpper extends AbsCCDataWarpper {

    protected ListCCDataWarpper(Class pojo, Map params) {
        super(pojo, params);
        setFileName(getPojo().getSimpleName() + "ListCache");
        setFtlName("list.ftl");
    }

    @Override
    protected void dataModel(Map data) {
    }
}
