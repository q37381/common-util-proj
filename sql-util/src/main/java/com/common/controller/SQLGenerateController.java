package com.common.controller;

import java.util.List;

import com.common.handler.impl.CreateSQLHandler;
import com.common.model.SqlFieldDefinition;
import com.common.service.impl.MeiProducer;
import com.common.util.WordTableTranslateUtils;

public class SQLGenerateController {

    // TODOM 后期策略
    private MeiProducer meiProducer = new MeiProducer();

    private CreateSQLHandler sqlHandler = new CreateSQLHandler();

    public String generateSQL(String doc) {

        List<List<String>> list = WordTableTranslateUtils.splitWordToListByTab(doc);
        List<SqlFieldDefinition> definitionList = meiProducer.produce(list);
        return sqlHandler.doHandler(definitionList);
    }
}
