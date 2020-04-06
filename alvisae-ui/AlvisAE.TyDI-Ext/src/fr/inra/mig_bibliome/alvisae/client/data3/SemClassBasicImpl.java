/*
 *
 *      This software is a result of Quaero project and its use must respect the rules of the Quaero Project Consortium Agreement.
 *
 *      Copyright Institut National de la Recherche Agronomique, 2010-2011.
 *
 */
package fr.inra.mig_bibliome.alvisae.client.data3;

import com.google.gwt.core.client.JavaScriptObject;
import fr.inra.mig_bibliome.alvisae.shared.data3.SemClassBasic;

/**
 *
 * @author fpapazian
 */
public class SemClassBasicImpl extends JavaScriptObject implements SemClassBasic {

    protected SemClassBasicImpl() {
    }

    @Override
    public final native String getId() /*-{ return this.groupId.toString(); }-*/;

    @Override
    public final native String getCanonicId() /*-{ return this.canonicId.toString(); }-*/;

    @Override
    public final native String getCanonicLabel() /*-{ return this.canonicLabel; }-*/;

    @Override
    public final native int getVersion() /*-{ return this.version; }-*/;

}
