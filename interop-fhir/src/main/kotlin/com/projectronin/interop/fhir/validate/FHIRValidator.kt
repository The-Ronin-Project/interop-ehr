package com.projectronin.interop.fhir.validate

import mu.KotlinLogging
import org.hl7.fhir.r5.elementmodel.Manager
import org.hl7.fhir.utilities.TimeTracker
import org.hl7.fhir.utilities.VersionUtilities
import org.hl7.fhir.validation.ValidationEngine
import org.hl7.fhir.validation.cli.services.StandAloneValidatorFetcher

/**
 * Validator to detect any problems with the format or content of a FHIR resource.  Requires a target FHIR version as well
 * as an optional path to a FHIR implementation guide (defaults to the bundled guide).
 */
class FHIRValidator(version: String, implementationGuide: String = "/package.tgz") {
    private val logger = KotlinLogging.logger { }
    private val validator: ValidationEngine

    init {
        logger.info { "Starting FHIR validation engine for $version" }
        val definitions: String =
            VersionUtilities.packageForVersion(version).toString() + "#" + VersionUtilities.getCurrentVersion(version)
        val timeTracker = TimeTracker()

        logger.info { "Loading FHIR Version $version with definitions $definitions" }
        validator = ValidationEngine(definitions, version, timeTracker)

        logger.info { "Loading implementation guide from $implementationGuide" }
        val igLoader = ClasspathIgLoader(validator.pcm, validator.context, validator.version, validator.isDebug)
        igLoader.loadIg(validator.igs, validator.binaries, implementationGuide, false)

        val fetcher = StandAloneValidatorFetcher(validator.pcm, validator.context, validator)
        validator.fetcher = fetcher
        validator.context.locator = fetcher
        validator.prepare()
        logger.info { "FHIR validation engine started" }
    }

    /**
     * Runs validation against the provided FHIR [raw] and returns the [ValidationResult].
     */
    fun validate(raw: String): ValidationResult {
        logger.info("Starting FHIR validation.")
        val opOutcome = validator.validate(Manager.FhirFormat.JSON, raw.byteInputStream(), listOf())
        val result = ValidationResult(
            opOutcome.issue.map {
                ValidationEntry(
                    it.severity.toCode(),
                    it.location.toString(),
                    it.details.text
                )
            }
        )

        logger.info("FHIR validation complete.")
        if (result.hasInfo()) logger.info { "${result.getInfo().size} FHIR validation informational items found." }
        if (result.hasFatal()) logger.error { "${result.getFatal().size} FHIR fatal validation errors found." }
        if (result.hasError()) logger.error { "${result.getErrors().size} FHIR validation errors found." }
        if (result.hasWarn()) logger.warn { "${result.getWarnings().size} FHIR validation warnings found." }
        return result
    }
}
