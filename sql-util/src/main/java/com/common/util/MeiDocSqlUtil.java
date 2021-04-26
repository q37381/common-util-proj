package com.common.util;

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
import com.common.service.SqlGenerateService;
import com.common.util.enums.SQLFieldTypeEnum;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MeiDocSqlUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeiDocSqlUtil.class);

    // 后期这些特殊分割或描述字符可能根据场景变更，可以做成配置
    private static final String DATABASE_PREFIX = "db_";

    private static final String TABLE_PREFIX = "t_";

    private static final String INDEX_REGEX = "(?:[\\w ]+)[\\(|\\`]+(\\w+)(?:\\W+)?(\\w+)?";

    private static final Pattern INDEX_PATTERN = Pattern.compile(INDEX_REGEX);

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z]+\\w+$");

    private static final String[] SQL_FIELD_TYPE = "int varchar( datetime text ".split(" ");

    // =================

    public static List<SqlFieldDefinition> splitWord(String doc) {

        AssertUtils.notEmpty(doc, "doc");

        LOGGER.debug("传入待分割字符串");
        LOGGER.debug("{}", doc);

        String[] splitDocArr = splitDoc(doc.toLowerCase());

        List<List<String>> list = transToListString(splitDocArr);

        // 暂时判定一行有4列及以上的是字段行，否则为表名或主键索引设置
        List<SqlFieldDefinition> definitionList = transListToDefinition(list);

        return definitionList;
    }

    private static List<SqlFieldDefinition> transListToDefinition(List<List<String>> list) {

        List<SqlFieldDefinition> definitionlist = Lists.newArrayList();

        int tableLine = -1;
        SqlFieldColumn sqlFieldColumn = null;
        boolean body = false;
        for (int i = 0; i < list.size(); i++) {
            List<String> row = list.get(i);

            // 非字段行
            if (!body && row.size() < 4) {
                for (int j = 0; j < row.size(); j++) {
                    // 找表名，通常是最后一列
                    if (tableLine == -1) {
                        SqlFieldDefinition definition = searchTableGetDefinition(row.get(row.size() - 1));

                        // 找到表了 继续找表的注释，如果有的话
                        if (definition != null) {
                            definitionlist.add(definition);
                            tableLine = i;

                            int findLine = searchTableAndSetComment(tableLine, list, definition);
                            // 找到了就跳到那一行，下次就从这行的下一行继续找其他的
                            if (findLine > i) {
                                i = findLine;
                            }

                            break;
                        }
                    }

                    // 找索引字段 每列匹配
                    searchIndexSetDefinition(definitionlist, row.get(j));
                }
            } else {
                // 字段行
                if (sqlFieldColumn == null) {
                    body = true;
                    sqlFieldColumn = searchFieldNameColumnIndex(i, list);
                }
                if (sqlFieldColumn != null) {
                    searchFieldSetDefinition(definitionlist, sqlFieldColumn, row);
                }
            }
        }

        if (sqlFieldColumn == null) {
            LOGGER.info("没有找到匹配到完整的sql字段描述");
        }

        return definitionlist;
    }

    private static void searchFieldSetDefinition(List<SqlFieldDefinition> definitionlist, SqlFieldColumn sqlFieldColumn,
            List<String> row) {

        SQLFieldTypeEnum sqlFieldTypeEnum;

        String fieldType = row.get(sqlFieldColumn.getTypeIndex());
        if (fieldType.indexOf(SQLFieldTypeEnum.UNSIGNED_FIELD.getSearchName()) != -1) {
            sqlFieldTypeEnum = SQLFieldTypeEnum.UNSIGNED_FIELD;
        } else {
            sqlFieldTypeEnum = SQLFieldTypeEnum.SIGNED_FIELD;
        }

        String comment = null;
        if (row.size() > sqlFieldColumn.getCommentIndex()) {
            comment = row.get(sqlFieldColumn.getCommentIndex());
        }
        SqlFieldDefinition sqlFieldDefinition = new SqlFieldDefinition(row.get(sqlFieldColumn.getNameIndex()),
                sqlFieldTypeEnum, row.get(sqlFieldColumn.getTypeIndex()), row.get(sqlFieldColumn.getNullFlagIndex()),
                comment);
        definitionlist.add(sqlFieldDefinition);
    }

    private static SqlFieldColumn searchFieldNameColumnIndex(int continueIndex, List<List<String>> list) {

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

        SqlFieldColumn sqlFieldColumn = assumeEveryColumnMean(columnListMap);

        if (!sqlFieldColumn.isComplete()) {
            return null;
        }
        return sqlFieldColumn;
    }

    private static SqlFieldColumn assumeEveryColumnMean(Map<Integer, List<String>> columnListMap) {
        SqlFieldColumn sqlFieldColumn = new SqlFieldColumn();
        for (int i = 0; i < columnListMap.size(); i++) {
            List<String> columnList = columnListMap.get(i);

            int nameNum = 0;
            int typeNum = 0;
            int nullFlagNum = 0;

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

                // 是否为空，最多只有俩值，以此判断
                Set<String> nullFlagSet = new HashSet<String>(columnList);
                nullFlagNum = nullFlagSet.size();

            }

            int max = MathUtils.max(nameNum, typeNum, nullFlagNum);

            LOGGER.debug("nameNum:{},typeNum:{},nullFlagNum:{}", nameNum, typeNum, nullFlagNum);

            // 根据概率来判断每一列是啥
            if (nameNum == max && sqlFieldColumn.getNameIndex() == null) {
                sqlFieldColumn.setNameIndex(i);
            } else if (typeNum == max && sqlFieldColumn.getTypeIndex() == null) {
                sqlFieldColumn.setTypeIndex(i);
            } else if (nullFlagNum >= 2 && sqlFieldColumn.getNullFlagIndex() == null) {
                sqlFieldColumn.setNullFlagIndex(i);
            } else {
                sqlFieldColumn.setCommentIndex(i);
            }
        }
        return sqlFieldColumn;
    }

    /**
     * 
     * @param tableLine
     * @param list
     * @param definition
     * @return 表注释在哪一行 0开始
     */
    private static int searchTableAndSetComment(int tableLine, List<List<String>> list, SqlFieldDefinition definition) {
        // -1代表没找到
        int findLine = -1;
        // 表注释在下方
        if (tableLine > 0) {
            List<String> rowUp1 = list.get(tableLine - 1);
            // 取一行最末一列来判断
            if (searchTableCommentSetName(definition, rowUp1.get(rowUp1.size() - 1))) {
                findLine = tableLine - 1;
            }
        }

        if (findLine == -1) {
            List<String> rowUp1 = list.get(tableLine + 1);
            // 取一行最末一列来判断
            if (searchTableCommentSetName(definition, rowUp1.get(rowUp1.size() - 1))) {
                findLine = tableLine + 1;
            }
        }

        return findLine;
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
     * TODOM 目前就这仨
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
        Matcher matcher = INDEX_PATTERN.matcher(column);
        if (!matcher.find()) {
            LOGGER.info("没有匹配到任何索引字段，字符串[{}]", column);
        }
        if (column.indexOf(SQLFieldTypeEnum.PRIMARY_KEY.getSearchName()) != -1) {
            definition = new SqlFieldDefinition(null, matcher.group(1), SQLFieldTypeEnum.PRIMARY_KEY);
        }
        if (column.indexOf(SQLFieldTypeEnum.UNIQUE_INDEX.getSearchName()) != -1) {
            definition = new SqlFieldDefinition(matcher.group(1), matcher.group(2), SQLFieldTypeEnum.UNIQUE_INDEX);
        }
        if (column.indexOf(SQLFieldTypeEnum.INDEX.getSearchName()) != -1) {
            definition = new SqlFieldDefinition(matcher.group(1), matcher.group(2), SQLFieldTypeEnum.INDEX);
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
            // 该行为表名
            SqlFieldDefinition definition = new SqlFieldDefinition(column.trim(), SQLFieldTypeEnum.TABLE);
            // 找该表的备注，可能在之前可能在之后，依情况而定
            return definition;
        }
        return null;
    }

    private static List<List<String>> transToListString(String[] splitDocArr) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("第一次切分，切分出每一行：");
            for (int i = 0; i < splitDocArr.length; i++) {
                LOGGER.debug("{}={}", i, splitDocArr[i]);
            }
            LOGGER.debug("第一次切分结束========");
        }

        List<List<String>> list = Lists.newArrayList();
        for (String first : splitDocArr) {
            if (StringUtils.isBlank(first)) {
                continue;
            }

            // 第二次切分一行中的列
            // 不能分割unique index， primary key，index 之间的空格
            String[] split2 = first.split("(\\t|(?<!unique) (?!key|\\(|\\`))");
            List<String> columnList = Lists.newArrayList();

            LOGGER.debug("第二次切分，切分出每行的列：");
            for (int j = 0; j < split2.length; j++) {
                String second = split2[j];
                if (StringUtils.isBlank(second)) {
                    continue;
                }
                columnList.add(second);
                LOGGER.debug("{}={}", j, second);
            }
            list.add(columnList);

            LOGGER.debug("第二次切分结束==========");
        }

        return list;
    }

    private static String[] splitDoc(String doc) {
        String[] split = doc.split("\\r\\n");
        if (split == null) {
            split = doc.split("[\\r\\n]");
        }

        if (split == null) {
            return null;
        }
        return split;
    }

    private static class SqlFieldColumn {
        private Integer nameIndex;
        private Integer typeIndex;
        private Integer nullFlagIndex;
        private Integer commentIndex;

        public Integer getNameIndex() {
            return nameIndex;
        }

        public void setNameIndex(Integer nameIndex) {
            this.nameIndex = nameIndex;
        }

        public Integer getTypeIndex() {
            return typeIndex;
        }

        public void setTypeIndex(Integer typeIndex) {
            this.typeIndex = typeIndex;
        }

        public Integer getNullFlagIndex() {
            return nullFlagIndex;
        }

        public void setNullFlagIndex(Integer nullFlagIndex) {
            this.nullFlagIndex = nullFlagIndex;
        }

        public Integer getCommentIndex() {
            return commentIndex;
        }

        public void setCommentIndex(Integer commentIndex) {
            this.commentIndex = commentIndex;
        }

        public boolean isComplete() {
            return nameIndex != null && typeIndex != null && nullFlagIndex != null && commentIndex != null;
        }

    }

    public static void main(String[] args) {

        String str = "primary key (`id`),";

        str = "asdas    db_ddd.t_ddd\r\n" + "sadasd  的雷克萨进度款拉丝机大数据库里\r\n" + "dasd    primary key (`qweqwl`),\r\n"
                + "UNIQUE INDEX `uk_parent_id` (`parent_id`),\r\n" + "index `idx_parent_id` (`parent_id`)\r\n"
                + "dsalkj_dsa  int y dasjkl\r\n" + "qweqwl varchar(42) n \r\n" + "dslakdas varchar(42) n\r\n"
                + "dsaldal int n";

        List<SqlFieldDefinition> splitWord = splitWord(str);

        for (SqlFieldDefinition string : splitWord) {
            System.out.println(string);
        }

        SqlGenerateService service = new SqlGenerateService();
        String sql = service.generateCreateSql(splitWord);
        System.out.println(sql);

    }
}
