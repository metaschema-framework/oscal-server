package gov.nist.secauto.oscal.tools.server

import dev.metaschema.oscal.lib.model.Catalog
import dev.metaschema.oscal.lib.model.Profile
import dev.metaschema.oscal.lib.model.Document
import dev.metaschema.oscal.lib.format.Format
import dev.metaschema.oscal.lib.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentValidator {
    suspend fun validate(document: String, format: String): String = withContext(Dispatchers.IO) {
        val doc = Document.fromString(document, Format.valueOf(format.uppercase()))
        val results = doc.validate()
        ValidationResult.toJson(results)
    }
}

class DocumentConverter {
    suspend fun convert(document: String, fromFormat: String, toFormat: String): String = withContext(Dispatchers.IO) {
        val doc = Document.fromString(document, Format.valueOf(fromFormat.uppercase()))
        doc.toString(Format.valueOf(toFormat.uppercase()))
    }
}

class ProfileResolver {
    suspend fun resolve(profile: String, format: String): String = withContext(Dispatchers.IO) {
        val profileDoc = Profile.fromString(profile, Format.valueOf(format.uppercase()))
        val catalog = profileDoc.resolve()
        catalog.toString(Format.JSON)
    }
}

class PackageHandler {
    suspend fun createPackage(documents: List<Document>): String = withContext(Dispatchers.IO) {
        val docs = documents.map { doc ->
            Document.fromString(doc.content, Format.valueOf(doc.format.uppercase()))
        }
        Document.createPackage(docs).toString(Format.JSON)
    }
}

data class Document(
    val content: String,
    val format: String
)
