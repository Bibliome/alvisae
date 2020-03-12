# Creating Campaign, adding Documents and importing Annotations with the Command Line Interface

## How to build the UI package (`AlvisAEGenericUI.war`) 

### What you'll need :

* JDK 7
* [Apache Maven](https://maven.apache.org/download.cgi)


### prerequisite

1. Get the source code
2. Set-up database parameters in a property file (named `dbparam.props` hereafter)

## Build the CLI program

### go to AlvisAE server-side folder

```bash
cd alvisae-ws
```

### generate the CLI jar


A. on <ins>legacy platform</ins>, i.e. using **Maven 2 (and JDK6)**:

```bash
mvn clean compile assembly:single
```

or, more likely,

B. using **Maven 3 (and JDK7)**, on a clean platform (i.e. with an empty local maven2 repo)

```bash
mvn -Dhttps.protocols=TLSv1.2 -U clean compile assembly:single
```

**Note:** `-U`  and  `-Dhttps.protocols=TLSv1.2` instruct maven to force checking Maven2 repository with legacy protocol in order to download dependancies and artefacts.

The jar package `AlvisAE-cli.jar` will be generated in the `target` subdirectory 

## modus operandi

the command line interface program provides several distinct commands:
* **`create-user`** 
 create a new user in the AlvisAE DataBase
* **`create-campaign`**
 create a new campaign in the AlvisAE DataBase, with its Annotation schema and Workflow definition
* **`add-documents`** 
 add documents to the AlvisAE DataBase, including their optional HTML formattings
* **`import-annotations`**
 import annotations related to an already added documents and associate them to a (user, task, campaign)
* **`assign-document`** 
 assign a document to a specific campaign (required when no annotation has been imported for this doc)
* **`export-campaign`** 
 export the documents and annotations of the specified campaign as a zip archive

All of these commands require a mandatory parameter (`dbparam.props`) which specifies the AlvisAE Database to work with. 

### Creating a new user

Will create a new user with the specified name and password

<ins>command:</ins>
* **`--create-user`**

<ins>parameters:</ins>
* **`-p`* properties file containing connection parameters
* **`--userName`** user name
* **`--password`** user's password

<ins>standard ouput:</ins>
* `userInternalId` \t `"userName"`

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --create-user -p dbparam.props --userName foo --password bar

...
4	"foo"
```

### Creating a new campaign

Will create a new campaign with the specified name, Annotation schema and Workflow definition

<ins>command:</ins>
* **`--create-campaign`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`-c`** campaign name
* **`-s`** JSON schema definition file
* [**`-w`**]_ XML workflow definition file
* [**`--guidelines`**] URL to the annotation guidelines of the campaign

<ins>standard ouput:</ins>
* `campaignInternalId` \t `"campaignName"`

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --create-campaign -p dbparam.props -c "New Campaign #1" -s embedded_schema.json -w workflow.xml

...

14	"New Campaign #1"
```


### Adding new document(s) to AlvisAE DB

Will add all documents present in the specified directory (files with .json extension).

**Note**: An optional unique document identifier may be set at the property key `DocumentID` in the JSON file (makes the following import-annotations step easier).

**Note**: If present, `--add-documents` will store the first annotation set with type `HtmlAnnotation` as the HTML layout of the document.

<ins>command:</ins>
* **`--add-documents`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`-d`** documents input directory

<ins>standard ouput (one line per added doc):</ins>
* `documentInternalId` \t `"importedDocumentPath"` \t `"optionalDocumentExternalId"`

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --add-documents  -p dbparam.props -d /tmp/json

...
2014	"/tmp/json/BTID-20052.json"	"BTID20052"
2015	"/tmp/json/BTID-60597.json"
```

### Importing Annotations into AlvisAE DB (one AnnotationSet at a time)

Will import the Annotation Set present in the specified JSON file, and associate it to the specified document, user, task and campaign
(as side effects, the document and the user will be associated to the campaign, the document will be assigned to the user)

<ins>command:</ins>
* **`--import-annotations`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`--annotationSetFile`** JSON file containing the Annotation Set to be imported
* **`--docInternalId`** internal Id of Document corresponding to the imported Annotation Set
or,  **`--docExternalId`**  external id of the Document (as specified during document import)
* **`-c`** campaign name
or,  **`--campaignId`**  internal id of the Campaign where to import the Annotation Set
* **`--taskName`** name of the Task where to import the Annotation Set
* **`--userName`** name of the User to be associated with the imported Annotation Set
or, **`--userId`** internal id of the User to be associated with the imported Annotation Set
* [**`--annotationSetId`**] id of the AnnotationSet (in the JSON file) to import, by default the first of type UserAnnotationSet will be imported

<ins>standard ouput:</ins>

_na_

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --import-annotations  -p dbparam.props --annotationSetFile /tmp/json/BTID-20052.json --docInternalId 2014 -c "New Campaign #1" --userName foo --taskName "default-task"

```






### Importing Annotations into AlvisAE DB (bulk import)

Will import, in the specified campaign, all user's Annotation Sets contained in the json files present in the specified directory.
The corresponding documents (identified by their external ids) must have already been imported in the database.
Only Annotation sets corresponding to actual user & task (identified by their internal ids, or translated thanks to `taskList` and `userList`) will be imported
(as side effects, the document and the user will be associated to the campaign, the document will be assigned to the user)

<ins>command:</ins>
* **`--import-annotations`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`-c`** campaign name
or,  **`--campaignId`**  internal id of the Campaign where to import the Annotation Set
* **`-d`* input directory

* [**`--taskList`**]  CSV file containing the map of task ids and name (if none specified, the internal id are reused)
* [**`--userList`**]  CSV file containing the map of users ids and name (if none specified, the internal id are reused)

**Notes** about `taskList` and `userList`

* In the JSON format, task and user are specified by numeric internal id. `taskList` (and `userList`) is used to convert the numeric id found in the Json file to the task name (respectively user name) found in the destination campaign.  
* Since these files are produced during Json [[CLICreateCampaign#Export-a-campaign|export of a campaign]] , this allows seamless import (in the same database) to another campaign sharing the same workflow; Just reuse the `taskList` file unchanged during import. If the task names (or user names) are different between campaigns, you'll need to explicitly make the mapping by editing the second column of `taskList` (resp. `userList`)


<ins>standard ouput:</ins>

_na_

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --import-annotations  -p dbparam.props -d /tmp/json/ -c "New Campaign #1""

```








### Assigning a document to a specific campaign

Will associate the document to the specified campaign, so it can be later on assigned to any user participating in the campaign (via the web UI)

<ins>command:</ins>
* **`--assign-document`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`--docInternalId`** internal Id of Document corresponding to the imported Annotation Set
or,  **`--docExternalId`**  external id of the Document (as specified during document import)
* **`-c`** campaign name
or,  **`--campaignId`**  internal id of the Campaign where to import the Annotation Set

<ins>standard ouput:</ins>

_na_

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --assign-document  -p dbparam.props --docInternalId 2014 -c "New Campaign #1" 

```

### Export a campaign

Will create a zip archive containing the documents and the annotations of the specified campaign

<ins>command:</ins>
* **`--export-campaign`**

<ins>parameters:</ins>
* **`-p`** properties file containing connection parameters
* **`-c`** campaign name
or,  **`--campaignId`**  internal id of the Campaign to be exported
* [**`-o`**] output directory where the export archive file will be created
* [**`--format`**] format of the exported files ( +CSV+ | Json )

<ins>standard ouput:</ins>
* `zipArchiveFileName`

<ins>example:</ins>
```bash
 java -jar target/AlvisAE-cli.jar --export-campaign -p dbparam.props -c "New Campaign #1" -f json -o /tmp

...
/tmp/aae_14.zip
```