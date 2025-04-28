package com.dtctest.larkapitesting.enums.lark;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LarkSearchOperatorType {

    IS(1, "is"),
    ISNOT(2, "isNot"),
    CONTAINS(3, "contains"),
    DOESNOTCONTAIN(4, "doesNotContain"),
    ISEMPTY(5, "isEmpty"),
    ISNOTEMPTY(6, "isNotEmpty"),
    ISGREATER(7, "isGreater"),
    ISGREATEREQUAL(8, "isGreaterEqual"),
    ISLESS(9, "isLess"),
    ISLESSEQUAL(10, "isLessEqual"),
    LIKE(11, "equal"),
    IN(12, "in"),
    ;

    public final int id;
    @JsonValue
    @EnumValue
    public final String desc;
}
