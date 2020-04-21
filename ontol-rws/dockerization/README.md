# ontol-rws - Ontology lite web services tester container

This section describe how to build a docker image that contains `ontol-rws` web services with a minimal configuration and a basic web tester in order to test OntoLRWS basic functionalities.

## Pre-requisites

* having Docker installed on your platform,
* having built `ontol-rws` from sources, see [here](../README.md##build-the-packages-from-sources)
* having built AlvisAE UI from sources, see [here](../../alvisae-ui/README.md)

---

## Build the Docker image

from alvisae main folder,

1. build a deployable package for the ontology/terminology extension of AlvisAE

```sh 
cd ontol-rws/dockerization
ant -f ../../alvisae-ui/AlvisAE.TyDI-Ext clean war
```

2. copy required war packages to current dir, so they'll be availlable for docker context

```sh
cp ../target/ontolrws.war .
cp ../../alvisae-ui/AlvisAE.TyDI-Ext/alvisaeTyDIExt.war .
```

3. build the actual Docker image

```sh
sudo docker image build -f Dockerfile -t bibliome/ontolrws.tester:1.0.1 .
```
**Notes**:

* during first build, image dependencies will be downloaded (e.g. Payara application server, about ~300MB)
* the built image will be stored locally, hence it doesn't need to be built each time you run it in a container

---

## Start a container to run the Docker image

**1.** Run the image

```sh
sudo docker run --rm --name ontolrws_tester -p 8181:8181  bibliome/ontolrws.tester:1.0.1
```

**Note**:

- once the above command is entered, the application server log will be displayed in the terminal, and you can begin testing after startup is completed  


**2.** Launch the tester

Using a web browser, open the web-tester page, https://localhost:8181/alvisae/ontolrws-tester/#biotope2015 , and enter `demo` / `demo` when asked for user/password

**Notes**:

- The web tester provides access to any ontologies loaded and configured in the container by specifying the ontology id in the url fragment (=suffix after the `#` in the url)
- To add more ontologies to the running tester Docker container, use dedicated web service as described [here](../README.md##management-and-test):

e.g.
```sh
curl --insecure --user demo:demo -X PUT --data-binary @/path/to/ontology/onto_002.obo  https://localhost:8181/ontolrws/TEST/projects/Onto002
```

- To add more ontologies to the tester Docker image,  simply copy the Obo files to `ontologies/` sub-folder, register them in the `config/config.yaml` file, and build again the Docker image.

- This Docker configuration is provided for testing purposes, specifically, **any changes made to the ontologies will be lost as soon as the container is stopped**. If you want to keep the modified ontology, use the dedicated web service to retrieve it before stopping the container :

```sh
wget --no-check-certificate --user demo --password demo -O /path/to/ontology/onto_002.obo https://localhost:8181/ontolrws/TEST/projects/Onto002/getfile
```


**3.** Stop the container
from an other `terminal` window

```sh
sudo docker stop ontolrws_tester
```

**Note**:

- since the container was started in an auto-clean up mode, it will be removed when stopped, but the corresponding image is still available for subsequent tests
