package com.common.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.common.model.SqlFieldDefinition;
import com.common.service.SQLFieldDefinitionProducer;
import com.common.util.MathUtils;
import com.common.util.enums.MySQLPartTypeEnum;
import com.common.util.enums.SQLFieldTypeEnum;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MeiProducer implements SQLFieldDefinitionProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeiProducer.class);

    // 后期这些特殊分割或描述字符可能根据场景变更，可以做成配置
    private static final String DATABASE_PREFIX = "db_";

    private static final String TABLE_PREFIX = "t_";

    private static final Pattern PRIMARY_PATTERN = Pattern.compile(".+?[\\(|\\`](.+)");

    private static final Pattern INDEX_PATTERN = Pattern.compile("(?:[\\w ]+)\\W+(\\w+)\\W+(.+)");

    private static final Pattern CAHRACTOR_PATTERN = Pattern.compile("(\\w+)");

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z]+\\w+$");

    private static final String[] SQL_FIELD_TYPE = "int varchar( datetime text ".split(" ");

    private static final String[] NOT_NULL_FLAG_STR = "y,r,非,不".split(",");

    // =================

    @Override
    public List<SqlFieldDefinition> produce(List<List<String>> docTableList) {
        return transListToDefinition(docTableList);
    }

    private List<SqlFieldDefinition> transListToDefinition(List<List<String>> list) {
        List<SqlFieldDefinition> definitionlist = Lists.newArrayList();

        // 找开始列和最大列数
        SqlFieldColumn sqlFieldColumn = new SqlFieldColumn();
        for (int i = 0; i < list.size(); i++) {
            List<String> row = list.get(i);
            int maxColumnCount = Math.max(sqlFieldColumn.columnCount, row.size());
            if (maxColumnCount > sqlFieldColumn.columnCount) {
                sqlFieldColumn.columnCount = maxColumnCount;
                sqlFieldColumn.startLineIndex = i;
            }
        }

        List<SqlFieldDefinition> headerDefination = searchTableHeaderDefination(sqlFieldColumn, list);
        definitionlist.addAll(headerDefination);

        List<SqlFieldDefinition> bodyDefination = searchTableBodyDefination(sqlFieldColumn, list);

        if (bodyDefination.isEmpty()) {
            LOGGER.info("没有找到匹配到完整的sql字段描述");
        } else {
            definitionlist.addAll(bodyDefination);
        }

        return definitionlist;
    }

    private List<SqlFieldDefinition> searchTableBodyDefination(SqlFieldColumn sqlFieldColumn, List<List<String>> list) {
        List<SqlFieldDefinition> definitionlist = Lists.newArrayList();

        for (int lineIndex = sqlFieldColumn.startLineIndex; lineIndex < list.size(); lineIndex++) {
            List<String> row = list.get(lineIndex);

            if (!sqlFieldColumn.isComplete()) {
                searchFieldNameColumnIndex(lineIndex, list, sqlFieldColumn);
            }

            // 每个列的搜索完毕
            if (sqlFieldColumn.isComplete()) {
                SqlFieldDefinition definition = searchFieldSetDefinition(sqlFieldColumn, row);
                if (definition != null) {
                    definitionlist.add(definition);
                }
            }
        }

        return definitionlist;
    }

    private List<SqlFieldDefinition> searchTableHeaderDefination(SqlFieldColumn sqlFieldColumn,
            List<List<String>> list) {

        List<SqlFieldDefinition> definitionlist = Lists.newArrayList();
        boolean tableFind = false;

        for (int lineIndex = 0; lineIndex < sqlFieldColumn.startLineIndex; lineIndex++) {
            List<String> row = list.get(lineIndex);

            for (int j = 0; j < row.size(); j++) {
                // 找表名，通常是最后一列
                if (!tableFind) {
                    SqlFieldDefinition definition = searchTableGetDefinition(row.get(row.size() - 1));

                    // 找到表了 继续找表的注释，如果有的话
                    if (definition != null) {
                        definitionlist.add(definition);

                        lineIndex = searchTableAndSetComment(lineIndex, list, definition, sqlFieldColumn);
                        break;
                    }
                }

                // 找索引字段 每列匹配
                searchIndexSetDefinition(definitionlist, row.get(j));
            }
        }

        return definitionlist;
    }

    // TODOM 可以抽出来
    private static SqlFieldDefinition searchFieldSetDefinition(SqlFieldColumn sqlFieldColumn, List<String> row) {

        MySQLPartTypeEnum sqlPart;

        String fieldType = row.get(sqlFieldColumn.typeIndex);

        boolean isSqlFieldType = false;
        for (String sqlFieldType : SQL_FIELD_TYPE) {
            if (fieldType.indexOf(sqlFieldType) != -1) {
                isSqlFieldType = true;
                break;
            }
        }
        if (!isSqlFieldType) {
            return null;
        }

        if (fieldType.indexOf(SQLFieldTypeEnum.UNSIGNED_FIELD.getSearchName()) != -1) {
            sqlPart = MySQLPartTypeEnum.UNSIGNED_FIELD;
        } else {
            sqlPart = MySQLPartTypeEnum.SIGNED_FIELD;
        }

        String nullFlag = row.get(sqlFieldColumn.nullFlagIndex);

        boolean required = false;
        // 非空字段标识查找
        for (String flag : NOT_NULL_FLAG_STR) {
            if (nullFlag.indexOf(flag) != -1) {
                required = true;
                break;
            }
        }

        return new SqlFieldDefinition(row.get(sqlFieldColumn.nameIndex), sqlPart, row.get(sqlFieldColumn.typeIndex),
                required, row.get(sqlFieldColumn.commentIndex));
    }

    /**
     * 搜寻每一列代表的意义
     * 
     * @param continueIndex
     * @param list
     * @param sqlFieldColumn
     */
    private static void searchFieldNameColumnIndex(int continueIndex, List<List<String>> list,
            SqlFieldColumn sqlFieldColumn) {

        // 首先根据字段类型找，确定起始行，和列数
        int startLineIndex = -1;
        int columnCount = 0;
        for (int lineIndex = continueIndex; lineIndex < list.size(); lineIndex++) {
            List<String> row = list.get(lineIndex);
            columnCount = Math.max(columnCount, row.size());

            if (startLineIndex != -1) {
                continue;
            }

            for (int i = 0; i < row.size(); i++) {
                String column = row.get(i);
                // 找到sql类型的起始行
                for (String sqlType : SQL_FIELD_TYPE) {
                    if (column.indexOf(sqlType) != -1) {
                        startLineIndex = lineIndex;
                    }
                }
            }
        }

        // 每一列单独分析处理
        Map<Integer, List<String>> columnListMap = Maps.newHashMap();

        // 找到字段的起始行，开始找
        for (int lineIndex = startLineIndex; lineIndex < list.size(); lineIndex++) {
            List<String> row = list.get(lineIndex);
            // 保存一下列数
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                List<String> columnList = columnListMap.get(columnIndex);
                if (columnList == null) {
                    columnList = Lists.newArrayList();
                    columnListMap.put(columnIndex, columnList);
                }
                if (columnIndex >= row.size()) {
                    columnList.add(null);
                } else {
                    columnList.add(row.get(columnIndex));
                }
            }
        }

        SqlFieldColumn sqlFieldColumnAssume = assumeEveryColumnMean(columnListMap);

        if (sqlFieldColumnAssume.isComplete()) {
            sqlFieldColumn.nameIndex = sqlFieldColumnAssume.nameIndex;
            sqlFieldColumn.typeIndex = sqlFieldColumnAssume.typeIndex;
            sqlFieldColumn.nullFlagIndex = sqlFieldColumnAssume.nullFlagIndex;
            sqlFieldColumn.defaultIndex = sqlFieldColumnAssume.defaultIndex;
            sqlFieldColumn.commentIndex = sqlFieldColumnAssume.commentIndex;
        }
    }

    private static SqlFieldColumn assumeEveryColumnMean(Map<Integer, List<String>> columnListMap) {
        SqlFieldColumn sqlFieldColumn = new SqlFieldColumn();
        for (int i = 0; i < columnListMap.size(); i++) {
            List<String> columnList = columnListMap.get(i);

            int nameNum = 0;
            int typeNum = 0;
            int nullFlagNum = 0;
            int nullFlagSetNum = 0;
            int defaultNum = 0;

            // 是否为空，最多只有俩值，以此判断 且不为空
            if (sqlFieldColumn.nullFlagIndex == null) {
                Set<String> nullFlagSet = new HashSet<String>();
                for (String column : columnList) {
                    if (StringUtils.isNotBlank(column)) {
                        nullFlagSet.add(column);
                    }

                    for (String flag : NOT_NULL_FLAG_STR) {
                        if (column.indexOf(flag) != -1) {
                            nullFlagNum++;
                            break;
                        }
                    }
                }
                nullFlagSetNum = nullFlagSet.size();

                // 大概率是是否空列
                if (nullFlagSetNum == 2 && nullFlagNum > 0) {
                    sqlFieldColumn.nullFlagIndex = i;
                    continue;
                }
            }

            for (String columns : columnList) {
                if (columns == null) {
                    continue;
                }
                // 确定是类型字段
                boolean typeColumnsFlag = false;
                for (String sqlType : SQL_FIELD_TYPE) {
                    // 类型
                    if (columns.indexOf(sqlType) != -1) {
                        // 匹配到了varchar(
                        if (sqlType.equals(SQL_FIELD_TYPE[1])) {
                            typeNum++;
                            typeColumnsFlag = true;
                        } else if (columns.equals(sqlType)) {
                            typeColumnsFlag = true;
                        }
                        typeNum++;
                        break;
                    }
                }
                if (typeColumnsFlag) {
                    continue;
                }

                // 字段名
                if (FIELD_NAME_PATTERN.matcher(columns).matches()) {
                    nameNum++;
                    continue;
                }

                // 空 默认列
                defaultNum++;
                if (StringUtils.isBlank(columns)) {
                    defaultNum++;
                }

            }

            int max = MathUtils.max(nameNum, typeNum, nullFlagNum);

            LOGGER.debug("nameNum:{},typeNum:{},nullFlagNum:{}", nameNum, typeNum, nullFlagNum);

            // 根据概率来判断每一列是啥
            if (nameNum == max && sqlFieldColumn.nameIndex == null) {
                sqlFieldColumn.nameIndex = i;
            } else if (typeNum == max && sqlFieldColumn.typeIndex == null) {
                sqlFieldColumn.typeIndex = i;
            } else if (defaultNum >= max && sqlFieldColumn.defaultIndex == null) {
                sqlFieldColumn.defaultIndex = i;
            } else {
                sqlFieldColumn.commentIndex = i;
            }
        }
        return sqlFieldColumn;
    }

    /**
     * 
     * @param tableLine
     * @param list
     * @param definition
     * @param sqlFieldColumn
     * @return 表注释在哪一行 0开始
     */
    private static int searchTableAndSetComment(int tableLine, List<List<String>> list, SqlFieldDefinition definition,
            SqlFieldColumn sqlFieldColumn) {
        // -1代表没找到

        boolean find = false;
        // 表注释在下方
        if (tableLine > 0) {
            List<String> rowUp1 = list.get(tableLine - 1);
            // 取一行最末一列来判断
            if (searchTableCommentSetName(definition, rowUp1.get(rowUp1.size() - 1))) {
                find = true;
            }
        }

        if (!find) {
            List<String> rowUp1 = list.get(tableLine + 1);
            // 比最大列数小
            if (sqlFieldColumn.columnCount > rowUp1.size()) {
                // 取一行最末一列来判断
                if (searchTableCommentSetName(definition, rowUp1.get(rowUp1.size() - 1))) {
                    tableLine = tableLine + 1;
                }
            }
            // 与最大列数相同，可能往下也没有表的注释了
        }

        return tableLine;

    }

    private static boolean searchTableCommentSetName(SqlFieldDefinition tableDefinition, String column) {
        if (textIsNotIndex(column)) {
            tableDefinition.setComment(column);
            return true;
        }

        return false;
    }

    /**
     * 找索引主键唯一索引
     * 
     * 格式：
     * 
     * primary key (`id`),
     * 
     * UNIQUE INDEX `uk_parent_id` (`parent_id`)
     * 
     * index `idx_parent_id` (`parent_id`)
     * 
     * TODOM 目前就这仨，后面增加多索引
     * 
     * @param definitionlist
     * @param column
     */
    private static void searchIndexSetDefinition(List<SqlFieldDefinition> definitionlist, String column) {
        if (textIsNotIndex(column)) {
            return;
        }

        // 具体哪一个类型索引
        SqlFieldDefinition definition = null;

        /*
         * Matcher matcher = INDEX_PATTERN.matcher(column); if (!matcher.find()) {
         * LOGGER.info("没有匹配到任何索引字段，字符串[{}]", column); }
         */

        // 主键
        if (column.indexOf(MySQLPartTypeEnum.PRIMARY_KEY.getSearchName()) != -1) {
            Matcher matcher = PRIMARY_PATTERN.matcher(column);
            if (!matcher.find()) {
                LOGGER.info("没有匹配到主键字段，字符串[{}]", column);
            } else {
                definition = new SqlFieldDefinition(null, MySQLPartTypeEnum.PRIMARY_KEY);
                matcher = CAHRACTOR_PATTERN.matcher(matcher.group(1));
                while (matcher.find()) {
                    definition.addIndexField(matcher.group());
                }
            }
        } else {

            // 索引
            Matcher matcher = INDEX_PATTERN.matcher(column);
            if (!matcher.find()) {
                LOGGER.info("没有匹配到索引字段，字符串[{}]", column);
            } else {
                if (column.indexOf(MySQLPartTypeEnum.UNIQUE_INDEX.getSearchName()) != -1) {
                    definition = new SqlFieldDefinition(matcher.group(1), MySQLPartTypeEnum.UNIQUE_INDEX);
                }
                if (column.indexOf(MySQLPartTypeEnum.INDEX.getSearchName()) != -1) {
                    definition = new SqlFieldDefinition(matcher.group(1), MySQLPartTypeEnum.INDEX);
                }

                matcher = CAHRACTOR_PATTERN.matcher(matcher.group(1));
                while (matcher.find()) {
                    definition.addIndexField(matcher.group());
                }

            }
        }

        definitionlist.add(definition);

    }

    private static boolean textIsNotIndex(String text) {
        return text.indexOf(SQLFieldTypeEnum.PRIMARY_KEY.getSearchName()) == -1
                && text.indexOf(SQLFieldTypeEnum.UNIQUE_INDEX.getSearchName()) == -1
                && text.indexOf(SQLFieldTypeEnum.INDEX.getSearchName()) == -1;
    }

    private static SqlFieldDefinition searchTableGetDefinition(String column) {
        if (column.indexOf(DATABASE_PREFIX) != -1 || column.indexOf(TABLE_PREFIX) != -1) {
            return new SqlFieldDefinition(column, MySQLPartTypeEnum.TABLE);
        }
        return null;
    }

    // TODOM 可以抽出来
    private static class SqlFieldColumn {
        Integer nameIndex;
        Integer typeIndex;
        Integer nullFlagIndex;
        Integer commentIndex;
        Integer defaultIndex;

        int startLineIndex;
        int columnCount;

        boolean isComplete() {
            return nameIndex != null && typeIndex != null && nullFlagIndex != null && commentIndex != null;
        }

    }
}
