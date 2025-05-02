package com.dtctest.larkapitesting.model.lark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CandidateRecord {
    public static final String CANDIDATE_NAME = "Candidate Name";
    public static final String CANDIDATE_EMAIL = "Candidate Email";
    public static final String RESUME_LINK = "Resume Link";

    @JsonProperty("Candidate Name")
    public String name;
    @JsonProperty("Candidate Email")
    public String email;
    @JsonProperty("Resume Link")
    public String resumeLink;

    public <T> Map<String, T> toMap(Class<T> clazz) {
        Map<String, T> map = new HashMap<>();
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                if ("log".equals(field.getName())) {
                    continue;
                }
                Object value = field.get(this);
                if (clazz.isInstance(value)) {
                    JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
                    String key = (jsonProp != null) ? jsonProp.value() : field.getName();
                    map.put(key, clazz.cast(value));
                }
            }
        } catch (Exception e) {
            log.error("Failed to convert object to map: {}", e.getMessage(), e);
        }
        return map;
    }

    public static List<String> getJsonPropertyNames() {
        List<String> propertyNames = new ArrayList<>();
        for (Field field : CandidateRecord.class.getDeclaredFields()) {
            JsonProperty annotation = field.getAnnotation(JsonProperty.class);
            if (annotation != null) {
                propertyNames.add(annotation.value());
            }
        }
        return propertyNames;
    }

    public static String getJsonPropertyValue(String fieldName) throws NoSuchFieldException {
        Field field = CandidateRecord.class.getDeclaredField(fieldName);
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
        return annotation != null ? annotation.value() : fieldName;
    }
}
