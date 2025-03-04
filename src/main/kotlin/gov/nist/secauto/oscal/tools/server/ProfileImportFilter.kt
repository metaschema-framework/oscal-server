/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.oscal.lib.OscalUtils
import gov.nist.secauto.oscal.lib.model.BackMatter
import gov.nist.secauto.oscal.lib.model.Profile
import gov.nist.secauto.oscal.lib.model.ProfileImport
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URI
import java.nio.file.Path
import java.util.UUID

/**
 * A filter that processes profile imports to handle special cases.
 */
class ProfileImportFilter {
    private val logger: Logger = LogManager.getLogger(ProfileImportFilter::class.java)

    /**
     * Process a profile to handle special import cases.
     * 
     * @param profile The profile to process
     * @param profilePath The path to the profile file
     * @return The processed profile
     */
    fun processProfile(profile: Profile, profilePath: Path): Profile {
        val imports = profile.getImports()
        if (imports.isEmpty()) {
            return profile
        }

        // Process each import
        for (import in imports) {
            val href = import.getHref() ?: continue
            
            // Check if it's an internal reference
            if (OscalUtils.isInternalReference(href)) {
                logger.info("Found internal reference import: $href")
                processInternalReference(profile, import, href, profilePath)
            }
        }

        return profile
    }

    /**
     * Process an internal reference import.
     * 
     * @param profile The profile containing the import
     * @param import The import to process
     * @param href The href of the import
     * @param profilePath The path to the profile file
     */
    private fun processInternalReference(profile: Profile, import: ProfileImport, href: URI, profilePath: Path) {
        // Extract the UUID from the href
        val uuid = OscalUtils.internalReferenceFragmentToId(href)
        val resourceUuid = UUID.fromString(uuid)
        
        // Find the resource in the back-matter
        val resource = profile.getResourceByUuid(resourceUuid)
        if (resource == null) {
            logger.warn("Resource not found for UUID: $uuid")
            return
        }
        
        // Get the rlink from the resource
        val rlink = OscalUtils.findMatchingRLink(resource, null)
        if (rlink == null) {
            logger.warn("No rlink found for resource: $uuid")
            return
        }
        
        val rlinkHref = rlink.getHref()
        if (rlinkHref == null) {
            logger.warn("No href found in rlink for resource: $uuid")
            return
        }
        
        logger.info("Found rlink href: $rlinkHref for resource: $uuid")
        
        // Get the parent directory of the profile
        val parentDir = profilePath.parent
        if (parentDir == null) {
            logger.warn("No parent directory found for profile: $profilePath")
            return
        }
        
        // Resolve the rlink href against the parent directory
        val resolvedPath = parentDir.resolve(rlinkHref.toString()).toUri()
        logger.info("Resolved rlink href to: $resolvedPath")
        
        // Update the import href to point directly to the resolved path
        import.setHref(resolvedPath)
        logger.info("Updated import href to: $resolvedPath")
    }
}
