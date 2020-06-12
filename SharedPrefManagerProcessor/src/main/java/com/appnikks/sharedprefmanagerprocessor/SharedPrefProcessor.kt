package com.appnikks.sharedprefmanagerprocessor

import com.appnikks.sharedprefmanagerannotation.SharedPrefManager
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.jvm.synchronized
import java.io.File
import java.util.*
import javax.annotation.Nullable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementKindVisitor6
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedOptions(SharedPrefProcessor.GENERATED_PACKAGE_LOCATION)
class SharedPrefProcessor : AbstractProcessor() {

    companion object {
        const val GENERATED_PACKAGE_LOCATION = "kapt.kotlin.generated"
        private val CLASS_CONTEXT = ClassName("android.content", "Context")
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(SharedPrefManager::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ): Boolean {
        roundEnv.getElementsAnnotatedWith(SharedPrefManager::class.java)
            .forEach {
                if (it.kind != ElementKind.CLASS && it.modifiers.contains(Modifier.ABSTRACT)
                        .not()
                ) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Only Classes can be Annotated"
                    )
                    return true
                }
                processAnnotation(it)
            }
        return false
    }

    private fun processAnnotation(element: Element) {
        val className = element.simpleName.toString()
        val pack = processingEnv.elementUtils.getPackageOf(element).toString()

        val fileName = className + "SharedPreferenceManager"
        val classBuilder = TypeSpec.classBuilder(fileName)
            .addModifiers(KModifier.PUBLIC)

        val classLevelAnnotation = element.getAnnotation(SharedPrefManager::class.java)

        /** Constructor Builder
         * Private Constructor With Context
         */
        classBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("appContext", CLASS_CONTEXT, KModifier.PRIVATE)
                        .build()
                )
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
            .addProperty(
                PropertySpec.builder("appContext", CLASS_CONTEXT)
                    .initializer("appContext")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

            /** Private Field
             * Shared Preferences Instance
             */
            .addProperty(
                PropertySpec.builder(
                    "mPreferences",
                    ClassName("android.content", "SharedPreferences")
                )
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

            /** Private Field
             * Shared Preferences Editor Instance
             */
            .addProperty(
                PropertySpec.builder(
                    "mEditor",
                    ClassName("android.content", "SharedPreferences.Editor")
                )
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

            /**
             * Init Block
             * Initializing The Shared Pref Fields
             */
            .addInitializerBlock(
                CodeBlock.builder()
                    .addStatement(
                        "mPreferences = appContext.getSharedPreferences(%S , Context.MODE_PRIVATE)",
                        classLevelAnnotation.preferenceName.let {
                            if (it.isEmpty()) className.toLowerCase(Locale.US) else it
                        }
                    )
                    .addStatement("mEditor = mPreferences.edit()")
                    .build()
            )

            /**
             * Companion Builder For Instance
             * Has An Field INSTANCE For Singleton
             * Has an Method To getInstance
             * Which Returns An Singleton Object Of Class
             */
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addProperty(
                        PropertySpec.builder("INSTANCE", ClassName(pack, fileName))
                            .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                            .mutable(true)
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getInstance")
                            .addParameter(ParameterSpec.builder("context", CLASS_CONTEXT).build())
                            .addCode(
                                CodeBlock.builder()
                                    .beginControlFlow("if (this::INSTANCE.isInitialized.not())")
                                    .addStatement("INSTANCE = $fileName(context.applicationContext)")
                                    .endControlFlow()
                                    .addStatement("return INSTANCE")
                                    .build()
                            )
                            .returns(ClassName(pack, fileName))
                            .synchronized()
                            .build()
                    )
                    .build()
            )

        /**
         * Fields Builder
         * We Take All Fields From Class
         * And Make Them To Variables
         * With Custom Getter And Setter For Shared Pref
         * Also default value will be the value assigned to them
         */
        for (enclosed in element.enclosedElements) {
            if (enclosed.kind == ElementKind.FIELD) {
                val enclosedTypeName = enclosed.asNullableTypeName()
                classBuilder.addProperty(
                    PropertySpec.builder(
                        enclosed.simpleName.toString(),
                        enclosedTypeName
                    )
                        .initializer("%L", enclosed.getValueBasedOnDataType(enclosedTypeName))
                        .mutable(true)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement(
                                    "return ${enclosedTypeName.getPreferenceGetterStringBasedOnValue(
                                        enclosed.simpleName.toString(),
                                        enclosed.getValueBasedOnDataType(enclosedTypeName)
                                    )}"
                                )
                                .build()
                        )
                        .setter(
                            FunSpec.setterBuilder()
                                .addParameter(
                                    ParameterSpec.builder(
                                        "value",
                                        enclosedTypeName
                                    ).build()
                                )
                                .addModifiers(KModifier.PUBLIC)
                                .addStatement("field = value")
                                .addStatement(
                                    enclosedTypeName.getPreferenceSetterStringBasedOnDataType(
                                        enclosed.simpleName.toString()
                                    )
                                )
                                .build()
                        )
                        .build()
                )
            }
        }

        val kaptKotlinGeneratedDir = processingEnv.options[GENERATED_PACKAGE_LOCATION]!!
        FileSpec.builder(pack, fileName)
            .addType(classBuilder.build()).build()
            .writeTo(File(kaptKotlinGeneratedDir))
    }


    /**
     * Check For Nullable Annotations in JVM Code
     * @return TypeName With Nullability set
     */
    private fun Element.asNullableTypeName(): TypeName {
        val javaxNullAnnotation = this.getAnnotation(Nullable::class.java)
        val jetBrainsNullAnnotation =
            this.getAnnotation(org.jetbrains.annotations.Nullable::class.java)
        val typeName = this.asType().asKotlinTypeName()
        return if (isSupportedNullableDataType(
                javaxNullAnnotation != null || jetBrainsNullAnnotation != null,
                typeName
            )
        ) {
            typeName.copy(nullable = true)
        } else {
            typeName
        }
    }

    /**
     * Does An Sanity On Data Type
     * Nullability is Only Supported for String
     * For Other Data Types Nullable are rejected and Given an Error For it
     */
    private fun isSupportedNullableDataType(isNullable: Boolean, typeName: TypeName): Boolean {
        if (isNullable) {
            if (typeName.toString().contains("kotlin.String").not()) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Nullable Field Not Supported For $typeName"
                )
            } else {
                return true
            }
        }
        return false
    }

    /**
     * Converts Java Data Types To Kotlin Data Types
     * due to issue in KotlinPoet Taking JVM Datatypes instead of Kotlin Ones
     * Had To Add This Workaround
     */
    private fun TypeMirror.asKotlinTypeName(): TypeName {
        return when {
            this.toString().contains("java.lang.String") -> ClassName(
                "kotlin",
                "String"
            )
            this.toString().contains("java.lang.Boolean") -> ClassName(
                "kotlin",
                "Boolean"
            )
            else -> this.asTypeName()
        }
    }

    /**
     * Get Elements Initialized Value From User Class Based On Type
     * It Gets the DataType of Element and Gets the respective Value
     * Used To set Default Value for Shared Preferences
     */
    private fun Element.getValueBasedOnDataType(classType: TypeName): Any? {
        return when (classType.toString()) {
            "kotlin.String?" -> null
            "kotlin.String" -> accept(ElementValueGetter<String>(), null).let {
                "\"$it\""
            }
            "kotlin.Boolean" -> accept(ElementValueGetter<Boolean>(), null)
            else -> throw RuntimeException("getValueBasedOnDataType For TypeName $this not found")
        }
    }

    /**
     * Returns The Respective SharedPreferences Getter String Based on Type
     */
    private fun TypeName.getPreferenceGetterStringBasedOnValue(
        key: String,
        value: Any?
    ): CodeBlock {
        val prefGetterString = when (this.toString()) {
            "kotlin.String?" -> "mPreferences.getString(%S, null)"
            "kotlin.String" -> "mPreferences.getString(%S, %L)"
            "kotlin.Boolean" -> "mPreferences.getBoolean(%S, %L)"
            else -> throw RuntimeException("getPreferenceGetterBasedOnType not found for $this")
        }
        return if (value != null) {
            CodeBlock.builder().addStatement(prefGetterString, key, value).build()
        } else {
            CodeBlock.builder().addStatement(prefGetterString, key).build()
        }
    }

    /**
     * Returns The Respective SharedPreferences Getter String Based on Type
     */
    private fun TypeName.getPreferenceSetterStringBasedOnDataType(key: String): String {
        val formattedKey = "\"$key\""
        return when (this.toString()) {
            "kotlin.String", "kotlin.String?" -> "mEditor.putString($formattedKey, field).apply()"
            "kotlin.Boolean" -> "mEditor.putBoolean($formattedKey, field).apply()"
            else -> throw RuntimeException("getPreferenceGetterBasedOnType not found for $this")
        }
    }
}

/**
 * Gets the Element Assigned Value
 */
@Suppress("UNCHECKED_CAST")
class ElementValueGetter<T> : ElementKindVisitor6<T, Nothing>() {
    override fun visitVariable(variableElement: VariableElement, p1: Nothing?): T {
        return variableElement.constantValue as T
    }
}

