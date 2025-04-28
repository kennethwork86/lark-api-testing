package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;

@ToString
public class TableRecordItemsResp {
    @JsonProperty("items")
    public List<TableRecordInfo> tableRecordInfos;
    @JsonProperty("has_more")
    public boolean hasMore;
    @JsonProperty("page_token")
    public String pageToken;
}
