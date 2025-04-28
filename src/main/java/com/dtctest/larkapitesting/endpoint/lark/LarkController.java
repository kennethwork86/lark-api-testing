package com.dtctest.larkapitesting.endpoint.lark;

import com.dtctest.larkapitesting.constant.ApiHeaderConstant;
import com.dtctest.larkapitesting.service.LarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.dtc.common.model.api.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/lark")
@RequiredArgsConstructor
public class LarkController {

    private final LarkService larkService;

    //供lark应用使用，应用中取得当前的hr-appId
    @GetMapping("/get-app-id")
    public ApiResponse<String> getAppId() {
        return new ApiResponse<>(ApiHeaderConstant.SUCCESS, larkService.getAppId());
    }

    @PostMapping("/handle-event-subscription")
    public ResponseEntity<Map<String, String>> handleEventSubscription(@RequestBody Map<String, Object> body) {
        log.info("Received event subscription request: {}", body);

        Object challenge = body.get("challenge");
        Map<String, String> response = new HashMap<>();

        if (challenge != null) {
            response.put("challenge", challenge.toString());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } else {
            // Not a verification event; you can handle actual events here
            return ResponseEntity.ok().build();
        }
    }


}
