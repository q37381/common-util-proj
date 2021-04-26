package com.common.model;

import com.common.util.enums.SQLFieldTypeEnum;

/**
 * sql字段定义 TODOM 后期需优化
 * 
 * @author Nbb
 *
 */
public class SqlFieldDefinition {

    private String name;

    /**
     * 主键索引 使用这个字段
     */
    private String field;

    private String fieldType;

    private SQLFieldTypeEnum type;

    private String nullFlag;

    private String comment;

    /**
     * 字段
     * 
     * @param name
     * @param type
     * @param fieldType
     * @param nullFlag
     * @param comment
     */
    public SqlFieldDefinition(String name, SQLFieldTypeEnum type, String fieldType, String nullFlag, String comment) {
        super();
        this.name = name;
        this.type = type;
        this.fieldType = fieldType;
        this.nullFlag = nullFlag;
        this.comment = comment;
    }

    /**
     * 表
     * 
     * @param name
     * @param type
     */
    public SqlFieldDefinition(String name, SQLFieldTypeEnum type) {
        super();
        this.name = name;
        this.type = type;
    }

    /**
     * 索引
     * 
     * @param name
     * @param field
     * @param type
     */
    public SqlFieldDefinition(String name, String field, SQLFieldTypeEnum type) {
        super();
        this.name = name;
        this.field = field;
        this.type = type;
    }

    public SqlFieldDefinition(SQLFieldTypeEnum type) {
        super();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public SQLFieldTypeEnum getType() {
        return type;
    }

    public void setType(SQLFieldTypeEnum type) {
        this.type = type;
    }

    public String getNullFlag() {
        return nullFlag;
    }

    public void setNullFlag(String nullFlag) {
        this.nullFlag = nullFlag;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "SqlFieldDefinition [name=" + name + ", field=" + field + ", fieldType=" + fieldType + ", type=" + type
                + ", nullFlag=" + nullFlag + ", comment=" + comment + "]";
    }
}
