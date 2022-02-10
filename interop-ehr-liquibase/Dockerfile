FROM liquibase/liquibase
WORKDIR /liquibase/changelog

# Pull in the full directory structure and files
COPY src/main/resources/ /liquibase/changelog/

ENV CHANGE_LOG_FILE ehr/db/changelog/ehr.db.changelog-master.yaml
# DATA_LOAD is optional, by default will be skipped
ENV DATA_LOAD ${DATA_LOAD:-false}

RUN lpm add mysql --global
# Run Schema Update, then optionally run content update.
CMD liquibase --url=${JDBC_URL} --changeLogFile=$CHANGE_LOG_FILE update && \
    if [ "$DATA_LOAD" = "false" ] ;  \
    then echo 'No Additional data to load' ;  \
    else liquibase --url=${JDBC_URL} --changeLogFile=${DATA_LOAD} update ; \
    fi;
