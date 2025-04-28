package com.dtctest.larkapitesting.model.lark;

import com.dtctest.larkapitesting.utils.LarkSnakeCaseStrategy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.dtc.common.json.JSON;

import java.util.Map;

@Slf4j
public class LarkDictResp extends LarkResponse<Map<String,Object>> {

    @JsonIgnore
    final ObjectMapper objMapper = new ObjectMapper(new JsonFactory());

    public LarkDictResp() {
        objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objMapper.setPropertyNamingStrategy(new LarkSnakeCaseStrategy());
    }

    public <T> T asObject(Class<T> clazz) {
        return asObject(null, clazz);
    }

    //将Map先转换回json字符串，再将拿到的json转换成clazz对象
    public <T> T asObject(String key, Class<T> clazz) {
        if (this.data == null) return null;
        Object rawValue = this.data;
        if (key != null && !key.isEmpty()) {
            rawValue = this.data.get(key);
        }

        String jsonStr = JSON.stringify(rawValue);

        try {
            return objMapper.readValue(jsonStr, clazz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
