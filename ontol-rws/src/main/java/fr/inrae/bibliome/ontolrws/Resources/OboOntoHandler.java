package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;

/**
 *
 * @author fpa
 */
public class OboOntoHandler implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(OboOntoHandler.class.getName());

    public static final String ROOT_ID = "0";

    private static final String OBOBASE_URI = "http://purl.obolibrary.org/obo/";

    private static final String SEMCLASSID_PREFIX = "AE";
    private static final String SEMCLASSVERSION_PROPNAME = "#semclass-version";

    private static final IRI OBODBXREF_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
    private static final IRI OBONAMESPACE_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasOBONamespace");

    private static final IRI OBOEXACTSYN_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");
    private static final IRI OBORELSYN_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");

    private static final IRI OBOID_URI = IRI.create("http://www.geneontology.org/formats/oboInOwl#id");

    private static final IRI XSDSTR_URI = IRI.create("http://www.w3.org/2001/XMLSchema#string");
    private static final IRI XSDINT_URI = IRI.create("http://www.w3.org/2001/XMLSchema#integer");

    public static OboOntoHandler getHandler(Ontology ontoConfig) {
        return new OboOntoHandler(ontoConfig);
    }

    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final Map<Ontology, OWLOntology> loadedOntologies = new HashMap<>();
    private static final OWLDataFactory df = manager.getOWLDataFactory();
    private static final PrefixManager pm = new DefaultPrefixManager(OBOBASE_URI);

    private final OWLOntology onto;
    private final Ontology config;
    private final IRI SEMCLASSVERSPROP_IRI;

    protected OboOntoHandler(Ontology ontoConfig) {
        config = ontoConfig;

        //avoid reloading same ontology again and again, especially because Owl-Api is aggressively caching them anyway
        if (loadedOntologies.containsKey(ontoConfig)) {
            logger.log(Level.INFO, "Reusing already loaded ontology : {0}", ontoConfig.getLongName());
            onto = loadedOntologies.get(ontoConfig);
            SEMCLASSVERSPROP_IRI = getSemClassVersionIri(onto);
        } else {
            File file = new File(ontoConfig.getFilePath());
            try {
                logger.log(Level.INFO, "Loading ontology from file : {0}", ontoConfig.getFilePath());
                onto = manager.loadOntologyFromOntologyDocument(file);
                SEMCLASSVERSPROP_IRI = getSemClassVersionIri(onto);
                checkAndSetVersionning();
                loadedOntologies.put(ontoConfig, onto);
            } catch (OWLOntologyCreationException ex) {
                throw new IllegalArgumentException("Could not load ontology file: " + ontoConfig.getFilePath(), ex);
            }
        }
    }

    @Override
    public void close() {
    }

    public static boolean isRootId(String semClassId) {
        return ROOT_ID.equals(semClassId);
    }

    private static String oboClassIdtoOwlClassId(String oboClassId) {
        return oboClassId.replace(":", "_");
    }

    private static String owlClassIdtoOboClassId(String owlClassId) {
        return owlClassId.replace("_", ":");
    }

    public static String getSemClassIdOf(IRI semClassIri) {
        return owlClassIdtoOboClassId(semClassIri.getShortForm());
    }

    public static String getSemClassIdOf(OWLClass semClass) {
        return getSemClassIdOf(semClass.getIRI());
    }

    public File getOntoFilePath() {
        return new File(config.getFilePath());
    }

    public Stream<OWLClass> getRootSemanticClasses() {
        OWLClass thing = df.getOWLThing();

        //amongst all classes in ontology, find those who have only OWLThing as superclass
        return onto.classesInSignature()
                .filter(c -> onto.subClassAxiomsForSubClass(c)
                .filter(s -> !thing.equals(s.getSuperClass()))
                .count() == 0);
    }

    public Stream<OWLClass> getSemanticClassesForId(String semClassId) {
        if (isRootId(semClassId)) {
            return getRootSemanticClasses();
        } else {
            String scId = oboClassIdtoOwlClassId(semClassId);
            //representation of the looked up class
            OWLClass semClassRepre = df.getOWLClass(":" + scId, pm);

            //is this class present in ontology?
            return onto.classesInSignature()
                    .filter(c -> c.getIRI().equals(semClassRepre.getIRI()));

        }
    }

    public Stream<OWLClass> getHyperonymsOf(OWLClass semClass) {
        return onto.subClassAxiomsForSubClass(semClass)
                .map(sca -> sca.getSuperClass().asOWLClass());
    }

    public Stream<OWLClass> getHyponymsOf(OWLClass semClass) {
        return onto.subClassAxiomsForSuperClass(semClass)
                .map(sca -> sca.getSubClass().asOWLClass());
    }

    private boolean isHyponymsOf(OWLClass hyper, OWLClass finalHypo) {
        return getHyponymsOf(hyper).map(
                hypo -> {
                    if (finalHypo.getIRI().equals(hypo.getIRI())) {
                        //found looked-up hyponym!
                        return true;
                    } else {
                        //carry on walking DAG until reaching leaves
                        return isHyponymsOf(hypo, finalHypo);
                    }
                }
        ).anyMatch(e -> e);
    }

    public boolean isHyponymsOf(String semClassId, String hyponymId) {
        //root class can not be hyponym of any class, neither any class of itself
        if (isRootId(hyponymId) || semClassId.equals(hyponymId)) {
            return false;
        } else {
            OWLClass finalHypo = getSemanticClassesForId(hyponymId).findFirst().get();
            return getSemanticClassesForId(semClassId).map(
                    c -> isHyponymsOf(c, finalHypo)
            )
                    //allow short-circuiting whenever final hyponym is found
                    .anyMatch(e -> e);
        }
    }

    private Structs.DetailSemClassNTerms initSemClassStruct(OWLClass semClass) {
        final Structs.DetailSemClassNTerms srStruct = new Structs.DetailSemClassNTerms();

        //retrieve and filter class properties
        onto.annotationAssertionAxioms(semClass.getIRI())
                .forEach(aaa -> {
                    String value = null;

                    OWLDatatype dataType = aaa.getValue().datatypesInSignature().findFirst().orElse(null);
                    OWLAnnotationValue propValue = aaa.getValue().annotationValue();
                    if (propValue.isLiteral()) {
                        value = propValue.asLiteral().get().getLiteral();
                    }

                    if (value != null) {
                        IRI propNameIRI = aaa.getProperty().getIRI();

                        //keep only a subset of props
                        if (OBOID_URI.equals(propNameIRI)) {
                            srStruct.groupId = value;
                        } else if (df.getRDFSLabel().getIRI().equals(propNameIRI)) {
                            srStruct.canonicLabel = value;
                        } else if (OBORELSYN_IRI.equals(propNameIRI)) {
                            srStruct.termMembers.add(Structs.Term.createRelatedSynonym(value));
                        } else if (OBOEXACTSYN_IRI.equals(propNameIRI)) {
                            srStruct.termMembers.add(Structs.Term.createExactSynonym(value));
                        } else if (OBODBXREF_IRI.equals(propNameIRI)) {
                        } else if (SEMCLASSVERSPROP_IRI.equals(propNameIRI)) {
                            if (XSDINT_URI.equals(dataType.getIRI())) {
                                srStruct.version = Integer.valueOf(value);
                            } else {
                                srStruct.version = 0;
                            }
                        } else {

                        }
                    }

                });

        return srStruct;
    }

    public Structs.DetailSemClassNTerms getSemanticClassData(OWLClass semClass, boolean withTerms) {

        Structs.DetailSemClassNTerms classStruct;

        List<OWLClass> hypoClasses;

        if (semClass == null) {
            classStruct = new Structs.DetailSemClassNTerms();
            //virtual root class
            classStruct.groupId = ROOT_ID;
            classStruct.canonicId = ROOT_ID;
            classStruct.canonicLabel = "";
            classStruct.version = 1;

            //actual root classes presented as hyponyms of the virtual root
            hypoClasses = getRootSemanticClasses().collect(Collectors.toList());

        } else {

            //fill up Semantic class structure from OwlClass properties
            classStruct = initSemClassStruct(semClass);
            //virtual canonic id
            classStruct.canonicId = classStruct.groupId + "-C";

            if (withTerms) {
                //produce virtual term ids
                IntStream.range(0, classStruct.termMembers.size()).forEach(
                        i -> classStruct.termMembers.get(i).id = classStruct.groupId + "-" + i
                );
                //add virtual canonic term
                Structs.Term canonic = Structs.Term.createCanonic(classStruct.canonicLabel);
                canonic.id = classStruct.canonicId;
                classStruct.termMembers.add(0, canonic);
            }

            classStruct.hypoGroupIds = getHyperonymsOf(semClass)
                    .map(hyper -> getSemClassIdOf(hyper))
                    .collect(Collectors.toList());
            if (classStruct.hypoGroupIds.isEmpty()) {
                //actual root classes are presented as hyperonym of the virtual root 
                classStruct.hypoGroupIds.add(ROOT_ID);
            }

            hypoClasses = getHyponymsOf(semClass).collect(Collectors.toList());
        }

        hypoClasses.forEach(hypoClass -> classStruct.hypoClasses
                .add(getSemanticClassData(hypoClass, false))
        );
        classStruct.hypoClasses.sort(
                (Structs.SemClass c1, Structs.SemClass c2) -> c1.canonicLabel.compareTo(c2.canonicLabel)
        );

        return classStruct;
    }

    public Stream<String> getClassIncludingTerm(String form) {
        return getClassesIdForLabelPredicate(
                //exact match 
                ax -> ax.getAnnotation().getValue().annotationValue().asLiteral().get().getLiteral().equals(form)
        );
    }

    public Stream<String> getClassesIdForMatchingLabelPattern(String pattern) {
        return getClassesIdForLabelPredicate(
                //label value contains searched pattern
                ax -> ax.getAnnotation().getValue().annotationValue().asLiteral().get().getLiteral().contains(pattern)
        );
    }

    private Stream<String> getClassesIdForLabelPredicate(Predicate<OWLAnnotationAssertionAxiom> predicate) {

        IRI rdfLabelIri = df.getRDFSLabel().getIRI();
        return onto.axioms(AxiomType.ANNOTATION_ASSERTION)
                .filter(
                        //subject is an Obo semantic class
                        ax -> ax.getSubject().asIRI().get().getNamespace().equals(OBOBASE_URI)
                )
                .filter(
                        //search within RDF labels (=canonic label) and every synonyms of the class
                        ax -> {
                            IRI propIri = ax.getAnnotation().getProperty().getIRI();
                            return rdfLabelIri.equals(propIri)
                            || OBOEXACTSYN_IRI.equals(propIri)
                            || OBORELSYN_IRI.equals(propIri);
                        }
                )
                .filter(predicate)
                .map(ax -> getSemClassIdOf(ax.getSubject().asIRI().get()))
                //remove duplicate class id (happens when predicate matches same class multiple times, e.g on its label and synonyms)
                .distinct();
    }

    public DefaultDirectedGraph<OWLClass, DefaultEdge> buildHyperonymyGraph() {

        DefaultDirectedGraph<OWLClass, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);

        onto.axioms(AxiomType.SUBCLASS_OF).forEach(
                ax -> {
                    OWLClass hyper = ax.getSuperClass().asOWLClass();
                    OWLClass hypo = ax.getSubClass().asOWLClass();
                    g.addVertex(hyper);
                    g.addVertex(hypo);
                    g.addEdge(hyper, hypo);
                });

        OWLClass thing = df.getOWLThing();
        getRootSemanticClasses().forEach(
                c -> {
                    g.addVertex(thing);
                    g.addVertex(c);
                    g.addEdge(thing, c);
                });

        return g;
    }

    List<GraphPath<OWLClass, DefaultEdge>> getHyperonymyPath(String fromclassid, String toclassid) {
        return getHyperonymyPaths(buildHyperonymyGraph(), fromclassid, toclassid, 1);
    }

    //throws NoSuchElementException if one of the semantic class doesn't exists in the ontology
    public List<GraphPath<OWLClass, DefaultEdge>> getHyperonymyPaths(
            DefaultDirectedGraph<OWLClass, DefaultEdge> g,
            String fromclassid, String toclassid, int nbPath
    ) {
        OWLClass fromClass = getSemanticClassesForId(fromclassid).findFirst().get();
        OWLClass toClass = getSemanticClassesForId(toclassid).findFirst().get();
        return getHyperonymyPaths(g, fromClass, toClass, nbPath);
    }

    public List<GraphPath<OWLClass, DefaultEdge>> getHyperonymyPaths(
            DefaultDirectedGraph<OWLClass, DefaultEdge> g,
            OWLClass start, OWLClass end, int nbPath
    ) {
        KShortestSimplePaths<OWLClass, DefaultEdge> kshortest = new KShortestSimplePaths<>(g, 100);
        return kshortest.getPaths(start, end, nbPath);
    }

    public OWLAxiomChange createRemoveHyponymyChange(OWLClass semclass, OWLClass hypo) {
        OWLSubClassOfAxiom subClassAx = onto.subClassAxiomsForSuperClass(semclass)
                .filter(
                        ax -> ax.getSubClass().asOWLClass().getIRI().equals(hypo.getIRI())
                ).findFirst().get();
        return new RemoveAxiom(onto, subClassAx);
    }

    public OWLAxiomChange createAddHyponymyChange(OWLClass semclass, OWLClass hypo) {
        OWLSubClassOfAxiom subClassAx = df.getOWLSubClassOfAxiom(hypo, semclass);
        return new AddAxiom(onto, subClassAx);
    }

    private AddAxiom createAddTermToClassChange(OWLClass semClass, String form, int memberType) {
        IRI synonymTypeIRI;
        switch (memberType) {
            case Structs.Term.SYNONYM:
                synonymTypeIRI = OBOEXACTSYN_IRI;
                break;

            default:
            case Structs.Term.QUASISYN:
                synonymTypeIRI = OBORELSYN_IRI;
                break;

        }
        OWLAnnotationProperty synProp = df.getOWLAnnotationProperty(synonymTypeIRI);
        OWLAnnotation synonymAnno = df.getOWLAnnotation(synProp, df.getOWLLiteral(form));
        OWLAxiom synAxiom = df.getOWLAnnotationAssertionAxiom(semClass.getIRI(), synonymAnno);
        return new AddAxiom(onto, synAxiom);
    }

    //currently only exact synonyms can be created
    public AddAxiom createAddTermToClassChange(OWLClass semClass, String form) {
        return createAddTermToClassChange(semClass, form, Structs.Term.SYNONYM);
    }

    public List<RemoveAxiom> createRemoveClassChanges(OWLClass semClass) {
        OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(onto));
        semClass.accept(remover);
        return remover.getChanges();
    }

    //Merged 2 classes by disolving the second one within the first one (adding non-redondant members and hyper/hypo links)
    public List<OWLOntologyChange> createChangesTomergeClasses(
            OWLClass semClass1, int version1, OWLClass semClass2, int version2,
            List<OWLClass> parentsToCutFrom2
    ) throws UnprocessableException {
        List<OWLOntologyChange> changes = new ArrayList<>();

        Structs.DetailSemClassNTerms classDta1 = getSemanticClassData(semClass1, true);
        Structs.DetailSemClassNTerms classDta2 = getSemanticClassData(semClass2, true);

        //compare terms by their surface form because there's no (reliable) termId stored in Ontology file (OBO)
        Map<String, Integer> term1ByForm = new HashMap<>();
        classDta1.termMembers.forEach(t1 -> term1ByForm.put(t1.form, t1.memberType));
        term1ByForm.put(classDta1.canonicLabel, Structs.Term.CANONIC);

        Map<String, Integer> term2ByForm = new HashMap<>();
        classDta2.termMembers.forEach(t2 -> term2ByForm.put(t2.form, t2.memberType));
        term2ByForm.put(classDta2.canonicLabel, Structs.Term.CANONIC);

        for (Map.Entry<String, Integer> e2 : term2ByForm.entrySet()) {
            String t2Form = e2.getKey();
            int t2type = e2.getValue();
            if (term1ByForm.containsKey(t2Form)) {

                //if term is member of both classes, so the member does not need to be copied, but member types consistency check must be performed           
                int t1type = term1ByForm.get(t2Form);

                //canonical representative is also a synonym
                if ((t2type == Structs.Term.CANONIC && t1type != Structs.Term.SYNONYM)
                        || (t2type == Structs.Term.SYNONYM && (t1type != Structs.Term.CANONIC || t1type != Structs.Term.SYNONYM))) {
                    throw new UnprocessableException("Can not merge class #" + classDta2.canonicId + " within #" + classDta1.canonicId + " because types of member '" + t2Form + "' are not consistent");
                } else {
                    //same member type on both classes
                }

            } else {
                //the term of second class was not present in first class, so it will be copied
                changes.add(
                        //canonical representative of second class become a simple synonym
                        createAddTermToClassChange(semClass1, t2Form, t2type == Structs.Term.CANONIC ? Structs.Term.SYNONYM : t2type)
                );
            }

        }

        List<String> parentsIdToCutFrom2 = parentsToCutFrom2.stream().map(c -> getSemClassIdOf(c)).collect(Collectors.toList());
        classDta2.hyperGroupIds.forEach(hyperId -> {

            if (parentsIdToCutFrom2.contains(hyperId)) {
                //do nothing to get rid of links between the 2 classes that would create cycle after the merging process
            } else {

                //attach hyperonyms of class2 to the merged class
                OWLClass hyperClass = getSemanticClassesForId(hyperId).findFirst().get();
                changes.add(
                        createAddHyponymyChange(hyperClass, semClass1)
                );

                //update old hyperonym version level
                changes.addAll(incSemClassVersion(hyperClass));
            }
        });

        classDta2.hypoGroupIds.forEach(hypoId -> {

            if (classDta1.hypoGroupIds.contains(hypoId)) {
                //do nothing if hyponym is already connected
            } else {
                //attach hyponyms of class2 to the merged class
                OWLClass hypoClass = getSemanticClassesForId(hypoId).findFirst().get();
                changes.add(
                        createAddHyponymyChange(semClass1, hypoClass)
                );

                //update old hyponym version level
                changes.addAll(incSemClassVersion(hypoClass));
            }
        });

        //delete the second class itself
        changes.addAll(createRemoveClassChanges(semClass2));
        
        return changes;
    }
    
    public String getNextSemClassId() {
        OptionalInt maxId = onto.classesInSignature()
                .filter(c -> c.getIRI().getRemainder().get().startsWith(SEMCLASSID_PREFIX))
                .mapToInt(c -> {
                    try {
                        return Integer.valueOf(c.getIRI().getRemainder().get().substring(SEMCLASSID_PREFIX.length()));
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .max();
        int newClassId = 1 + (maxId.isPresent() ? maxId.getAsInt() : 0);
        return SEMCLASSID_PREFIX + ":" + String.format("%07d", newClassId);
    }

    public List<OWLOntologyChange> createAddSubClassChanges(Optional<OWLClass> hyperSemClass, String semClassId, String surfForm) {
        List<OWLOntologyChange> changes = new ArrayList<>();

        //OWLClass semClass = df.getOWLClass(IRI.create(SEMCLASSVERSPROP_IRI.getNamespace(), oboClassIdtoOwlClassId(semClassId)));
        OWLClass semClass = df.getOWLClass(IRI.create(SEMCLASSVERSPROP_IRI.getNamespace(), semClassId));
        OWLDeclarationAxiom scDeclAx = df.getOWLDeclarationAxiom(semClass);
        changes.add(new AddAxiom(onto, scDeclAx));

        OWLAnnotation scNameAnnot = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral(surfForm));
        OWLAnnotationAssertionAxiom scNameAx = df.getOWLAnnotationAssertionAxiom(semClass.getIRI(), scNameAnnot);
        changes.add(new AddAxiom(onto, scNameAx));
        
        changes.add(getInitClassVersionPropChange(semClass));

        if (hyperSemClass.isPresent()) {
            changes.add(createAddHyponymyChange(hyperSemClass.get(), semClass));
        }

        return changes;
    }

    private IRI getSemClassVersionIri(OWLOntology onto) {
        //property name use current ontology IRI as prefix, without spurious ".owl" suffix that's sometimes added by loader
        String currOntoIri = onto.getOntologyID().getOntologyIRI().get().toString();
        if (currOntoIri.endsWith(".owl")) {
            currOntoIri = currOntoIri.substring(0, currOntoIri.length() - 4);
        }
        return IRI.create(currOntoIri, SEMCLASSVERSION_PROPNAME);
    }

    private OWLAxiomChange getInitClassVersionPropChange(OWLClass semClass) {
        OWLAnnotationProperty classVersAnnotProp = df.getOWLAnnotationProperty(SEMCLASSVERSPROP_IRI);
        OWLAnnotation versiond1Annot = df.getOWLAnnotation(classVersAnnotProp, df.getOWLLiteral(1));

        OWLAnnotationAssertionAxiom aaa = df.getOWLAnnotationAssertionAxiom(semClass.getIRI(), versiond1Annot);
        return new AddAxiom(onto, aaa);
    }

    //check that specified ontology includes elements to track updates, and if not add them
    private void checkAndSetVersionning() {

        OWLClass anySemClass = onto.classesInSignature().findAny().get();

        boolean hasVersioningProp = onto.annotationAssertionAxioms(anySemClass.getIRI())
                .filter(
                        aaa -> SEMCLASSVERSPROP_IRI.equals(aaa.getProperty().getIRI())
                ).count() > 0;

        if (!hasVersioningProp) {
            List<OWLOntologyChange> changes = new ArrayList<>();

            //set initial version number to all semantic classes
            onto.classesInSignature().forEach(c -> {
                changes.add( getInitClassVersionPropChange(c) );
            });

            if (applyChangesAndSaveOnto(changes, true)) {
                logger.log(Level.INFO, "Ontology versioning has been set-up [{0}]", config.getId());
            } else {
                logger.log(Level.SEVERE, "Error while trying to track Ontology versioning [{0}]", config.getId());
            }

        } else {
            logger.log(Level.INFO, "Ontology already versioned");
        }

    }

    public List<OWLOntologyChange> incSemClassVersion(OWLClass semClass) {
        try {
            return testAndSetSemClassVersion(semClass, Optional.empty());
        } catch (StaleVersionException ex) {
            //can not happen when empty knowVersion specified
            return Collections.EMPTY_LIST;
        }
    }

    public List<OWLOntologyChange> testAndSetSemClassVersion(OWLClass semClass, int knowVersion) throws StaleVersionException {
        return testAndSetSemClassVersion(semClass, Optional.of(knowVersion));
    }

    private List<OWLOntologyChange> testAndSetSemClassVersion(OWLClass semClass, Optional<Integer> knowVersion) throws StaleVersionException {
        List<OWLOntologyChange> changes = new ArrayList<>();

        //get semantic class current version number
        OWLAnnotationAssertionAxiom versionAAA = onto.annotationAssertionAxioms(semClass.getIRI())
                .filter(aaa -> SEMCLASSVERSPROP_IRI.equals(aaa.getProperty().getIRI()))
                .findFirst().get();

        int versionNumber = Integer.valueOf(versionAAA.getValue().annotationValue().asLiteral().get().getLiteral());
        //if version numbers don't match, it means the specified semantic class has been changed since it was loaded on the client,
        //therefore requested modification must be canceled
        if (knowVersion.isPresent() && versionNumber != knowVersion.get()) {
            throw new StaleVersionException(semClass.getIRI());
        }

        changes.add(new RemoveAxiom(onto, versionAAA));

        //increment version number
        versionNumber++;

        //set new version number to semantic class
        OWLAnnotationProperty classVersAnnotProp = df.getOWLAnnotationProperty(SEMCLASSVERSPROP_IRI);
        OWLAnnotation versiond1Annot = df.getOWLAnnotation(classVersAnnotProp, df.getOWLLiteral(versionNumber));
        OWLAnnotationAssertionAxiom aaa = df.getOWLAnnotationAssertionAxiom(semClass.getIRI(), versiond1Annot);

        changes.add(new AddAxiom(onto, aaa));

        return changes;
    }

    public static void createBackup(Path source, String suffix) {
        Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Path backup = Paths.get(source.toString() + formatter.format(new Date()) + ((suffix == null || suffix.isEmpty()) ? "" : suffix) + ".obo");
        try {
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not create ontology backup!", ex);
        }
    }

    public boolean applyChangesAndSaveOnto(List<OWLOntologyChange> changes, boolean createBackup) {

        ChangeApplied result = onto.getOWLOntologyManager().applyChanges(changes);

        if (ChangeApplied.SUCCESSFULLY.equals(result)) {
            Path source = Paths.get(config.getFilePath());
            if (createBackup) {
                createBackup(source, null);
            }
            try {
                manager.saveOntology(onto, new OBODocumentFormat(), new FileOutputStream(source.toFile()));
                return true;

            } catch (FileNotFoundException | OWLOntologyStorageException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public boolean applyChangesAndSaveOnto(List<OWLOntologyChange> changes) {
        return applyChangesAndSaveOnto(changes, false);
    }

    public static class StaleVersionException extends Exception {

        public StaleVersionException(IRI semClassIri) {
            super("Semantic Class '" + getSemClassIdOf(semClassIri) + "' state has changed. Requested modification cannot be performed");
        }
    }

    public static class UnprocessableException extends Exception {

        public UnprocessableException(String message) {
            super(message);
        }
    }
}
