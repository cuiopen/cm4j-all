<#--
模板中编码特别容易因为if等模板命令而出现生成出来的空格或回车不复合规范
因此：尽量把命令贴近第一列编写，不要有tab，回车也要注意使用
-->
package ${packageDir};

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import net.bojoy.king.dao.core.IEntity;
<#-- 复合主键 -->
<#if primaryKeySize &gt; 1>
import javax.persistence.EmbeddedId;
</#if>
<#-- 唯一主键 -->
<#if primaryKeySize == 1>
import javax.persistence.Id;
</#if>

/**
* ${comment!""}
*
* @desc     代码生成器生成.
* @date     ${.now?string("yyyy-MM-dd")}
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "${tableName}")
public class ${className} implements IEntity {

<#-- 属性字段 -->
<#-- 复合主键 -->
<#if primaryKeySize &gt; 1>
<#assign ID = className + "Id">
    // ID
    private ${ID} id;
</#if>
<#list columns as column>
<#-- 非主键 或者 主键个数<=1 -->
    <#if column.columnKey != "PRI" || primaryKeySize <= 1>
    // ${column.columnComment}
    private ${column.propertyType} ${column.propertyName};
    </#if>
</#list>

<#-- getter and setter -->
<#-- 复合主键 -->
<#if primaryKeySize &gt; 1>
    @EmbeddedId
    public ${ID} getId(){
        return this.id;
    }

    public void setId(${ID} id) {
        this.id = id;
    }

</#if>
<#list columns as column>
<#if column.columnKey == "PRI" && primaryKeySize == 1>
    @Id
</#if>
<#-- 非主键或者唯一主键 -->
<#if column.columnKey != "PRI" || primaryKeySize == 1>
<#if column.propertyType == "java.util.Date">@javax.persistence.Temporal(javax.persistence.TemporalType
    <#if column.columnType == "DATE" || column.columnType == "date">.DATE<#else>.TIMESTAMP</#if>)
</#if>
    @Column(name = "${column.columnName}"<#--<#if column.columnSize != 0>, length = ${column.columnSize}</#if><#if column.isNullable == "NO">, nullable = false</#if><#if column.columnKey == "UNI">, unique = true</#if>-->)
    public ${column.propertyType} get${column.propertyName?cap_first} (){
        return this.${column.propertyName} ;
    }

    public void set${column.propertyName?cap_first} (${column.propertyType} ${column.propertyName}){
        this.${column.propertyName} = ${column.propertyName} ;
    }

</#if>
</#list>
}