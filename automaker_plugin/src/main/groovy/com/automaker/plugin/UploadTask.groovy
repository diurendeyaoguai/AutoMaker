package com.automaker.plugin

import com.android.build.gradle.api.BaseVariant
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.TimeUnit

class UploadTask extends DefaultTask {
    BaseVariant mVariant
    Project mProject
    UploadServerExtension mExtension

    UploadTask() {
        setGroup("automaker")
    }

    void init(UploadServerExtension extension, BaseVariant variant, Project project) {
        this.mVariant = variant
        this.mProject = project
        this.mExtension = extension
    }

    @TaskAction
    void uploadApk() {
        println("AutoMakerPlugin==========" + "打包结束")
        mVariant.outputs.each { output ->
            def file = new File(mVariant.getPackageApplicationProvider().get().outputDirectory, output.apkData.outputFileName)
            if (file.exists()) {
                uploadFile(file)
            }
        }
    }

    void uploadFile(File file) {
        if (mExtension.url == null) {
            throw new GroovyRuntimeException('''
            文件服务器不能为空，请在build.gradle里配置如下字段
            buildUploadExtension {
               //文件上传服务器
              url "http://xxxx"
            }
            ''')
        }
        println("AutoMakerPlugin==========" + file)
        println("AutoMakerPlugin==========" + "正在上传,上传地址${mExtension.url},文件大小${file.size()}")
        def builder = new OkHttpClient.Builder()
        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .writeTimeout(10000, TimeUnit.MILLISECONDS)
        def okHttpClient = builder.build()

        def requestBody = RequestBody.create(MediaType.parse("*/*"), file.getBytes())

        /*mExtension.param.each {mapValue->
            println("=========================")
            println("key:"+mapValue.key+"    value:"+mapValue.value)
        }*/

        def request = new Request.Builder()
                .url(mExtension.url)
                .post(requestBody)
                .build()

        def response = okHttpClient.newCall(request).execute()

        if (response == null || response.body() == null) {
            println("AutoMakerPlugin==========apk上传失败")
            return
        }
        def json = response.body().string()
        println("AutoMakerPlugin==========apk上传接口返回:\n ${json}")
        parseResult(json, file.name)
        response.close()
    }

    private void parseResult(String result, String fileName) {
        UploadEntity data = new GsonBuilder().serializeNulls().create().fromJson(result, UploadEntity.class)
        //def data = new Gson().fromJson(result, LinkedHashMap.class)
        if (data == null) {
            DingTalkTask dingTalkTask = project.tasks.findByName("sendApkToDingTalk")
            if (dingTalkTask != null) {
                dingTalkTask.sendTextMsg("当前打包分支:${dingTalkTask.getGitBranch()}\n上传失败:\n${result}")
            }
            throw new IllegalArgumentException("AutoMakerPlugin==========上传失败")
        }
        DingTalkTask.setUrl(data.url, fileName)
    }
}