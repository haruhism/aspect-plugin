package com.crossgate.plugin.aspect.manifest

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

/**
 * @ClassName: ManifestWorkParameters
 * @Describe: 为所有的未适配Android 12的组件添加exported属性
 * @Author: nil
 * @Date: 2023/8/14 16:37
 */
interface ManifestWorkParameters : WorkParameters {

    /**
     * 需要处理的清单文件
     */
    val inputFile: RegularFileProperty

    /**
     * 是否仅抛出编译异常而不作处理
     */
    var warningOnly: Boolean

    /**
     * 自定义属性集合
     */
    var attributes: Map<String, String>

}
