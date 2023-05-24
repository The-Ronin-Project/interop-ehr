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
