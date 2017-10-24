package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResidueWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ResidueWrapper.class);

    public String id;
    public float angle;
    public float relativePos; // between 0 and 1
    /**
     * Residue can be defined either by an angle (for proteins) or by relative top position (for gene, rna, asrna).
     * This flag tells which one is used.
     */
    public boolean useAngle = true;
    public String state;
    public String name;

    public ResidueWrapper(String id) {
        this.id = id;
        this.state = "";
        this.name = "";
    }

    public static String getShortState(String state) {
        switch(state) {
            case "phosphorylated": return "P";
            case "acetylated": return "Ac";
            case "ubiquitinated": return "Ub";
            case "methylated": return "Me";
            case "hydroxylated": return "OH";
            case "don't care": return "*";
            case "unknown": return "?";
            case "glycosylated": return "G";
            case "myristoylated": return "My";
            case "palmytoylated": return "Pa";
            case "prenylated": return "Pr";
            case "protonated": return "H";
            case "sulfated": return "S";
            case "": return "";
            case "empty": return "";
            default:
                logger.warn("Residue state: "+state+" not recognized, left as is");
                return state;
        }
    }

    public String getSbgnText() {
        if(!this.state.equals("")) {
            if(!this.name.equals("")) {
                return ResidueWrapper.getShortState(this.state)+"@"+this.name;
            }
            return ResidueWrapper.getShortState(this.state);
        }
        return "";
    }
}
