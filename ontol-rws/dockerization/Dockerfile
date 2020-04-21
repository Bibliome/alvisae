FROM payara/server-full:5.201

LABEL maintainer="inrae/maiage bibliome@inrae.fr"

EXPOSE 8080/tcp 
EXPOSE 8181/tcp
EXPOSE 4848/tcp

ENV ontolbasedir /opt/payara/ontolrws
ENV ontoldir ${ontolbasedir}/ontologies/

RUN mkdir ${ontolbasedir}; \
mkdir ${ontoldir}; \
chown -R payara:payara ${ontolbasedir}

WORKDIR ${ontolbasedir}

COPY --chown=payara:payara ontolrws.war ${ontolbasedir}
COPY --chown=payara:payara alvisaeTyDIExt.war ${ontolbasedir}

COPY --chown=payara:payara config/config.yaml ${ontolbasedir}

COPY --chown=payara:payara ontologies ${ontoldir}

COPY --chown=payara:payara post-boot-commands.asadmin $CONFIG_DIR


