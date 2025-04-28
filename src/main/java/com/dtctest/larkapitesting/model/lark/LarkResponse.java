package com.dtctest.larkapitesting.model.lark;

import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class LarkResponse<T> {
    
    public int code;
    public T data;
    public String msg;

    public LarkResponse(T data) {
        this.data = data;
    }

    public LarkResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
}
