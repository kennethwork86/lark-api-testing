package com.dtctest.larkapitesting.model.lark;

import com.dtctest.larkapitesting.enums.lark.LarkSearchConjunctionType;
import lombok.ToString;

import java.util.List;

@ToString
public class TableRecordSearchFilter {
    public LarkSearchConjunctionType conjunction;
    public List<TableRecordSearchFilterCondition> conditions;
}
