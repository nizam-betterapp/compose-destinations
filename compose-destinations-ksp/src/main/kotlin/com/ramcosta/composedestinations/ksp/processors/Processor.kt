package com.ramcosta.composedestinations.ksp.processors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.ramcosta.composedestinations.codegen.CodeGenerator
import com.ramcosta.composedestinations.codegen.commons.ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.CORE_PACKAGE_NAME
import com.ramcosta.composedestinations.codegen.commons.DESTINATION_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.IllegalDestinationsSetup
import com.ramcosta.composedestinations.codegen.commons.JAVA_ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_DEFAULT_START_ARGS
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_DEFAULT_START_ARGS_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_TYPE_SERIALIZER_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.facades.Logger
import com.ramcosta.composedestinations.codegen.model.ClassKind
import com.ramcosta.composedestinations.codegen.model.DestinationResultSenderInfo
import com.ramcosta.composedestinations.codegen.model.Importable
import com.ramcosta.composedestinations.codegen.model.NavTypeSerializer
import com.ramcosta.composedestinations.codegen.model.SubModuleInfo
import com.ramcosta.composedestinations.ksp.codegen.KspCodeOutputStreamMaker
import com.ramcosta.composedestinations.ksp.codegen.KspLogger
import com.ramcosta.composedestinations.ksp.commons.DestinationMappingUtils
import com.ramcosta.composedestinations.ksp.commons.MutableKSFileSourceMapper
import com.ramcosta.composedestinations.ksp.commons.findActualClassDeclaration
import com.ramcosta.composedestinations.ksp.commons.findArgumentValue

class Processor(
    private val codeGenerator: KSPCodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val codeGenConfig = ConfigParser(options).parse(resolver)
        Logger.instance = KspLogger(codeGenConfig, logger)

        val composableDestinations = resolver.getComposableDestinationPaths()
        val activityDestinations = resolver.getActivityDestinations()
        val navGraphAnnotations = resolver.getNavGraphAnnotations()
        val navHostGraphAnnotations = resolver.getNavHostGraphAnnotations()

        if (!composableDestinations.iterator().hasNext() &&
            !activityDestinations.iterator().hasNext() &&
            !navGraphAnnotations.iterator().hasNext() &&
            !navHostGraphAnnotations.iterator().hasNext()
        ) {
            Logger.instance.info("NizamCosta: Processor No destinations found")
            return emptyList()
        }

        val navHostDefaultStartArgsByGraphAnnotationType = resolver.getNavHostDefaultStartArgsByGraphAnnotationType()
        val mutableKSFileSourceMapper = MutableKSFileSourceMapper()
        val kspCodeOutputStreamMaker = KspCodeOutputStreamMaker(codeGenerator, mutableKSFileSourceMapper)

        val navTypeSerializers = resolver.getNavTypeSerializers()
        val destinationMappingUtils = DestinationMappingUtils(resolver)
        val classesToNavGraphsMapper = KspToCodeGenNavGraphsMapper(
            resolver,
            destinationMappingUtils,
            mutableKSFileSourceMapper,
            navTypeSerializers.associateBy { it.genericType },
            navHostDefaultStartArgsByGraphAnnotationType
        )
        val navGraphs = classesToNavGraphsMapper.map(navGraphAnnotations, navHostGraphAnnotations)

        val functionsToDestinationsMapper = KspToCodeGenDestinationsMapper(
            resolver,
            destinationMappingUtils,
            mutableKSFileSourceMapper,
            navTypeSerializers.associateBy { it.genericType }
        )
        val destinations = functionsToDestinationsMapper.map(composableDestinations.map { it.immutable() }, activityDestinations)
        Logger.instance.info("NizamCosta: composableDestinations: size: ${destinations.size} all: ${destinations.joinToString("\n") { it.name }}")
        Logger.instance.info("NizamCosta: activityDestinations: size: ${activityDestinations.size} all: ${activityDestinations.joinToString("\n") { it.toString() }}")
        Logger.instance.info("NizamCosta: navGraphs: size: ${navGraphs.size} all: ${navGraphs.joinToString("\n") { it.name }}")

        CodeGenerator(
            codeGenerator = kspCodeOutputStreamMaker,
            codeGenConfig = codeGenConfig
        ).generate(
            destinations,
            navGraphs,
            navTypeSerializers,
            resolver.getSubModuleInfos()
        )

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun Resolver.getSubModuleInfos(): List<SubModuleInfo> {
        val moduleRegistryDeclarations = getDeclarationsFromPackage(
            "_generated._ramcosta._composedestinations._moduleregistry"
        )

        return moduleRegistryDeclarations
            .filter { it.simpleName.asString().startsWith("_ModuleRegistry_") }
            .flatMap { pckgDeclaration ->
                pckgDeclaration.annotations
                    .filter { it.shortName.asString().startsWith("_Info_") }
                    .map {
                        SubModuleInfo(
                            name = it.findArgumentValue<String>("moduleName"),
                            genPackageName = it.findArgumentValue<String>("packageName")!!,
                            hasNavArgsPackage = it.findArgumentValue<Boolean>("hasNavArgsPackage")!!,
                            topLevelGraphs = it.findArgumentValue<ArrayList<String>>("topLevelGraphs")!!,
                            publicResultSenders = it.findArgumentValue<ArrayList<KSAnnotation>>(
                                "typeResults"
                            )?.map { typeResultAnnotation ->
                                DestinationResultSenderInfo(
                                    typeResultAnnotation.findArgumentValue<String>("destination")!!,
                                    typeResultAnnotation.findArgumentValue<String>("resultType")!!,
                                    typeResultAnnotation.findArgumentValue<Boolean>("isResultNullable")!!,
                                    typeResultAnnotation.findArgumentValue<String>("resultNavType")!!,
                                )
                            }.orEmpty()
                        )
                    }
            }.toList()
    }

    private class DestinationAnnotationsPath {
        var annotations: Sequence<KSAnnotation> = emptySequence()
        var function: KSFunctionDeclaration? = null

        fun copy(): DestinationAnnotationsPath {
            return DestinationAnnotationsPath().also {
                it.annotations = annotations
                it.function = function
            }
        }

        override fun toString(): String {
            return "DestinationAnnotationsPath(annotations=${annotations.toList().map { it.shortName.asString() }}, function=${function?.qualifiedName?.asString()})"
        }

        fun immutable() = DestinationAnnotationsPath(annotations.toList(), function!!)
    }

    private fun Resolver.getComposableDestinationPaths(
        annotationQualifiedName: String = DESTINATION_ANNOTATION_QUALIFIED,
        annotationsPath: DestinationAnnotationsPath = DestinationAnnotationsPath()
    ): Sequence<DestinationAnnotationsPath> {
        val symbolsWithAnnotation = getSymbolsWithAnnotation(annotationQualifiedName)

        return symbolsWithAnnotation.flatMap {
            createPaths(
                annotationQualifiedName,
                it,
                annotationsPath.copy()
            )
        }
    }

    private fun Resolver.createPaths(
        annotationQualifiedName: String,
        annotated: KSAnnotated,
        annotationsPath: DestinationAnnotationsPath
    ) : Sequence<DestinationAnnotationsPath> {
        return when {
            annotated is KSFunctionDeclaration -> {
                annotated.annotations.filter { it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotationQualifiedName }.map { annotation ->
                    annotationsPath.copy().also {
                        it.annotations += annotation
                        it.function = annotated
                    }
                }
            }

            annotated is KSClassDeclaration && Modifier.ANNOTATION in annotated.modifiers -> {
                annotationsPath.annotations += annotated.annotations.find { it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotationQualifiedName }!!
                getComposableDestinationPaths(annotated.qualifiedName!!.asString(), annotationsPath)
            }

            else -> emptySequence()
        }
    }

    private fun Resolver.getActivityDestinations(
        names: List<String> = listOf(
            JAVA_ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED,
            ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED
        )
    ): List<KSClassDeclaration> {
        val symbolsWithAnnotation = names.flatMap { getSymbolsWithAnnotation(it) }

        return symbolsWithAnnotation
            .filterIsInstance<KSClassDeclaration>()
            .filter { Modifier.ANNOTATION !in it.modifiers } + getAnnotationActivityDestinations(symbolsWithAnnotation)
    }

    private fun Resolver.getAnnotationActivityDestinations(symbolsWithAnnotation: List<KSAnnotated>): List<KSClassDeclaration> {
        return symbolsWithAnnotation.filterIsInstance<KSClassDeclaration>()
            .filter { Modifier.ANNOTATION in it.modifiers && it.qualifiedName != null }
            .flatMap {
                getActivityDestinations(listOf(it.qualifiedName!!.asString()))
            }
    }

    private fun Resolver.getNavGraphAnnotations(): Sequence<KSClassDeclaration> {
        return getSymbolsWithAnnotation(NAV_GRAPH_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>()
    }

    private fun Resolver.getNavHostGraphAnnotations(): Sequence<KSClassDeclaration> {
        return getSymbolsWithAnnotation(NAV_HOST_GRAPH_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>()
    }

    private fun Resolver.getNavHostDefaultStartArgsByGraphAnnotationType(): Map<Importable, List<Importable>> {
        return getSymbolsWithAnnotation(NAV_HOST_DEFAULT_START_ARGS_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSPropertyDeclaration>()
            .mapTo(mutableListOf()) {
                val navGraphKsDeclaration =
                    it.annotations.first { it.shortName.asString() == NAV_HOST_DEFAULT_START_ARGS }
                        .annotationType.resolve().arguments.first().type!!.resolve().declaration
                val navGraph = Importable(
                    navGraphKsDeclaration.simpleName.asString(),
                    navGraphKsDeclaration.qualifiedName!!.asString()
                )
                val startArgsField = Importable(
                    it.simpleName.asString(),
                    it.qualifiedName!!.asString()
                )

                navGraph to startArgsField
            }.groupBy { it.first }
            .mapValues { entry -> entry.value.map { it.second } }
    }

    private fun Resolver.getNavTypeSerializers(): List<NavTypeSerializer> {
        return getSymbolsWithAnnotation(NAV_TYPE_SERIALIZER_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>().map { serializer ->
                if (serializer.classKind != KSPClassKind.CLASS && serializer.classKind != KSPClassKind.OBJECT) {
                    throw IllegalDestinationsSetup("${serializer.simpleName}: Type serializers must be either class or object!")
                }

                var superType: KSType? = null
                for (type in serializer.superTypes) {
                    val resolvedType = type.resolve()
                    val resolvedTypeString = resolvedType.declaration.qualifiedName?.asString()
                    if (resolvedTypeString ==
                        "$CORE_PACKAGE_NAME.navargs.DestinationsNavTypeSerializer") {
                        superType = resolvedType
                        break
                    }
                }

                if (superType == null) {
                    throw IllegalDestinationsSetup("${serializer.simpleName}: Type serializers must implement DestinationsNavTypeSerializer!")
                }

                val genericType = superType.arguments.first().type?.resolve()?.findActualClassDeclaration()
                    ?: throw IllegalDestinationsSetup("${serializer.simpleName} type serializer has an issue with its type argument!")

                NavTypeSerializer(
                    classKind = if (serializer.classKind == KSPClassKind.CLASS) ClassKind.CLASS else ClassKind.OBJECT,
                    serializerType = Importable(serializer.simpleName.asString(), serializer.qualifiedName!!.asString()),
                    genericType = Importable(genericType.simpleName.asString(), genericType.qualifiedName!!.asString()),
                )
            }.toList()
    }
}

typealias KSPClassKind = com.google.devtools.ksp.symbol.ClassKind
typealias KSPCodeGenerator = com.google.devtools.ksp.processing.CodeGenerator
