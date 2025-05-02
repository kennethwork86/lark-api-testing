package com.dtctest.larkapitesting.enums.lark;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LarkSearchOperatorType {

    IS              (1, "is"),
    IS_NOT          (2, "isNot"),
    CONTAINS        (3, "contains"),
    DOES_NOT_CONTAIN(4, "doesNotContain"),
    IS_EMPTY        (5, "isEmpty"),
    IS_NOT_EMPTY    (6, "isNotEmpty"),
    IS_GREATER      (7, "isGreater"),
    IS_GREATER_EQUAL(8, "isGreaterEqual"),
    IS_LESS         (9, "isLess"),
    IS_LESS_EQUAL   (10, "isLessEqual"),
    LIKE            (11, "equal"),
    IN              (12, "in"),
    ;

    public final int id;
    @JsonValue
    @EnumValue
    public final String desc;
}
