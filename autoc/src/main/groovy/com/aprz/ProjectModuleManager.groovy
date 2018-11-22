package com.aprz


import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * 工程中的组件module管理工具
 * 1. 用于管理组件module以application或library方式进行编译
 * 2. 组件默认按照Application的方式编译，如果运行主工程，则主工程依赖的其他组件会默认按照library的方式编译
 *      省去了切换导致重新编译的时间。
 * 3. 组件的配置也更简单了，不需要手动设置debug的目录集
 */
class ProjectModuleManager {

    public static final String PLUGIN_NAME = 'AUTOC'

    //组件单独以app方式运行时使用的测试代码所在目录(manifest/java/assets/res等),这个目录下的文件不会打包进主app
    static final String DEBUG_DIR = "src/main/debug/"
    //主app，一直以application方式编译
    static final String MODULE_MAIN_APP = "mainApp" 


    static String mainModuleName
    static boolean taskIsAssemble

    static boolean manageModule(Project project) {
        taskIsAssemble = false
        mainModuleName = null
        Properties localProperties = new Properties()
        try {
            def localFile = project.rootProject.file('local.properties')
            if (localFile != null && localFile.exists()) {
                localProperties.load(localFile.newDataInputStream())
            }
        } catch (Exception ignored) {
            println("${PLUGIN_NAME}: local.properties not found")
        }
        initByTask(project)

        def mainApp = isMainApp(project)
        def assembleFor = isAssembleFor(project)

        boolean runAsApp = false
        if (mainApp || assembleFor || !taskIsAssemble) {
            runAsApp = true
        }
        project.ext.runAsApp = runAsApp
        println "${PLUGIN_NAME}: project=${project.name}, runAsApp=${runAsApp} . taskIsAssemble:${taskIsAssemble}. "
        if (runAsApp) {
            project.apply plugin: 'com.android.application'

            project.android.sourceSets.main {
                //debug模式下，如果存在src/main/debug/AndroidManifest.xml，则自动使用其作为manifest文件
                def debugManifest = "${DEBUG_DIR}AndroidManifest.xml"
                if (project.file(debugManifest).exists()) {
                    manifest.srcFile debugManifest
                }
                //debug模式下，如果存在src/main/debug/assets，则自动将其添加到assets源码目录
                if (project.file("${DEBUG_DIR}assets").exists()) {
                    assets.srcDirs = ['src/main/assets', "${DEBUG_DIR}assets"]
                }
                //debug模式下，如果存在src/main/debug/java，则自动将其添加到java源码目录
                if (project.file("${DEBUG_DIR}java").exists()) {
                    java.srcDirs = ['src/main/java', "${DEBUG_DIR}java"]
                }
                //debug模式下，如果存在src/main/debug/res，则自动将其添加到资源目录
                if (project.file("${DEBUG_DIR}res").exists()) {
                    res.srcDirs = ['src/main/res', "${DEBUG_DIR}res"]
                }
            }
        } else {
            project.apply plugin: 'com.android.library'
        }
        //为build.gradle添加addComponent方法
        addComponentMethod(project, localProperties)
        return runAsApp
    }

    //需要集成打包相关的task
    static final String TASK_TYPES = ".*((((ASSEMBLE)|(BUILD)|(INSTALL)|((BUILD)?TINKER)|(RESGUARD)).*)|(ASR)|(ASD))"
    static void initByTask(Project project) {
        def taskNames = project.gradle.startParameter.taskNames
        def allModuleBuildApkPattern = Pattern.compile(TASK_TYPES)
        for (String task : taskNames) {
            if (allModuleBuildApkPattern.matcher(task.toUpperCase()).matches()) {
                println "taskIsAssemble = ${taskIsAssemble}"
                taskIsAssemble = true
                if (task.contains(":")) {
                    def arr = task.split(":")
                    mainModuleName = arr[arr.length - 2].trim()
                    println "mainModuleName = ${mainModuleName}"
                }
                break
            }
        }
    }

    static boolean isMainApp(Project project) {
        return project.ext.has(MODULE_MAIN_APP) && project.ext.mainApp
    }

    /**
     * 当前是否正在给指定的module集成打包
     */
    static boolean isAssembleFor(Project project) {
        println "projectName=${project.name}, mainModuleName=${mainModuleName}"
        return project.name == mainModuleName
    }

    //组件依赖的方法，用于进行代码隔离
    //对组件库的依赖格式： addComponent 'componentProjectName'
    static void addComponentMethod(Project project, Properties localProperties) {
        //当前task是否为给本module打apk包
        def curModuleIsBuildingApk = taskIsAssemble && (mainModuleName == null && isMainApp(project) || mainModuleName == project.name)
        project.ext.addComponent = { componentName, realDependency = null ->
            //不是在为本app module打apk包，不添加对组件的依赖
            if (!curModuleIsBuildingApk) {
                return
            }
            // 是否需要排除该组件
            def excludeModule = 'true' == localProperties.getProperty(componentName)
            if (excludeModule) {
                return
            }
            // 组件工程是否存在
            def componentProjectExist = project.rootProject.subprojects.find { it.name == componentName }
            def dependencyMode = (project.gradle.gradleVersion as float) >= 4.1F ? 'api' : 'compile'
            if (componentProjectExist) {
                project.dependencies.add(dependencyMode, project.project(":$componentName"))
                println "AUTOC >>>> add project(\":$componentName\") to ${project.name}'s dependencies"
            } else {
                throw new RuntimeException(
                        "AUTOC >>>> add dependency by [ addComponent '$componentName' ] occurred an error:" +
                                "\n请检查组件工程名是否正确!!!")
            }
        }
    }
}