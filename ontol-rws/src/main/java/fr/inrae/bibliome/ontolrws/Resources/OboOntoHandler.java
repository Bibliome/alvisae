package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import java.io.File;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
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
    private static final IRI RDFLABEL_URI = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

    private static final IRI XSDSTR_URI = IRI.create("http://www.w3.org/2001/XMLSchema#string");

    private static final Pattern StrPropValueExtractor = Pattern.compile("\"(.+)\"\\^\\^xsd\\:string");

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
    public void close()  {
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

        //amongst all classes in ontology, find those who have only OWLThing has superclass
        return onto.classesInSignature()
                .filter(c -> onto.subClassAxiomsForSubClass(c)
                .filter(s -> !thing.equals(s.getSuperClass()))
                .count() == 0);
    }

    public Stream<OWLClass> getSemanticClassesForId(String semClassId) {
        if (ROOT_ID.equals(semClassId)) {
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

    private Map.Entry<String, String> newEntry(String k, String v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    public Stream<Map.Entry<String, String>> classProps(OWLClass semClass) {

        //retrieve and filter class properties
        return onto.annotationAssertionAxioms(semClass.getIRI())
                .map(aaa -> {
                    String propName;
                    IRI propNameIRI = aaa.getProperty().getIRI();

                    //keep only a subset of props
                    if (OBOID_URI.equals(propNameIRI)) {
                        propName = "id";
                    } else if (RDFLABEL_URI.equals(propNameIRI)) {
                        propName = "label";
                    } else if (OBORELSYN_IRI.equals(propNameIRI)) {
                        propName = "relsyn";
                    } else if (OBOEXACTSYN_IRI.equals(propNameIRI)) {
                        propName = "exactsyn";
                    } else if (OBODBXREF_IRI.equals(propNameIRI)) {
                        propName = "dbxref";
                    } else {
                        propName = null;
                    }

                    if (propName != null) {
                        // every props datatype is string in OBO format used
                        OWLDatatype dataType = aaa.getValue().datatypesInSignature().findFirst().orElse(null);

                        if (XSDSTR_URI.equals(dataType.getIRI())) {
                            OWLAnnotationValue propValue = aaa.getValue().annotationValue();
                            Matcher matcher = StrPropValueExtractor.matcher(propValue.toString());
                            if (matcher.matches()) {
                                return newEntry(propName, matcher.group(1));
                            }
                        }
                    }
                    return null;

                }).
                filter(e -> e != null);
    }

}
