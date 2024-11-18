package com.crossgate.gradle.aspect.manifest

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

/**
 * @ClassName: AddExportWorkParameters
 * @Describe: 为所有的未适配Android 12的组件添加exported属性
 * @Author: zhanghaiyang
 * @Date: 2023/8/14 16:37
 */
interface AddExportedWorkParameters : WorkParameters {

    /**
     * 需要处理的manifest文件
     */
    val inputManifestFile: RegularFileProperty

    /**
     * 是否仅仅抛出编译异常
     */
    var isOnlyBuildError: Boolean

}
