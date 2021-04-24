package com.common.exce;

import com.common.constant.SomeEnums.DASErrorCode;

public class DASRuntimeException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -4649470579160615148L;

    private String code;

    private String desc;

    public DASRuntimeException(DASErrorCode das) {
        super();
        code = das.getCode();
        desc = das.getDesc();
    }

    public DASRuntimeException(String code, String desc) {
        super();
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
