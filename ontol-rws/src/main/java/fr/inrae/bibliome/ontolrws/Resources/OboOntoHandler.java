package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 *
 * @author fpa
 */
public class OboOntoHandler implements AutoCloseable {

    public static final String ROOT_ID = "0";

    private static final String OBOBASE_IRI = "http://purl.obolibrary.org/obo/";

    private static final IRI OBODBXREF_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
    private static final IRI OBONAMESPACE_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasOBONamespace");

    private static final IRI OBOEXACTSYN_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");
    private static final IRI OBORELSYN_IRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");

    private static final IRI OBOID_URI = IRI.create("http://www.geneontology.org/formats/oboInOwl#id");
    //private static final IRI RDFLABEL_URI = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

    private static final IRI XSDSTR_URI = IRI.create("http://www.w3.org/2001/XMLSchema#string");

    public static OboOntoHandler getHandler(Ontology ontoConfig) {
        return new OboOntoHandler(ontoConfig);
    }

    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private OWLOntology onto;
    private OWLDataFactory df;
    private PrefixManager pm;

    protected OboOntoHandler(Ontology ontoConfig) {
        File file = new File(ontoConfig.getFilePath());
        try {
            onto = manager.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException ex) {
            throw new IllegalArgumentException("Could not load ontology file: " + ontoConfig.getFilePath(), ex);
        }
        df = manager.getOWLDataFactory();
        pm = new DefaultPrefixManager(OBOBASE_IRI);

    }

    @Override
    public void close() {
        manager.clearOntologies();
        onto = null;
        df = null;
        pm = null;
    }

    public static boolean isRootId(String semClassId) {
        return ROOT_ID.equals(semClassId);
    }

    public static String getSemClassIdOf(OWLClass semClass) {
        return semClass.getIRI().getShortForm().replace("_", ":");
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
            String scId = semClassId.replace(":", "_");
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

    Structs.DetailSemClassNTerms initSemClassStruct(OWLClass semClass) {
        final Structs.DetailSemClassNTerms srStruct = new Structs.DetailSemClassNTerms();

        //retrieve and filter class properties
        onto.annotationAssertionAxioms(semClass.getIRI())
                .forEach(aaa -> {
                    String value = null;

                    OWLDatatype dataType = aaa.getValue().datatypesInSignature().findFirst().orElse(null);
                    // every props datatype is string in OBO format used
                    if (XSDSTR_URI.equals(dataType.getIRI())) {
                        OWLAnnotationValue propValue = aaa.getValue().annotationValue();
                        if (propValue.isLiteral()) {
                            value = propValue.asLiteral().get().getLiteral();
                        }
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

                        } else {

                        }
                    }

                });

        return srStruct;
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

    private void addTermToClass(OWLClass semClass, String form, int memberType) {
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
        manager.applyChange(new AddAxiom(onto, synAxiom));
    }

    //currently only exact synonyms can be created
    public void addTermToClass(OWLClass semClass, String form) {
        addTermToClass(semClass, form, Structs.Term.SYNONYM);
    }

}
