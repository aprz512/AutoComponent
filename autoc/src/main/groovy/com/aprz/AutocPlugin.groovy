package com.aprz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.commons.io.FileUtils

/**
 * TODO : 加上更加详细的注释，可以仿照bilibil的弹幕加载
 */
class AutocPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "project(${project.name}) apply ${ProjectModuleManager.PLUGIN_NAME} plugin"
        def mainApp = ProjectModuleManager.manageModule(project)
        performBuildTypeCache(project, mainApp)
    }

    private static void performBuildTypeCache(Project project, boolean isApp) {
        if (!RegisterCache.isSameAsLastBuildType(project, isApp)) {
            RegisterCache.cacheBuildType(project, isApp)
            println ">>准备删除缓存目录......"
            //兼容gradle3.0以上组件独立运行时出现的问题：https://github.com/luckybilly/CC/issues/62
            //切换app/lib编译时，将缓存目录清除
            def cachedJniFile = project.file("build/intermediates/transforms/")
            def cachedSourceFile = project.file("build/generated/source/")
            if (cachedJniFile && cachedJniFile.exists() && cachedJniFile.isDirectory()) {
                FileUtils.deleteDirectory(cachedJniFile)
            }
            if (cachedSourceFile && cachedSourceFile.exists() && cachedSourceFile.isDirectory()) {
                FileUtils.deleteDirectory(cachedSourceFile)
            }
            println ">>删除缓存目录完成......"
        }
    }

}
