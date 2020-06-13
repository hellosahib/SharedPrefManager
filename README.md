# SharedPrefManager
 :zap: An Android library which helps you create an Shared Preferences Manager with just annotations :zap:

![JitPack](https://img.shields.io/jitpack/v/github/sahibedu/SharedPrefManager?color=blue&label=Release)

In daily development, you may define SharedPreferenceshelper's for handling lightweight data storage, but every time you create a helper class you have to write a lot of boilerplate singleton code, create contract classes to have those keys saved somewhere and make getter setter every time you want to add something to it :sweat_drops:	. With this library, I want to reduce that boilerplate code with help of annotations so as to reduce the errors and time while creating **SharedPreferencesManager** 

### Gradle Setup
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.sahibedu.SharedPrefManager:Annotation:{RELEASE}'
    implementation 'com.github.sahibedu.SharedPrefManager:Processor:{RELEASE}'
}
```

### How To Use It
Define an abstract class that contain your fields you want to save and annotate it with **@SharedPrefManager**
```
@SharedPrefManager(preferenceName = "someRandomKey")
abstract class Sample1 {
    val isFirstRunDone: Boolean = false
    val iAmNullableString: String? = null
    val iAmInteger: Int = 0
    val iAmLong: Long = 0L

    @PrefKeyInfo(key = "butIWantNewName")
    val iAmFloat: Float = 5f
}
```

_Explaining the Annotations_
- @SharedPrefManager -> It is a Class Level Annotation to be Applied on Abstract Classes. If you want to have a custom name for your preference manager then add it to *preferencename* field else it takes the class name as key only
- @PrefKeyInfo -> It is a Field Level Annotation used to Give MetaDataInfo To Preferences Keys. It is an optional Annotation. If you want to have a custom key for respective field then add the key to the annotation, else keys are generated based on the field name

**_Also Value Given to Fields Will be Taken As Default Values For Shared Pref_**

## :boom: That's All For The Code Part from Your Side To Create An SharedPrefManager :boom:

Now For Using it. you just need to pass the context and thats all you require. 
```
Sample1SharedPreferenceManager.getInstance(this)
```
Now you can use this and access any field with simple code like
```
// Getter
Sample1SharedPreferenceManager.getInstance(this).isFirstRunDone
// Setter
Sample1SharedPreferenceManager.getInstance(this).isFirstRunDone = true
```
If you have any suggestions or feedback let me know and we will improve this library together :wink:


### Made With :dog: By Sahib Singh
