package com.crossgate.gradle.aspect

import com.android.build.gradle.internal.profile.AnalyticsService
import com.android.build.gradle.internal.services.getBuildServiceName
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.crossgate.gradle.aspect.manifest.AddExportedForPkgManifestParallelTask
import com.crossgate.gradle.aspect.packaging.CopyApkFileAfterAssembleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import java.util.regex.*

class AspectGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        println("$TAG apply on ${target.name}")
        addTasksForVariantAfterEvaluate(target)
    }

    /**
     * 在 build 过程中获取 variantName，需要注意这种方法不适用于 sync.
     * 在 sync 过程中该插件不执行
     */
    private fun findValidVariantInBuild(project: Project): String {
        val taskNames = project.gradle.startParameter.taskNames
        if (taskNames.isEmpty()) {
            return ""
        }
        println("findValidVariantInBuild-> taskNames=$taskNames")
        var variantName = ""
        for (taskName in taskNames) {
            variantName = takeVariantFromTaskName(taskName, project) ?: continue
            if (variantName.isNotBlank()) {
                break
            }
        }
        println("findValidVariantInBuild-> variantName='$variantName'")
        return variantName
    }

    /**
     * 通过正则从 taskName 中获取 variantName
     */
    private fun takeVariantFromTaskName(taskName: String, project: Project): String? {
        val prefix = SUPPORTED_TASK_PREFIXES.firstOrNull { taskName.contains(it) }
        if (prefix.isNullOrEmpty()) {
            return null
        }
        val pattern = Pattern.compile("(?::${project.name}:)?$prefix(\\w+)")
        val matcher = pattern.matcher(taskName)
        if (matcher.find()) {
            val variant = matcher.group(1)
            val processManifestTask = try {
                val name = PROCESS_MANIFEST_FORMAT.format(variant)
                project.tasks.getByName(name)
            } catch (e: Exception) {
                println(e)
                null
            }
            if (processManifestTask !is ProcessApplicationManifest) {
                val task = project.tasks.getByName("$prefix$variant")
                val dependsOnVariant = tryFindRealVariantInBuild(project, task)
                if (dependsOnVariant != null) {
                    return dependsOnVariant
                }
            }
            return variant
        }
        return null
    }

    /**
     * 当遇到执行任务是 assembleDebug/assembleRelease 时，
     * 需要从 dependsOn 找到最终执行的包含渠道名的 Task，再获取完整的 variantName
     */
    private fun tryFindRealVariantInBuild(project: Project, original: Task): String? {
        val dependsOn = original.dependsOn
        if (dependsOn.isEmpty()) {
            return null
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
            val variant = takeVariantFromTaskName(task.path, project)
            if (!variant.isNullOrBlank()) {
                return variant
            }
        }
        return null
    }

    /**
     * 在项目配置完之后添加自定义的 Task
     */
    private fun addTasksForVariantAfterEvaluate(project: Project) {
        project.afterEvaluate {
            val variantName = findValidVariantInBuild(project)
            if (variantName.isEmpty()) {
                return@afterEvaluate
            }
            try {
                registerAddExportedForManifestTask(project, variantName)
                registerCopyApkFileAfterAssembleTask(project, variantName)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun registerCopyApkFileAfterAssembleTask(project: Project, variantName: String) {
        val taskName = ASSEMBLE_TASK_FORMAT.format(variantName)
        val assembleTask = project.tasks.getByName(taskName)
        if (assembleTask !is Task) {
            println("assembleTask not found!!")
            return
        }
        val copyFileTask = project.tasks.register(
            CopyApkFileAfterAssembleTask.TAG,
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
            println("processManifestTask not found!!")
            return
        }
        //创建自定义Task
        val exportTask = project.tasks.register(
            AddExportedForPkgManifestParallelTask.TAG,
            AddExportedForPkgManifestParallelTask::class.javaObjectType,
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

        private val SUPPORTED_TASK_PREFIXES = setOf("assemble", "generate")

        private const val PROCESS_MANIFEST_FORMAT = "process%sMainManifest"
        private const val ASSEMBLE_TASK_FORMAT = "assemble%s"
    }
}
