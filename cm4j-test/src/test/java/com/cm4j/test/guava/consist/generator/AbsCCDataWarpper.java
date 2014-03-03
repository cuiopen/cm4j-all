package com.cm4j.test.guava.consist.generator;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成数据封转
 * <p/>
 * Created by yanghao on 14-3-1.
 */
public abstract class AbsCCDataWarpper {

    private final Class pojo;
    private Class pojoId;
    private String fileName;
    private String ftlName;
    private Map params;

    protected AbsCCDataWarpper(Class pojo, Map params) {
        this.pojo = pojo;
        this.params = params;

        // 查找主键类
        Field[] pojoFields = pojo.getDeclaredFields();
        for (Field pojoField : pojoFields) {
            if (pojoField.getAnnotation(EmbeddedId.class) != null || pojoField.getAnnotation(Id.class) != null) {
                this.pojoId = pojoField.getClass();
            }
        }
        if (this.pojoId == null) {
            Method[] pojoMethods = pojo.getDeclaredMethods();
            for (Method pojoMethod : pojoMethods) {
                if (pojoMethod.isAnnotationPresent(EmbeddedId.class) || pojoMethod.isAnnotationPresent(Id.class)) {
                    this.pojoId = pojoMethod.getReturnType();
                }
            }
        }
    }

    public Map dataModel() {
        Map data = new HashMap();
        data.put("package", StringUtils.replaceChars(CCGenerator.packagePath, "/", "."));
        data.put("author", "yanghao");
        data.put("data", new Date());

        data.put("file_name", fileName);
        data.put("pojo", pojo.getSimpleName());

        // 构造函数参数
        data.put("constructor_params", Joiner.on(",").withKeyValueSeparator(" ").join(params));
        data.put("constructor_values", Joiner.on(",").join(params.values()));

        // hibernate的映射主键
        data.put("hibernate_key", pojoId.getSimpleName());

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

    public Class getPojoId() {
        return pojoId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFtlName(String ftlName) {
        this.ftlName = ftlName;
    }

    public Map getParams() {
        return params;
    }
}
