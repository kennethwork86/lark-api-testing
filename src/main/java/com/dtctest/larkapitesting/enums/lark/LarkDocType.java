package com.dtctest.larkapitesting.enums.lark;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LarkDocType {
    DOC(1, "doc"),
    SHEET(2, "sheet"),
    BITABLE(3, "bitable"),
    MINDNOTE(4, "mindnote"),
    FILE(5, "file"),
    WIKI(6, "wiki"),
    DOCX(7, "docx"),
    FOLDER(8, "folder"),
    ;

    public final int id;
    @JsonValue
    @EnumValue
    public final String desc;

}
