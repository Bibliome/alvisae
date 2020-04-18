/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2010-2011.
 *
 */
package fr.inra.mig_bibliome.alvisae.shared.data3;

/**
 * @author fpapazian
 */
public interface SemClassBasic {

    public static final String ROOT_ID = "0";
    
    /**
     * @return the Id of this Semantic class
     */
    public String getId();

    /**
     * @return the Term Id of the Canonic representative of this Semantic class
     */
    public String getCanonicId();

    /**
     * @return the surface form of the Canonic representative of this Semantic class
     */
    public String getCanonicLabel();

    /**
     * 
     * @return the version of the class 
     */
    public int getVersion();

}
