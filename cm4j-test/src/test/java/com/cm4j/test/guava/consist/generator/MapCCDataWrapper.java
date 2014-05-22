package com.cm4j.test.guava.consist.generator;

import java.util.Map;

/**
 * Created by yanghao on 14-3-2.
 */
public class MapCCDataWrapper extends AbsCCDataWrapper {

    protected MapCCDataWrapper(Class pojo, Map params, String query) {
        super(pojo, params, query);
        setFileName(getPojo().getSimpleName() + "MapCache");
        setFtlName("map.ftl");
    }
}
