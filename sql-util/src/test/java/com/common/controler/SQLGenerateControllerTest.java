package com.common.controler;

import org.junit.jupiter.api.Test;

import com.common.controller.SQLGenerateController;

class SQLGenerateControllerTest {

    @Test
    void test() {
        String doc = "表名\tdb_background_manage.t_op_user_registration\r\n" + "描述\t开放平台用户信息登记表\r\n"
                + "索引\tPRIMARY KEY (`id`,`idc`),\r\n" + "INDEX `idx_create_time` (`create_time`,`create_time2`)\r\n"
                + "字段名\t类型\t非空\t默认值\t描述\r\n" + "id\tunsign-INT\tY\t\t主键，自增\r\n"
                + "company_name\tVARCHAR(128)\tN\t\t公司名称\r\n" + "contact_name\tVARCHAR(128)\tN\t\t联系人姓名\r\n"
                + "contact_phone\tVARCHAR(128)\tN\t\t联系人手机号 加密\r\n" + "create_time\tDATETIME\tY\t\t创建日期\r\n"
                + "modify_time\tDATETIME\tY\t\t修改日期\r\n" + "register_ip_address\tVARCHAR(24)\tY\t\t登记人的ip地址           ";
        String generateSQL = new SQLGenerateController().generateSQL(doc);
        System.out.println(generateSQL);
    }

}
