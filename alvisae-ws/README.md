# AlvisAE

AlvisAE is an annotation editor for textual documents with the goal of extracting knowledge.

This project is the server-side of AlvisAE, and it includes : 

1. a double set of **web services** , subdivised in :

    * web services natively used by the Web UI for its normal operations on campaigns, documents, annotations, curations, tasks, workflows and users.

    * web services designed for interoperability with other projects, which implement the [AERO protocol](https://github.com/openminted/omtd-aero), whose usage is detailed [here](documentation/ws-aero-usage.md).

2. a **command line interface** allowing campaign initialisation and management (creating campaign & users, adding docs, importing/exporting annotations), whose usage is detailed [here](documentation/cli-usage.md).

---
## Web services
### How to install
We assume that the user is familiar with the technologies and has glassfish and postgresql already installed in the server

#### what you'll need

- `JDK` 7
- maven

#### get the source code

```sh
git clone https://github.com/ ... /alvisae
cd alvisae/alvisae-ws
```

#### create the package

**0.** [**Optional**] build AlvisaE shared lib

This project is shipped with a copy of the shared library, but in case that anything has changed within AlvisAE shared lib, you may have to update the local one as follow:

```sh
ant -f ../alvisae-ui/AlvisAE.shared clean jar
cp ../alvisae-ui/AlvisAE.shared/dist/AlvisAE.shared-1.0.jar ./lib/fr/inra/mig_bibliome/AlvisAE.shared/1.0/
```

**1.** Build webservice package

```sh
mvn compile package
```

#### create and initialize the database
[See here on how to initialize the database using the scala console](documentation/create-database.md)

#### Deploy the war package

**Note:** The endpoint `URL` where the web services will be deployed is important for effective pairing with the AlvisAE UI, you might need to check again this [documentation](./documentation/pairing-ui-ws.md) for more details.

1. copy the generated package to a directory accessible from the GlassFish server

```sh
cp target/cdxws-lift-1.0-SNAPSHOT.war /tmp/
```

2. deploy

```sh
${GLASSFISH_HOME}/bin/asadmin deploy --force  --contextroot <context root of the instance> \
 --name <name of the instance> /tmp/cdxws-lift-1.0-SNAPSHOT.war
```

#### Set-up database parameters
The content of property file look like this:

```
db.type=postgresql
db.server=bddev
db.port=5432
db.dbname=annotation
db.username=annotation_admin
db.password=*****
db.schema=aae_newinstance
```

One simplest way to setup the parameters is by using these two glassfish commands :

```sh
${GLASSFISH_HOME}/bin/asadmin set-web-context-param --name configFilePath --value 'path/to/the/config/file' 'the_application_name'

${GLASSFISH_HOME}/bin/asadmin restart-domain
```


