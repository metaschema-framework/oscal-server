package gov.nist.secauto.oscal.tools.server.logging

import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.Appender

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

object RequestLogHolder {
    val logs = ConcurrentHashMap<String, StringBuilder>()
}

@Plugin(name = "RequestCapture", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
class RequestCaptureAppender(name: String, filter: Filter?, layout: Layout<out Serializable>?) :
    AbstractAppender(name, filter, layout, false, null) {

    override fun append(event: LogEvent) {
        val requestId = event.contextData.getValue("requestId") as String?
        if (requestId != null) {
            val builder = RequestLogHolder.logs.computeIfAbsent(requestId.toString()) { StringBuilder() }
            val message = layout.toByteArray(event)
            builder.append(String(message, Charsets.UTF_8))
        }
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createAppender(
            @PluginAttribute("name") name: String,
            @PluginElement("Layout") layout: Layout<out Serializable>?,
            @PluginElement("Filter") filter: Filter?
        ): RequestCaptureAppender {
            val actualLayout = layout ?: PatternLayout.createDefaultLayout()
            return RequestCaptureAppender(name, filter, actualLayout)
        }
    }
}