package com.crossgate.gradle.aspect.manifest

import groovy.util.Node
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.gradle.workers.WorkAction
import java.io.File

/**
 * @ClassName: AddExportedWorkAction
 * @Describe: 为所有的未适配Android 12的组件添加exported属性
 * @Author: zhanghaiyang
 * @Date: 2023/8/14 16:38
 */
abstract class AddExportedWorkAction : WorkAction<AddExportedWorkParameters> {

    private var isPrintThreadName = false

    override fun execute() {
        isPrintThreadName = false
        val manifestFile: File = parameters.inputManifestFile.get().asFile
        readAndWriteManifestForExported(manifestFile)
    }

    /**
     * 处理manifest文件
     */
    private fun readAndWriteManifestForExported(manifest: File) {
        if (!manifest.exists()) {
            println("input manifest file does not exist")
            return
        }
        val node = readAndResetComponentFromManifest(manifest)
        if (parameters.isOnlyBuildError) {
            println("warning only & skip writing back to file")
            return
        }
        if (node == null) {
            //println("nothing changed & no need rewriting")
            return
        }
        writeComponentToManifest(manifest, node)
    }

    /**
     * 读取manifest文件下的所有内容，存放到node中
     *
     * @return 若存在需要处理的不包含exported属性的组件，返回Node；不存在则返回null
     */
    private fun readAndResetComponentFromManifest(manifest: File): Node? {
        //标记manifest内容是否有更改
        var contentChanged = false
        val parser = XmlParser()
        //得到所有的节点树
        val node = parser.parse(manifest)
        //node.attributes();获取的一级内容<?xml> <manifest>里设置的内容如:key为package、encoding,value为对应的值
        //node.children();获取的二级内容 <application> <uses-sdk>
        val firstChildren = node.children()
        //从集合中找到application的节点
        for (child in firstChildren) {
            if (child !is Node
                || child.name().toString() != APPLICATION) {
                continue
            }
            //选择application节点
            val application = child.children()
            //从集合中找到里面的activity、service、receiver节点
            label@ for (component in application) {
                if (component !is Node) {
                    continue
                }
                val componentName = component.name().toString()
                if (componentName != ACTIVITY
                    && componentName != SERVICE
                    && componentName != RECEIVER
                ) {
                    continue
                }
                val attributes = component.attributes()
                var name = ""
                for ((key, value) in attributes) {
                    //已经含有android:exported
                    if (ATTRIBUTE_EXPORTED == key.toString()) {
                        //println("$TAG, 已有\"android:exported\", exported=$value")
                        continue@label
                    }
                    //获取android:name对应的属性值
                    if (ATTRIBUTE_NAME == key.toString()) {
                        name = value.toString()
                    }
                }
                //处理activity、service、receiver节点的属性
                val handled = handleNodeWithoutExported(component, name)
                if (handled) {
                    contentChanged = true
                }
            }
        }

        return if (contentChanged) node else null
    }

    /**
     * 处理没有android:exported的component
     *
     * @return 是否存在需要处理的不包含exported属性的组件
     */
    private fun handleNodeWithoutExported(node: Node, name: String): Boolean {
        var contentChanged = false
        for (child in node.children()) {
            //没有intent-filter，跳过
            if (child !is Node
                || child.name().toString() != INTENT_FILTER) {
                continue
            }
            //处理含有intent-filter所在的父结点上添加android:exported属性
            val handled = handleNodeAddExported(node, name)
            if (handled) {
                contentChanged = true
            }
        }
        return contentChanged
    }

    /**
     * 为符合条件的node添加android:exported
     *
     * @return 是否存在需要处理的不包含exported属性的组件
     */
    private fun handleNodeAddExported(node: Node, name: String): Boolean {
        if (parameters.isOnlyBuildError) {
            handleNodeAddExportedForMainManifest(name)
            return false
        }
        handleNodeAddExportedForPackagedManifest(node, name)
        return true
    }

    /**
     * 处理主模块的manifest文件,仅做报错信息提示
     */
    private fun handleNodeAddExportedForMainManifest(name: String) {
        printExecutedThread()
        println("$TAG, <<!!! error \n 必须为 < $name > 添加android:exported属性")
    }

    /**
     * 处理被打包到app的其他manifest文件中添加android:exported
     */
    private fun handleNodeAddExportedForPackagedManifest(node: Node, name: String) {
        printExecutedThread()
        println("$TAG, 为 < $name > 添加android:exported=true")
        node.attributes()["android:exported"] = true
    }

    private fun printExecutedThread() {
        if (isPrintThreadName) {
            return
        }
        println("$TAG, ${Thread.currentThread().name} is executed !")
        isPrintThreadName = true
    }

    /**
     * 将更新之后的node重新写入原文件
     */
    private fun writeComponentToManifest(manifest: File, node: Node) {
        if (parameters.isOnlyBuildError) {
            //如果是主app的manifest文件,只报错,不改写,需要开发者自行配置
            return
        }
        val result = XmlUtil.serialize(node)
        //重新写入原文件
        manifest.writeText(result, Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "AddExportedWorkAction"
        private const val APPLICATION = "application"
        private const val ACTIVITY = "activity"
        private const val SERVICE = "service"
        private const val RECEIVER = "receiver"
        private const val INTENT_FILTER = "intent-filter"
        private const val ATTRIBUTE_EXPORTED: String = "{http://schemas.android.com/apk/res/android}exported"
        private const val ATTRIBUTE_NAME: String = "{http://schemas.android.com/apk/res/android}name"
    }
}
