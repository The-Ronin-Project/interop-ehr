[![Lint](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml)

# interop-ehr-build

Multi-project build containing components related to integrating with EHRs.

### Components

#### Core

The following represent the core components for integrating with EHRs.

* [interop-ehr-liquibase](interop-ehr-liquibase) - Provides Liquibase changelogs defining the EHR configuration database
  used by InterOps.
* [interop-tenant](interop-tenant) - Provides access to Tenant configuration data.
* [interop-transform](interop-transform) - Provides APIs for transforming data between states
* [interop-ehr](interop-ehr) - Abstraction layer around the various supported EHR vendors
* [interop-ehr-auth](interop-ehr-auth) - Provides support for Authentication with an EHR.
* [interop-ehr-factory](interop-ehr-factory) - Provides a factory mechanism to ease the burden of associating a vendor
  with a requested interface.

#### Vendors

The following represent vendor-specific integrations.

* [interop-ehr-epic](interop-ehr-epic) - Client for accessing EHR content from Epic based systems.
