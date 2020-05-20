package com.automaker.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.TextUtil

class UploadPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def sendApkExtension = project.extensions.create('sendApkExtension', DingTalkExtension)
        def buildUploadExtension = project.extensions.create('buildUploadExtension', UploadServerExtension)
        def android = project.extensions.findByType(AppExtension)
        //android.registerTransform(new TestTransform())
        project.afterEvaluate {
            def variants = android.getApplicationVariants()
            variants.each{variant->
                def variantName = variant.buildType.name
                def buildType = buildUploadExtension.buildType
                //如果buildType为空则直接打release，否则打buildType对应的包
                if((buildType != null && variantName.equals(buildType))
                        || (buildType == null && "release".equals(variantName))){
                    println("AutoMakerPlugin==========打包开始")
                    //首字母大写方法 capitalize
                    variantName = "assemble" + variantName.capitalize()
                    println("AutoMakerPlugin=========="+variantName)
                    def assembleTask = project.tasks.findByName(variantName)
                    assembleTask.dependsOn(project.tasks.findByName("clean"))
                    def uploadTask = project.tasks.create("buildApkAndUpload",UploadTask)
                    uploadTask.init(buildUploadExtension,variant,project)
                    uploadTask.dependsOn(assembleTask)

                    def dingTalkTask = project.tasks.create("sendApkToDingTalk",DingTalkTask)
                    dingTalkTask.init(sendApkExtension,variant,project)
                    dingTalkTask.dependsOn(uploadTask)
                }
            }
        }
    }
}