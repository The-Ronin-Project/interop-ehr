[![codecov](https://codecov.io/gh/projectronin/interop-ehr/branch/master/graph/badge.svg?token=USQx2w2D36&flag=fhir-ronin)](https://app.codecov.io/gh/projectronin/interop-ehr/branch/master)
[![Tests](https://github.com/projectronin/interop-ehr/actions/workflows/fhir-ronin_test.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/fhir-ronin_test.yml)
[![Lint](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml)

# interop-fhir-ronin

Provides the Ronin FHIR profiles in a codified form.

## RCDM Updates

As of 3.19.0, all profiles should indicate their individual version and the overall RCDM version when they were last
updated. If you are making new profiles or making updates to match changes to existing ones, the profiles you touch
should update their `profileVersion` and `rcdmVersion` to meet the current spec. For new RCDM versions, you should add
any missing versions to the end of [RCDMVersion](src/main/kotlin/com/projectronin/interop/fhir/ronin/RCDMVersion.kt)

## Multiple Profile Resources

To orient you to the com.projectronin.interop.fhir.ronin.resource package,
below is an outline of how it implements profiles when the
[RCDM](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home.html) 
has defined multiple profiles that apply the same FHIR resource
The following outlines shows a sample order of inheritance for classes
that use the MultipleProfileResource convention in the 
com.projectronin.interop.fhir.ronin.resource package:
Observation, Condition, and DiagnosticReport. 

A qualifier class selects a resource profile based on attribute values:

1. RoninObservations 
    - potentialProfiles: List<BaseProfile<Observation>>
    - defaultProfile: BaseProfile<Observation>
2. MultipleProfileResource<Observation>
3. BaseProfile<Observation>

[Ronin Observation Blood Pressure](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation-Blood-Pressure.html)
(and many siblings in the observation package)

1. observation.RoninBloodPressure
2. observation.BaseRoninVitalSign
3. observation.BaseRoninProfileObservation
4. observation.BaseRoninObservation
5. USCoreBasedProfile
6. BaseRoninProfile<Observation>
7. BaseProfile<Observation>
8. BaseValidator<Observation>, ProfileTransformer<Observation>, ProfileQualifier<Observation>

[Ronin Observation Laboratory Report](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation-Laboratory-Result.html)

1. observation.RoninLaboratoryReport
2. observation.BaseRoninProfileObservation
3. observation.BaseRoninObservation
4. USCoreBasedProfile
5. BaseRoninProfile<Observation>
6. BaseProfile<Observation>
7. BaseValidator<Observation>, ProfileTransformer<Observation>, ProfileQualifier<Observation>

[Ronin Observation Staging Related](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation-Staging-Related.html)

1. observation.RoninStagingRelated
2. observation.BaseRoninProfileObservation
3. observation.BaseRoninObservation
4. USCoreBasedProfile
5. BaseRoninProfile<Observation>
6. BaseProfile<Observation>
7. BaseValidator<Observation>, ProfileTransformer<Observation>, ProfileQualifier<Observation>

[Ronin Observation](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation-Staging-Related.html) (RoninObservations defaultProfile)

1. observation.RoninObservation
2. observation.BaseRoninObservation
3. USCoreBasedProfile
4. BaseRoninProfile<Observation>
5. BaseProfile<Observation>
6. BaseValidator<Observation>, ProfileTransformer<Observation>, ProfileQualifier<Observation>

