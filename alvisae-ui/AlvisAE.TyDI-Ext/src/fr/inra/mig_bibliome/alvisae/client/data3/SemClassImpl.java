/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2010-2011.
 *
 */
package fr.inra.mig_bibliome.alvisae.client.data3;

import com.google.gwt.core.client.JsArrayString;
import fr.inra.mig_bibliome.alvisae.shared.data3.SemClass;

/**
 *
 * @author fpapazian
 */
public class SemClassImpl extends SemClassBasicImpl implements SemClass {

    private static final native SemClassImpl _create(String groupId, String canonicLabel) /*-{
    a = {};
    a.groupId=groupId;
    a.canonicId=0;
    a.canonicLabel=canonicLabel;
    a.version=0;
    a.hyperGroupIds=[];
    a.hypoGroupIds=[];
    return a;
    }-*/;

    public final static SemClassImpl createRoot() {
        return _create(ROOT_ID, "");
    }

    protected SemClassImpl() {
    }

    @Override
    public final native JsArrayString getHyperGroupIds() /*-{ return this.hyperGroupIds; }-*/;

    @Override
    public final native JsArrayString getHypoGroupIds() /*-{ return this.hypoGroupIds; }-*/;

    @Override
    public final boolean isRooted() {
        return getHyperGroupIds().length() == 0 || (getHyperGroupIds().length() == 1 && ROOT_ID.equals(getHyperGroupIds().get(0)) );
    }
}
