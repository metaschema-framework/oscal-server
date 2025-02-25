/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.validation
import gov.nist.secauto.metaschema.modules.sarif.SarifValidationHandler
import gov.nist.secauto.metaschema.core.model.validation.IValidationFinding
import gov.nist.secauto.metaschema.core.util.ObjectUtils
import gov.nist.secauto.oscal.lib.OscalBindingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.net.URI

class SarifHandler(private val inputPath: Path, private val outputPath: Path) {
    private val logger: Logger = LogManager.getLogger(SarifHandler::class.java)
    private val context = OscalBindingContext.instance()
    private val handler: SarifValidationHandler

    init {
        val uri = ObjectUtils.notNull(inputPath.toUri())
        handler = SarifValidationHandler(uri, null)
    }

    fun addFinding(finding: IValidationFinding) {
        handler.addFinding(finding)
    }

    fun addFindings(findings: Collection<IValidationFinding>) {
        handler.addFindings(findings)
    }

    fun writeSarifOutput() {
        try {
            handler.write(outputPath, context)
            logger.info("SARIF output written to: $outputPath")
        } catch (e: Exception) {
            logger.error("Failed to write SARIF output", e)
            throw e
        }
    }
}
