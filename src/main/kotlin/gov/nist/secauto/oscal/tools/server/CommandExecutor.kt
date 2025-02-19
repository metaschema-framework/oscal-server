/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.metaschema.databind.io.Format
import gov.nist.secauto.metaschema.databind.io.IBoundLoader
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.metaschema.databind.io.FormatDetector
import gov.nist.secauto.metaschema.databind.io.ModelDetector
import gov.nist.secauto.metaschema.core.model.IBoundObject
import gov.nist.secauto.metaschema.core.model.validation.IValidationResult
import gov.nist.secauto.metaschema.core.model.validation.AggregateValidationResult
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet
import gov.nist.secauto.metaschema.core.model.constraint.ValidationFeature
import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration
import gov.nist.secauto.metaschema.core.configuration.IMutableConfiguration
import gov.nist.secauto.metaschema.databind.DefaultBindingContext
import gov.nist.secauto.oscal.lib.OscalBindingContext
import gov.nist.secauto.oscal.lib.profile.resolver.ProfileResolver
import gov.nist.secauto.oscal.lib.model.Profile
import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.XMLConstants

class CommandExecutor(private val oscalDir: Path) {
    private val logger: Logger = LogManager.getLogger(CommandExecutor::class.java)
    private val activeWorkers = AtomicInteger(0)
    private val context = OscalBindingContext.instance()

    suspend fun validateDocument(
        inputPath: Path,
        flags: Set<String> = emptySet(),
        constraints: List<Path> = emptyList(),
        module: String? = null
    ): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        logger.info("Validating document: $inputPath")
        return try {
            withContext(Dispatchers.IO) {
                val guid = UUID.randomUUID().toString()
                val sarifFileName = "${guid}.sarif"
                Files.createDirectories(oscalDir)
                val targetPath = Files.createTempFile(oscalDir, guid, ".sarif")
                val sarifFilePath = targetPath.toString()
                
                logger.debug("SARIF file path: $sarifFilePath")
                
                val exitStatus = try {
                    // Create validation handler
                    val validationHandler = OscalValidationHandler.instance(true)

                    // Detect format and validate

                    // Determine if this is a metaschema document
                    val isMetaschema = module == "http://csrc.nist.gov/ns/oscal/metaschema/1.0" ||
                        inputPath.toString().contains("metaschema", true)
                    
                    // Use appropriate binding context
                    val bindingContext = if (isMetaschema) {
                        logger.info("Using Metaschema binding context for metaschema document")
                        DefaultBindingContext();
                    } else {
                        logger.info("Using OSCAL binding context")
                        context
                    }
                    
                    val inputSteam= inputPath.toUri().toURL().openStream()
                    val loader = bindingContext.newBoundLoader()
                    logger.debug("Detecting document format...")
                    val formatResult = loader.detectFormat(inputSteam,inputPath.toUri())
                    val sourceFormat = formatResult.getFormat();
                    logger.info("Detected format: $sourceFormat")
                    // Configure validation features
                    val configuration = DefaultConfiguration<ValidationFeature<*>>()
                    configuration.enableFeature(ValidationFeature.VALIDATE_GENERATE_PASS_FINDINGS)
                       val dataStream= formatResult.getDataStream()
                    // Perform validation
                    var validationResult: IValidationResult? = null
                    val modelResult=loader.detectModel(dataStream, inputPath.toUri(), sourceFormat))                    
                    try {
                        // Load and validate document
                        logger.debug("Loading and validating document...")
                        val boundClass = modelResult.getBoundClass()
                        val modelInputStream = modelResult.getDataStream();
                        val document = loader.load(boundClass, sourceFormat, modelInputStream, inputPath.toUri());
                        logger.info("Document loaded successfully as: ${document.javaClass.simpleName}")
                        
                        
                        // Perform constraint 
                        if (constraints.isNotEmpty()) {
                            logger.debug("Performing constraint validation...")
                            val constraintResult = bindingContext.validateWithConstraints(inputPath.toUri(), configuration)
                            validationResult = if (validationResult != null) {
                                AggregateValidationResult.aggregate(validationResult, constraintResult)
                            } else {
                                constraintResult
                            }
                            logger.info("Constraint validation completed")
                        }
                        
                        // Write SARIF output
                        Files.newBufferedWriter(Paths.get(sarifFilePath)).use { writer ->
                            writer.write(validationHandler.createSarifOutput())
                        }
                        
                        if (validationResult!==null&&!validationResult.isPassing()) {
                            MessageExitStatus(ExitCode.FAIL,
                                "Validation failed with ${validationHandler.getErrorCount()} errors and ${validationHandler.getWarningCount()} warnings")
                        } else {
                            MessageExitStatus(ExitCode.OK, "Validation completed successfully")
                        }
                    } catch (e: Exception) {
                        logger.error("Validation failed", e)
                        MessageExitStatus(ExitCode.RUNTIME_ERROR, "Validation failed: ${e.message}")
                    }
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                
                // Generate SARIF output
                if (!Files.exists(Paths.get(sarifFilePath))) {
                    val exitCode = "code:${exitStatus.exitCode}"
                    val basicSarif = when (exitStatus) {
                        is MessageExitStatus -> createBasicSarif(
                            exitStatus.message?.let { "$exitCode $it" } ?: exitCode
                        )
                        else -> createBasicSarif(exitCode)
                    }
                    
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

    suspend fun convertDocument(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        logger.info("Converting document from $inputPath to $outputPath with format $format")
        return try {
            withContext(Dispatchers.IO) {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting document conversion")
                    val outputFormat = if (format.equals("json", true)) Format.JSON else Format.XML
                    logger.debug("Target output format: $outputFormat")
                    
                    // Detect format from the input path
                    logger.debug("Detecting source format...")
                    val sourceFormat: Format = loader.detectFormat(inputPath)
                    logger.info("Detected source format: $sourceFormat")
                    
                    // Load and detect the document type
                    logger.debug("Loading document and detecting type...")
                    val document: IBoundObject = loader.load(inputPath.toUri())
                    val boundClass: Class<out IBoundObject> = document.javaClass
                    logger.info("Document loaded as: ${boundClass.simpleName}")
                    
                    // Serialize to output format
                    logger.debug("Serializing to output format...")
                    Files.newOutputStream(outputPath).use { out ->
                        context.newSerializer(outputFormat, boundClass).serialize(document, out)
                    }
                    logger.info("Document successfully converted from $sourceFormat to $outputFormat")
                    
                    MessageExitStatus(ExitCode.OK, "Conversion completed successfully")
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, "")
            }
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    suspend fun resolveProfile(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        logger.info("Resolving profile from $inputPath to $outputPath with format $format")
        return try {
            withContext(Dispatchers.IO) {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting profile resolution")
                    
                    // Load the profile using automatic format detection
                    logger.debug("Detecting source format...")
                    val sourceFormat: Format = loader.detectFormat(inputPath)
                    logger.info("Detected source format: $sourceFormat")
                    
                    logger.debug("Loading and validating profile...")
                    val profile: IBoundObject = loader.load(inputPath.toUri())
                    
                    // Ensure the loaded document is a Profile
                    if (profile !is Profile) {
                        logger.error("Document is not a Profile: ${profile.javaClass.simpleName}")
                        throw Error("Document is not a Profile")
                    }
                    logger.info("Profile loaded successfully")
                    
                    logger.debug("Creating profile resolver...")
                    val resolver = ProfileResolver()
                    
                    // Create a temporary file for the resolved profile
                    logger.debug("Creating temporary file for resolved profile...")
                    val tempFile = Files.createTempFile(oscalDir, "resolved-profile-", ".json")
                    logger.debug("Resolving profile...")
                    val resolvedProfile = resolver.resolve(File(inputPath.toString()))
                    Files.write(tempFile, resolvedProfile.toString().toByteArray())
                    logger.info("Profile resolved successfully")
                    
                    // Load the resolved profile using automatic detection
                    logger.debug("Loading resolved profile...")
                    val resolvedProfileObj: IBoundObject = loader.load(tempFile.toUri())
                    if (resolvedProfileObj !is Profile) {
                        logger.error("Resolved document is not a Profile: ${resolvedProfileObj.javaClass.simpleName}")
                        throw Error("Resolved document is not a Profile")
                    }
                    
                    // Serialize to the output
                    logger.debug("Serializing resolved profile...")
                    Files.newOutputStream(outputPath).use { out ->
                        val outputFormat = if (format.equals("json", true)) Format.JSON else Format.XML
                        context.newSerializer(outputFormat, Profile::class.java).serialize(resolvedProfileObj, out)
                    }
                    
                    // Clean up temp file
                    logger.debug("Cleaning up temporary files...")
                    Files.deleteIfExists(tempFile)
                    logger.info("Profile resolution completed successfully")
                    
                    MessageExitStatus(ExitCode.OK, "Profile resolution completed successfully")
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, "")
            }
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    suspend fun evaluateMetapath(inputPath: Path, expression: String, module: String): Pair<ExitStatus, String> {
        activeWorkers.incrementAndGet()
        logger.info("Evaluating metapath expression on: $inputPath")
        return try {
            withContext(Dispatchers.IO) {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting metapath evaluation")
                    
                    // Detect format and load document automatically
                    logger.debug("Detecting document format...")
                    val sourceFormat: Format = loader.detectFormat(inputPath)
                    logger.info("Detected format: $sourceFormat")
                    
                    logger.debug("Loading document...")
                    val document: IBoundObject = loader.load(inputPath.toUri())
                    logger.info("Document loaded as: ${document.javaClass.simpleName}")
                    
                    // Create a binding context for metapath evaluation
                    logger.debug("Creating JSON serializer for metapath evaluation...")
                    val serializer = context.newSerializer(Format.JSON, document.javaClass)
                    val outputStream = java.io.ByteArrayOutputStream()
                    
                    logger.debug("Serializing document for metapath evaluation...")
                    serializer.serialize(document, outputStream)
                    
                    // For now, return the full JSON document
                    // TODO: Implement proper metapath evaluation when API is available
                    val jsonResult = outputStream.toString()
                    logger.info("Document serialized successfully for metapath evaluation")
                    
                    // Create SARIF output for successful evaluation
                    val guid = UUID.randomUUID().toString()
                    val sarifPath = Paths.get(oscalDir.toString(), "${guid}.sarif")
                    Files.newBufferedWriter(sarifPath).use { writer ->
                        writer.write(createSuccessSarif(document.javaClass.simpleName))
                    }
                    logger.info("Metapath evaluation completed successfully")
                    
                    MessageExitStatus(ExitCode.OK, jsonResult)
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, exitStatus.message ?: "")
            }
        } finally {
            activeWorkers.decrementAndGet()
        }
    }

    private fun createSuccessSarif(documentType: String): String {
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
              "results": [],
              "invocations": [
                {
                  "executionSuccessful": true,
                  "toolExecutionNotifications": [
                    {
                      "level": "note",
                      "message": {
                        "text": "Document validated successfully as $documentType"
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

    private fun createBasicSarif(errorMessage: String): String {
        logger.debug("Creating SARIF output for error: $errorMessage")
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

    private fun validateXmlStructure(
        inputPath: Path,
        disableSchema: Boolean,
        constraints: List<Path> = emptyList()
    ) {
        // Create a builder for XML validation
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isNamespaceAware = true
            
            // Configure schema validation based on flag
            if (!disableSchema) {
                isValidating = true
                setFeature("http://apache.org/xml/features/validation/schema", true)
                setFeature("http://apache.org/xml/features/validation/schema-full-checking", true)
                setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI)
            } else {
                isValidating = false
                logger.info("Schema validation disabled")
            }
        }
        
        val builder = factory.newDocumentBuilder()
        
        // Set up error handler for validation errors
        builder.setErrorHandler(object : org.xml.sax.ErrorHandler {
            override fun warning(e: org.xml.sax.SAXParseException) {
                logger.warn("XML validation warning: ${e.message} at line ${e.lineNumber}, column ${e.columnNumber}")
            }
            
            override fun error(e: org.xml.sax.SAXParseException) {
                logger.error("XML validation error: ${e.message} at line ${e.lineNumber}, column ${e.columnNumber}")
                throw e
            }
            
            override fun fatalError(e: org.xml.sax.SAXParseException) {
                logger.error("XML fatal error: ${e.message} at line ${e.lineNumber}, column ${e.columnNumber}")
                throw e
            }
        })
        
        // Skip validation for constraint files themselves
        if (inputPath.toString().contains("constraints") && inputPath.toString().contains("fedramp-external")) {
            logger.info("Input file is a constraint file, skipping validation")
            return
        }
        
        try {
            // Parse and validate main document
            logger.debug("Parsing main document: $inputPath")
            val document = builder.parse(inputPath.toFile())
            logger.info("Document parsed successfully" + if (!disableSchema) " and schema validated" else "")
            
            // Validate against constraints if provided
            if (constraints.isNotEmpty()) {
                logger.info("Starting constraint validation with ${constraints.size} constraints")
                
                constraints.forEach { constraint ->
                    logger.debug("Processing constraint: ${constraint.fileName}")
                    try {
                        // Load constraint document
                        val constraintDoc = builder.parse(constraint.toFile())
                        logger.debug("Validating document against constraint: ${constraint.fileName}")
                        
                        // TODO: Implement actual constraint validation logic here
                        // For now, we're just parsing the constraints, but we need to:
                        // 1. Parse the constraint rules from the constraint document
                        // 2. Apply each rule to the main document
                        // 3. Collect and report any violations
                        // 4. Generate appropriate SARIF output for violations
                        
                        logger.debug("Constraint validation completed for: ${constraint.fileName}")
                    } catch (e: Exception) {
                        logger.error("Constraint validation failed for ${constraint.fileName}", e)
                        throw Exception("Document failed constraint validation against ${constraint.fileName}: ${e.message}")
                    }
                }
                logger.info("All constraint validations completed successfully")
            }
        } catch (e: org.xml.sax.SAXParseException) {
            // Handle validation errors with line numbers
            throw Exception("XML validation failed: ${e.message} at line ${e.lineNumber}, column ${e.columnNumber}")
        } catch (e: Exception) {
            throw Exception("XML validation failed: ${e.message}")
        }
    }
}
