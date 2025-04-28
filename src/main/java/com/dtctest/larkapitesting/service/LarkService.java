package com.dtctest.larkapitesting.service;

import com.dtctest.larkapitesting.constant.LarkConstant;
import com.dtctest.larkapitesting.enums.lark.LarkSearchConjunctionType;
import com.dtctest.larkapitesting.enums.lark.LarkSearchOperatorType;
import com.dtctest.larkapitesting.model.lark.*;
import com.dtctest.larkapitesting.properties.LarkProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

    private List<FileInfo> resumeFolderCache = new ArrayList<>();

    private boolean firstRun = true;

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
    @Scheduled(cron = "0 */2 * * * *")
    @Async
    // 填充角色/用户的缓存列表，每5分钟刷新一次
    public void init() {
        log.info("[START] - HR Lark scheduler");
        String resumeFolderToken = larkProperties.resumeFolderToken;
        String candidateAppToken = larkProperties.candidateAppToken;
        String candidateTableId = larkProperties.candidateTableId;
        List<String> candidateRecordFields = CandidateRecord.getJsonPropertyNames();
        List<FileInfo> resumeFolders;
        List<TableRecordInfo> tableRecordInfos;

        // 抓取所有resume folder
        FileItemsResp resumeFolderResp =  getFileItems(resumeFolderToken).data;
        resumeFolders = resumeFolderResp.fileInfos;
        if(resumeFolderResp.fileInfos == null) return;

        // 抓取candidate file
        resumeFolders.removeIf(resumeFolder -> {
            FileItemsResp candidateFileResp = getFileItems(resumeFolder.token).data;
            resumeFolder.resumeFile = candidateFileResp.fileInfos.stream().findFirst().orElse(null);
            return resumeFolder.resumeFile == null;
        });

        if(firstRun){ // 第一次会把resume cache起来，之后就不用对比已存起在的resume
            // 抓取所有记录的candidates
            TableRecordItemsResp tableRecordItemsResp = listTableRecords(candidateAppToken, candidateTableId, candidateRecordFields, null).data;
            tableRecordInfos = tableRecordItemsResp.tableRecordInfos;
        }else{
            resumeFolders = resumeFolders.stream().filter(resumeFolder -> !resumeFolderCache.contains(resumeFolder)).toList();

            // 抓取candidate file
            for(FileInfo resumeFolder : resumeFolders) {
                FileItemsResp candidateFileResp =  getFileItems(resumeFolder.token).data;
                resumeFolder.resumeFile = candidateFileResp.fileInfos.stream().findFirst().orElse(null);
            }

            Map<String, String> filters = mapCandidateResumeFilter(resumeFolders);
            String query = queryBuilder(LarkSearchConjunctionType.OR, filters);

            // 抓取有相关candidates的name/email/resume link
            TableRecordItemsResp tableRecordItemsResp = listTableRecords(candidateAppToken, candidateTableId, candidateRecordFields, query).data;
            tableRecordInfos = tableRecordItemsResp.tableRecordInfos;
        }

        Map<String, TableRecordInfo> existingCandidates = Optional.ofNullable(tableRecordInfos)
                .orElse(Collections.emptyList())
                .stream()
                .filter(record -> record.fields != null)
                .collect(Collectors.toMap(
                        record -> {
                            String name = String.valueOf(record.fields.get("Candidate Name"));
                            String email = String.valueOf(record.fields.get("Candidate Email"));
                            return name + "_" + email;
                        },
                        record -> record
                ));

        for(FileInfo resumeFolder : resumeFolders) {
            TableRecordInfo candidateRecordInfo = existingCandidates.get(resumeFolder.name);
            if(candidateRecordInfo != null) {
                // 不同的resume link：更新记录
                String candidateResumeLink = String.valueOf(candidateRecordInfo.fields.get("Resume Link"));
                if(resumeFolder.resumeFile != null && !resumeFolder.resumeFile.url.equals(candidateResumeLink)){
                    candidateRecordInfo.fields.put("Resume Link", resumeFolder.resumeFile.url);
                    updateTableRecord(candidateAppToken, candidateTableId, candidateRecordInfo.recordId, candidateRecordInfo.fields);
                }
            }else{
                String[] parts = resumeFolder.name.split("_", 2); // [name, email]
                String namePart = parts[0];
                String emailPart = parts.length > 1 ? parts[1] : "";

                TableRecordInfo matchedByName = existingCandidates.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(namePart + "_"))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);

                TableRecordInfo matchedByEmail = existingCandidates.entrySet().stream()
                        .filter(entry -> entry.getKey().endsWith("_" + emailPart))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);

                if(matchedByName == null && matchedByEmail == null) { // candidate不存在：添加记录
                    CandidateRecord newCandidateRecord = new CandidateRecord();
                    newCandidateRecord.name = resumeFolder.name.split("_")[0];
                    newCandidateRecord.email = resumeFolder.name.split("_")[1];
                    if(resumeFolder.resumeFile == null) continue;
                    newCandidateRecord.resumeLink = resumeFolder.resumeFile.url;
                    createTableRecord(candidateAppToken, candidateTableId, newCandidateRecord.toMap(Object.class));
                }else if(matchedByName != null) { // 不同的email：添加记录
                    matchedByName.fields.put("Candidate Email", emailPart);
                    createTableRecord(candidateAppToken, candidateTableId, matchedByName.fields);
                }else if(matchedByEmail != null) { // 不同的name：更新记录
                    matchedByEmail.fields.put("Candidate Name", namePart);
                    updateTableRecord(candidateAppToken, candidateTableId, matchedByEmail.recordId, matchedByEmail.fields);
                }
            }
        }

        firstRun = false;
        resumeFolderCache = resumeFolders;
        log.info("[END] - HR Lark scheduler");
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

    public LarkResponse<String> createTableRecord(String appToken, String tableId, Map<String, Object> fields) {
        Map<String, Object> body = new HashMap<>();
        body.put("fields", fields);

        LarkDictResp resp = postWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_CREATE_TABLE_RECORD(appToken, tableId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        return getTableRecordId(resp);
    }

    public LarkResponse<String> updateTableRecord(String appToken, String tableId, String recordId, Map<String, Object> fields) {
        Map<String, Object> body = new HashMap<>();
        body.put("fields", fields);

        LarkDictResp resp = putWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_UPDATE_TABLE_RECORD(appToken, tableId, recordId))
                .bodyJson(body)
                .asObject(LarkDictResp.class).body();

        return getTableRecordId(resp);
    }

    public LarkResponse<TableRecordItemsResp> listTableRecords(String appToken, String tableId, List<String> fieldNames, String query){
        HTTP.Requester requester = getWithAppToken(getAppToken().tenantAccessToken, LarkConstant.LARK_LIST_TABLE_RECORDS(appToken, tableId));
        if (query != null) {
            requester.queryString("query", query);
        }

        LarkDictResp resp = requester
                .queryString("field_names", convertListToJsonArray(fieldNames))
                .asObject(LarkDictResp.class)
                .body();

        if (resp == null || resp.data == null) return null;
        TableRecordItemsResp response = resp.asObject(TableRecordItemsResp.class);

        return new LarkResponse<>(response);
    }

    public String queryBuilder(LarkSearchConjunctionType conjunction, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();
            joiner.add("CurrentValue.[" + field + "] = \"" + value + "\"");
        }

        return conjunction.name() + "(" + joiner + ")";
    }

    @NotNull
    private static Map<String, String> mapCandidateResumeFilter(List<FileInfo> resumeFolders) {
        Map<String, String> filters = new HashMap<>();
        for(FileInfo resumeFolder : resumeFolders) {
            String[] parts = resumeFolder.name.split("_", 2); // [name, email]
            String namePart = parts[0];
            String emailPart = parts.length > 1 ? parts[1] : "";
            filters.put("Candidate Name", namePart);
            filters.put("Candidate Email", emailPart);
            filters.put("Resume Link", resumeFolder.resumeFile.url);
        }
        return filters;
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

    private String convertListToJsonArray(List<String> fieldNames) {
        return fieldNames.stream()
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
