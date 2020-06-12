package com.appnikks.sharedprefmanagerannotation

/**
 * @author Sahib Singh
 * @since 13/06/2020
 * Annotation for Shared Preference Manager
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class SharedPrefManager(val preferenceName: String = "")