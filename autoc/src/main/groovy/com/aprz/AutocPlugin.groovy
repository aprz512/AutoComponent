package com.aprz

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 插件：用于组件化工程的自动管理
 */
class AutocPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "AUTOC >>>> 正在准备扫描..."
        println "AUTOC >>>> 外部电源接触...没有异常"
        println "AUTOC >>>> 连通率 100%"
        println "AUTOC >>>> 思考形态以中文作为基准，进行思维连接...连接没有异常"
        println "AUTOC >>>> 同步率为 100%"
        println "AUTOC >>>> 交互界面连接..."
        println "AUTOC >>>> 安全装置接触..."
        println "AUTOC >>>> 移往扫描端口..."
        println "AUTOC >>>> 检测到工程 (${project.name})，往扫描端口注入 ${ProjectModuleManager.PLUGIN_NAME} 插件..."
        def mainApp = ProjectModuleManager.manageModule(project)
        performBuildTypeCache(project, mainApp)
    }

    private static void performBuildTypeCache(Project project, boolean isApp) {
        if (!RegisterCache.isSameAsLastBuildType(project, isApp)) {
            deleteCache(project)
            RegisterCache.cacheBuildType(project, isApp)
        }
    }

    /**
     * 兼容gradle3.0以上组件独立运行时出现的问题：https://github.com/luckybilly/CC/issues/62
     * 切换app/lib编译时，将缓存目录清除
     * @param project
     */
    private static void deleteCache(Project project) {
        println "AUTOC >>>> 检测到编译类型不一致，准备清空缓存..."
        def cachedJniFile = project.file("build/intermediates/transforms/")
        def cachedSourceFile = project.file("build/generated/source/")
        if (cachedJniFile && cachedJniFile.exists() && cachedJniFile.isDirectory()) {
            FileUtils.deleteDirectory(cachedJniFile)
        }
        if (cachedSourceFile && cachedSourceFile.exists() && cachedSourceFile.isDirectory()) {
            FileUtils.deleteDirectory(cachedSourceFile)
        }
        println "AUTOC >>>> 清空缓存完毕，完成率 100%"
    }

}
