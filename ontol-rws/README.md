# ontol-rws - Ontology lite server rest web services for AlvisAE 

## Install & configure rest web services in GlassFish 5.1

Let's say you want to set up a new instance called _Training_ :

1. Download the latest version of [`ontolrws.war`]( https://github.com/Bibliome/alvisae-1/raw/master/ontol-rws/target/ontolrws.war ) package from this repository

2. Deploy war package in GlassFish using admin console CLI :

```sh
${glassfish.home}/bin/asadmin  deploy --force  --contextroot '/ontolrws/training' --name 'OntoLRWS_Training' ontolrws.war
```

3. prepare config file containing list of ontologies and users :

e.g.  `/pathto/configFile/ontolrws_settings.yaml` :
```yaml
ontologies:

  - id: "Ontbtp2019"
    longName: "Ontobiotope_2019"
    filePath: "/home/dev/projects/inrae/ontologies/OntoBiotope2019.obo"

  - id: "animal"
    longName: "Animal"
    filePath: "/home/dev/projects/inrae/ontologies/animal.obo"

users:

  - id: 1
    name: "foo"
    password: "#!@>$$"
    ontologies: ["Ontbtp2019", "animal"]

  - id: 2
    name: "robert"
    password: "#!@>$$"
    ontologies: ["Ontbtp2019"]
```

**Note**: yaml file MUST not contain any tab character.


4. add a parameter, **after** web services are deployed, so they can locate their configuration file :

```sh
${glassfish.home}/bin/asadmin set-web-context-param --name=configFilePath --value=/pathto/configFile/ontolrws_settings.yaml --ignoredescriptoritem=true OntoLRWS_Training
```

5. test web services
```sh
curl -i --user foo:bar  http://glassfishserver:8080/ontolrws/training/user/me
```

6. Configure AlvisAE schema to access Ontol/termino ressource with new instance url as a prefix :

e.g.  `http://glassfishserver:8080/ontolrws/training/projects/` `[ontologies.id]` `/`...

---
## Build the war package from sources

### What you'll need

* JDK __8__ or later
* maven 3

```sh
cd ontol-rws
mvn clean package
```

The deployable package is generated in `target/ontolrws.war`
