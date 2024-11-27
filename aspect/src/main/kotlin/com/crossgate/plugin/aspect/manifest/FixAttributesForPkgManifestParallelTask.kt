package com.crossgate.plugin.aspect.manifest

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.NewIncrementalTask
import com.crossgate.plugin.aspect.logger.GLog
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.work.InputChanges
import org.gradle.workers.WorkQueue
import java.io.File

/**
 * @ClassName: FixAttributesForPkgManifestParallelTask
 * @Describe: 处理非主模块清单文件中存在的可能导致编译失败的配置
 * @Author: nil
 * @Date: 2023/8/14 16:34
 */
abstract class FixAttributesForPkgManifestParallelTask : NewIncrementalTask() {

    init {
        outputs.upToDateWhen { false }
    }

    @get:InputFiles
    abstract val inputManifestFiles: ConfigurableFileCollection

    @get:InputFile
    abstract val inputMainManifestFile: RegularFileProperty

    override fun doTaskAction(inputChanges: InputChanges) {
        val workQueue = workerExecutor.noIsolation()

        val android: AppExtension
        try {
            android = project.extensions.getByType(AppExtension::class.java)
        } catch (e: Exception) {
            GLog.w(TAG, "problem occurred when getAppExtension. $e")
            return
        }
        val apiLevel = android.defaultConfig.minSdkVersion?.apiLevel
        val minSdkAttrs = apiLevel?.let {
            hashMapOf(Attribute.MIN_SDK_VERSION to "$apiLevel")
        }

        //处理非主模块清单文件
        val files = inputManifestFiles.asFileTree.files
        GLog.l(TAG, "all input manifest file size:${files.size}")
        files.forEach {
            submitAddExportedAction(workQueue, it, false)
            submitRewriteMinSdkAction(workQueue, it, minSdkAttrs ?: return@forEach)
        }

        //处理主模块清单文件，默认只打印警告日志，不作实际处理
        submitAddExportedAction(workQueue, inputMainManifestFile.get().asFile, true)
    }

    private fun submitAddExportedAction(
        workQueue: WorkQueue,
        manifestFile: File,
        warningOnly: Boolean
    ) {
        workQueue.submit(AddExportedWorkAction::class.javaObjectType) {
            it.inputFile.set(manifestFile)
            it.warningOnly = warningOnly
        }
    }

    private fun submitRewriteMinSdkAction(
        workQueue: WorkQueue,
        manifestFile: File,
        attributes: Map<String, String>
    ) {
        workQueue.submit(RewriteMinSdkWorkAction::class.javaObjectType) {
            it.inputFile.set(manifestFile)
            it.warningOnly = false
            it.attributes = attributes
        }
    }

    companion object {
        const val TAG = "fixAttributesForPkgManifest"
        const val NAME_FORMAT = "fixAttributesFor%sManifest"
    }
}
