/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2010-2012.
 *
 */
package fr.inra.mig_bibliome.alvisae.client.SemClass;

import fr.inra.mig_bibliome.alvisae.client.data3.SemClassImpl;
import fr.inra.mig_bibliome.alvisae.shared.data3.SemClass;
import fr.inra.mig_bibliome.alvisae.shared.data3.SemClassNTerms;
import fr.inra.mig_bibliome.alvisae.shared.data3.SemClassTreeLevel;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class serves as a global repository of instances of Semantic Classes
 * DataProviders, hence allowing dynamic update of the tree after modifications.
 */
/**
 *
 * @author fpapazian
 */
public class ProviderStore {

    /**
     * Part of the repository specific to a terminology project
     */
    public static class ByProject {

        private final String projectId;
        private final HashMap<String, HashSet<SemClassDataProvider>> parentProvidersBySemClass = new HashMap<String, HashSet<SemClassDataProvider>>();
        private final HashMap<String, SemClassDataProvider> providerBySemClass = new HashMap<String, SemClassDataProvider>();
        private final HashMap<String, ClassDetailMembersDataProvider> detailsProviderBySemClass = new HashMap<String, ClassDetailMembersDataProvider>();
        private String marked = "";
        private final SemClass root = SemClassImpl.createRoot();
        private final HashMap<String, SemClass> semClasses = new HashMap<String, SemClass>();
        private final HashMap<String, SemClassNTerms> semClassNTerms = new HashMap<String, SemClassNTerms>();
        private final HashMap<String, SemClassTreeLevel> treeLevel = new HashMap<String, SemClassTreeLevel>();

        public ByProject(String projectId) {
            this.projectId = projectId;
        }

        public boolean isMarked(String semmClassId) {
            return marked.equals(semmClassId);
        }

        public void toggleMarked(String semmClassId) {
            if (marked.equals(semmClassId)) {
                marked = "";
            } else {
                SemClassDataProvider.refreshSemClassNodes(projectId, marked);
                marked = semmClassId;
            }
            SemClassDataProvider.refreshSemClassNodes(projectId, semmClassId);
        }

        /**
         * Retrieve the unique DataProvider that can generate the children
         * NodeInfo of the specified Semantic Class Id
         */
        public SemClassDataProvider getProvider(String semmClassId) {
            return providerBySemClass.get(semmClassId);
        }

        /**
         * Retrieve the list of all DataProviders that generated NodeInfos
         * corresponding to the specified Semantic Class Id
         */
        public HashSet<SemClassDataProvider> getParentProviders(String semmClassId) {
            return parentProvidersBySemClass.get(semmClassId);
        }

        /*
         *
         */
        public ClassDetailMembersDataProvider getDetailsProvider(String semmClassId) {
            return detailsProviderBySemClass.get(semmClassId);
        }

        /**
         * Remember that the specified DataProvider has generated a NodeInfo
         * corresponding to the specified Semantic Class Id
         */
        public void setParentProvider(String semmClassId, SemClassDataProvider provider) {
            HashSet<SemClassDataProvider> parentProviders = parentProvidersBySemClass.get(semmClassId);
            if (parentProviders == null) {
                parentProviders = new HashSet<SemClassDataProvider>();
                parentProvidersBySemClass.put(semmClassId, parentProviders);
            }
            parentProviders.add(provider);
        }

        /**
         * Forget that the specified DataProvider has generated a NodeInfo
         * corresponding to the specified Semantic Class Id
         */
        public void unsetParentProvider(String semmClassId, SemClassDataProvider provider) {
            HashSet<SemClassDataProvider> parentProviders = parentProvidersBySemClass.get(semmClassId);
            if (parentProviders != null) {
                parentProviders.remove(provider);
            }
        }

        public void clear() {
            for (HashSet<SemClassDataProvider> value : parentProvidersBySemClass.values()) {
                value.clear();
            }
            parentProvidersBySemClass.clear();
            providerBySemClass.clear();
            detailsProviderBySemClass.clear();
            treeLevel.clear();
        }

        private void disposeOutdatedTreeLevel(String semClassId, int semClassVersion) {
            SemClassTreeLevel level = treeLevel.get(semClassId);
            if (level != null) {
                if (level.getVersion() < semClassVersion) {
                    treeLevel.remove(semClassId);
                }
            }
        }

        public void cacheSemClass(SemClass semClass) {
            if (semClass != null) {
                String semClassId = semClass.getId();
                disposeOutdatedTreeLevel(semClassId, semClass.getVersion());
                //GWT.log("Caching class:" + semClass.getId() + "@" + semClass.getVersion() + " " + semClass.getCanonicLabel());
                SemClass previous = semClasses.put(semClassId, semClass);
            }
        }

        public void cacheSemClassTreeLevel(SemClassTreeLevel semClassTreeLevel) {
            String semClassId = semClassTreeLevel.getId();
            //GWT.log("Caching level:" + semClassTreeLevel.getId() + "@" + semClassTreeLevel.getVersion() + " " + semClassTreeLevel.getCanonicLabel());
            SemClassTreeLevel previous = treeLevel.put(semClassId, semClassTreeLevel);
            if (semClasses.containsKey(semClassId)) {
                semClasses.remove(semClassId);
            }
            for (SemClass s : semClassTreeLevel.getHypoGroupsDetails().values()) {
                cacheSemClass(s);
            }
        }

        public SemClass getCacheSemClass(String semClassId) {
            if (SemClass.ROOT_ID.equals(semClassId)) {
                return root;
            } else {
                SemClass semClass = treeLevel!=null ? treeLevel.get(semClassId) : null;
                if (semClass == null) {
                    semClass = semClasses!=null ? semClasses.get(semClassId) :null;
                }
                return semClass;
            }
        }

        public void unloadSemClassTreeLevel(String semClassId) {
            treeLevel.remove(semClassId);
            semClasses.remove(semClassId);
            semClassNTerms.remove(semClassId);
        }

        public SemClassTreeLevel getCacheSemClassTreeLevel(String semClassId) {
            SemClass semClass = getCacheSemClass(semClassId);
            if (semClass instanceof SemClassTreeLevel) {
                return (SemClassTreeLevel) semClass;
            } else {
                return null;
            }
        }

        public void cacheSemClassNTerms(SemClassNTerms semClass) {
            String semClassId = semClass.getId();
            int semClassVersion = semClass.getVersion();
            //GWT.log("Caching details:" + semClass.getId() + "@" + semClass.getVersion() + "  " + semClass.getCanonicLabel());
            SemClassNTerms previous = semClassNTerms.put(semClassId, semClass);
        }

        public SemClassNTerms getCacheSemClassNTerms(String semClassId) {
            return semClassNTerms.get(semClassId);
        }
    }
    private final HashMap<String, ByProject> providersByProject = new HashMap<String, ByProject>();

    private ByProject _forProject(String projectId) {
        return providersByProject.get(projectId);
    }

    private void _clear() {
        for (ByProject value : providersByProject.values()) {
            value.clear();
        }
        providersByProject.clear();
    }

    /**
     * Retrieve, or create if necessary, the DataProvider that can generate the
     * children NodeInfo of the specified Semantic Class.
     */
    private SemClassDataProvider _getOrCreateSemClassProvider(String projectId, SemClassInfo parentClassInfo) {
        ByProject forProject = providersByProject.get(projectId);
        if (forProject == null) {
            forProject = new ByProject(projectId);
            providersByProject.put(projectId, forProject);
        }
        String parentSemClassId = parentClassInfo != null ? parentClassInfo.getId() : null;
        SemClassDataProvider forSemclass = forProject.providerBySemClass.get(parentSemClassId);
        if (forSemclass == null) {
            forSemclass = new SemClassDataProvider(SemClassInfo.KEY_PROVIDER, projectId, parentClassInfo);
            forProject.providerBySemClass.put(parentSemClassId, forSemclass);
        }
        return forSemclass;
    }

    private ClassDetailMembersDataProvider _getOrCreateDetailSemClassProvider(String projectId, SemClassInfo semClassInfo) {
        ByProject forProject = providersByProject.get(projectId);
        if (forProject == null) {
            forProject = new ByProject(projectId);
            providersByProject.put(projectId, forProject);
        }
        String semClassId = semClassInfo.getId();
        ClassDetailMembersDataProvider forSemclass = forProject.detailsProviderBySemClass.get(semClassId);
        if (forSemclass == null) {
            forSemclass = new ClassDetailMembersDataProvider(TermInfo.KEY_PROVIDER, projectId, semClassInfo);
            forProject.detailsProviderBySemClass.put(semClassId, forSemclass);
        }
        return forSemclass;
    }
    // The global repository of DataProviders
    private static final ProviderStore providerStore = new ProviderStore();

    /**
     * @return the part of the repository specific to the specified terminology
     * project
     */
    public static ByProject forProject(String projectId) {
        return providerStore._forProject(projectId);
    }

    public static SemClassDataProvider getOrCreateSemClassProvider(String projectId, SemClassInfo parentClassInfo) {
        return providerStore._getOrCreateSemClassProvider(projectId, parentClassInfo);
    }

    public static ClassDetailMembersDataProvider getOrCreateSemClassDetailProvider(String projectId, SemClassInfo classInfo) {
        return providerStore._getOrCreateDetailSemClassProvider(projectId, classInfo);
    }

    public static void clear() {
        providerStore._clear();
    }
}
