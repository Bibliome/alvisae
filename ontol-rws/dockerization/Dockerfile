FROM payara/server-full:5.201

LABEL maintainer="inrae/maiage bibliome@inrae.fr"

EXPOSE 8080/tcp 
EXPOSE 8181/tcp
EXPOSE 4848/tcp


ENV ontoldir /opt/payara/
WORKDIR ${ontoldir}

COPY --chown=payara:payara ontolrws.war ${ontoldir}
COPY --chown=payara:payara alvisaeTyDIExt.war ${ontoldir}

COPY --chown=payara:payara config/config.yaml ${ontoldir}

COPY --chown=payara:payara ontologies ${ontoldir}

COPY --chown=payara:payara post-boot-commands.asadmin $CONFIG_DIR


