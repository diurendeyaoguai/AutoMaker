package com.automaker.plugin

import com.android.build.gradle.AppExtension
import com.android.tools.r8.CompilationFailedException
import com.automaker.plugin.bean.MsgContentParam
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.tasks.InputChangesAwareTaskAction
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.util.TextUtil

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class UploadPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def sendApkExtension = project.extensions.create('sendApkExtension', DingTalkExtension)
        def buildUploadExtension = project.extensions.create('buildUploadExtension', UploadServerExtension)
        def android = project.extensions.findByType(AppExtension)
        //android.registerTransform(new TestTransform())

        /**
         * 构建失败发送钉钉消息，需要在taskGraph中
         * 判断是否有我们的sendApkToDingTalk Task
         * 如果不进行判断，那执行所有的task失败都将发送钉钉消息
         *
         * taskGraph 在gradle的配置完成之后会将本次需要执行的task生成有向无环图，存放在taskGraph中
         */
        project.gradle.taskGraph.whenReady {
            TaskExecutionGraph graph->
                //graph.hasTask 判断的是Task的path :app:sendApkToDingTalk
                if(graph.hasTask("${project.path}:sendApkToDingTalk")){
                    def currentTime = System.currentTimeMillis()
                    println("AutoMakerPlugin==========打包开始")
                    project.tasks.findByName("sendApkToDingTalk").doLast {
                        printTaskExecuteTime(System.currentTimeMillis() - currentTime)
                    }
                    project.gradle.buildFinished {BuildResult it->
                        if (it != null && it.failure) {
                            def exceptionDef = it.failure.properties.find {
                                it.key == "reportableCauses"
                            }
                            def exceptionDt = exceptionDef.value[0]
                            //打包异常
                            DingTalkTask dingTalkTask = project.tasks.findByName("sendApkToDingTalk")
                            if(dingTalkTask != null) {
                                dingTalkTask.sendTextMsg("当前打包分支:${dingTalkTask.getGitBranch()}\n打包失败:\n${exceptionDt.toString()}\n${it.failure.getCause()}")
                            }
                        }
                    }
                }
        }

        project.afterEvaluate {
            def variants = android.getApplicationVariants()
            variants.each { variant ->
                def variantName = variant.buildType.name
                def buildType = buildUploadExtension.buildType
                //如果buildType为空则直接打release，否则打buildType对应的包
                if ((buildType != null && variantName.equals(buildType))
                        || (buildType == null && "release".equals(variantName))) {
                    //首字母大写方法 capitalize
                    variantName = "assemble" + variantName.capitalize()
                    def assembleTask = project.tasks.findByName(variantName)
                    assembleTask.dependsOn(project.tasks.findByName("clean"))
                    def uploadTask = project.tasks.create("buildApkAndUpload", UploadTask)
                    uploadTask.init(buildUploadExtension, variant, project)
                    uploadTask.dependsOn(assembleTask)

                    def dingTalkTask = project.tasks.create("sendApkToDingTalk", DingTalkTask)
                    dingTalkTask.init(sendApkExtension, variant, android.defaultConfig.versionName)
                    dingTalkTask.dependsOn(uploadTask)
                }
            }
        }
    }

    /**
     * 时间转换
     * @param mss
     */
    void printTaskExecuteTime(long time){
            long minutes = time / (1000 * 60)
            long seconds = (time % (1000 * 60)) / 1000
            println("AutoMakerPlugin==========执行时间：${minutes}分${seconds}秒")
    }
}