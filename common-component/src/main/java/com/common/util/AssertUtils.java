package com.common.util;

import org.apache.commons.lang3.StringUtils;

import com.common.constant.SomeEnums.DASErrorCode;
import com.common.exce.DASRuntimeException;

public class AssertUtils {

    public static void notNull(Object o, String field) {
        if (o == null)
            throw new DASRuntimeException(DASErrorCode.PARAM_ERROR.getCode(),
                    "[" + String.valueOf(field) + "]" + "参数不能为空");
    }

    public static void notEmpty(String str, String field) {
        if (StringUtils.isEmpty(str))
            throw new DASRuntimeException(DASErrorCode.PARAM_ERROR.getCode(),
                    "[" + String.valueOf(field) + "]" + "参数不能为空");
    }
}
