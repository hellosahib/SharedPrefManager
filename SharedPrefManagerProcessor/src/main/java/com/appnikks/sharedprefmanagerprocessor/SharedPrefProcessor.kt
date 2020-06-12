package com.appnikks.sharedprefmanagerprocessor

import com.appnikks.sharedprefmanagerannotation.SharedPrefManager
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.jvm.synchronized
import java.io.File
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
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

        // Field Builder
        for (enclosed in element.enclosedElements) {
            if (enclosed.kind == ElementKind.FIELD) {
//                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,enclosed.accept(StringValueGetter<Int>(),null).toString())
                classBuilder.addProperty(
                    PropertySpec.builder(
                        enclosed.simpleName.toString(),
                        enclosed.asType().asTypeName(),
                        KModifier.PUBLIC
                    )
                        .mutable(true)
                        .getter(
                            FunSpec.getterBuilder()
                                .addModifiers(KModifier.PUBLIC)
                                .addStatement("print(\"hello\")")
                                .addStatement("return ${enclosed.simpleName}")
                                .build()
                        )
                        .setter(
                            FunSpec.setterBuilder()
                                .addParameter(
                                    ParameterSpec.builder(
                                        "value",
                                        enclosed.asType().asTypeName()
                                    ).build()
                                )
                                .addModifiers(KModifier.PUBLIC)
                                .addStatement("field = value")
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

    class StringValueGetter<T> : ElementKindVisitor6<T, Nothing>() {
        override fun visitVariable(variableElement: VariableElement, p1: Nothing?): T {
            return variableElement.constantValue as T
        }
    }

}
