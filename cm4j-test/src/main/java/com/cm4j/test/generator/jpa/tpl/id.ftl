package ${packageDir};

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * ID类
 * ${comment!""}
 *
 * @desc     代码生成器生成.
 * @date     ${.now?string("yyyy-MM-dd")}
 */
@SuppressWarnings("serial")
@Embeddable
public class ${className}Id implements java.io.Serializable {

<#-- 属性字段 -->
<#list columns as column>
<#-- 复合主键 -->
<#if column.columnKey == "PRI">
    // ${column.columnComment}
    private ${column.propertyType} ${column.propertyName};
</#if>
</#list>

<#-- getter and setter -->
<#list columns as column>
<#if column.columnKey = "PRI">
<#if column.propertyType == "java.util.Date">
    @javax.persistence.Temporal(javax.persistence.TemporalType<#if column.columnType == "DATE" || column.columnType == "date">.DATE<#else>.TIMESTAMP</#if>)
</#if>
    @Column(name = "${column.columnName}"<#--<#if column.columnSize != 0>, length = ${column.columnSize}</#if><#if column.isNullable == "NO">, nullable = false</#if><#if column.columnKey == "UNI">, unique = true</#if>-->)
    public ${column.propertyType} get${column.propertyName?cap_first}(){
        return this.${column.propertyName};
    }

    public void set${column.propertyName?cap_first}(${column.propertyType} ${column.propertyName}){
        this.${column.propertyName} = ${column.propertyName};
    }

</#if>
</#list>
}