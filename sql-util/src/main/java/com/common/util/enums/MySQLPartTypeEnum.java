package com.common.util.enums;

public enum MySQLPartTypeEnum {

    TABLE("table", "CREATE TABLE", 1),

    PRIMARY_KEY("primary", "PRIMARY KEY", 3),

    UNIQUE_INDEX("unique", "UNIQUE INDEX", 4),

    INDEX("idx", "INDEX", 5),

    SIGNED_FIELD(null, null, 2),

    UNSIGNED_FIELD("unsign", "UNSIGNED", 2),

    ;

    private String searchName;

    private String realName;

    private int sortNo;

    private MySQLPartTypeEnum(String searchName, String realName, int sortNo) {
        this.searchName = searchName;
        this.realName = realName;
        this.sortNo = sortNo;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getRealName() {
        return realName;
    }

    public int getSortNo() {
        return sortNo;
    }

}
