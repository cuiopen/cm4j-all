package com.cm4j.test.guava.consist.generator;

import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成数据封转
 * <p/>
 * Created by yanghao on 14-3-1.
 */
public abstract class AbsCCDataWarpper {

    private Class pojo;
    protected String fileName;
    protected String ftlName;

    protected AbsCCDataWarpper(Class pojo) {
        this.pojo = pojo;
    }

    public Map dataModel() {
        Map data = new HashMap();
        data.put("package", StringUtils.replaceChars(CCGenerator.packagePath, "/", "."));
        data.put("author", "yanghao");
        data.put("data", new Date());

        data.put("file_name", fileName);
        data.put("pojo", pojo.getSimpleName());

        dataModel(data);
        return data;
    }

    protected abstract void dataModel(Map data);

    public Class getPojo() {
        return pojo;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFtlName() {
        return ftlName;
    }
}
