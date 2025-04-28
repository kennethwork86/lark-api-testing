package com.dtctest.larkapitesting.model.lark;

import com.dtctest.larkapitesting.enums.lark.LarkSearchOperatorType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString
@RequiredArgsConstructor
public class TableRecordSearchFilterCondition {
    @JsonProperty("field_name")
    public String fieldName;
    public LarkSearchOperatorType operator;
    public List<String> value;
}
