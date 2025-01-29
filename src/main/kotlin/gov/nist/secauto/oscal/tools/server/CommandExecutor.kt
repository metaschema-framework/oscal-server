/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.oscal.tools.cli.core.CLI
import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class CommandExecutor(private val oscalDir: Path) {
    private val logger: Logger = LogManager.getLogger(CommandExecutor::class.java)
    private val activeWorkers = AtomicInteger(0)

    suspend fun executeCommand(args: List<String>): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        return try {
            withContext(Dispatchers.IO) {  // Use IO dispatcher for better threading
                val mutableArgs = args.toMutableList()
                val guid = UUID.randomUUID().toString()
                val sarifFileName = "${guid}.sarif"
                
                // Use memory-efficient path resolution
                val sarifFilePath = oscalDir.resolve(sarifFileName).let { 
                    if (Files.exists(it)) Files.delete(it)
                    it.toString()
                }
                
                logger.debug("SARIF file path: $sarifFilePath")  // Changed to debug level
                
                if (mutableArgs.contains("-o")) {
                    throw Error("Do not specify sarif file")
                }
    
                // Optimize argument processing
                val isQuery = mutableArgs.getOrNull(1) == "metapath"
                if (!isQuery && mutableArgs[0] in listOf("metaschema", "validate")) {
                    mutableArgs.apply {
                        add("--sarif-include-pass")
                        add("-o")
                        add(sarifFilePath)
                    }
                }
    
                // Execute CLI command with optimized error handling
                val exitStatus = try {
                    CLI.runCli(*mutableArgs.toTypedArray())
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
    
                // Efficient SARIF file handling
                if (!Files.exists(Paths.get(sarifFilePath))) {
                    val exitCode = "code:${exitStatus.exitCode}"
                    val basicSarif = when (exitStatus) {
                        is MessageExitStatus -> createBasicSarif(
                            exitStatus.message?.let { "$exitCode $it" } ?: exitCode
                        )
                        else -> createBasicSarif(exitCode)
                    }
                    
                    // Use buffered writer for better IO performance
                    Files.newBufferedWriter(Paths.get(sarifFilePath)).use { writer ->
                        writer.write(basicSarif)
                    }
                }
    
                Pair(exitStatus, sarifFilePath)
            }
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    private fun createBasicSarif(errorMessage: String): String {
        val version = OscalCliVersion().getVersion()
        return """
        {
          "${'$'}schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
          "version": "2.1.0",
          "runs": [
            {
              "tool": {
                "driver": {
                  "name": "OSCAL Server",
                  "informationUri": "https://github.com/metaschema-framework/oscal-server",
                  "version": "$version"
                }
              },
              "results": [
                {
                  "message": {
                    "text": "Error occurred during OSCAL command execution: $errorMessage"
                  },
                  "level": "error"
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }

    fun getActiveWorkers(): Int {
        return activeWorkers.get()
    }
}
