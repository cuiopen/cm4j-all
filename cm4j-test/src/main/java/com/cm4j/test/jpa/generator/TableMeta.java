package com.cm4j.test.jpa.generator;

import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * 表结构信息
 *
 * @author yanghao
 * @date 2013-6-6
 */
public class TableMeta {

    // 包路径
    private String packageDir = Consts.PACK_DIR;
    // ftl中暂时未用
    private String schemaName;
    private String tableName;
    private String comment;
    private List<ColumnMeta> columns;
    private int primaryKeySize = 0;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMeta> columns) {
        this.columns = columns;
    }

    public String getClassName() {
        if (tableName == null) return "";
        StringBuffer className = new StringBuffer();

        boolean need = false;
        if (StringUtils.isNotBlank(Consts.DB_TABLE) && Consts.DB_TABLE.equals(tableName)) {
            need = true;
        }
        if (StringUtils.isBlank(Consts.DB_TABLE) && StringUtils.startsWith(tableName, Consts.DB_TABLE_PREFIX)) {
            need = true;
        }

        if (need) {
            String[] names = StringUtils.split(tableName.toLowerCase(), "_");
            for (int i = 0, len = names.length; i < len; i++) {
                className.append(names[i].substring(0, 1).toUpperCase() + names[i].substring(1));
            }
        } else {
//            System.out.println("==不支持的表前缀:" + tableName + "==");
        }
        return className.toString();
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public int getPrimaryKeySize() {
        return primaryKeySize;
    }

    public void setPrimaryKeySize(int primaryKeySize) {
        this.primaryKeySize = primaryKeySize;
    }

    public String getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(String packageDir) {
        this.packageDir = packageDir;
    }
}