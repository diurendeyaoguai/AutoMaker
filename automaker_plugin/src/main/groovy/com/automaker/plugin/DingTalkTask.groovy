package com.automaker.plugin

import com.android.build.gradle.api.BaseVariant
import com.automaker.plugin.bean.BaseMsgParam
import com.automaker.plugin.bean.DingTalkResponse
import com.automaker.plugin.bean.LinkMsgParam
import com.automaker.plugin.bean.MarkdownMsgParam
import com.automaker.plugin.bean.MsgContentParam
import com.automaker.plugin.bean.TextMsgParam
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class DingTalkTask extends DefaultTask {
    private DingTalkExtension mExtension
    private BaseVariant mVariant
    private static String mUrl
    private static String mFileName
    private static String mVersionName
    //钉钉机器人，keyword，必须与要发送的钉钉机器人keyword保持一致
    private static String mKeyWord = "打包通知"
    //默认二维码服务器
    private static String mDefaultPicUrl = "http://qr.liantu.com/api.php?text="

    DingTalkTask() {
        setGroup("automaker")
    }

    void init(DingTalkExtension extension, BaseVariant variant, String versionName) {
        this.mExtension = extension
        this.mVariant = variant
        this.mVersionName = versionName
        if (mExtension.linkPicUrl != null) {
            mDefaultPicUrl = mExtension.linkPicUrl
        }
        if(mExtension.dingKeyword != null){
            mKeyWord = mExtension.dingKeyword
        }
    }

    static void setUrl(String url, String fileName) {
        this.mUrl = url
        this.mFileName = fileName
    }

    @TaskAction
    void sendMsgToDingTalk() {
        def date = new Date()
        def dateFormat = new SimpleDateFormat("MM-dd hh:mm:ss")
        //sendMarkDownMsg("#### **打包通知** \n ##### 当前打包分支:${getGitBranch()} \n ##### 版本号:${mVersionName} \n ![screenshot]($mDefaultPicUrl$mUrl)\n ##### ${dateFormat.format(date)} [手动下载]($mUrl) \n")

        //sendLinkMsg("$mFileName 点击下载链接下载安装包",mExtension.picUrl,mUrl)
    }

    void sendLinkMsg(String text, String picUrl, String messageUrl) {
        def linkMsgParam = new LinkMsgParam()
        linkMsgParam.msgtype = "link"
        def msgContentParam = new MsgContentParam()
        msgContentParam.title = mKeyWord
        msgContentParam.text = text
        msgContentParam.picUrl = picUrl
        msgContentParam.messageUrl = messageUrl
    }

    void sendMarkDownMsg(String text) {
        def markdownParam = new MarkdownMsgParam()
        markdownParam.msgtype = "markdown"
        def markdownMsgContent = new MsgContentParam()
        markdownMsgContent.title = mKeyWord
        markdownMsgContent.text = text
        markdownParam.markdown = markdownMsgContent
        sendBaseMsg(markdownParam)
    }

    void sendTextMsg(String content) {
        def textMsgParam = new TextMsgParam()
        textMsgParam.msgtype = "text"
        def textMsgContent = new MsgContentParam()
        textMsgContent.content = mKeyWord + "\n" + content
        textMsgParam.text = textMsgContent
        sendBaseMsg(textMsgParam)
    }

    void sendBaseMsg(BaseMsgParam baseMsgParam) {
        def builder = new OkHttpClient.Builder()
        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .writeTimeout(10000, TimeUnit.MILLISECONDS)
        def okHttpClient = builder.build()

        def gson = new GsonBuilder().serializeNulls().create()
        println(gson.toJson(baseMsgParam))

        def requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(baseMsgParam))
        mExtension.access_token.each { token ->
            println("AutoMakerPlugin==========发送钉钉消息,access_token=${token}")
            def url = "https://oapi.dingtalk.com/robot/send?access_token=${token}"

            def request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
            def response = okHttpClient.newCall(request).execute()
            if (response == null || response.body() == null) {
                println("AutoMakerPlugin==========发送钉钉消息失败")
                return
            }
            def json = response.body().string()
            //def dingTalkResponse = gson.fromJson(json, DingTalkResponse.class)
            println("AutoMakerPlugin==========发送钉钉接口返回:${json}")
        }
    }

    def getGitBranch() {
        return 'git symbolic-ref --short -q HEAD'.execute().text.trim()
    }
}