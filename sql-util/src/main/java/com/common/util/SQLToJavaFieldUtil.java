package com.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 根据建表语句生成Java代码
 * 
 * @author Nbb
 *
 */
public class SQLToJavaFieldUtil {

    // 后面可以加日志

    private static final String INDEX_STR = "PRIMARY KEY|INDEX|UNIQUE INDEX";

    private static final String RETURN_NEWLINE = "\n";

    private static final String NULL_STR = "null";

    /*
     * CREATE TABLE `t_menu_info` (
     * 
     * `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
     * 
     * `title` VARCHAR(50) NOT NULL COMMENT '标题' COLLATE 'utf8_unicode_ci',
     * 
     * `icon` VARCHAR(50) NOT NULL DEFAULT 'fa fa-window-maximize' COLLATE
     * 'utf8_unicode_ci',
     * 
     * `href` VARCHAR(50) NOT NULL DEFAULT '' COLLATE 'utf8_unicode_ci',
     * 
     * `parent_id` INT(11) NULL DEFAULT NULL, ;
     * 
     * PRIMARY KEY (`id`) USING BTREE,
     * 
     * INDEX `idx_parent_id` (`parent_id`) USING BTREE
     * 
     * 注 ?: 匹配 但是不放在group里面,后面加? 非贪婪匹配
     * 
     * 1、(?:\\`?) 匹配开始 `符号 或者下面的索引语句 匹配索引字段是为了特殊处理
     * 
     * 2、(PRIMARY KEY|INDEX|UNIQUE INDEX|\\w+) 匹配id、title 。
     * 
     * 后面的primary index，主要是用于后面筛选是否匹配的是主键或者索引语句
     * 
     * 3、(?:\\W*?) 用于匹配 id后面的 '` '
     * 
     * 4、(\\w+) 匹配INT，也就是类型
     * 
     * 5、(?:(?:(?:.*?)(?:COMMENT '))(.+?)'|(?:.*))
     * 
     * ?:不看:
     * 
     * (((.*?)(COMMENT '))(.+?)'|(.*)) 拆分成两部分
     * 
     * ((.*?)(COMMENT '))(.+?)' 和 (.*)
     * 
     * 前者
     * 
     * 一是 ((.*?)(COMMENT ')) 匹配加了注释的那一列
     * 
     * (50) NOT NULL COMMENT '
     * 
     * 二是 (.+?)'
     * 
     * id' (.+?) 即是注释内容
     * 
     * 后者
     * 
     * 没有注释的字段就直接匹配起走了，(.+?) 就是null，因为走的是后者，前者匹配不到
     * 
     * 6、(?:.*\\s*) 匹配之后的字符再到换行，然后到第一步，继续匹配下一行
     * 
     * 
     * ，或者没加注释就直接匹配着走
     */
    private static final String PATTERN_STR = "(?:\\`?)(" + INDEX_STR
            + "|\\w+)(?:\\W*?)(\\w+)(?:(?:(?:.*?)(?:COMMENT '))(.+?)'|(?:.*))(?:.*\\s*)";

    private static final Pattern COMPILE = Pattern.compile(PATTERN_STR);

    private static final Pattern NUMBER = Pattern.compile("^\\d+$");

    /**
     * 根据sql语句生成javabean字段的代码 格式如下
     * 
     * <br>
     * /**<br>
     * * <br>
     * *\/<br>
     * private int id;
     *
     * @param sql
     * 
     * @return
     */
    public static String getJavaFieldStr(String sql) {

        AssertUtils.notEmpty(sql, "sql");

        List<SQLToJavaField> listFieldInSQL = SQLToJavaFieldUtil.listFieldInSQL(sql);

        if (listFieldInSQL == null || listFieldInSQL.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (SQLToJavaField sqlToJavaField : listFieldInSQL) {
            sb.append(sqlToJavaField.toField()).append(RETURN_NEWLINE).append(RETURN_NEWLINE);
        }

        return sb.toString();
    }

    /**
     * 取得sql语句转换成的java字段列表
     * 
     * @param sql
     * @return
     */
    private static List<SQLToJavaField> listFieldInSQL(String sql) {

        int start = sql.indexOf("(") + 1;
        // 假设复制的括号内
        int assume = sql.indexOf(")");

        String matchStr = sql;

        if (start != -1 && assume != -1) {
            if (!NUMBER.matcher(sql.substring(start, assume)).matches()) {
                int end = sql.lastIndexOf(")");
                matchStr = sql.substring(start, end);
            }
        }

        Matcher matcher = COMPILE.matcher(matchStr);

        List<SQLToJavaField> list = null;

        while (matcher.find()) {

            if (list == null) {
                list = new ArrayList<>();
            }

            String field = matcher.group(1);
            // 域完了
            if (INDEX_STR.indexOf(field.trim()) != -1) {
                break;
            }

            String javaField = genJavaField(field.toLowerCase());

            String type = matcher.group(2);

            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println(matcher.group(i));
            }
            System.out.println();

            list.add(new SQLToJavaField(javaField, analyseSQLTypeToJavaType(type), matcher.group(3)));
        }

        return list;
    }

    public static String genJavaField(String field) {
        StringBuilder sb = new StringBuilder();

        int start = 0;
        int next = 0;

        while (next != field.length()) {
            next = field.indexOf("_", start);
            if (next == -1) {
                next = field.length();
            }

            if (start == 0) {
                sb.append(field.substring(start, next));
            } else {
                sb.append(field.substring(start, start + 1).toUpperCase()).append(field.substring(start + 1, next));
            }

            start = next + 1;
        }

        return sb.toString();
    }

    private static String analyseSQLTypeToJavaType(String type) {

        String javaType = null;

        switch (type.toLowerCase()) {
        case "tinyint":
        case "smallint":
        case "mediumint":
        case "int":
            javaType = "Integer";
            break;
        case "bigint":
            javaType = "Long";
            break;
        case "datetime":
            javaType = "Date";
            break;
        default:
            javaType = "String";
            break;
        }
        return javaType;
    }

    private static class SQLToJavaField {

        private String field;

        private String type;

        private String comment;

        public SQLToJavaField(String field, String type, String comment) {
            super();
            this.field = field;
            this.type = type;
            this.comment = comment;
        }

        public String toField() {
            StringBuilder sb = new StringBuilder();
            sb.append("/**\n").append(" * ");
            if (StringUtils.isNotBlank(comment) && !comment.trim().equalsIgnoreCase(NULL_STR)) {
                sb.append(comment);
            }
            sb.append("\n");
            sb.append(" */\n");
            sb.append("private ").append(type).append(" ").append(field).append(";");
            return sb.toString();
        }
    }

}
