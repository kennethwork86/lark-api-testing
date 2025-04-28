package com.dtctest.larkapitesting.constant;

public class LarkConstant {

    public static final String LARK_HOST = "https://open.larksuite.com";
    public static final String LARK_GET_TENANT_TOKEN = "/open-apis/auth/v3/tenant_access_token/internal";
    public static final String LARK_GET_LIST_FOLDER_ITEMS = "/open-apis/drive/v1/files";

    // https://open.larksuite.com/document/server-docs/docs/bitable-v1/app-table-record/create
    public static String LARK_CREATE_TABLE_RECORD(String appToken, String tableId){
        return String.format("/open-apis/bitable/v1/apps/%s/tables/%s/records", appToken, tableId);
    }

    // https://open.larksuite.com/document/server-docs/docs/bitable-v1/app-table-record/update
    public static String LARK_UPDATE_TABLE_RECORD(String appToken, String tableId, String recordId){
        return String.format("/open-apis/bitable/v1/apps/%s/tables/%s/records/%s", appToken, tableId, recordId);
    }

    // https://open.larksuite.com/document/uAjLw4CM/ukTMukTMukTM/reference/bitable-v1/app-table-record/search
    public static String LARK_SEARCH_TABLE_RECORD(String appToken, String tableId){
        return String.format("/open-apis/bitable/v1/apps/%s/tables/%s/records/search", appToken, tableId);
    }

    // https://open.larksuite.com/document/server-docs/docs/bitable-v1/app-table-record/list
    public static String LARK_LIST_TABLE_RECORDS(String appToken, String tableId){
        return String.format("/open-apis/bitable/v1/apps/%s/tables/%s/records", appToken, tableId);
    }



    // https://open.larksuite.com/document/server-docs/docs/drive-v1/event/subscribe
    public static String LARK_SUBSCRIBE_DOCS_EVENTS(String fileToken){
        return String.format("/open-apis/drive/v1/files/%s/subscribe", fileToken);
    }

}
