package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;

@ToString
public class FileItemsResp {
    @JsonProperty("files")
    public List<FileInfo> fileInfos;
    @JsonProperty("has_more")
    public boolean hasMore;
}
