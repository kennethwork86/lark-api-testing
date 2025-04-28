package com.dtctest.larkapitesting.service;

import com.dtctest.larkapitesting.constant.LarkConstant;
import com.dtctest.larkapitesting.enums.lark.LarkSearchOperatorType;
import com.dtctest.larkapitesting.model.lark.*;
import com.dtctest.larkapitesting.properties.LarkProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.dtc.common.http.HTTP;
import top.dtc.common.util.UrlBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LarkService {

    private List<String> resumeFolderCache = new ArrayList<>();

    private final LarkProperties larkProperties;

    private LarkAppTokenResp larkAppTokenResp;

    private HTTP.Requester getWithAppToken(String token, String subUri) {
        UrlBuilder builder = UrlBuilder.by(LarkConstant.LARK_HOST, subUri);

        return HTTP.get(builder.getFullUrl()).accessToken(token);
    }

    private HTTP.Requester postWithAppToken(String token, String subUri) {
        UrlBuilder builder = UrlBuilder.by(LarkConstant.LARK_HOST, subUri);

        return HTTP.post(builder.getFullUrl()).accessToken(token);
    }

    private HTTP.Requester patchWithAppToken(String token, String subUri) {
        UrlBuilder builder = UrlBuilder.by(LarkConstant.LARK_HOST, subUri);

        return HTTP.patch(builder.getFullUrl()).accessToken(token);
    }

    private HTTP.Requester putWithAppToken(String token, String subUri) {
        UrlBuilder builder = UrlBuilder.by(LarkConstant.LARK_HOST, subUri);

        return HTTP.put(builder.getFullUrl()).accessToken(token);
    }

    //获得lark的appToken，lark的token会对客户端做缓存，只在临近过期的30分钟内，token信息会变化，否则取得的token信息是不会变的
    private LarkAppTokenResp createNewToken() {
        UrlBuilder builder = UrlBuilder.by(LarkConstant.LARK_HOST, LarkConstant.LARK_GET_TENANT_TOKEN);
        return HTTP.post(builder.getFullUrl())
                .field("app_id", larkProperties.appId)
                .field("app_secret", larkProperties.appSecret)
                .asObject(LarkAppTokenResp.class).body();
    }

    //取得titan的appId
    public String getAppId() {
        return larkProperties.appId;
    }

    public LarkAppTokenResp getAppToken() {
        return getAppToken(true);
    }

    //取得当前的larkToken，如果临近过期会自动刷新
    public LarkAppTokenResp getAppToken(boolean useCached) {
        if (useCached) {
            if (larkAppTokenResp == null
                    || larkAppTokenResp.createTime == null
                    || larkAppTokenResp.createTime.plusMinutes(20).isBefore(LocalDateTime.now())
                    || larkAppTokenResp.getExpireTime().isBefore(LocalDateTime.now())) {
                LarkAppTokenResp token = createNewToken();
                token.createTime = LocalDateTime.now();
                larkAppTokenResp = token;
                return token;
            }
            return larkAppTokenResp;
        } else {
            LarkAppTokenResp token = createNewToken();
            token.createTime = LocalDateTime.now();
            larkAppTokenResp = token;
            return token;
        }

    }


    @PostConstruct
    @Scheduled(cron = "0 */5 * * * *")
    @Async
    // 填充角色/用户的缓存列表，每5分钟刷新一次
    public void init() throws NoSuchFieldException {
        String resumeFolderToken = larkProperties.resumeFolderToken;
        String candidateAppToken = larkProperties.candidateAppToken;
        String candidateTableId = larkProperties.candidateTableId;
        List<String> candidateRecordFields = CandidateRecord.getJsonPropertyNames();

        // 抓取所有resume folder
        FileItemsResp fileItemsResp =  getFileItems(resumeFolderToken).data;
        List<FileInfo> fileInfos = fileItemsResp.fileInfos;
        if(fileItemsResp.fileInfos.isEmpty()) return;

        // 抓取所有记录的candidates
        TableRecordItemsResp tableRecordItemsResp = listTableRecords(candidateAppToken, candidateTableId, candidateRecordFields).data;
        List<TableRecordInfo> tableRecordInfos = tableRecordItemsResp.tableRecordInfos;
        if(tableRecordItemsResp.tableRecordInfos.isEmpty()) return;

        //


        Set<String> existingCandidates = tableRecordInfos.stream()
                .map(record -> record.fields.get("Candidate Name").toString())
                .collect(Collectors.toSet());

        for(FileInfo fileInfo : fileInfos) {

        }




        //if new subfolder and file created then add record

        //if the name on the subfolder changed then update record

        //if the email on the subfolder changed then add record

//        log.info("Tenant Access Token: {}", getAppToken().tenantAccessToken);
//        log.info("Lark API Token: {}", larkProperties.resumeFolderToken);
//        LarkResponse<LarkFileItemsResp> response = getFileItems(larkProperties.resumeFolderToken);
//        log.info("resume folder response: {}", response);
//        response.data.larkFileInfos.forEach(larkFileInfo -> {
//            CandidateTableRecord candidateTableRecord = new CandidateTableRecord();
//            candidateTableRecord.candidateEmail = larkFileInfo.name.split("_")[0];
//            candidateTableRecord.candidateName = larkFileInfo.name.split("_")[1];
//            candidateTableRecord.candidateResumeLink = larkFileInfo.url;
//            LarkResponse<String> nextResponse = createCandidateTableRecord(larkProperties.candidateTableId, candidateTableRecord);
//            log.info("candidate table response: {}", nextResponse);
//        });

//        subscribeDocEvents(larkProperties.resumeFolderToken, LarkDocType.FOLDER.desc);
//        subscribeDocEvents("ZLBddtY5ZoQJYVxwn3YuXWn8sfb", LarkDocType.DOCX.desc);

//        TableRecordSearchFilter searchFilter = new TableRecordSearchFilter();
//        searchFilter.conjunction = LarkSearchConjunctionType.AND;
//        TableRecordSearchFilterCondition condition1 = new TableRecordSearchFilterCondition();
//        condition1.fieldName = "Candidate Name";
//        condition1.operator = LarkSearchOperatorType.CONTAINS;
//        condition1.value = new ArrayList<>();
//        condition1.value.add("benjamin@dtcpay.com");
//        TableRecordSearchFilterCondition condition2 = new TableRecordSearchFilterCondition();
//        condition2.fieldName = "Candidate Email";
//        condition2.operator = LarkSearchOperatorType.IS;
//        condition2.value = List.of("Benjamin", "Rong");
//        searchFilter.conditions = new ArrayList<>();
//        searchFilter.conditions.add(condition1);
//        searchFilter.conditions.add(condition2);
//        log.info("searchFilter: {}", searchFilter);
//        log.info("tenant {}", getAppToken().tenantAccessToken);
//        LarkResponse<TableRecordItemsResp> response = searchTableRecord(larkProperties.candidateAppToken, larkProperties.candidateTableId, searchFilter, List.of("Candidate Name", "Candidate Email", "Candidate Id", "Resume Link"));
//        log.info("response: {}", response);

//        Map<String, String> fields = new HashMap<>();
//        fields.put("Candidate Name", "LOLLLL");
//        LarkResponse<String> data = updateTableRecord(larkProperties.candidateAppToken, larkProperties.candidateTableId, "recuJbpL6I4iOt", fields);
//        log.info("data: {}", data);
    }

    private TableRecordSearchFilterCondition mapTableRecordSearchFilterCondition(String fieldName, LarkSearchOperatorType operator, List<String> value){
        TableRecordSearchFilterCondition condition = new TableRecordSearchFilterCondition();
        condition.fieldName = fieldName;
        condition.operator = operator;
        condition.value = value;
        return condition;
    }

    public LarkResponse<FileItemsResp> getFileItems(String folderToken) {
        LarkDictResp resp = getWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_GET_LIST_FOLDER_ITEMS)
                .queryString("folder_token", folderToken)
                .asObject(LarkDictResp.class)
                .body();

        if (resp == null || resp.data == null) return null;
        FileItemsResp response = resp.asObject(FileItemsResp.class);
        return new LarkResponse<>(response);
    }

    public LarkResponse<String> createTableRecord(String appToken, String tableId, Map<String, String> fields) {
        Map<String, Object> body = new HashMap<>();
        body.put("fields", fields);

        LarkDictResp resp = postWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_CREATE_TABLE_RECORD(appToken, tableId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        return getTableRecordId(resp);
    }

    public LarkResponse<String> updateTableRecord(String appToken, String tableId, String recordId, Map<String, String> fields) {
        Map<String, Object> body = new HashMap<>();
        body.put("fields", fields);

        LarkDictResp resp = putWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_UPDATE_TABLE_RECORD(appToken, tableId, recordId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        return getTableRecordId(resp);
    }

    public LarkResponse<TableRecordItemsResp> searchTableRecord(String appToken, String tableId, TableRecordSearchFilter searchFilter, List<String> fieldNames) {
        Map<String, Object> body = new HashMap<>();
        body.put("filter", searchFilter);
        body.put("field_names", fieldNames);

        LarkDictResp resp = postWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_SEARCH_TABLE_RECORD(appToken, tableId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        if (resp == null || resp.data == null) return null;
        TableRecordItemsResp response = resp.asObject(TableRecordItemsResp.class);

        return new LarkResponse<>(response);
    }

    public LarkResponse<TableRecordItemsResp> listTableRecords(String appToken, String tableId, List<String> fieldNames){
        Map<String, Object> body = new HashMap<>();
        body.put("field_names", fieldNames);

        LarkDictResp resp = getWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_LIST_TABLE_RECORDS(appToken, tableId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        if (resp == null || resp.data == null) return null;
        TableRecordItemsResp response = resp.asObject(TableRecordItemsResp.class);

        return new LarkResponse<>(response);
    }


    private LarkResponse<String> getTableRecordId(LarkDictResp resp) {
        if (resp == null || resp.data == null) return null;
        Map<String, Object> dataMap = resp.data;
        if (!dataMap.containsKey("record")) return null;
        Map<?, ?> record = (Map<?, ?>) dataMap.get("record");
        if (!record.containsKey("record_id")) return null;
        String recordId = (String) record.get("record_id");
        return new LarkResponse<>(recordId);
    }
}
