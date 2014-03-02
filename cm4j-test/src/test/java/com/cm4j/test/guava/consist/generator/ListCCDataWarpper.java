package com.cm4j.test.guava.consist.generator;

import java.util.Map;

/**
 * Created by yanghao on 14-3-2.
 */
public class ListCCDataWarpper extends AbsCCDataWarpper {

    protected ListCCDataWarpper(Class pojo) {
        super(pojo);
        this.fileName = getPojo().getSimpleName() + "ListCache";
        this.ftlName = "list.ftl";
    }

    @Override
    protected void dataModel(Map data) {
        data.put("map_key", "Integer");
    }
}
