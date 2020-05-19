package com.automaker.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class UploadPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('talkExtension', DingTalkExtension)
        project.extensions.create('serverExtension', UploadServerExtension)
        def android = project.extensions.findByType(AppExtension)
        //android.registerTransform(new TestTransform())
        project.afterEvaluate {
            def variants = android.getApplicationVariants()
            variants.each{variant->
                println(variant.buildType.name)
                def variantName = variant.buildType.name
                if("release".equals(variantName)){
                    println("UploadPlugin==========打包开始")
                    //首字母大写方法 capitalize
                    variantName = "assemble" + variantName.capitalize()
                    println("UploadPlugin=========="+variantName)
                    def assembleTask = project.tasks.findByName(variantName)
                    assembleTask.dependsOn(project.tasks.findByName("clean"))
                    def uploadTask = project.tasks.create("uploadRelease",UploadTask)
                    uploadTask.init(project.serverExtension,variant,project)
                    uploadTask.dependsOn(assembleTask)

                    def dingTalkTask = project.tasks.create("dingTalkTask",DingTalkTask)
                    dingTalkTask.init(project.talkExtension,variant,project)
                    dingTalkTask.dependsOn(uploadTask)
                }/*else{
                    //buildType debug
                }*/
            }
        }
    }
}