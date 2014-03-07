package com.cm4j.test.jpa.generator;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * 字段结构
 *
 * @author yanghao
 * @date 2013-6-6
 */
public class ColumnMeta {

    private String columnName;
    private String ordinalPosition;
    private String isNullable;
    private String columnDefault;
    // 数据类型
    private String columnType;
    // 主键PRI or 唯一键UNI
    private String columnKey;
    private String extra;
    private String columnComment;
    // 字段长度，将在计算属性类型时赋值
    private int columnSize;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(String ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    public String getIsNullable() {
        return isNullable;
    }

    public void setIsNullable(String isNullable) {
        this.isNullable = isNullable;
    }

    public String getColumnDefault() {
        return columnDefault;
    }

    public void setColumnDefault(String columnDefault) {
        this.columnDefault = columnDefault;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getColumnComment() {
        if (StringUtils.isBlank(columnComment)) {
            return columnComment;
        }
        return StringUtils.replace(columnComment, "\n","\n//");
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    /**
     * 获取属性名称
     *
     * @return
     */
    public String getPropertyName() {
        StringBuffer propertyName = new StringBuffer();
        String[] names = StringUtils.split(columnName.toLowerCase(), "_");
        // level，则直接返回
        if (names.length == 1) {
            return columnName.toLowerCase();
        }
        // n_level 默认忽略 n_
        propertyName.append(names[1]);
        for (int i = 2, len = names.length; i < len; i++) {
            propertyName.append(names[i].substring(0, 1).toUpperCase() + names[i].substring(1));
        }
        return propertyName.toString();
    }

    /**
     * 获取属性类型
     *
     * @return
     */
    public String getPropertyType() {
        String type = columnType.toLowerCase();
        // 默认未找到
        String propertyType = "NOT_FOUND_PROPERTY_TYPE";
        if (StringUtils.startsWith(type, "int")) {                // int/long
            columnSize = NumberUtils.toInt(StringUtils.substring(type, StringUtils.indexOf(type, "(") + 1, StringUtils.indexOf(type, ")")));
            if (columnSize <= 4) {
                propertyType = isNullable.equals("NO") ? "byte" : "Byte";
            } else {
                propertyType = isNullable.equals("NO") ? "int" : "Integer";
            }
        } else if (StringUtils.startsWith(type, "bigint")) {        // long
            propertyType = isNullable.equals("NO") ? "long" : "Long";
        } else if (StringUtils.startsWith(type, "tinyint")) {        // long
            propertyType = isNullable.equals("NO") ? "byte" : "Byte";
        } else if (StringUtils.startsWith(type, "double")) {        // double
            propertyType = isNullable.equals("NO") ? "double" : "Double";
        } else if (StringUtils.startsWith(type, "float")) {        // float
            propertyType = isNullable.equals("NO") ? "float" : "Float";
        } else if (StringUtils.startsWith(type, "varchar")) {    // String
            columnSize = NumberUtils.toInt(StringUtils.substring(type, StringUtils.indexOf(type, "(") + 1, StringUtils.indexOf(type, ")")));
            propertyType = "String";
        } else if (StringUtils.startsWith(type, "char")) {        // String
            columnSize = NumberUtils.toInt(StringUtils.substring(type, StringUtils.indexOf(type, "(") + 1, StringUtils.indexOf(type, ")")));
            propertyType = "String";
        } else if (StringUtils.startsWith(type, "text")) {        // String
            propertyType = "String";
        } else if (StringUtils.startsWith(type, "date")) {        // date
            propertyType = "java.util.Date";
        } else if (StringUtils.startsWith(type, "datetime")) {    // date
            propertyType = "java.util.Date";
        } else if (StringUtils.startsWith(type, "timestamp")) {    // date
            propertyType = "java.util.Date";
        } else {
            System.out.println("==类型[" + type + "]解析尚不支持==");
        }
        return propertyType;
    }

    public int getColumnSize() {
        return columnSize;
    }

}