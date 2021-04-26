package com.common.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.common.constant.SomeEnums.DASErrorCode;
import com.common.exce.DASRuntimeException;
import com.common.model.SqlFieldDefinition;
import com.common.util.AssertUtils;
import com.common.util.MeiDocSqlUtil;
import com.common.util.enums.SQLFieldTypeEnum;

public class SqlGenerateService {

    private static final String SPACE = " ";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    // TODOM 紧急 后期修改
    public String generateSqlByWord(String word) {
        List<SqlFieldDefinition> list = MeiDocSqlUtil.splitWord(word);
        return generateCreateSql(list);
    }

    /**
     * TODOM 暂定必有表名，必有字段 索引可有
     * 
     * @param definitionList
     * @return
     */
    private String generateCreateSql(List<SqlFieldDefinition> definitionList) {

        StringBuilder sb = new StringBuilder();

        SqlFieldDefinition tableDefinition = findTableDefinition(definitionList);

        String primaryField = findPrimariKeyField(definitionList);

        sb.append(generateTableLine(tableDefinition));

        sb.append(generateFieldLines(definitionList, primaryField));

        String indexLines = generateIndexLines(definitionList);
        if (StringUtils.isNotBlank(indexLines)) {
            sb.append(indexLines);
        }

        sb.append(")").append(LINE_SEPARATOR);
        if (StringUtils.isNotBlank(tableDefinition.getComment())) {
            sb.append("COMMENT='").append(tableDefinition.getComment()).append("'").append(LINE_SEPARATOR);
        }

        return sb.toString();
    }

    private SqlFieldDefinition findTableDefinition(List<SqlFieldDefinition> definitionList) {
        for (SqlFieldDefinition definition : definitionList) {
            if (definition.getType() == SQLFieldTypeEnum.TABLE) {
                return definition;
            }
        }

        // TODOM 错误相关都需要调整
        throw new DASRuntimeException(DASErrorCode.COMMON_ERROR);
    }

    private String generateIndexLines(List<SqlFieldDefinition> definitionList) {
        StringBuilder sb = new StringBuilder();
        for (SqlFieldDefinition definition : definitionList) {
            SQLFieldTypeEnum type = definition.getType();
            if (type == SQLFieldTypeEnum.PRIMARY_KEY) {
                sb.append(type.getRealName()).append(SPACE).append("(`").append(definition.getField()).append("`),")
                        .append(LINE_SEPARATOR);
            } else if (type == SQLFieldTypeEnum.UNIQUE_INDEX || type == SQLFieldTypeEnum.INDEX) {
                sb.append(type.getRealName()).append(SPACE).append("`").append(definition.getName()).append("`")
                        .append(SPACE).append("(`").append(definition.getField()).append("`),").append(LINE_SEPARATOR);
            }
        }

        return sb.toString();
    }

    private String generateFieldLines(List<SqlFieldDefinition> definitionList, String primaryField) {
        StringBuilder sb = new StringBuilder();
        for (SqlFieldDefinition definition : definitionList) {
            // TODOM 先一起处理了
            if (definition.getType() == SQLFieldTypeEnum.UNSIGNED_FIELD
                    || definition.getType() == SQLFieldTypeEnum.SIGNED_FIELD) {
                sb.append("`").append(definition.getName()).append("`");
                sb.append(SPACE);
                if (definition.getType() == SQLFieldTypeEnum.UNSIGNED_FIELD) {
                    sb.append(definition.getFieldType()
                            .replaceAll("[^a-z]?\\w*" + SQLFieldTypeEnum.UNSIGNED_FIELD.getSearchName() + "\\w*[^a-z]?",
                                    SPACE)
                            .trim()).append(SPACE).append(SQLFieldTypeEnum.UNSIGNED_FIELD.getRealName()).append(SPACE);
                } else {
                    sb.append(definition.getFieldType()).append(SPACE);
                }

                // 假设，不为空判断
                if (definition.getNullFlag().indexOf("y") != -1 || definition.getNullFlag().indexOf("不") != -1
                        || definition.getNullFlag().indexOf("非") != -1 || definition.getNullFlag().indexOf("r") != -1) {
                    sb.append("NOT NULL");
                } else {
                    sb.append("NULL");
                }

                // 主键 默认自增 假设
                if (definition.getName().equals(primaryField)) {
                    sb.append(" AUTO_INCREMENT");
                }

                if (StringUtils.isNoneBlank(definition.getComment())) {
                    sb.append(" COMMENT '").append(definition.getComment()).append("',").append(LINE_SEPARATOR);
                } else {
                    sb.append(",").append(LINE_SEPARATOR);
                }

            }
        }

        AssertUtils.notEmpty(sb.toString(), "fields");

        return sb.toString();
    }

    private String generateTableLine(SqlFieldDefinition tableDefinition) {
        return new StringBuilder().append(tableDefinition.getType().getRealName()).append(SPACE)
                .append(tableDefinition.getName()).append(SPACE).append("(").append(LINE_SEPARATOR).toString();
    }

    private String findPrimariKeyField(List<SqlFieldDefinition> definitionList) {
        for (SqlFieldDefinition definition : definitionList) {
            if (definition.getType() == SQLFieldTypeEnum.PRIMARY_KEY) {
                return definition.getField();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        "unsigned-int".replaceAll("[^a-z]?\\w*" + SQLFieldTypeEnum.UNSIGNED_FIELD.getSearchName() + "\\w*[^a-z]?",
                SPACE);
    }
}
