/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.oscal.tools.server.validation.IValidationFinding
import gov.nist.secauto.oscal.tools.server.validation.IValidationFindingHandler
import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.atomic.AtomicInteger

class OscalValidationHandler private constructor(private val generatePassFindings: Boolean) : IValidationFindingHandler {
    private val logger: Logger = LogManager.getLogger(OscalValidationHandler::class.java)
    private val errorCount = AtomicInteger(0)
    private val warningCount = AtomicInteger(0)
    private val findings = mutableListOf<IValidationFinding>()

    companion object {
        @JvmStatic
        fun instance(generatePassFindings: Boolean): OscalValidationHandler {
            return OscalValidationHandler(generatePassFindings)
        }
    }

    override fun handleFinding(finding: IValidationFinding) {
        when (finding.level) {
            IValidationFinding.Level.ERROR -> errorCount.incrementAndGet()
            IValidationFinding.Level.WARNING -> warningCount.incrementAndGet()
            else -> { /* No action needed for other levels */ }
        }
        findings.add(finding)
        logger.debug("Validation finding: ${finding.message} (${finding.level})")
    }

    fun getErrorCount(): Int = errorCount.get()
    fun getWarningCount(): Int = warningCount.get()

    fun createSarifOutput(): String {
        val version = OscalCliVersion().getVersion()
        val results = findings.map { finding ->
            """
            {
              "message": {
                "text": "${finding.message.replace("\"", "\\\"")}"
              },
              "level": "${finding.level.name.lowercase()}",
              "locations": [
                {
                  "physicalLocation": {
                    "artifactLocation": {
                      "uri": "${finding.location?.uri?.toString()?.replace("\"", "\\\"") ?: ""}".trimIndent()
                    },
                    "region": {
                      "startLine": ${finding.location?.lineNumber ?: 0},
                      "startColumn": ${finding.location?.columnNumber ?: 0}
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        }.joinToString(",\n")

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
                ${if (findings.isEmpty()) "[]" else results}
              ],
              "invocations": [
                {
                  "executionSuccessful": ${errorCount.get() == 0 && findings.isNotEmpty()},
                  "toolExecutionNotifications": [
                    {
                      "level": "note",
                      "message": {
                        "text": "${if (findings.isEmpty()) "No validation findings" else "Validation completed with ${errorCount.get()} errors and ${warningCount.get()} warnings"}"
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
}
