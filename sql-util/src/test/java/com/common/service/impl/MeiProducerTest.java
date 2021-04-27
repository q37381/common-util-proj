package com.common.service.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class MeiProducerTest {
    private static final String INDEX_REGEX = "(?:[\\w ]+)\\W+(\\w+)\\W+(.+)";

    @SuppressWarnings("unused")
    private static final Pattern INDEX_PATTERN = Pattern.compile(INDEX_REGEX);
    
    private static final Pattern PRIMARY_PATTERN = Pattern.compile(".+?[\\(|\\`](.+)");

    private static final Pattern FIELD_PATTERN = Pattern.compile("(\\w+)+");

    @Test
    void test() {
        Matcher matcher = PRIMARY_PATTERN.matcher("PRIMARY KEY (`id`,`id`),");
        matcher.find();

        for (int i = 1; i <= matcher.groupCount(); i++) {

            //System.out.println(matcher.group(i));

            String group = matcher.group(1);
           
            Matcher matcher2 = FIELD_PATTERN.matcher(group);
            

            while(matcher2.find()) {
                 System.out.println(matcher2.group());
            }

        }
    }

}
