package com.common.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.common.util.enums.SQLFieldTypeEnum;
import com.google.common.collect.Lists;

public class MeiDocSqlUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeiDocSqlUtil.class);

    // 后期这些特殊分割或描述字符可能根据场景变更，可以做成配置
    private static final String COMMA = ",";

    private static final String DATABASE_PREFIX = "db_";

    private static final String TABLE_PREFIX = "t_";

    private static final String INDEX_REGEX = "(?:[\\w ]+)[\\(|\\`]+(\\w+)(?:\\W+)?(\\w+)?";

    private static final Pattern INDEX_PATTERN = Pattern.compile(INDEX_REGEX);

    // =================

    public static Object splitWord(String doc) {

        AssertUtils.notEmpty(doc, "doc");

        LOGGER.debug("传入待分割字符串");
        LOGGER.debug("{}", doc);
        String[] splitDocArr = splitDoc(doc);

        List<List<String>> list = transToListString(splitDocArr);

        // 暂时判定一行有4列及以上的是字段行，否则为表名或主键索引设置
        transListToDefinition(list);

        return list;
    }

    private static void transListToDefinition(List<List<String>> list) {

        int nameIndex = 0;
        int typeIndex = 1;
        int nullFlagIndex = 2;
        int commentIndex = 3;

        List<SqlFieldDefinition> definitionlist = Lists.newArrayList();

        boolean tableFind = false;

        for (int i = 0; i < list.size(); i++) {
            List<String> row = list.get(i);
            // 非字段行
            if (row.size() < 4) {
                for (int j = 0; j < row.size(); j++) {
                    String line = row.get(j);

                    // 找表名
                    if (!tableFind) {
                        SqlFieldDefinition definition = searchTableGetDefinition(line);
                        if (definition != null) {
                            definitionlist.add(definition);
                        }
                    }

                    searchIndexSetDefinition(definitionlist, line);
                }
            } else {

            }
        }
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
     * @param line
     */
    private static void searchIndexSetDefinition(List<SqlFieldDefinition> definitionlist, String line) {
        if (line.indexOf(SQLFieldTypeEnum.PRIMARY_KEY.getSearchName()) != -1
                || line.indexOf(SQLFieldTypeEnum.UNIQUE_INDEX.getSearchName()) != -1
                || line.indexOf(SQLFieldTypeEnum.INDEX.getSearchName()) != -1) {

            String[] split = line.split("\\r\\n");
            if (split == null) {
                split = line.split("[\\r\\n]");
            }

            // 具体哪一个类型索引
            for (String one : split) {
                SqlFieldDefinition definition = null;
                Matcher matcher = INDEX_PATTERN.matcher(one);
                if (!matcher.find()) {
                    LOGGER.info("没有匹配到任何索引字段，字符串[{}]", one);
                }
                if (one.indexOf(SQLFieldTypeEnum.PRIMARY_KEY.getSearchName()) != -1) {
                    definition = new SqlFieldDefinition(null, matcher.group(1), SQLFieldTypeEnum.PRIMARY_KEY);
                }
                if (one.indexOf(SQLFieldTypeEnum.UNIQUE_INDEX.getSearchName()) != -1) {
                    definition = new SqlFieldDefinition(matcher.group(1), matcher.group(2),
                            SQLFieldTypeEnum.UNIQUE_INDEX);
                }
                if (one.indexOf(SQLFieldTypeEnum.INDEX.getSearchName()) != -1) {
                    definition = new SqlFieldDefinition(matcher.group(1), matcher.group(2), SQLFieldTypeEnum.INDEX);
                }

                definitionlist.add(definition);
            }
        }

    }

    private static SqlFieldDefinition searchTableGetDefinition(String line) {
        if (line.indexOf(DATABASE_PREFIX) != -1 || line.indexOf(TABLE_PREFIX) != -1) {
            // 该行为表名
            SqlFieldDefinition definition = new SqlFieldDefinition(line.trim(), SQLFieldTypeEnum.TABLE, null, null);
            // 找该表的备注，可能在之前可能在之后，依情况而定
            // TODOM 这里先不存表名
            return definition;
            // 先不处理 表名
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
            String[] split2 = first.split("[\\t ]");
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

    /**
     * 目前为这四个字段
     * 
     * @author Nbb
     *
     */
    private static class SqlFieldDefinition {

        private String name;

        /**
         * 主键索引 使用这个字段
         */
        private String field;

        private SQLFieldTypeEnum type;

        private String nullFlag;

        private String comment;

        public SqlFieldDefinition(String name, SQLFieldTypeEnum type, String nullFlag, String comment) {
            super();
            this.name = name;
            this.type = type;
            this.nullFlag = nullFlag;
            this.comment = comment;
        }

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

        public void setName(String name) {
            this.name = name;
        }

        public void setType(SQLFieldTypeEnum type) {
            this.type = type;
        }

        public void setField(String field) {
            this.field = field;
        }

        public void setNullFlag(String nullFlag) {
            this.nullFlag = nullFlag;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

    }

    public static void main(String[] args) {

        String str = "primary key (`id`),";

        Matcher matcher = INDEX_PATTERN.matcher(str);

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println(i + ":" + matcher.group(i));
            }
        }

        str = "   fsdfdsfsdadasddasdasdasd\r\n" + "dsad\tdasdad\tdasd\tsdas\r\n" + "123 2312    123 3123\r\n" + "";

        // splitWord(str);
    }
}
