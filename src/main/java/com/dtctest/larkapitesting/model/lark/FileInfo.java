package com.dtctest.larkapitesting.model.lark;

import com.dtctest.larkapitesting.enums.lark.LarkDocType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class FileInfo {
    public String token;
    public String name;
    @JsonProperty("owner_id")
    public String ownerId;
    @JsonProperty("type")
    public LarkDocType docType;
    @JsonProperty("parent_token")
    public String parentToken;
    public String url;
    @JsonProperty("shortcut_info")
    public FileShortcutInfo shortcutInfo;
    @JsonProperty("next_page_token")
    public String nextPageToken;
}
