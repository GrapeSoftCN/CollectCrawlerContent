package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import apps.appsProxy;
import httpServer.grapeHttpUnit;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;
import offices.excelHelper;
import privacyPolicy.privacyPolicy;
import rpc.execRequest;
import security.codec;
import string.StringHelper;

/**
 * 爬虫回调函数，存在隐私数据，则将该条数据存入数据库
 * 
 * 导出数据库中的隐私数据信息
 *
 */
public class CollectionContent {
    private GrapeTreeDBModel group;
    private GrapeDBSpecField gDbSpecField;

    public CollectionContent() {
        group = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("ContentLog"));
        group.descriptionModel(gDbSpecField);
        group.bind();
    }

    /**
     * 爬虫数据收集
     * 
     * @param columnName
     * @return
     */
    public String ScanContent(String columnName) {
        String info = "";
        // 获取post参数
        JSONObject object = JSONObject.toJSON(execRequest.getChannelValue(grapeHttpUnit.formdata).toString());
        if (object != null && object.size() > 0) {
            info = object.getString("param");
        }
        return ScanContent(columnName, info);
    }

    /**
     * 爬虫数据收集
     * 
     * @param columnName
     * @param info
     *            {"content_0":{"url":"","content":""},"title_0":{"url":"",
     *            "content":""}}
     * @return
     */
    @SuppressWarnings("unchecked")
    public String ScanContent(String columnName, String info) {
        String key, contentInfo, urls = "", temp;
        JSONObject tempjson, titlejson = new JSONObject();
        privacyPolicy pp = new privacyPolicy();
        info = codec.DecodeFastJSON(info);
        JSONObject object = JSONObject.toJSON(info);
        if (object == null || object.size() <= 0) {
            return rMsg.netMSG(false, "爬取的页面数据无效");
        }
        for (Object object2 : object.keySet()) {
            key = object2.toString();
            tempjson = object.getJson(key); // {"url":"","content":""}
            if (tempjson != null && tempjson.size() > 0) {
                // 获取title字段，重组为{"url":"title"}
                if (key.contains("title")) {
                    titlejson.put(tempjson.getString("url"), tempjson.getString("content"));
                }
                // 获取content字段，判断是否存在隐私数据，含有隐私数据，则保存对应url
                if (key.contains("content")) {
                    contentInfo = tempjson.getString("content");
                    contentInfo = pp.scanText(contentInfo);
                    if (pp.hasPrivacyPolicy()) { // 获取含有隐私信息的url
                        temp = tempjson.getString("url");
                        if (StringHelper.InvaildString(temp) && !urls.contains(temp)) {
                            urls += temp + ",";
                        }
                    }
                }
            }
        }
        return AddData(urls, titlejson, columnName);
    }

    /**
     * 将隐私数据添加至数据库
     * 
     * @param urls
     * @param titlejson
     * @param fileName
     * @return
     */
    private String AddData(String urls, JSONObject titlejson, String columnName) {
        String title = "";
        Object info = null;
        String result = rMsg.netMSG(false, "");
        // 将隐私信息数据添加至数据库，url，title
        if (StringHelper.InvaildString(urls) && titlejson != null && titlejson.size() > 0) {
            String[] value = urls.split(",");
            for (String string : value) {
                if (StringHelper.InvaildString(string)) {
                    title = titlejson.getString(string);
                    if (contentIsExist(title, string, columnName)) {
                        info = group.data((new JSONObject("title", title)).puts("url", string).puts("columnName", columnName)).autoComplete().insertOnce();
                    }
                }
            }
        } else {
            return rMsg.netMSG(true, "不存在隐私数据");
        }
        return info != null ? rMsg.netMSG(true, "") : result;
    }

    /**
     * 验证库中是否已存在该条数据
     * @param title
     * @param url
     * @param columnName
     * @return
     */
    private boolean contentIsExist(String title,String url,String columnName) {
        JSONObject object = null;
        object = group.eq("title", title).eq("url", url).eq("columnName", columnName).find();
        return object==null || object.size() <= 0;
    }
    /**
     * 导出隐私数据
     * 
     * @param fileName
     * @return
     */
    public Object Export(String fileName) {
        JSONArray array = null;
        try {
            array = group.select();
            if (array == null || array.size() <= 0) {
                return rMsg.netMSG(1, "未检查到隐私文章");
            }
            return excelHelper.out(array.toJSONString());
        } catch (Exception e) {
            nlogger.logout(e);
        }
        return rMsg.netMSG(100, "导出异常");
    }
}
