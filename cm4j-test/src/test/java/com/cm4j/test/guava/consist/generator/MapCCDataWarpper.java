package com.cm4j.test.guava.consist.generator;

import java.util.Map;

/**
 * Created by yanghao on 14-3-2.
 */
public class MapCCDataWarpper extends AbsCCDataWarpper {

    protected MapCCDataWarpper(Class pojo, Map params) {
        super(pojo, params);
        setFileName(getPojo().getSimpleName() + "MapCache");
        setFtlName("map.ftl");
    }

    @Override
    protected void dataModel(Map data) {
        data.put("map_key", "Integer");
    }
}
