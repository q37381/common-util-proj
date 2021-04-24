package com.common.util.enums;

public enum SQLFieldTypeEnum {

    TABLE("table", "CREATE TABLE"),

    PRIMARY_KEY("primary", "PRIMARY KEY"),

    UNIQUE_INDEX("unique", "UNIQUE INDEX"),

    INDEX("idx", "INDEX"),

    FIELD(null, null),

    ;

    private String searchName;

    private String realName;

    private SQLFieldTypeEnum(String searchName, String realName) {
        this.searchName = searchName;
        this.realName = realName;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getRealName() {
        return realName;
    }

}
