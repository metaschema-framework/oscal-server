package gov.nist.secauto.metaschema.core.metapath.item
import gov.nist.secauto.metaschema.databind.io.SerializationFeature;
import gov.nist.secauto.metaschema.core.model.IBoundObject;
import gov.nist.secauto.metaschema.core.metapath.ICollectionValue
import gov.nist.secauto.metaschema.core.metapath.ISequence
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyAtomicItem
import gov.nist.secauto.metaschema.core.metapath.item.function.IArrayItem
import gov.nist.secauto.metaschema.core.metapath.item.function.IMapItem
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.metaschema.databind.io.Format
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Produces a JSON representation of a Metapath sequence.
 */
class JsonItemWriter(
    private val writer: PrintWriter,
    private val bindingContext: IBindingContext
) : IItemWriter {
    private var indentLevel = 0
    private val visitor = Visitor()

    companion object {
        private const val INDENT = "  " // 2 spaces for indentation
    }

    private fun writeIndent() {
        repeat(indentLevel) {
            writer.append(INDENT)
        }
    }

private fun Any?.serializeValue(): String {
    return try {
        when (this) {
            null -> "null"
            is String -> "\"${escapeJson()}\""
            is Number, is Boolean -> toString()
            else -> try {
                StringWriter().use { stringWriter ->
                    val boundObject = this as? IBoundObject ?: run {
                        // Add debug information before potential NPE
                        println("Debug: toString() called on object of type: ${this?.javaClass}")
                        return "\"${toString()?.escapeJson() ?: "null"}\""
                    }
                    
                    val boundClass = boundObject::class.java
                    println("Debug: Processing bound class: ${boundClass.name}")
                    
                    val boundDefinition = bindingContext.getBoundDefinitionForClass(boundClass)
                    println("Debug: Bound definition: ${boundDefinition != null}")                    
                    if (boundDefinition != null) {
                        val serializer = bindingContext.newSerializer(Format.JSON, boundClass)
                        serializer.set(SerializationFeature.SERIALIZE_ROOT, false);
                        serializer.serialize(boundObject, stringWriter)
                    println(stringWriter.toString());
                        stringWriter.toString()
                    } else {
                        "\"${boundObject.toString()?.escapeJson() ?: "null"}\""
                    }
                }
            } catch (e: Exception) {
                StringWriter().use { sw ->
                    PrintWriter(sw).use { pw ->
                        e.printStackTrace(pw)
                        println("Inner Exception Stack Trace:")
                        println(sw.toString())
                        "\"Error during serialization: ${e.message}\nStack trace: ${sw.toString().escapeJson()}\""
                    }
                }
            }
        }
    } catch (e: Exception) {
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                e.printStackTrace(pw)
                println("Outer Exception Stack Trace:")
                println(sw.toString())
                "\"Error in main serializeValue: ${e.message}\nStack trace: ${sw.toString().escapeJson()}\""
            }
        }
    }
}

    private fun writeJsonObject(
        type: String,
        item: IItem,
        additionalContent: JsonItemWriter.() -> Unit = {}
    ) {
        writer.append("{\n")
        indentLevel++
        writeIndent()
        writer.append("\"type\": \"$type\",\n")
        writeIndent()
        writer.append("\"value\": ${item.getValue().serializeValue()},\n")
        additionalContent()
        indentLevel--
        writeIndent()
        writer.append("}")
    }

    private fun writeJsonArray(
        name: String,
        content: JsonItemWriter.() -> Unit
    ) {
        writeIndent()
        writer.append("\"$name\": [\n")
        indentLevel++
        content()
        writer.append('\n')
        indentLevel--
        writeIndent()
        writer.append("]\n")
    }

    override fun writeSequence(sequence: ISequence<*>) {
        writer.append("{\n")
        indentLevel++
        writeIndent()
        writer.append("\"type\": \"sequence\",\n")
        writeJsonArray("items") {
            var first = true
            sequence.forEach { item ->
                if (!first) writer.append(",\n")
                writeIndent()
                item.accept(visitor)
                first = false
            }
        }
        indentLevel--
        writeIndent()
        writer.append("}")
    }

    override fun writeArray(array: IArrayItem<*>) {
        writeJsonObject("array", array) {
            writeJsonArray("values") {
                var first = true
                array.forEach { value ->
                    checkNotNull(value)
                    if (!first) writer.append(",\n")
                    writeIndent()
                    writeCollectionValue(value)
                    first = false
                }
            }
        }
    }

    override fun writeMap(map: IMapItem<*>) {
        writeJsonObject("map", map) {
            writeJsonArray("entries") {
                var first = true
                val mapValues = map.values
                mapValues.forEach { value ->
                    checkNotNull(value)
                    if (!first) writer.append(",\n")
                    writeIndent()
                    writeCollectionValue(value)
                    first = false
                }
            }
        }
    }

    override fun writeNode(node: INodeItem) {
        writeJsonObject("node", node) {
            writeIndent()
            writer.append("\"baseUri\": \"${node.baseUri.toString().escapeJson()}\",\n")
            writeIndent()
            writer.append("\"path\": \"${node.metapath.escapeJson()}\"\n")
        }
    }

    override fun writeAtomicValue(node: IAnyAtomicItem) {
        writeJsonObject("atomic", node) {
            writeIndent()
            writer.append("\"text\": \"${node.asString().escapeJson()}\"\n")
        }
    }

    protected fun writeCollectionValue(value: ICollectionValue) {
        when (value) {
            is IItem -> value.accept(visitor)
            is ISequence<*> -> writeSequence(value)
        }
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private fun String.escapeJson(): String = buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') {
                    append(String.format("\\u%04x", char.code))
                } else {
                    append(char)
                }
            }
        }
    }

    private inner class Visitor : IItemVisitor {
        override fun visit(array: IArrayItem<*>) = writeArray(array)
        override fun visit(map: IMapItem<*>) = writeMap(map)
        override fun visit(node: INodeItem) = writeNode(node)
        override fun visit(node: IAnyAtomicItem) = writeAtomicValue(node)
    }
}