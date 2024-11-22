package com.ramcosta.composedestinations.ksp.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.ramcosta.composedestinations.codegen.facades.CodeOutputStreamMaker
import com.ramcosta.composedestinations.ksp.commons.KSFileSourceMapper
import java.io.OutputStream

class KspCodeOutputStreamMaker(
    private val codeGenerator: CodeGenerator,
    private val sourceMapper: KSFileSourceMapper
) : CodeOutputStreamMaker {

    override val packageNamesWrittenTo = mutableListOf<String>()

    override fun makeFile(
        name: String,
        packageName: String,
        extensionName: String,
        vararg sourceIds: String
    ): OutputStream {

        val sources = sourceIds.mapNotNull { sourceMapper.mapToKSFile(it) }.toTypedArray()
        val dependencies = if (sources.isEmpty() && false) {
            Dependencies.ALL_FILES
        } else {
            Dependencies(
                true,
                *sources
            )
        }

        packageNamesWrittenTo.add(packageName)

        return codeGenerator.createNewFile(
            dependencies = dependencies,
            fileName = name,
            packageName = packageName,
            extensionName = extensionName
        )
    }

}