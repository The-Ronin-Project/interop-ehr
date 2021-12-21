package com.projectronin.interop.fhir.validate

/**
 * The categorized results of a FHIR validation call, includes errors, warning, and informational notes.  Constructed by a list of [ValidationEntry].
 */
class ValidationResult(entries: List<ValidationEntry>) {
    private val fatals = entries.filter { it.severity == "fatal" }
    private val errors = entries.filter { it.severity == "error" }
    private val warnings = entries.filter { it.severity == "warning" }
    private val infos = entries.filter { it.severity == "information" }

    /**
     * FHIR validation fatal error(s) were found.
     */
    fun hasFatal(): Boolean {
        return fatals.isNotEmpty()
    }

    /**
     * FHIR validation fatal error(s) found.
     */
    fun getFatal(): List<ValidationEntry> {
        return fatals
    }

    /**
     * FHIR validation error(s) were found.
     */
    fun hasError(): Boolean {
        return errors.isNotEmpty()
    }

    /**
     * FHIR validation error(s) found.
     */
    fun getErrors(): List<ValidationEntry> {
        return errors
    }

    /**
     * FHIR validation warnings(s) were found.
     */
    fun hasWarn(): Boolean {
        return warnings.isNotEmpty()
    }

    /**
     * FHIR validation warnings(s) found.
     */
    fun getWarnings(): List<ValidationEntry> {
        return warnings
    }

    /**
     * FHIR validation information(s) were found.
     */
    fun hasInfo(): Boolean {
        return infos.isNotEmpty()
    }

    /**
     * FHIR validation information(s) found.
     */
    fun getInfo(): List<ValidationEntry> {
        return infos
    }
}
