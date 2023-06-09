package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.NoteInput
import com.projectronin.interop.tenant.config.model.Tenant

interface NoteService {
    /**
     * Sends a specified patient note based on the values in [noteInput], to the specified [tenant]
     * and returns a unique ID for the note created.
     */
    fun sendPatientNote(
        tenant: Tenant,
        noteInput: NoteInput
    ): String

    /**
     * Sends a specified patient note based on the values in [noteInput]
     * as an addendum to the note referenced in [parentDocumentId],
     * to the specified [tenant]
     * and returns a unique ID for the addendum note created.
     */
    fun sendPatientNoteAddendum(
        tenant: Tenant,
        noteInput: NoteInput,
        parentDocumentId: String
    ): String
}
