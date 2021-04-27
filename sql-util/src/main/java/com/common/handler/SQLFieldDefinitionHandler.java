package com.common.handler;

import java.util.List;

import com.common.model.SqlFieldDefinition;

public interface SQLFieldDefinitionHandler {

    String doHandler(List<SqlFieldDefinition> definitionList);
}
