package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.Map;

@ToString
public class TableRecordInfo {
    public Map<String, Object> fields;
    @JsonProperty("record_id")
    public String recordId;
}
