package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class LarkAppTokenResp {

    @JsonProperty("tenant_access_token")
    public String tenantAccessToken;

    @JsonProperty("app_access_token")
    public String appAccessToken;

    @JsonProperty("code")
    public int code;

    @JsonProperty("msg")
    public String msg;

    @JsonProperty("expire")
    public int expire;

    //lark返回的token中没有token创建时间，所以需要手工加入创建时间
    @JsonProperty("createTime")
    public LocalDateTime createTime;

    //计算token到期时间，保留25分钟的缓冲，到此时间需要重新取得Token
    public LocalDateTime getExpireTime() {
        return createTime.plusSeconds(expire).plusMinutes(-25);
    }
}
