package fr.inrae.bibliome.ontolrws.Settings;

import java.util.List;

/**
 *
 * @author fpa
 */
public class User {

    private int id;
    private String name;
    private String password;
    private List<String> ontologies;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<String> ontologies) {
        this.ontologies = ontologies;
    }

}
