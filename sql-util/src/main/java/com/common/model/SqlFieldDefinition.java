package com.common.model;

import java.util.List;

import com.common.util.enums.MySQLPartTypeEnum;
import com.google.common.collect.Lists;

/**
 * sql字段定义
 * 
 * @author Nbb
 *
 */
public class SqlFieldDefinition implements Comparable<SqlFieldDefinition> {

    /**
     * 表名，索引名，字段名
     */
    private String name;

    /**
     * 索引使用字段
     */
    private List<String> indexFieldList;

    private String fieldType;

    private MySQLPartTypeEnum sqlPart;

    private boolean required;

    private String comment;

    /**
     * 字段
     * 
     * @param name
     * @param sqlPart
     * @param fieldType
     * @param required
     * @param comment
     */
    public SqlFieldDefinition(String name, MySQLPartTypeEnum sqlPart, String fieldType, boolean required,
            String comment) {
        super();
        this.name = name;
        this.sqlPart = sqlPart;
        this.fieldType = fieldType;
        this.required = required;
        this.comment = comment;
    }

    /**
     * 表
     * 
     * @param name
     * @param sqlPart
     */
    public SqlFieldDefinition(String name, MySQLPartTypeEnum sqlPart) {
        super();
        this.name = name;
        this.sqlPart = sqlPart;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public MySQLPartTypeEnum getSqlPart() {
        return sqlPart;
    }

    public void setSqlPart(MySQLPartTypeEnum sqlPart) {
        this.sqlPart = sqlPart;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getIndexFieldList() {
        return indexFieldList;
    }

    public void setIndexFieldList(List<String> indexFieldList) {
        this.indexFieldList = indexFieldList;
    }

    public void addIndexField(String indexField) {
        if (indexFieldList == null) {
            indexFieldList = Lists.newArrayList();
        }
        indexFieldList.add(indexField);
    }

    @Override
    public int compareTo(SqlFieldDefinition o) {
        return Integer.compare(this.sqlPart.getSortNo(), o.getSqlPart().getSortNo());
    }

}
