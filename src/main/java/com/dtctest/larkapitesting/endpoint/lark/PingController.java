package com.dtctest.larkapitesting.endpoint.lark;

import com.dtctest.larkapitesting.constant.ApiHeaderConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.dtc.common.model.api.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return new ApiResponse<>(ApiHeaderConstant.SUCCESS, "pong");
    }

}
