# alvisae-ui

AlvisAE is a tool designed to seamlessly perform the annotation of textual documents. It is a Web Application, no installation is needed to start using AlvisAE : Indeed, AlvisAE runs on any platform providing a modern web browser (such as Firefox, Safari, Chrome).

AlvisAE has been designed to facilitates the annotation of textual documents, with the goal of extracting knowledge. To do so, AlvisAE displays the text of the document, that can optionally includes some simple formatting;
Annotations are superimposed to the text, in a way that do not impair the readability.
Each Annotations belongs to a Type, and is displayed according to this Type (the list of Types and their associated colors are specified in the Annotation Schema).

---

## How to build the UI package (`AlvisAEGenericUI.war`) 

### What you'll need :
* [JDK __6__ ](https://www.oracle.com/java/technologies/javase-java-archive-javase6-downloads.html)
* [Apache Ant __v1.9.*__ ](https://ant.apache.org/index.html)  (i.e. a version compatible with JDK6, unlike Ant v1.10)
* [GWT __2.5.1__ ](http://www.gwtproject.org/versions.html)


### Compilation :
1. Retrieve source code ( e.g. `git clone ...`)

2. Create link to GWT in main folder 
```
cd alvisae
ln -s <path-to-your-gwt-parent-folder>/gwt-2.5.1 GWT2
```

3. build
```
ant -f alvisae-ui/AlvisAE.shared clean jar

ant -f alvisae-ui/AlvisAE.Core clean distribute

ant -f alvisae-ui/AlvisAE.TyDI-Ext clean distribute

ant -f alvisae-ui/AlvisAE.Editor clean distribute

ant -f AlvisAE.GenericUI clean war
```

The package is produced in `alvisae-ui/AlvisAE.GenericUI/AlvisAEGenericUI.war`

