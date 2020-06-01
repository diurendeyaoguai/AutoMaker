package com.screenadapt.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.AndroidSourceSet
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.text.DecimalFormat

class ScreenAdaptTask extends DefaultTask {
    Project mProject
    ScreenAdaptExtension mExtension
    Double baseDp = 360
    List<Double> matchDp = [384, 392, 400, 410, 411, 432, 454, 480, 533, 592, 600, 640, 662, 720]
    String dimenPath = "/values/dimens.xml"
    String resPath
    String absoluteDimenPath
    DecimalFormat df = new DecimalFormat("#.0000")
    List<File> oldValuesDir//原有的适配目录

    ScreenAdaptTask() {
        setGroup("automaker")
    }

    void init(Project project, ScreenAdaptExtension extension) {
        this.mProject = project
        this.mExtension = extension

    }

    private void initParam() {
        //获取Android默认res目录
        //com.android.application AppPlugin 对应extension为android类型为AppExtension
        //com.android.library LibraryPlugin 对应extension为android类型为LibraryExtension
        def android = project.extensions.findByName("android")
        android.sourceSets.each { AndroidSourceSet set ->
            if (set.name == "main") {
                resPath = set.res.srcDirs[0].absolutePath
                absoluteDimenPath = resPath + dimenPath
                println("screenadapt==========默认dimens文件路径：\n${absoluteDimenPath}")
            }
        }

        //根据extension配置获取参数
        if (mExtension != null) {
            baseDp = mExtension.baseDp != null
                    ? mExtension.baseDp : baseDp
            matchDp = mExtension.matchDp != null
                    ? mExtension.matchDp : matchDp
            println("screenadapt==========基准Dp:${baseDp}\n适配Dp：${matchDp}")
            if (mExtension.resPath != null) {
                resPath = project.projectDir.absolutePath + mExtension.resPath
                absoluteDimenPath = resPath + dimenPath
                println("screenadapt==========指定dimens文件路径：\n${absoluteDimenPath}")
            }
        }

        //加载已经适配过的目录，目的是为了分辨是否有已经适配过的目录不在此次配置中
        FileFilter fileFilter = new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.startsWith("values-w")
            }
        }
        oldValuesDir = new File(resPath).listFiles(fileFilter).toList()
    }

    @TaskAction
    void adaptScreen() {
        //配置参数
        initParam()

        def file = new File(absoluteDimenPath)
        if (!file.exists()) {
            throw new GroovyRuntimeException("没有找到${file.absolutePath}文件")
        }
        def xmlParser = new XmlSlurper()
        try {
            def resources = xmlParser.parse(file)
            //println(oldValuesDir.toString())
            matchDp.each {
                //获取当前dp除以baseDP后的倍数
                def multiple = it / baseDp

                //创建适配目录以及文件
                def fileDirTemp = new File(resPath + "/values-w${it}dp")
                if (!fileDirTemp.exists()) {
                    fileDirTemp.mkdirs()
                }
                def fileTemp = new File(fileDirTemp, "dimens.xml")
                if (!fileTemp.exists()) {
                    fileTemp.createNewFile()
                }

                //移除存在配置中的适配目录
                if (oldValuesDir != null) {
                    def result = oldValuesDir.find {
                        it.name == fileDirTemp.name
                    }
                    if (result != null) {
                        oldValuesDir.remove(result)
                    }
                }

                //根据dimens.xml生成适配文件
                def makeUpBuilder = new MarkupBuilder(fileTemp.newPrintWriter())
                makeUpBuilder.resources() {
                    resources.dimen.each {
                        def tempValue
                        if (it.toString().contains("dp")) {
                            tempValue = it.toString() - "dp"
                            if (!tempValue.isDouble()) {
                                makeUpBuilder.dimen(name: it.@name.toString(), it.toString())
                            } else {
                                def tempDoubleValue = tempValue.toDouble()
                                makeUpBuilder.dimen(name: it.@name.toString(), "${df.format(tempDoubleValue * multiple)}dp")
                            }
                        } else if (it.toString().contains("sp")) {
                            tempValue = it.toString() - "sp"
                            if (!tempValue.isDouble()) {
                                makeUpBuilder.dimen(name: it.@name.toString(), it.toString())
                            } else {
                                def tempDoubleValue = tempValue.toDouble()
                                makeUpBuilder.dimen(name: it.@name.toString(), "${df.format(tempDoubleValue * multiple)}sp")
                            }
                        } else {
                            throw new GroovyRuntimeException("dimen值不正确${it.@name.toString()}")
                        }

                    }
                }
            }

            if (oldValuesDir != null && oldValuesDir.size() > 0) {
                oldValuesDir.each {
                    println("screenadapt==========原有适配目录${it.name}未在此次配置中，可能存在适配问题")
                }
            }

            println("screenadapt==========已生成适配dimens文件")
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
