package com.appnikks.sharedprefmanagerannotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class PrefKeyInfo(val key: String = "")