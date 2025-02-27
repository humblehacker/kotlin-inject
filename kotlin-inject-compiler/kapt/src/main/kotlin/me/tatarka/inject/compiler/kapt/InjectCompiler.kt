package me.tatarka.inject.compiler.kapt

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.compiler.FailedToGenerateException
import me.tatarka.inject.compiler.InjectGenerator
import me.tatarka.inject.compiler.Profiler
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import me.tatarka.kotlin.ast.ModelAstProvider

class InjectCompiler(private val profiler: Profiler? = null) : BaseInjectCompiler() {

    private val annotationNames = mutableSetOf<String>()
    private lateinit var provider: ModelAstProvider
    private lateinit var generator: InjectGenerator

    init {
        annotationNames.add(Component::class.java.canonicalName)
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        provider = ModelAstProvider(env)
        generator = InjectGenerator(provider, options)
    }

    @Suppress("LoopWithTooManyJumpStatements")
    override fun process(elements: Set<TypeElement>, env: RoundEnvironment): Boolean {
        if (elements.isEmpty()) {
            return false
        }

        profiler?.onStart()

        for (element in env.getElementsAnnotatedWith(Component::class.java)) {
            if (element !is TypeElement) continue

            val astClass = with(provider) { element.toAstClass() }

            try {
                val file = generator.generate(astClass)
                generator.scopeType?.let {
                    annotationNames.add(it.toString())
                }
                file.writeTo(filer)
            } catch (e: FailedToGenerateException) {
                provider.error(e.message.orEmpty(), e.element)
                // Continue so we can see all errors
                continue
            }
        }

        profiler?.onStop()

        return false
    }

    override fun getSupportedAnnotationTypes(): Set<String> = annotationNames
}
