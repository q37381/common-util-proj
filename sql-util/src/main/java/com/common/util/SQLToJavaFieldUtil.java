package com.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据建表语句生成Java代码
 * 
 * @author Nbb
 *
 */
public class SQLToJavaFieldUtil {

    private final static String INDEX_STR = "PRIMARY KEY|INDEX|UNIQUE INDEX";

    private final static String PATTERN_STR = "(?:\\`*)(" + INDEX_STR + "|\\w+)(?:\\W*?)(\\w+)(?:.*[\\f\\n\\r\\t\\v]*)";

    private final static Pattern COMPILE = Pattern.compile(PATTERN_STR);

    private final static Pattern NUMBER = Pattern.compile("^\\d+$");

    public static String getJavaFieldStr(String sql) {

        List<SQLToJavaField> listFieldInSQL = SQLToJavaFieldUtil.listFieldInSQL(sql);

        if (listFieldInSQL == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (SQLToJavaField sqlToJavaField : listFieldInSQL) {
            sb.append(sqlToJavaField.toField()).append("\r\n\r\n");
        }

        return sb.toString();
    }

    private static List<SQLToJavaField> listFieldInSQL(String sql) {

        AssertUtils.notEmpty(sql, "sql");

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

            list.add(new SQLToJavaField(javaField, analyseSQLTypeToJavaType(type)));
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

        type = type.toLowerCase();

        String javaType = null;

        switch (type) {
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

        public SQLToJavaField(String field, String type) {
            super();
            this.field = field;
            this.type = type;
        }

        public String toField() {
            return "private ".concat(type).concat(" ").concat(field).concat(";");
        }
    }

}
