package com.dtctest.larkapitesting.enums.lark;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LarkSearchConjunctionType {

    AND(1, "and"),
    OR(2, "or"),
    ;

    public final int id;
    @JsonValue
    @EnumValue
    public final String desc;
}
