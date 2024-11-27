package com.crossgate.plugin.aspect.manifest

object Tag {
    const val APPLICATION = "application"
    const val ACTIVITY = "activity"
    const val SERVICE = "service"
    const val RECEIVER = "receiver"
    const val INTENT_FILTER = "intent-filter"
    const val USES_SDK = "uses-sdk"
}

object Attribute {
    const val NAMESPACE = "http://schemas.android.com/apk/res/android"
    const val PREFIX = "android"
    const val EXPORTED = "exported"
    const val NAME = "name"
    const val MIN_SDK_VERSION = "minSdkVersion"
}
