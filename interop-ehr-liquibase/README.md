[![Lint](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-ehr/actions/workflows/lint.yml)

# interop-ehr-liquibase

Provides Liquibase changelogs defining the EHR configuration database used by InterOps.

## Conventions

### Indexes and Constraints ###

All names should be written in lowercase.

Note that there is a 64-character limit for MySQL; therefore, any examples that may exceed that limit should have
appropriate truncation or name simplification done such that they are still easy to follow.

#### Primary Key ####

pk_TABLE

#### Foreign Key ####

fk_TABLE_REFERENCEDTABLE

#### Unique ####

uk_TABLE_COLUMN uk_TABLE_COLUMN1_COLUMN2...

## Docker Support

This project is available in Docker format for facilitating liquibase operations, such as schema updates. The docker
image builds on the official [liquibase docker image](https://github.com/liquibase/docker) and incorporates the full
change log from this project. The docker image will run a liquibase update with the InterOps Queue schema upon start up.
This enables simple integration into a docker compose or container orchestration approach.

### Building the Docker Container Image

```shell
docker image build -t interop-ehr-liquibase . 
```

#### Environment Variables

1. JDBC_URL (Required) - The JDBC connection string for accessing the mysql instance to update.
2. CHANGE_LOG_FILE (defaults to ehr.db.changelog-master.yaml) - The changelog file that should be used for the schema
   update
3. DATA_LOAD (Optional) - The changelog that should be used for loading any required data after schema updates are
   applied.
   1. *Note: Files will need to be provided in the /liquibase/changelog/ directory.*
