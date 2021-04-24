package com.common.util;

import org.junit.jupiter.api.Test;

class SQLToJavaFieldUtilTest {

    @Test
    void GenJavaField() {
        String sql = "CREATE TABLE `t_menu_info` (\r\n" + 
                "    `id` INT(11) NOT NULL AUTO_INCREMENT,\r\n" + 
                "    `title` VARCHAR(50) NOT NULL COLLATE 'utf8_unicode_ci',\r\n" + 
                "    `icon` VARCHAR(50) NOT NULL DEFAULT 'fa fa-window-maximize' COLLATE 'utf8_unicode_ci',\r\n" + 
                "    `href` VARCHAR(50) NOT NULL DEFAULT '' COLLATE 'utf8_unicode_ci',\r\n" + 
                "    `parent_id` INT(11) NULL DEFAULT NULL,\r\n" + 
                "    `create_time` DATETIME NOT NULL,\r\n" + 
                "    `update_time` DATETIME NULL DEFAULT NULL,\r\n" + 
                "    PRIMARY KEY (`id`) USING BTREE,\r\n" + 
                "    UNIQUE INDEX `idx_parent_id` (`parent_id`) USING BTREE\r\n" + 
                ")\r\n" + 
                "COLLATE='utf8_unicode_ci'\r\n" + 
                "ENGINE=InnoDB\r\n" + 
                ";\r\n" + 
                "";
        System.out.println(SQLToJavaFieldUtil.getJavaFieldStr(sql));
        
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < 10000000; i++) {
            SQLToJavaFieldUtil.genJavaField("create_time_dasdsa");
        }
        
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }

}
