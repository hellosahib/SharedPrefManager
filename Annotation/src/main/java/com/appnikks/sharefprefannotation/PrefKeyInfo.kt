package com.appnikks.sharefprefannotation

/**
 * @author Sahib Singh
 * @since 13/06/2020
 * Annotation for Shared Preference Fields To Provide Meta Data to it
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class PrefKeyInfo(val key: String = "")