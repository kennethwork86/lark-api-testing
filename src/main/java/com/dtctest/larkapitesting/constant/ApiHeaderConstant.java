package com.dtctest.larkapitesting.constant;

import top.dtc.common.model.api.ApiHeader;

public class ApiHeaderConstant {
    public static final ApiHeader SUCCESS = new ApiHeader(true);

    // STARTS WITH "00"
    public static class COMMON {
        public static final ApiHeader INVALID_PARAMS = new ApiHeader("00001", "Invalid Params");
        public static final ApiHeader INVALID_TYPE = new ApiHeader("00003", "Invalid Type");
        public static ApiHeader OTHER_ERROR(String errMsg) {
            return new ApiHeader("00999", errMsg);
        }
    }
}
