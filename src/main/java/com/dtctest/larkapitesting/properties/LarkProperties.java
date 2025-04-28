package com.dtctest.larkapitesting.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LarkProperties {

    @Value("${HR_LARK_APP_ID}")
    public String appId;

    @Value("${HR_LARK_APP_SECRET}")
    public String appSecret;

    @Value("${HR_LARK_APP_RESUME_FOLDER_TOKEN}")
    public String resumeFolderToken;

    @Value("${HR_LARK_APP_CANDIDATE_APP_TOKEN}")
    public String candidateAppToken;

    @Value("${HR_LARK_APP_CANDIDATE_TABLE_ID}")
    public String candidateTableId;

}
