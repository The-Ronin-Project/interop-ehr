package com.projectronin.interop.ehr.epic.apporchard.model.exceptions

import com.projectronin.interop.common.exceptions.LogMarkingException
import com.projectronin.interop.common.logmarkers.LogMarkers

class AppOrchardError(message: String?) : LogMarkingException(message ?: "No Epic Error Provided") {
    override val logMarker = LogMarkers.CLIENT_FAILURE
}
