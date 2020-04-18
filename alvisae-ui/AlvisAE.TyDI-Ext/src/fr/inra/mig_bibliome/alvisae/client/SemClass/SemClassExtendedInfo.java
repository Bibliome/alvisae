/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2011.
 *
 */
package fr.inra.mig_bibliome.alvisae.client.SemClass;

/**
 *
 * @author fpapazian
 */
public class SemClassExtendedInfo extends SemClassInfo {

    private final String parentClassId;

    public SemClassExtendedInfo(String projectId, String classId, String parentClassId) {
        super(projectId, classId);
        this.parentClassId = parentClassId;
    }

    public String getParentClassId() {
        return parentClassId;
    }

    public String getExtendedSemClassId() {
        return (getParentClassId() == null ? "" : getParentClassId()) + "_" + getId();
    }
}
