// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import evaka.codegen.actionenum.checkGeneratedActionEnumTypes
import evaka.codegen.actionenum.generateActionEnumTypes
import evaka.codegen.api.TsFile
import evaka.codegen.api.TsProject
import evaka.codegen.api.generateApiFiles
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun generate() {
    val srcPath = locateFrontendSrcDirectory()
    val generatedPath = srcPath / "lib-common" / "generated"
    if (!generatedPath.exists()) {
        logger.error("Root path $generatedPath does not exist -> aborting")
        exitProcess(1)
    }

    generateActionEnumTypes(generatedPath)
    generateLanguages(generatedPath / "language.ts")

    val apiFiles = generateApiFiles()
    val apiPaths =
        listOf(
            srcPath / "lib-common" / "generated" / "api-types",
            srcPath / "citizen-frontend" / "generated" / "api-clients",
            srcPath / "employee-frontend" / "generated" / "api-clients",
            srcPath / "employee-mobile-frontend" / "generated" / "api-clients",
            srcPath / "e2e-test" / "generated",
        )
    apiPaths.forEach {
        it.toFile().deleteRecursively()
        it.createDirectory()
    }
    apiFiles.forEach { (file, content) ->
        val path = absolutePath(srcPath, file)
        path.writeText(content)
    }
}

fun check() {
    val srcPath = locateFrontendSrcDirectory()
    val generatedPath = srcPath / "lib-common" / "generated"

    checkGeneratedActionEnumTypes(generatedPath)
    checkLanguages(generatedPath / "language.ts")

    val apiFiles = generateApiFiles()
    apiFiles.forEach { (file, content) ->
        val path = absolutePath(srcPath, file)
        val currentContent = path.readText()
        val relativePath = path.relativeTo(srcPath)
        if (content == currentContent) {
            logger.info("Generated api files up to date ($relativePath)")
        } else {
            logger.error("Generated api files were not up to date ($relativePath)")
            exitProcess(1)
        }
    }
}

private fun locateFrontendSrcDirectory(): Path {
    // the working directory is expected to be the "service" directory
    val workingDir = Path("")
    val path = (workingDir / "../frontend/src").absolute().normalize()
    return path
}

private fun absolutePath(srcPath: Path, file: TsFile): Path =
    when (file.project) {
        TsProject.LibCommon -> srcPath / "lib-common" / file.path
        TsProject.CitizenFrontend -> srcPath / "citizen-frontend" / file.path
        TsProject.EmployeeFrontend -> srcPath / "employee-frontend" / file.path
        TsProject.EmployeeMobileFrontend -> srcPath / "employee-mobile-frontend" / file.path
        TsProject.E2ETest -> srcPath / "e2e-test" / file.path
    }
