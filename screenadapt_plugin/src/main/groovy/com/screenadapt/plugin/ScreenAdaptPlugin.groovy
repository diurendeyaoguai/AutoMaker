package com.screenadapt.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


class ScreenAdaptPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def screenAdaptExt = project.extensions.create("screenAdaptExtension",ScreenAdaptExtension)
        project.afterEvaluate {
            def screenAdaptTask = project.tasks.create("screenAdapt",ScreenAdaptTask)
            screenAdaptTask.init(project,screenAdaptExt)
        }
    }
}