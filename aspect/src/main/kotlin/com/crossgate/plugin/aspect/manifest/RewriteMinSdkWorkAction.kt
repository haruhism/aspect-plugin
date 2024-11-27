package com.crossgate.plugin.aspect.manifest

import com.crossgate.plugin.aspect.logger.GLog
import groovy.namespace.QName
import groovy.util.Node
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.gradle.workers.WorkAction
import java.io.File

/**
 * @ClassName: RewriteMinSdkWorkAction
 * @Describe: 重写 minSdkVersion 属性
 * @Author: nil
 * @Date: 2023/8/14 16:38
 */
abstract class RewriteMinSdkWorkAction : WorkAction<ManifestWorkParameters> {

    private var mainMinSdkVersion = 0

    private var isThreadNamePrinted = false

    override fun execute() {
        mainMinSdkVersion = parameters.attributes[Attribute.MIN_SDK_VERSION]?.toIntOrNull() ?: return
        isThreadNamePrinted = false
        val manifestFile: File = parameters.inputFile.get().asFile
        readAndWriteManifest(manifestFile)
    }

    /**
     * 处理清单文件
     */
    private fun readAndWriteManifest(manifest: File) {
        if (!manifest.exists()) {
            GLog.l(TAG, "input manifest file does not exist")
            return
        }
        val node = readAndResetMinSdkFromManifest(manifest)
            ?: run {
                GLog.i(TAG, "nothing changed & no need rewriting")
                return
            }
        if (parameters.warningOnly) {
            GLog.l(TAG, "warning only & skip writing back to file")
            return
        }
        writeAlterationToManifest(manifest, node)
        printExecutedThread()
    }

    /**
     * 读取清单文件根元素下的所有内容，重置 minSdkVersion 大于主模块编译配置的，并存放到 Node 中
     *
     * @return 若存在 minSdkVersion 大于主模块编译配置版本的，返回 Node；不存在则返回 null
     */
    private fun readAndResetMinSdkFromManifest(manifest: File): Node? = runCatching {
        if (mainMinSdkVersion <= 0) return@runCatching null
        //标记清单内容是否有更改
        var contentChanged = false
        val node = XmlParser().parse(manifest) ?: return@runCatching null
        label@ for (child in node.children()) {
            if (child !is Node
                || child.name().toString() != Tag.USES_SDK) {
                continue
            }
            var minSdkVersionKey: Any? = null
            for ((key, value) in child.attributes()) {
                if (key !is QName) continue
                if (key.namespaceURI != Attribute.NAMESPACE) continue
                if (key.localPart == Attribute.MIN_SDK_VERSION) {
                    val minSdkVersion = value.toString().toIntOrNull()
                    if (minSdkVersion == null || minSdkVersion <= mainMinSdkVersion) {
                        continue@label
                    }
                    GLog.l(
                        TAG,
                        "minSdkVersion $minSdkVersion from ${manifest.path} " +
                                "is greater than from gradle configurations:$mainMinSdkVersion"
                    )
                    minSdkVersionKey = key
                }
            }
            minSdkVersionKey ?: continue
            child.attributes()[minSdkVersionKey] = "$mainMinSdkVersion"
            GLog.i(TAG, "after-> element.attributes:${child.attributes()}")
            if (!contentChanged) {
                contentChanged = true
            }
        }

        if (contentChanged) node else null
    }.onFailure { e -> GLog.w(TAG, e) }.getOrNull()

    /**
     * 打印当前线程名
     */
    private fun printExecutedThread() {
        if (isThreadNamePrinted) {
            return
        }
        GLog.l(TAG, "${Thread.currentThread().name}: task is executed!")
        isThreadNamePrinted = true
    }

    /**
     * 将更新之后的 Node 重新写入原文件
     */
    private fun writeAlterationToManifest(manifest: File, node: Node) = kotlin.runCatching {
        if (parameters.warningOnly) {
            return@runCatching
        }
        val result = XmlUtil.serialize(node)
        //重新写入原文件
        manifest.writeText(result, Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "RewriteMinSdkWorkAction"
    }
}
