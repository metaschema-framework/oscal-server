module gov.nist.secauto.oscal.tools.server {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.kotlin.coroutines;
    requires dev.metaschema.oscal.lib;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.kotlin;

    exports gov.nist.secauto.oscal.tools.server;
    exports gov.nist.secauto.oscal.tools.server.mcp;
}
