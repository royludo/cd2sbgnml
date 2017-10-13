package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliasInfoWrapper {

    private static final Logger logger = LoggerFactory.getLogger(AliasInfoWrapper.class);

    public float angle;
    public String prefix;
    public String label;

    public AliasInfoWrapper() {
        this.prefix = "";
        this.label = "";
    }

    public AliasInfoWrapper(float angle, String prefix, String label) {
        this.angle = angle;
        this.prefix = prefix;
        this.label = label;
    }

    public String getSbgnText() {
        return prefix+":"+label;

    }
}
