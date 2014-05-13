package com.cm4j.test.generator.dict;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by yanghao on 14-3-21.
 */
public class AnalysisEntity {
    private String clsName;
    private String entityName;
    private String idType;
    private String idGetterName;
    private String comment;

    public static AnalysisEntity analysis(Class cls) {
        AnalysisEntity result = new AnalysisEntity();

        String clsName = cls.getSimpleName();
        clsName = StringUtils.replace(clsName, "Dict", "");
        clsName += "DictCache";

        result.setClsName(clsName);
        result.setEntityName(cls.getSimpleName());

        // 查找主键
        Field[] declaredFields = cls.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                result.setIdType(transferPrimitive(field.getType()));
                result.setIdGetterName("get" + upperCaseFirst(field.getName()));
                break;
            }
        }

        if (StringUtils.isBlank(result.getIdType())) {
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.isAnnotationPresent(Id.class) || method.isAnnotationPresent(EmbeddedId.class)) {
                    result.setIdType(transferPrimitive(method.getReturnType()));
                    result.setIdGetterName(method.getName());
                    break;
                }
            }
        }

        Preconditions.checkArgument(StringUtils.isNotBlank(result.getIdType()), "未找到主键");
        return result;
    }

    private static String transferPrimitive(Class<?> type) {
        String simpleName = type.getSimpleName();
        if (type.isPrimitive()) {
            if (simpleName.equals("byte")) {
                simpleName = "Byte";
            } else if (simpleName.equals("short")) {
                simpleName = "Short";
            } else if (simpleName.equals("int")) {
                simpleName = "Integer";
            } else if (simpleName.equals("long")) {
                simpleName = "Long";
            }
        }
        return simpleName;
    }

    private static String upperCaseFirst(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return StringUtils.upperCase(str.substring(0, 1)) + str.substring(1);
    }

    public String getClsName() {
        return clsName;
    }

    public void setClsName(String clsName) {
        this.clsName = clsName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdGetterName() {
        return idGetterName;
    }

    public void setIdGetterName(String idGetterName) {
        this.idGetterName = idGetterName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
