package com.common.handler.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.common.constant.Constants;
import com.common.constant.SomeEnums.DASErrorCode;
import com.common.exce.DASRuntimeException;
import com.common.handler.SQLFieldDefinitionHandler;
import com.common.model.SqlFieldDefinition;
import com.common.util.AssertUtils;
import com.common.util.enums.MySQLPartTypeEnum;

public class CreateSQLHandler implements SQLFieldDefinitionHandler {

    /**
     * TODOM 暂定必有表名，必有字段 索引可有
     * 
     * @param definitionList
     * @return
     */
    @Override
    public String doHandler(List<SqlFieldDefinition> definitionList) {

        StringBuilder sb = new StringBuilder();

        SqlFieldDefinition tableDefinition = findTableDefinition(definitionList);

        String primaryField = findPrimariKeyField(definitionList);

        sb.append(generateTableLine(tableDefinition));

        sb.append(generateFieldLines(definitionList, primaryField));

        String indexLines = generateIndexLines(definitionList);
        if (StringUtils.isNotBlank(indexLines)) {
            sb.append(indexLines);
        }
        sb.deleteCharAt(sb.lastIndexOf(","));

        sb.append(")").append(Constants.LINE_SEPARATOR);
        if (StringUtils.isNotBlank(tableDefinition.getComment())) {
            sb.append("COMMENT='").append(tableDefinition.getComment()).append("'").append(Constants.LINE_SEPARATOR);
        }

        return sb.toString();
    }

    private SqlFieldDefinition findTableDefinition(List<SqlFieldDefinition> definitionList) {
        for (SqlFieldDefinition definition : definitionList) {
            if (definition.getSqlPart() == MySQLPartTypeEnum.TABLE) {
                return definition;
            }
        }

        // TODOM 错误相关都需要调整
        throw new DASRuntimeException(DASErrorCode.COMMON_ERROR);
    }

    private String generateIndexLines(List<SqlFieldDefinition> definitionList) {
        StringBuilder sb = new StringBuilder();
        for (SqlFieldDefinition definition : definitionList) {
            MySQLPartTypeEnum type = definition.getSqlPart();
            if (type == MySQLPartTypeEnum.PRIMARY_KEY) {
                sb.append(type.getRealName()).append(Constants.SPACE).append("(");
                for (String indexField : definition.getIndexFieldList()) {
                    sb.append("`").append(indexField).append("`,");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("),").append(Constants.LINE_SEPARATOR);
            } else if (type == MySQLPartTypeEnum.UNIQUE_INDEX || type == MySQLPartTypeEnum.INDEX) {
                sb.append(type.getRealName()).append(Constants.SPACE).append("`").append(definition.getName())
                        .append("`").append(Constants.SPACE).append("(");
                for (String indexField : definition.getIndexFieldList()) {
                    sb.append("`").append(indexField).append("`,");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("),").append(Constants.LINE_SEPARATOR);
            }
        }

        return sb.toString();
    }

    private String generateFieldLines(List<SqlFieldDefinition> definitionList, String primaryField) {
        StringBuilder sb = new StringBuilder();
        for (SqlFieldDefinition definition : definitionList) {
            // TODOM 先一起处理了
            if (definition.getSqlPart() == MySQLPartTypeEnum.UNSIGNED_FIELD
                    || definition.getSqlPart() == MySQLPartTypeEnum.SIGNED_FIELD) {
                sb.append("`").append(definition.getName()).append("`");
                sb.append(Constants.SPACE);
                if (definition.getSqlPart() == MySQLPartTypeEnum.UNSIGNED_FIELD) {
                    sb.append(
                            // 替换unsigned-int 为 int
                            definition.getFieldType()
                                    .replaceAll("[^a-z]?\\w*" + MySQLPartTypeEnum.UNSIGNED_FIELD.getSearchName()
                                            + "\\w*[^a-z]?", Constants.SPACE)
                                    .trim())
                            .append(Constants.SPACE).append(MySQLPartTypeEnum.UNSIGNED_FIELD.getRealName())
                            .append(Constants.SPACE);
                } else {
                    sb.append(definition.getFieldType()).append(Constants.SPACE);
                }

                if (definition.isRequired()) {
                    sb.append("NOT NULL");
                } else {
                    sb.append("NULL");
                }

                // 主键 默认自增 假设
                if (definition.getName().equals(primaryField)) {
                    sb.append(" AUTO_INCREMENT");
                }

                if (StringUtils.isNoneBlank(definition.getComment())) {
                    sb.append(" COMMENT '").append(definition.getComment()).append("',")
                            .append(Constants.LINE_SEPARATOR);
                } else {
                    sb.append(",").append(Constants.LINE_SEPARATOR);
                }

            }
        }

        AssertUtils.notEmpty(sb.toString(), "fields");

        return sb.toString();
    }

    private String generateTableLine(SqlFieldDefinition tableDefinition) {
        return new StringBuilder().append(tableDefinition.getSqlPart().getRealName()).append(Constants.SPACE)
                .append(tableDefinition.getName()).append(Constants.SPACE).append("(").append(Constants.LINE_SEPARATOR)
                .toString();
    }

    private String findPrimariKeyField(List<SqlFieldDefinition> definitionList) {
        for (SqlFieldDefinition definition : definitionList) {
            if (definition.getSqlPart() == MySQLPartTypeEnum.PRIMARY_KEY) {
                return definition.getIndexFieldList().get(0);
            }
        }
        return null;
    }
}
