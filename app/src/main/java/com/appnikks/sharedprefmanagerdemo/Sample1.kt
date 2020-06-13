package com.appnikks.sharedprefmanagerdemo

import com.appnikks.sharedprefmanagerannotation.PrefKeyInfo
import com.appnikks.sharedprefmanagerannotation.SharedPrefManager

/**
 * An Demo Class To Check Usage
 * For SharedPreferences Manager Annotation
 */
@SharedPrefManager(preferenceName = "someRandomKey")
abstract class Sample1 {
    val isFirstRunDone: Boolean = false
    val iAmNullableString: String? = null
    val iAmInteger: Int = 0
    val iAmLong: Long = 0L

    @PrefKeyInfo(key = "butIWantNewName")
    val iAmFloat: Float = 5f
}