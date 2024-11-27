package com.crossgate.plugin.aspect.manifest

import com.crossgate.plugin.aspect.logger.GLog
import groovy.namespace.QName
import groovy.util.Node
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.gradle.workers.WorkAction
import java.io.File

/**
 * @ClassName: AddExportedWorkAction
 * @Describe: 为所有的未适配 Android 12 的组件添加 exported 属性
 * @Author: nil
 * @Date: 2023/8/14 16:38
 */
abstract class AddExportedWorkAction : WorkAction<ManifestWorkParameters> {

    private var isThreadNamePrinted = false

    override fun execute() {
        isThreadNamePrinted = false
        val manifestFile: File = parameters.inputFile.get().asFile
        readAndWriteManifestForExported(manifestFile)
    }

    /**
     * 处理清单文件
     */
    private fun readAndWriteManifestForExported(manifest: File) {
        if (!manifest.exists()) {
            GLog.l(TAG, "input manifest file does not exist")
            return
        }
        val node = readAndResetComponentFromManifest(manifest)
            ?: run {
                GLog.i(TAG,"nothing changed & no need rewriting")
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
     * 读取清单文件下的所有内容，处理的不包含 exported 属性的组件，并存放到 Node 中
     *
     * @return 若存在需要处理的不包含 exported 属性的组件，返回 Node；不存在则返回 null
     */
    private fun readAndResetComponentFromManifest(manifest: File): Node? {
        //标记清单文件内容是否有更改
        var contentChanged = false
        val parser = XmlParser()
        //得到所有的节点树
        val node = parser.parse(manifest)
        //node.attributes(); 获取的一级内容 <?xml> <manifest> 里设置的内容如: key 为 package|encoding, value为对应的值
        //node.children(); 获取的二级内容 <application> <uses-sdk>
        for (child in node.children()) {
            //从集合中找到 application 的节点
            if (child !is Node
                || child.name().toString() != Tag.APPLICATION) {
                continue
            }
            //遍历 application 节点
            label@ for (component in child.children()) {
                if (component !is Node) {
                    continue
                }
                //从集合中找到里面的 activity|service|receiver 节点
                val componentName = component.name().toString()
                if (componentName != Tag.ACTIVITY
                    && componentName != Tag.SERVICE
                    && componentName != Tag.RECEIVER
                ) {
                    continue
                }
                var name = ""
                for ((key, value) in component.attributes()) {
                    /**
                     * attributes 为键值对集合，key 类型为 [groovy.namespace.QName]
                     * 其中 [QName.namespaceURI] 为前缀的命名空间URI，如 {http://schemas.android.com/apk/res/android}
                     * [QName.localPart] 为属性名，如 name, exported
                     * [QName.prefix] 为实际显示的前缀，如 android
                     * [QName.getQualifiedName] 为完整的属性名，如 android:exported
                     */
                    if (key !is QName) continue
                    if (key.namespaceURI != Attribute.NAMESPACE) continue
                    //已经包含 android:exported 属性
                    if (Attribute.EXPORTED == key.localPart) {
                        GLog.i(TAG,"已有\"android:exported\", exported=$value")
                        continue@label
                    }
                    //获取 android:name 对应的属性值
                    if (Attribute.NAME == key.localPart) {
                        name = value.toString()
                    }
                }
                //处理 activity|service|receiver 节点的属性
                val handled = handleNodeWithoutExported(component, name)
                if (handled) {
                    GLog.i(TAG, "file from ${manifest.path} processed")
                    contentChanged = true
                }
            }
        }

        return if (contentChanged) node else null
    }

    /**
     * 处理没有 android:exported 的组件
     *
     * @return 是否存在需要处理的不包含 exported 属性的组件
     */
    private fun handleNodeWithoutExported(node: Node, name: String): Boolean {
        var contentChanged = false
        for (child in node.children()) {
            //没有 intent-filter 标签，跳过
            if (child !is Node
                || child.name().toString() != Tag.INTENT_FILTER) {
                continue
            }
            //处理含有 intent-filter 所在的父结点上添加 android:exported 属性
            val handled = handleNodeAddExported(node, name)
            if (handled && !contentChanged) {
                contentChanged = true
            }
        }
        return contentChanged
    }

    /**
     * 为符合条件的 Node 添加 android:exported
     *
     * @return 是否存在需要处理的不包含 exported 属性的组件
     */
    private fun handleNodeAddExported(node: Node, name: String): Boolean {
        if (parameters.warningOnly) {
            //处理主模块的清单文件,仅做报错信息提示
            GLog.l(TAG, "<<!!! error \n 必须为 <$name> 添加android:exported属性")
            return false
        }

        GLog.l(TAG, "为 <$name> 添加android:exported=true")
        //为被打包到主模块的其他清单文件中添加 android:exported 属性
        val qName = QName(Attribute.NAMESPACE, Attribute.EXPORTED, Attribute.PREFIX)
        node.attributes()[qName] = true
        GLog.i(TAG, "after-> element.attributes:${node.attributes()}")
        return true
    }

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
    private fun writeAlterationToManifest(manifest: File, node: Node) {
        if (parameters.warningOnly) {
            //如果是主模块的清单文件，只报错，不改写，需要开发者自行配置
            return
        }
        val result = XmlUtil.serialize(node)
        //重新写入原文件
        manifest.writeText(result, Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "AddExportedWorkAction"
    }
}
