package com.dtctest.larkapitesting.utils;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/*
由于Lark的json标准是下划线间隔返回，而我们前端希望是返回的方式是首字母小写的驼峰
所以采用SnakeCaseStrategy，但是由于不同尺寸的用户头像URL在Lark返回时也加了下划线(这里标准的Snake是不会转换出下划线的)
所以做了一点特殊处理
 */

public class LarkSnakeCaseStrategy extends PropertyNamingStrategies.NamingBase {

    final PropertyNamingStrategies.SnakeCaseStrategy snakeCaseStrategy = new PropertyNamingStrategies.SnakeCaseStrategy();

    @Override
    public String translate(String s) {
        String value = snakeCaseStrategy.translate(s);
        if (value == null || value.isEmpty()) return value;
        return switch (value) {
            case "avatar240" -> "avatar_240";
            case "avatar640" -> "avatar_640";
            case "avatar72" -> "avatar_72";
            default -> value;
        };
    }
}
