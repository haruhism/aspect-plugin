package com.crossgate.plugin.aspect.packaging

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.NonIncrementalTask
import com.crossgate.plugin.aspect.logger.GLog
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

/**
 * @ClassName: CopyApkFileTask
 * @Describe: Android打包任务执行完成后复制APK文件到指定目录
 * @Author: nil
 * @Date: 2024/11/11 09:57
 */
abstract class CopyApkFileAfterAssembleTask : NonIncrementalTask() {

    override fun doTaskAction() {
        val android: AppExtension
        try {
            android = project.extensions.getByType(AppExtension::class.java)
        } catch (e: Exception) {
            GLog.w(TAG, "Problem occurred when getAppExtension. $e")
            return
        }

        val rootDirectory = project.rootDir
        if (!rootDirectory.exists()) {
            return
        }
        val destFolder = File(COPY_DIRECTORY_FORMAT.format(rootDirectory))
        val workQueue = workerExecutor.noIsolation()
        android.applicationVariants.filter { variant ->
            // 筛选出本次打包任务包含的变量(渠道+类型)
            variant.name.equals(variantName, true)
        }.forEach { variant ->
            val flavorName = variant.flavorName
            val outputDir = if (flavorName.isNullOrEmpty()) {
                destFolder
            } else {
                File(destFolder, flavorName)
            }
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            variant.outputs.forEachIndexed { index, output ->
                val extension = output.outputFile.extension
                val copyFileName = if (extension.isEmpty()) {
                    COPY_FILE_NAME_FORMAT.format(variant.buildType.name, "${index + 1}")
                } else {
                    COPY_FILE_NAME_FORMAT.format(variant.buildType.name, extension)
                }
                workQueue.submit(CopyFileRunnable::class.javaObjectType) {
                    it.sourceFile.set(output.outputFile)
                    it.outputFile.set(File(outputDir, copyFileName))
                }
            }
        }
    }

    interface AndroidExtensionParams : WorkParameters {
        val sourceFile: RegularFileProperty

        val outputFile: RegularFileProperty
    }

    abstract class CopyFileRunnable : WorkAction<AndroidExtensionParams> {
        override fun execute() {
            val startTime = System.currentTimeMillis()
            val sourceFile = parameters.sourceFile.orNull?.asFile ?: return
            val outputFile = parameters.outputFile.orNull?.asFile ?: return
            try {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                val source = sourceFile.toPath()
                val outputStream = FileOutputStream(outputFile)
                Files.copy(source, outputStream)

                GLog.l(TAG, "copied to directory: ${outputFile.parent}")
                GLog.l(TAG, "output file name: ${outputFile.name}")
            } catch (e: Exception) {
                GLog.w(TAG, "copying file with failure-> $e")
            }
            val elapsed = System.currentTimeMillis() - startTime
            GLog.l(TAG, "copying file costs $elapsed ms")
        }
    }

    companion object {
        const val TAG = "copyApkFileAfterAssemble"
        const val NAME_FORMAT = "copyApkFileAfterAssemble%s"

        private const val COPY_DIRECTORY_FORMAT = "%s/build/outputs/"
        private const val COPY_FILE_NAME_FORMAT = "app_%s.%s"
    }
}
