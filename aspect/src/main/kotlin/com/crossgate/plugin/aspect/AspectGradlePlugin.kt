package com.crossgate.plugin.aspect

import com.android.build.gradle.internal.profile.AnalyticsService
import com.android.build.gradle.internal.services.getBuildServiceName
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.crossgate.plugin.aspect.logger.GLog
import com.crossgate.plugin.aspect.manifest.FixAttributesForPkgManifestParallelTask
import com.crossgate.plugin.aspect.packaging.CopyApkFileAfterAssembleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import java.util.regex.*

class AspectGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        setup(target)
        addTasksForVariantAfterEvaluate(target)
    }

    private fun setup(project: Project) {
        println("$TAG apply on ${project.name}")
        GLog.setLogger(project.logger)
    }

    /**
     * 在 build 过程中获取 variantName，需要注意这种方法不适用于 sync.
     * 在 sync 过程中该插件不执行
     */
    private fun findValidVariantsInBuild(project: Project): Set<String> {
        val taskNames = project.gradle.startParameter.taskNames
        if (taskNames.isEmpty()) {
            return emptySet()
        }
        GLog.l(TAG, "findValidVariantsInBuild-> taskNames=$taskNames")
        val variants = mutableSetOf<String>()
        for (taskName in taskNames) {
            val results = takeVariantsFromTaskName(project, taskName)
            if (results.isEmpty()) {
                continue
            }
            variants += results
        }
        GLog.l(TAG, "findValidVariantsInBuild-> variantNames='$variants'")
        return variants
    }

    /**
     * 通过正则从 taskName 中获取所有可用的 variantName
     */
    private fun takeVariantsFromTaskName(project: Project, taskName: String): Set<String> {
        val prefix = SUPPORTED_TASK_PREFIXES.firstOrNull { taskName.contains(it) }
        if (prefix.isNullOrEmpty()) {
            return emptySet()
        }
        val pattern = Pattern.compile("(?::${project.name}:)?$prefix(\\w+)")
        val matcher = pattern.matcher(taskName)
        if (matcher.find()) {
            val variant = matcher.group(1)
            kotlin.runCatching {
                val name = PROCESS_MANIFEST_FORMAT.format(variant)
                project.tasks.getByName(name)
            }.onSuccess { task ->
                if (task is ProcessApplicationManifest) {
                    return setOf(variant)
                }
            }.onFailure { e ->
                GLog.w(TAG, e.message)
            }

            /**
             * 当遇到 gradle 执行任务是 assembleDebug/assembleRelease 这种省略渠道名的格式，而配置里区分多渠道的时候
             * 只有 builtType 并不能获取到实际的打包任务，需要从原始任务的依赖集合中获取完整的打包任务名称
             */
            kotlin.runCatching {
                project.tasks.getByName("$prefix$variant")
            }.onSuccess { task ->
                return tryFindRealVariantsInBuild(project, task)
            }.onFailure { e ->
                GLog.w(TAG, e.message)
            }
        }
        return emptySet()
    }

    /**
     * 从任务的 dependsOn 集合中找到最终执行的包含渠道名的 Task，再获取完整的 variantName
     */
    private fun tryFindRealVariantsInBuild(project: Project, original: Task): Set<String> {
        val set = mutableSetOf<String>()
        val dependsOn = original.dependsOn
        if (dependsOn.isEmpty()) {
            return set
        }
        dependsOn.flatMap { dependsOnItem ->
            val tasks = arrayListOf<Task>()
            if (dependsOnItem is Collection<*>) {
                dependsOnItem.mapNotNullTo(tasks) { subItem ->
                    if (subItem is Provider<*>) {
                        subItem.orNull as? Task
                    } else {
                        null
                    }
                }
            }
            tasks
        }.forEach { task ->
            val variants = takeVariantsFromTaskName(project, task.path)
            if (variants.isNotEmpty()) {
                set += variants
            }
        }
        return set
    }

    /**
     * 在项目配置完之后添加自定义的 Task
     */
    private fun addTasksForVariantAfterEvaluate(project: Project) {
        project.afterEvaluate {
            val variantsInBuild = findValidVariantsInBuild(project)
            if (variantsInBuild.isEmpty()) {
                return@afterEvaluate
            }
            kotlin.runCatching {
                for (variantName in variantsInBuild) {
                    registerAddExportedForManifestTask(project, variantName)
                    registerCopyApkFileAfterAssembleTask(project, variantName)
                }
            }.onFailure { e -> GLog.w(TAG, e.message) }
        }
    }

    private fun registerCopyApkFileAfterAssembleTask(project: Project, variantName: String) {
        val taskName = ASSEMBLE_TASK_FORMAT.format(variantName)
        val assembleTask = project.tasks.getByName(taskName)
        if (assembleTask !is Task) {
            GLog.l(TAG, "assembleTask not found!!")
            return
        }
        val name = CopyApkFileAfterAssembleTask.NAME_FORMAT.format(variantName)
        val copyFileTask = project.tasks.register(
            name,
            CopyApkFileAfterAssembleTask::class.javaObjectType
        ).get()
        copyFileTask.variantName = variantName
        copyFileTask.analyticsService.set(buildAnalyticsService(project))
        assembleTask.finalizedBy(copyFileTask)
    }

    private fun registerAddExportedForManifestTask(project: Project, variantName: String) {
        val taskName = PROCESS_MANIFEST_FORMAT.format(variantName)
        val processManifestTask = project.tasks.getByName(taskName)
        if (processManifestTask !is ProcessApplicationManifest) {
            GLog.l(TAG, "processManifestTask not found!!")
            return
        }
        val name = FixAttributesForPkgManifestParallelTask.NAME_FORMAT.format(variantName)
        //创建自定义Task
        val exportTask = project.tasks.register(
            name,
            FixAttributesForPkgManifestParallelTask::class.javaObjectType,
        ).get()
        exportTask.variantName = variantName
        exportTask.analyticsService.set(buildAnalyticsService(project))
        exportTask.inputManifestFiles.from(processManifestTask.getManifests())
        exportTask.inputMainManifestFile.set(processManifestTask.mainManifest.get())
        processManifestTask.dependsOn(exportTask)
    }

    private fun buildAnalyticsService(project: Project): Provider<AnalyticsService> {
        val serviceClass = AnalyticsService::class.java
        val serviceName = getBuildServiceName(serviceClass)
        return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
        }
    }

    companion object {
        private const val TAG = "AspectGradlePlugin"

        private val SUPPORTED_TASK_PREFIXES = setOf("assemble")

        private const val PROCESS_MANIFEST_FORMAT = "process%sMainManifest"
        private const val ASSEMBLE_TASK_FORMAT = "assemble%s"
    }
}
