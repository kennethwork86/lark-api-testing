package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;


public class FileShortcutInfo {
    @JsonProperty("target_type")
    public String targetType;
    @JsonProperty("target_token")
    public String targetToken;
}
