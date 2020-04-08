package fr.inrae.bibliome.ontolrws;

import fr.inrae.bibliome.ontolrws.Resources.Resources;
import fr.inrae.bibliome.ontolrws.Settings.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author fpa
 */
@ApplicationPath("")
public class JAXRSConfig extends Application {

    private static final String CONFIGFILE_PARAMNAME = "configFilePath";
    private static final String SETTINGS_PROPNAME = "ONTOLRWS_SETTINGS";
    private static final Logger logger = Logger.getLogger(JAXRSConfig.class.getName());
    
    // -------------------------------------------------------------------------

    public JAXRSConfig() {
        //explicitely list acting classes (resources, providers, etc.) instead of relying on classpath
        actingClassesSet.add(PreMatchRequestFilter.class);

        actingClassesSet.add(Resources.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return actingClassesSet;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletonsSet;
    }

    @Override
    public Map<String, Object> getProperties() {
        return appPropertiesMap;
    }

    public Settings getSettings() {
        ensureConfiguration();
        return (Settings) appPropertiesMap.get(SETTINGS_PROPNAME);
    }

    public void ensureConfiguration() {
//        if (!appPropertiesMap.containsKey(CONFIGFILE_PARAMNAME)) {

        String filePath = servletContext.getInitParameter(CONFIGFILE_PARAMNAME);

        if (filePath == null) {
            logger.log(Level.SEVERE, "Missing configuration file parameter [{0}]", CONFIGFILE_PARAMNAME);
            throw new IllegalArgumentException("Missing configuration file parameter [" + CONFIGFILE_PARAMNAME + "], this service won't be able to operate");
        }
        File configFile = new File(filePath);
        if (!configFile.exists() || !configFile.isFile()) {
            throw new IllegalArgumentException("Missing or Invalid configuration file [" + filePath + "], this service won't be able to operate");
        } else {

        }
        InputStream input;
        try {
            input = new FileInputStream(configFile);
            Yaml yaml = Settings.getYamlHandler();
            Settings data = (Settings) yaml.load(input);
            appPropertiesMap.put(SETTINGS_PROPNAME, data);

        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "Missing or Invalid configuration file [{0}]", filePath);
        }

        //add application-wide properties
        appPropertiesMap.put(CONFIGFILE_PARAMNAME, filePath);

        
//        }
    }

    // -------------------------------------------------------------------------
    private final Set<Class<?>> actingClassesSet = new HashSet<>();
    private final Map<String, Object> appPropertiesMap = new HashMap<>();
    private final Set<Object> singletonsSet = new HashSet<>();

    @Context
    private ServletContext servletContext;

}
