package com.crossgate.gradle.aspect.manifest

import com.android.build.gradle.internal.tasks.NewIncrementalTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.work.InputChanges
import org.gradle.workers.WorkQueue
import java.io.File

/**
 * @ClassName: AddExportForPkgManifestParallelTask
 * @Describe: 为所有的未适配Android 12的组件添加exported属性
 * @Author: zhanghaiyang
 * @Date: 2023/8/14 16:34
 */
abstract class AddExportedForPkgManifestParallelTask : NewIncrementalTask() {

    init {
        outputs.upToDateWhen { false }
    }

    @get:InputFiles
    abstract val inputManifestFiles: ConfigurableFileCollection

    @get:InputFile
    abstract val inputMainManifestFile: RegularFileProperty

    override fun doTaskAction(inputChanges: InputChanges) {
        val workQueue = workerExecutor.noIsolation()

        //处理第三方的manifest
        val files = inputManifestFiles.asFileTree.files
        println("all input manifest file size:${files.size}")
        files.forEach {
            workQueueSubmit(workQueue, it, false)
        }

        //处理app下的manifest
        workQueueSubmit(workQueue, inputMainManifestFile.get().asFile, true)
    }

    private fun workQueueSubmit(
        workQueue: WorkQueue,
        manifestFile: File,
        isOnlyBuildError: Boolean
    ) {
        workQueue.submit(AddExportedWorkAction::class.javaObjectType) {
            it.inputManifestFile.set(manifestFile)
            it.isOnlyBuildError = isOnlyBuildError
        }
    }

    companion object {
        const val TAG = "addExportedForPkgManifestParallelTask"
    }
}
