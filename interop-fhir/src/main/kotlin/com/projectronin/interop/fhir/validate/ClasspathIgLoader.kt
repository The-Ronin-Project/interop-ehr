package com.projectronin.interop.fhir.validate

import org.hl7.fhir.r5.context.SimpleWorkerContext
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.NpmPackage
import org.hl7.fhir.validation.IgLoader

/**
 * Loads a FHIR Implementation guide directly from the class path.
 */
class ClasspathIgLoader(
    packageCacheManager: FilesystemPackageCacheManager,
    context: SimpleWorkerContext,
    theVersion: String,
    isDebug: Boolean
) : IgLoader(packageCacheManager, context, theVersion, isDebug) {
    /**
     * Override loadIgSource that will attempt to read a given [src] from the class path, otherwise fall back to existing logic.
     */
    override fun loadIgSource(src: String?, recursive: Boolean, explore: Boolean): MutableMap<String, ByteArray> {
        val resourceStream = this::class.java.getResourceAsStream(src)
        resourceStream?.let { return loadPackage(NpmPackage.fromPackage(resourceStream)) }
        return super.loadIgSource(src, recursive, explore)
    }
}
