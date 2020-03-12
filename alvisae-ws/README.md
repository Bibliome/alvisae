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

#### get the source code and package the web service

```sh
git clone https://github.com/ ... /alvisae
cd alvisae/alvisae-ws
mvn compile package
```

#### create and initialize the database
[See here on how to initialize the database using the scala console](documentation/create-database.md)


#### deploy the war
copy the generated package to a directory accessible from the GlassFish server

```
cp target/cdxws-lift-1.0-SNAPSHOT.war /tmp/
```

login as a user that is authorized to deploy packages on the Glassfish server

```
su glassfish \
cd \
glassfishv3/bin/asadmin  -p 5848 deploy --force  --contextroot <context root of the instance> --name <name of the instance> /tmp/cdxws-lift-1.0-SNAPSHOT.war \
```

#### set-up database parameters
The content of property file look like this. Save the file with name of the following format <user>.<hostname>.props

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

```
GLASSFISH_HOME/bin/asadmin set-web-context-param --name configFilePath --value 'path/to/the/config/file' 'the_application_name'

GLASSFISH_HOME/bin/asadmin restart-domain
```


