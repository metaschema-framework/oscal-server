/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.core.model.constraint.ConstraintValidationFinding
import gov.nist.secauto.metaschema.core.model.constraint.IConstraint.Level
import gov.nist.secauto.metaschema.core.model.validation.AbstractValidationResultProcessor
import gov.nist.secauto.metaschema.core.model.validation.IValidationFinding
import gov.nist.secauto.metaschema.core.model.validation.JsonSchemaContentValidator.JsonValidationFinding
import gov.nist.secauto.metaschema.core.model.validation.XmlSchemaContentValidator.XmlValidationFinding
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.xml.sax.SAXParseException
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class OscalValidationHandler private constructor(private val logExceptions: Boolean = false) : AbstractValidationResultProcessor() {
    private val logger: Logger = LogManager.getLogger(OscalValidationHandler::class.java)
    private val errorCount = AtomicInteger(0)
    private val warningCount = AtomicInteger(0)
    private val findings = mutableListOf<ValidationResult>()

    companion object {
        private val NO_LOG_EXCEPTION_INSTANCE = OscalValidationHandler(false)
        private val LOG_EXCEPTION_INSTANCE = OscalValidationHandler(true)

        @JvmStatic
        fun instance(logExceptions: Boolean = false): OscalValidationHandler {
            return if (logExceptions) LOG_EXCEPTION_INSTANCE else NO_LOG_EXCEPTION_INSTANCE
        }
    }

    data class ValidationResult(
        val level: Level,
        val message: String,
        val location: String,
        val documentUri: URI? = null,
        val lineNumber: Int? = null,
        val columnNumber: Int? = null
    )

    override fun handleJsonValidationFinding(finding: JsonValidationFinding) {
        val location = finding.cause.pointerToViolation
        val message = finding.message
        val documentUri = finding.documentUri

        trackFinding(finding.severity)
        logger.atLevel(getLogLevel(finding.severity))
            .log("JSON validation ${finding.severity}: $message at $location${documentUri?.let { " in $it" } ?: ""}")

        findings.add(
            ValidationResult(
                level = finding.severity,
                message = message ?: "",
                location = location ?: "",
                documentUri = documentUri
            )
        )
    }

    override fun handleXmlValidationFinding(finding: XmlValidationFinding) {
        val ex = finding.cause
        val message = finding.message
        val documentUri = finding.documentUri

        trackFinding(finding.severity)
        logger.atLevel(getLogLevel(finding.severity))
            .log("XML validation ${finding.severity}: $message${documentUri?.let { " in $it" } ?: ""} at line ${ex.lineNumber}, column ${ex.columnNumber}")

        findings.add(
            ValidationResult(
                level = finding.severity,
                message = message ?: "",
                location = "line ${ex.lineNumber}, column ${ex.columnNumber}",
                documentUri = documentUri,
                lineNumber = ex.lineNumber,
                columnNumber = ex.columnNumber
            )
        )
    }

    override fun handleConstraintValidationFinding(finding: ConstraintValidationFinding) {
        val target = finding.target.metapath
        val message = finding.message
        val id = finding.identifier

        trackFinding(finding.severity)
        logger.atLevel(getLogLevel(finding.severity))
            .log("Constraint validation ${finding.severity}${id?.let { " ($it)" } ?: ""}: $message at $target")

        findings.add(
            ValidationResult(
                level = finding.severity,
                message = message ?: "",
                location = target ?: ""
            )
        )
    }

    private fun trackFinding(level: Level) {
        when (level) {
            Level.ERROR, Level.CRITICAL -> errorCount.incrementAndGet()
            Level.WARNING -> warningCount.incrementAndGet()
            else -> { /* no tracking needed */ }
        }
    }

    private fun getLogLevel(level: Level) = when (level) {
        Level.CRITICAL -> org.apache.logging.log4j.Level.FATAL
        Level.ERROR -> org.apache.logging.log4j.Level.ERROR
        Level.WARNING -> org.apache.logging.log4j.Level.WARN
        Level.INFORMATIONAL -> org.apache.logging.log4j.Level.INFO
        Level.NONE -> org.apache.logging.log4j.Level.INFO
        Level.DEBUG -> org.apache.logging.log4j.Level.DEBUG
    }

    fun createSarifOutput(): String {
        val version = gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion().getVersion()
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
                ${findings.joinToString(",\n") { finding ->
                    """
                    {
                      "message": {
                        "text": "${finding.message}"
                      },
                      "level": "${finding.level.name.lowercase()}",
                      "locations": [
                        {
                          "physicalLocation": {
                            "artifactLocation": {
                              "uri": "${finding.documentUri ?: ""}"
                            },
                            "region": {
                              "startLine": ${finding.lineNumber ?: 0},
                              "startColumn": ${finding.columnNumber ?: 0}
                            }
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                }}
              ],
              "invocations": [
                {
                  "executionSuccessful": ${errorCount.get() == 0},
                  "toolExecutionNotifications": [
                    {
                      "level": "note",
                      "message": {
                        "text": "Validation completed with ${errorCount.get()} errors and ${warningCount.get()} warnings"
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

    fun hasErrors(): Boolean = errorCount.get() > 0
    fun getErrorCount(): Int = errorCount.get()
    fun getWarningCount(): Int = warningCount.get()
    fun getFindings(): List<ValidationResult> = findings.toList()
}
