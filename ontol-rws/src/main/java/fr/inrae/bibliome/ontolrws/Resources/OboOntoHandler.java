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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 *
 * @author fpa
 */
public class OboOntoHandler implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(OboOntoHandler.class.getName());

    public static final String ROOT_ID = "0";

    private static final String OBOBASE_URI = "http://purl.obolibrary.org/obo/";

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

    Structs.DetailSemClassNTerms initSemClassStruct(OWLClass semClass) {
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

    private DefaultDirectedGraph<OWLClass, DefaultEdge> buildHyperonymyGraph() {

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

    private List<GraphPath<OWLClass, DefaultEdge>> getHyperonymyPaths(
            OWLClass start, OWLClass end
    ) {
        DefaultDirectedGraph<OWLClass, DefaultEdge> g = buildHyperonymyGraph();
        KShortestSimplePaths<OWLClass, DefaultEdge> kshortest = new KShortestSimplePaths<>(g, 100);
        return kshortest.getPaths(start, end, 1);
    }

    //throws NoSuchElementException if one of the semantic class doesn't exists in the ontology
    List<GraphPath<OWLClass, DefaultEdge>> getHyperonymyPaths(String fromclassid, String toclassid) {
        OWLClass fromClass = getSemanticClassesForId(fromclassid).findFirst().get();
        OWLClass toClass = getSemanticClassesForId(toclassid).findFirst().get();
        DefaultDirectedGraph<OWLClass, DefaultEdge> g = buildHyperonymyGraph();
        KShortestSimplePaths<OWLClass, DefaultEdge> kshortest = new KShortestSimplePaths<>(g, 100);
        return kshortest.getPaths(fromClass, toClass, 1);
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

    private IRI getSemClassVersionIri(OWLOntology onto) {
        //property name use current ontology IRI as prefix, without spurious ".owl" suffix that's sometimes added by loader
        String currOntoIri = onto.getOntologyID().getOntologyIRI().get().toString();
        if (currOntoIri.endsWith(".owl")) {
            currOntoIri = currOntoIri.substring(0, currOntoIri.length() - 4);
        }
        return IRI.create(currOntoIri, SEMCLASSVERSION_PROPNAME);
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
            OWLAnnotationProperty classVersAnnotProp = df.getOWLAnnotationProperty(SEMCLASSVERSPROP_IRI);
            OWLAnnotation versiond1Annot = df.getOWLAnnotation(classVersAnnotProp, df.getOWLLiteral(1));

            onto.classesInSignature().forEach(c -> {
                OWLAnnotationAssertionAxiom aaa = df.getOWLAnnotationAssertionAxiom(c.getIRI(), versiond1Annot);
                changes.add(new AddAxiom(onto, aaa));
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

    public List<OWLOntologyChange> testAndSetSemClassVersion(OWLClass semClass, int knowVersion) throws StaleVersionException {
        List<OWLOntologyChange> changes = new ArrayList<>();

        //get semantic class current version number
        OWLAnnotationAssertionAxiom versionAAA = onto.annotationAssertionAxioms(semClass.getIRI())
                .filter(aaa -> SEMCLASSVERSPROP_IRI.equals(aaa.getProperty().getIRI()))
                .findFirst().get();

        int versionNumber = Integer.valueOf(versionAAA.getValue().annotationValue().asLiteral().get().getLiteral());
        //if version numbers don't match, it means the specified semantic class has been changed since it was loaded on the client,
        //therefore requested modification must be canceled
        if (versionNumber != knowVersion) {
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

}
