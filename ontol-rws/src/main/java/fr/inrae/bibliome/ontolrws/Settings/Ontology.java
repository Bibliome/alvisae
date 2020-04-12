package fr.inrae.bibliome.ontolrws.Settings;

import java.util.Objects;

/**
 *
 * @author fpa
 */
public class Ontology {

    private String id;
    private String longName;
    private String filePath;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.id);
        hash = 29 * hash + Objects.hashCode(this.longName);
        hash = 29 * hash + Objects.hashCode(this.filePath);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Ontology other = (Ontology) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.longName, other.longName)) {
            return false;
        }
        if (!Objects.equals(this.filePath, other.filePath)) {
            return false;
        }
        return true;
    }
}
