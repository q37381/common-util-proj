package com.common.service;

import java.util.List;

import com.common.model.SqlFieldDefinition;

public interface SQLFieldDefinitionProducer {

    List<SqlFieldDefinition> produce(List<List<String>> docTableList);
}
