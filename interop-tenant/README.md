[![codecov](https://codecov.io/gh/projectronin/interop-ehr/branch/master/graph/badge.svg?token=USQx2w2D36&flag=tenant)](https://app.codecov.io/gh/projectronin/interop-ehr/branch/master)
[![Tests](https://github.com/projectronin/interop-ehr/actions/workflows/tenant_test.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/tenant_test.yml)
[![Lint](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml)

# interop-tenant

Provides access to Tenant configuration data.

## Configuration

Tenant information is driven by a yaml file by default ([Example](src/test/resources/valid_tenants.yaml)). The file
location can be specified via spring configuration ```interop.tenant.config```, by default looks for a file
named ```tenants.yaml```on the CLASSPATH.

### Note:

Additional configuration options follow
Spring [ResourceLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/ResourceLoader.html#getResource-java.lang.String-)
specifications. For example ```file:/path/to/file.yaml``` will reference the local file system.
