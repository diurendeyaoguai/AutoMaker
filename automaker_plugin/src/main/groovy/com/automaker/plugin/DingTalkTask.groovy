package com.automaker.plugin

import com.android.build.gradle.api.BaseVariant
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.TimeUnit

class DingTalkTask extends DefaultTask {
    private DingTalkExtension mExtension
    private BaseVariant mVariant
    private Project mProject
    private static String mUrl
    private static String mFileName

    DingTalkTask() {
        setGroup("quickRelease")
    }

    void init(DingTalkExtension extension, BaseVariant variant, Project project) {
        this.mExtension = extension
        this.mVariant = variant
        this.mProject = project
    }

    static void setUrl(String url, String fileName) {
        this.mUrl = url
        this.mFileName = fileName
    }

    @TaskAction
    void sendMsgToDingTalk() {
        println("UploadPlugin==========发送钉钉消息,access_token=${mExtension.access_token}")
        def url = "https://oapi.dingtalk.com/robot/send?access_token=" + mExtension.access_token

        def builder = new OkHttpClient.Builder()
        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .writeTimeout(10000, TimeUnit.MILLISECONDS)
        def okHttpClient = builder.build()

        def contentText = "{\n" +
                "    \"msgtype\": \"link\", \n" +
                "    \"link\": {\n" +
                "        \"text\": \"$mFileName 点击下载链接下载安装包\", \n" +
                "        \"title\": \"下载链接\", \n" +
                "        \"picUrl\": \"$mExtension.picUrl\", \n" +
                "        \"messageUrl\": \"$mUrl\"\n" +
                "    }\n" +
                "}"
        def requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), contentText)
        def request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        def response = okHttpClient.newCall(request).execute()
        if (response == null || response.body() == null) {
            println("UploadPlugin==========发送钉钉消息失败")
            return
        }
        def json = response.body().string()
        println("UploadPlugin==========发送钉钉接口返回:${json}")
    }
}