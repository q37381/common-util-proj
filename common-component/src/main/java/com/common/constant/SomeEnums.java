package com.common.constant;

public interface SomeEnums {

    public enum DASErrorCode {

        UNKNOWN_ERROR("0000000000", "未知错误"),

        COMMON_ERROR("1000000001", "通用错误"),

        PARAM_ERROR("1000000002", "参数错误"),

        ;

        private String code;

        private String desc;

        DASErrorCode(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

    }
}
