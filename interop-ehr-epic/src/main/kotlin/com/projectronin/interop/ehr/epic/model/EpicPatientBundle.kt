package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.ehr.model.helper.fhirResourceList

class EpicPatientBundle(override val raw: String) : FHIRBundle<EpicPatient>(raw) {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val links: List<Link> by lazy {
        jsonObject.fhirElementList("link", ::EpicLink)
    }

    override val resources: List<EpicPatient> by lazy {
        jsonObject.fhirResourceList("entry", "Patient", ::EpicPatient)
    }
}
