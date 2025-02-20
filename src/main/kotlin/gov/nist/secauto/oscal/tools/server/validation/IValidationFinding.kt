/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.validation

import java.net.URI

interface IValidationFinding {
    enum class Level {
        ERROR,
        WARNING,
        INFO
    }

    val message: String
    val level: Level
    val location: Location?

    data class Location(
        val uri: URI?,
        val lineNumber: Int,
        val columnNumber: Int
    )
}

interface IValidationFindingHandler {
    fun handleFinding(finding: IValidationFinding)
}
