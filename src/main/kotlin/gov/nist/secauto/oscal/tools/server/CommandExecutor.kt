/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandExecutor(private val oscalDir: Path) {
    private val logger: Logger = LogManager.getLogger(CommandExecutor::class.java)
    private val activeWorkers = AtomicInteger(0)
    private val documentValidator = DocumentValidator(oscalDir)
    private val documentConverter = DocumentConverter()
    private val profileResolver = ProfileResolverService()
    private val metapathEvaluator = MetapathEvaluator()

    suspend fun validateDocument(
        inputPath: Path,
        flags: Set<String> = emptySet(),
        constraints: List<Path> = emptyList(),
        module: String? = null
    ): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        return try {
            documentValidator.validateDocument(inputPath, flags, constraints, module)
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    suspend fun convertDocument(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        return try {
            documentConverter.convertDocument(inputPath, outputPath, format)
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    suspend fun resolveProfile(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        return try {
            profileResolver.resolveProfile(inputPath, outputPath, format)
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    suspend fun evaluateMetapath(inputPath: Path, expression: String, module: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        return try {
            metapathEvaluator.evaluateMetapath(inputPath, expression, module, oscalDir)
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    fun getActiveWorkers(): Int {
        return activeWorkers.get()
    }
}
