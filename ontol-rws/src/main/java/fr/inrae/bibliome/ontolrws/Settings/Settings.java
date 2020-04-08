package fr.inrae.bibliome.ontolrws.Settings;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 *
 * @author fpa
 */
public class Settings {

    public static final String AUTHUSER_PROPNAME = "auth_user";

    public static Yaml getYamlHandler() {
        Constructor constructor = new Constructor(Settings.class);
        TypeDescription settingsDescr = new TypeDescription(Settings.class);

        settingsDescr.putListPropertyType("ontologies", Ontology.class);
        settingsDescr.putListPropertyType("users", User.class);

        constructor.addTypeDescription(settingsDescr);

        return new Yaml(constructor);
    }

    // -------------------------------------------------------------------------
    public List<Ontology> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<Ontology> ontologies) {
        this.ontologies = ontologies;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public Settings() {

    }

    public Settings(List<Ontology> ontologies, List<User> users) {
        this.ontologies = ontologies;
        this.users = users;
    }

    public boolean isAuthenticatedUser(String username, String password) {
        return getUsers().stream()
                .filter(u -> u.getName().equals(username))
                //FIX handle enccrypted password
                .anyMatch(u -> u.getPassword().equals(password));
    }


    public Optional<User> getAuthenticatedUser(String username, String password) {
        return getUsers().stream()
                .filter(u -> u.getName().equals(username) && u.getPassword().equals(password))
                .findFirst();
    }

    public Optional<User> getUser(int userId) {
        return getUsers().stream()
                .filter(u -> u.getId() == userId)
                .findFirst();
    }

    public Stream<Ontology> getOntologiesForUser(User authUser) {
        return getOntologies().stream()
                .filter(o -> authUser.getOntologies().contains(o.getId()));
    }

    public Optional<Ontology> getOntologyForUser(User authUser, String ontoid) {
        return getOntologiesForUser(authUser)
                .filter(o -> o.getId().equals(ontoid)).findFirst();
    }

    // -------------------------------------------------------------------------
    private List<Ontology> ontologies;
    private List<User> users;

}
