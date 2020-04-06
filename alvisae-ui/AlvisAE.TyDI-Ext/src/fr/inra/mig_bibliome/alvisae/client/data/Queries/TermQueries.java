/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2010-2014.
 *
 */
package fr.inra.mig_bibliome.alvisae.client.data.Queries;

import com.google.gwt.core.client.JsArray;
import fr.inra.mig_bibliome.alvisae.client.data3.SemClassTreeLevelImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.ProjectInfoListImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.SemClassNTermsImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.SemClassListImpl;
import com.google.gwt.user.client.rpc.AsyncCallback;
import fr.inra.mig_bibliome.alvisae.client.data.DetailedAsyncCallback;
import fr.inra.mig_bibliome.alvisae.client.data3.Extension.CheckedSemClassImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.StructTermChangesImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.VersionedBranchesImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.VersionedSemClassImpl;
import fr.inra.mig_bibliome.alvisae.client.data3.VersionedSemClassTreeLevelImpl;
import fr.inra.mig_bibliome.alvisae.shared.data3.Extension.TyDIResRefPropVal;
import java.util.List;

/**
 *
 * @author fpapazian
 */
public interface TermQueries {

    // [baseurl] :  bibliome.jouy.inra.fr/tydiws/[instance_name]/
    void getUserProjects(int userId, AsyncCallback<ProjectInfoListImpl> resultCallback);

    /**
     * Retrieve the detail of a Semantic Class and its direct hyponyms
     *
     * <b>Rest Method & URL:</b>
     * GET http://[baseurl]/projects/[projectId]/semClass/[semClassId]
     *
     * @param projectId Id of a Termino Project
     * @param semClassId Id of a SemanticClass (0 to get the virtual root class)
     *
     * @throws IllegalArgumentException if the specified projectId or semClassId does not exist (400 Bad Request)
     * @throws SecurityException if the current user is not authenticated (401 Unauthorized) or has no authorization to perform the operation (403 Forbidden)
     */
    void getSemanticClass(String projectId, String semClassId, final AsyncCallback<VersionedSemClassTreeLevelImpl> resultCallback);
    
    /**
     * Ensure that the specified Semantic Class is stored in a snapshot of the Structured Terminology, and return the corresponding  Structured Terminology version number.
     */
    void ensureSemanticClassVersioned(String projectId, String semClassId, AsyncCallback<VersionedSemClassImpl> resultCallback);
    
    void getSemanticClassNTerms(String projectId, String semClassId, AsyncCallback<SemClassNTermsImpl> resultCallback);

    void searchSemanticClassesByPattern(String projectId, String pattern, AsyncCallback<SemClassListImpl> resultCallback);

    void getBranchesBetweenClasses(String projectId, String fromSemClassId, String toSemClassId, AsyncCallback<VersionedBranchesImpl> resultCallback);

    void replaceHyperonym(String projectId, String semClassId, int semClassVersion, String prevHyperSemClassId, int prevHyperSemClassVersion, String newHyperSemClassId, int newHyperSemClassVersion, DetailedAsyncCallback<SemClassTreeLevelImpl> resultCallback);

    void createClassAndRepresentativeTerm(String projectId, String surfaceForm, String lemmatizedForm, String newHyperSemClassId, int newHyperSemClassVersion, boolean force, DetailedAsyncCallback<SemClassTreeLevelImpl> resultCallback);

    void createTermSynonym(String projectId, String surfaceForm, String lemmatizedForm, String hyperSemClassId, int hyperSemClassVersion, DetailedAsyncCallback<SemClassTreeLevelImpl> resultCallback);

    void addSynonymToSemanticClass(String projectId, String termId, String semClassId, int semClassVersion, AsyncCallback<SemClassNTermsImpl> resultCallback);

    void mergeClasses(String projectId, String semClassId1, int semClassVersion1, String semClassId2, int semClassVersion2, DetailedAsyncCallback<SemClassTreeLevelImpl> resultCallback);
    
    void getStructTermChanges(String projectId, int fromVersionNum, List<String> semClassIds, AsyncCallback<StructTermChangesImpl> resultCallback);
    
    void checkStructTermChanges(String projectId, List<CheckedSemClassImpl> resRefs, AsyncCallback<JsArray<CheckedSemClassImpl>> resultCallback);
    
}
