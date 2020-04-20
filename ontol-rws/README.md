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
    password: "37B51D194A7513E45B56F6524F2D51F2"
    ontologies: ["Ontbtp2019", "animal"]

  - id: 2
    name: "robert"
    password: "FE01CE2A7FBAC8FAFAED7C982A04E229"
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
## Build the packages from sources

### What you'll need

* JDK __8__ or later
* maven 3

```sh
cd ontol-rws
mvn clean package
```

Two packages are generated in `target/` sub-folder:

1. the deployable package, providing web services, is `ontolrws.war`

2. the jar package, providing cli services, is `ontolrws-classes.jar`
 

**Note**: currently, the cli only generates the hashed password that must be copied-pasted in the configuration file described above. 

```sh
$ java  -jar target/ontolrws-classes.jar aTrivialPassword anotherPass '!more$#Complicated1'
"C528B8E28222377010259DAEE9AD8C2F" <== aTrivialPassword
"8EC122935C70740FBD40DC538FEBDB93" <== anotherPass
"BF5E43B2263CC373B601893689EDC986" <== !more$#Complicated1

```

---

## Test within a Docker container

see [here](./dockerization/README.md)