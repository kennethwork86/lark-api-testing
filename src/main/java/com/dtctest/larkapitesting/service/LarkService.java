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

    private final LarkProperties larkProperties;
    private List<FileInfo> resumeFolderCache = new ArrayList<>();
    private boolean firstRun = true;
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
    // 填充角色/用户的缓存列表，每2分钟刷新一次
    public void init() {
        log.info("[START] - HR Lark scheduler");
        String resumeFolderToken = larkProperties.resumeFolderToken;
        String candidateAppToken = larkProperties.candidateAppToken;
        String candidateTableId = larkProperties.candidateTableId;
        List<String> candidateRecordFields = CandidateRecord.getJsonPropertyNames();
        List<FileInfo> resumeFolders;
        List<TableRecordInfo> tableRecordInfos;

        // 1. 基于folder token获取文件夹下的文档清单
        FileItemsResp resumeFolderResp = getFileItems(resumeFolderToken).data;
        resumeFolders = resumeFolderResp.fileInfos;
        if (resumeFolderResp.fileInfos == null) return;

        resumeFolders.removeIf(resumeFolder -> { // 排除不存在简历的文档清单
            FileItemsResp candidateFileResp = getFileItems(resumeFolder.token).data;
            resumeFolder.resumeFile = candidateFileResp.fileInfos.stream().findFirst().orElse(null);
            return resumeFolder.resumeFile == null;
        });

        resumeFolders.removeIf(resumeFolder -> { // 排除不符合规范的命名方式：名字_邮件
            if (resumeFolder.name == null) return true;
            String[] parts = resumeFolder.name.split("_");
            return parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty();
        });

        // 2. 初始会提前缓存目标文档现有候选者纪录，用于省略步骤3对比已存在的候选者纪录
        if (firstRun) {
            // 获取现有的候选者记录
            TableRecordItemsResp tableRecordItemsResp = listTableRecords(candidateAppToken, candidateTableId, candidateRecordFields, null).data;
            tableRecordInfos = tableRecordItemsResp.tableRecordInfos;
            firstRun = false;
        } else {
            resumeFolders = resumeFolders.stream().filter(resumeFolder -> !resumeFolderCache.contains(resumeFolder)).toList();

            // 获取不存在缓存中的简历
            for (FileInfo resumeFolder : resumeFolders) {
                FileItemsResp candidateFileResp = getFileItems(resumeFolder.token).data;
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
                            String name = String.valueOf(record.fields.get(CandidateRecord.CANDIDATE_NAME));
                            String email = String.valueOf(record.fields.get(CandidateRecord.CANDIDATE_EMAIL));
                            return name + "_" + email;
                        },
                        record -> record
                ));

        // 3. 判断排除缓存后，文件夹下的文档清单是否需要更新或插入候选者信息
        for (FileInfo resumeFolder : resumeFolders) {
            TableRecordInfo existingCandidateRecordInfo = existingCandidates.get(resumeFolder.name);
            if (existingCandidateRecordInfo != null) {
                // 不同的resume link：更新记录
                String candidateResumeLink = String.valueOf(existingCandidateRecordInfo.fields.get(CandidateRecord.RESUME_LINK));
                if (resumeFolder.resumeFile != null && !resumeFolder.resumeFile.url.equals(candidateResumeLink)) {
                    existingCandidateRecordInfo.fields.put(CandidateRecord.RESUME_LINK, resumeFolder.resumeFile.url);
                    updateTableRecord(candidateAppToken, candidateTableId, existingCandidateRecordInfo.recordId, existingCandidateRecordInfo.fields);
                }
            } else {
                String[] parts = resumeFolder.name.split("_", 2); // 格式：名字_邮件
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

                if (matchedByName == null && matchedByEmail == null) { // candidate不存在：添加记录
                    CandidateRecord newCandidateRecord = new CandidateRecord();
                    for (String part : parts) {
                        if (part.contains("@")) {
                            newCandidateRecord.email = part;
                        } else {
                            newCandidateRecord.name = part;
                        }
                    }

                    if (resumeFolder.resumeFile == null) continue;
                    newCandidateRecord.resumeLink = resumeFolder.resumeFile.url;
                    createTableRecord(candidateAppToken, candidateTableId, newCandidateRecord.toMap(Object.class));
                } else if (matchedByName != null) { // 不同的email：添加记录
                    matchedByName.fields.put(CandidateRecord.CANDIDATE_EMAIL, emailPart);
                    createTableRecord(candidateAppToken, candidateTableId, matchedByName.fields);
                } else { // 不同的name：更新记录
                    matchedByEmail.fields.put(CandidateRecord.CANDIDATE_NAME, namePart);
                    updateTableRecord(candidateAppToken, candidateTableId, matchedByEmail.recordId, matchedByEmail.fields);
                }
            }
        }

        resumeFolderCache = resumeFolders;
        log.info("[END] - HR Lark scheduler");
    }

    private TableRecordSearchFilterCondition mapTableRecordSearchFilterCondition(String fieldName, LarkSearchOperatorType operator, List<String> value) {
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

        // 插入新纪录
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

    public LarkResponse<TableRecordItemsResp> listTableRecords(String appToken, String tableId, List<String> fieldNames, String query) {
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
        for (FileInfo resumeFolder : resumeFolders) {
            String[] parts = resumeFolder.name.split("_", 2); // [name, email]
            String namePart = parts[0];
            String emailPart = parts.length > 1 ? parts[1] : "";
            filters.put(CandidateRecord.CANDIDATE_NAME, namePart);
            filters.put(CandidateRecord.CANDIDATE_EMAIL, emailPart);
            filters.put(CandidateRecord.RESUME_LINK, resumeFolder.resumeFile.url);
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
