package fr.inrae.bibliome.ontolrws.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fpa
 */
public class Structs {

    public static class SemClass {

        String groupId;
        String canonicId;
        String canonicLabel;
        long version;
        List<String> hyperGroupIds = new ArrayList<>();
        List<String> hypoGroupIds = new ArrayList<>();
    }

    public static class DetailedSemClass extends SemClass {

        Map<String, SemClass> hypoGroupsDetails = new HashMap<>();
    }

    public static class Term {

        public final static int SYNONYM = 5;
        public final static int CANONIC = 13;
        public final static int QUASISYN = 15;

        static Term createCanonic(String form) {
            return new Term(form, CANONIC);

        }

        static Term createExactSynonym(String form) {
            return new Term(form, SYNONYM);

        }

        static Term createRelatedSynonym(String form) {
            return new Term(form, QUASISYN);
        }

        String id;
        String form;
        int memberType;
        List<String> linkedTerms = new ArrayList<>();
        List<String> englobingGroups = new ArrayList<>();

        public Term(String form, int memberType) {
            this.form = form;
            this.memberType = memberType;
        }

    }

    public static class DetailSemClassNTerms extends DetailedSemClass {

        List<Term> termMembers = new ArrayList<>();
    }
}
