package gov.nist.secauto.oscal.tools.cli.core.commands

import gov.nist.secauto.metaschema.cli.commands.AbstractValidateContentCommand
import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet
import gov.nist.secauto.metaschema.core.model.xml.ExternalConstraintsModulePostProcessor
import gov.nist.secauto.metaschema.core.util.CollectionUtil
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.oscal.lib.OscalBindingContext
import org.apache.commons.cli.CommandLine
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import javax.xml.transform.Source
abstract class OscalValidateCommand : AbstractValidateContentCommand() {

    protected class OscalCommandExecutor() : ICommandExecutor {

        override fun execute(): ExitStatus {
            return MessageExitStatus(ExitCode.OK)
         }
    }
}