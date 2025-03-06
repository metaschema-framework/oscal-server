/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

 package gov.nist.secauto.oscal.tools.server

 import gov.nist.secauto.metaschema.cli.processor.ExitCode
 import gov.nist.secauto.metaschema.cli.processor.ExitStatus
 import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
 import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration
 import gov.nist.secauto.metaschema.core.model.IModule
 import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet
 import gov.nist.secauto.metaschema.core.model.validation.AggregateValidationResult
 import gov.nist.secauto.metaschema.core.model.validation.XmlSchemaContentValidator
 import gov.nist.secauto.metaschema.core.model.validation.IValidationResult
 import gov.nist.secauto.metaschema.core.model.validation.JsonSchemaContentValidator
 import gov.nist.secauto.metaschema.core.model.constraint.ValidationFeature
 import gov.nist.secauto.metaschema.core.model.util.JsonUtil
 import gov.nist.secauto.metaschema.core.model.util.XmlUtil
 import gov.nist.secauto.metaschema.core.util.ObjectUtils
 import gov.nist.secauto.metaschema.core.util.CollectionUtil
 import gov.nist.secauto.metaschema.databind.DefaultBindingContext
 import gov.nist.secauto.metaschema.databind.IBindingContext
 import gov.nist.secauto.oscal.lib.OscalBindingContext
 import gov.nist.secauto.oscal.lib.model.OscalCompleteModule
 import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion
 import gov.nist.secauto.oscal.tools.server.validation.SarifHandler
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.withContext
 import org.apache.logging.log4j.LogManager
 import org.apache.logging.log4j.Logger
 import org.json.JSONObject
 import org.xml.sax.SAXException
 import java.io.IOException
 import java.io.InputStream
 import java.net.URL
 import java.nio.file.Files
 import java.nio.file.Path
 import java.nio.file.Paths
 import java.util.UUID
 import java.util.LinkedList
import java.lang.Exception
 import javax.xml.XMLConstants
 import javax.xml.parsers.DocumentBuilderFactory
 import javax.xml.transform.Source
 import kotlin.jvm.java
 
 class DocumentValidator(private val oscalDir: Path) {
     private val logger: Logger = LogManager.getLogger(DocumentValidator::class.java)
     private val context = OscalBindingContext.instance()
 
     private class OscalValidationCommandExecutor : IBindingContext.ISchemaValidationProvider {
 
         fun getBindingContext(constraintSets: Set<IConstraintSet>): IBindingContext {
             return OscalBindingContext.builder()
                 .constraintSet(constraintSets)
                 .build()
         }
 
         @Throws(IOException::class, SAXException::class)
         override fun getXmlSchemas(source:URL,bindingContext: IBindingContext) :XmlSchemaContentValidator {
             val retval = LinkedList<Source>();
             retval.add(
                 XmlUtil.getStreamSource(ObjectUtils.requireNonNull(
                     OscalBindingContext::class.java.getResource("/schema/xml/oscal-complete_schema.xsd"))));
             return XmlSchemaContentValidator(CollectionUtil.unmodifiableList(retval));
           }
 
         override fun getJsonSchema(json: JSONObject, bindingContext: IBindingContext): JsonSchemaContentValidator {
             val inputStream = ObjectUtils.requireNonNull(
                 OscalBindingContext::class.java.getResourceAsStream("/schema/json/oscal-complete_schema.json"));
               return JsonSchemaContentValidator(JsonUtil.toJsonObject(inputStream));
         }
 
         fun getSchemaValidationProvider(
             module: IModule,
             bindingContext: IBindingContext
         ): IBindingContext.ISchemaValidationProvider {
             return this
         }
 
         fun getModule( bindingContext: IBindingContext): IModule {
             return bindingContext.registerModule(OscalCompleteModule::class.java)
         }
     }
 
     fun validateDocument(
         inputPath: Path,
         flags: Set<String> = emptySet(),
         constraints: List<Path> = emptyList(),
         module: String? = null
     ): Pair<ExitStatus, String> {
         logger.info("Validating document: $inputPath")
         return try {
                 val guid = UUID.randomUUID().toString()
                 Files.createDirectories(oscalDir)
                 val targetPath = Files.createTempFile(oscalDir, guid, ".sarif")
                 val sarifFilePath = targetPath.toString()
                 
                 logger.debug("SARIF file path: $sarifFilePath")
                 
                 val exitStatus = try {
                     // Create SARIF handler
                     val sarifHandler = SarifHandler(inputPath, Paths.get(sarifFilePath))
 
                     // Determine if this is a metaschema document
                     val isMetaschema = module == "http://csrc.nist.gov/ns/oscal/metaschema/1.0";
                     
                     // Load constraint sets if provided
                     val constraintSets = if (constraints.isNotEmpty()) {
                         constraints.flatMap { constraintPath ->
                             try {
                                 val constraintLoader = IBindingContext.getConstraintLoader()
                                 constraintLoader.load(constraintPath.toUri())
                             } catch (e: Exception) {
                                 logger.error("Failed to load constraint set from ${constraintPath}", e)
                                 emptyList()
                             }
                         }.toSet()
                     } else emptySet()
 
                     // Use appropriate binding context
                     val bindingContext = if (isMetaschema) {
                         logger.info("Using Metaschema binding context for metaschema document")
                         DefaultBindingContext()
                     } else if (constraintSets.isNotEmpty()) {
                         logger.info("Using OSCAL binding context with constraint sets")
                         OscalValidationCommandExecutor().getBindingContext(constraintSets)
                     } else {
                         logger.info("Using OSCAL binding context")
                         context
                     }
                     
                     val inputStream = inputPath.toUri().toURL().openStream()
                     val loader = bindingContext.newBoundLoader()
                     logger.debug("Detecting document format...")
                     val formatResult = loader.detectFormat(inputStream, inputPath.toUri())
                     val sourceFormat = formatResult.getFormat()
                     logger.info("Detected format: $sourceFormat")
 
                     // Configure validation features
                     val configuration = DefaultConfiguration<ValidationFeature<*>>()
                     configuration.enableFeature(ValidationFeature.VALIDATE_GENERATE_PASS_FINDINGS)
                     val dataStream = formatResult.getDataStream()
 
                     // Perform validation
                     var validationResult: IValidationResult? = null
                     val modelResult = loader.detectModel(dataStream, inputPath.toUri(), sourceFormat)
                     
                     try {
                         // Load and validate document
                         logger.debug("Loading and validating document...")
                         val boundClass = modelResult.getBoundClass()
                         val modelInputStream = modelResult.getDataStream()
                         val document = loader.load(boundClass, sourceFormat, modelInputStream, inputPath.toUri())
                         logger.info("Document loaded successfully as: ${document.javaClass.simpleName}")
                                                  
                         // Perform constraint validation if needed
                         if ("disable-constraint" !in flags) {
                             logger.debug("Performing constraint validation...")
                             val constraintResult = bindingContext.validateWithConstraints(inputPath.toUri(), configuration)
                             validationResult = if (validationResult == null) {
                                 constraintResult
                             } else {
                                 AggregateValidationResult.aggregate(validationResult, constraintResult)
                             }
                             logger.info("Constraint validation completed")
                         }
                         
                         // Add validation findings to SARIF handler
                         if (validationResult != null) {
                             sarifHandler.addFindings(validationResult.findings)
                         }
                         
                         // Write SARIF output
                         sarifHandler.writeSarifOutput()
                         
                         if (validationResult !== null && !validationResult.isPassing()) {
                             MessageExitStatus(
                                 ExitCode.FAIL,
                                 "Validation failed. See SARIF output for details."
                             )
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
                 
                 Pair(exitStatus, sarifFilePath)
             
         } catch (e: Exception) {
             MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message) to ""
         }
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
         } catch (e: org.xml.sax.SAXParseException) {
             throw Exception("XML validation failed: ${e.message} at line ${e.lineNumber}, column ${e.columnNumber}")
         } catch (e: Exception) {
             throw Exception("XML validation failed", e)
         }
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
         """.trimIndent()
     }
 }