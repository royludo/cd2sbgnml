package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;

public class LogicGateWrapper {

    public enum LogicGateType {AND, OR, NOT, UNKNOWN}

    private LogicGateType type;
    private String modificationType;
    /**
     * The index of the reactant in the corresponding list (modification,  additional product...) in the
     * xml file
     */
    private int positionIndex;

    public LogicGateWrapper(CelldesignerModification modif, int i) {
        this.positionIndex = i;
        this.modificationType = modif.getDomNode().getAttributes().getNamedItem("modificationType").getNodeValue();
        switch (modif.getType()) {
            case "BOOLEAN_LOGIC_GATE_OR": this.type = LogicGateType.OR; break;
            case "BOOLEAN_LOGIC_GATE_AND": this.type = LogicGateType.AND; break;
            case "BOOLEAN_LOGIC_GATE_NOT": this.type = LogicGateType.NOT; break;
            case "BOOLEAN_LOGIC_GATE_UNKNOWN": this.type = LogicGateType.UNKNOWN; break;
            default:
                throw new IllegalArgumentException("Modification type: "+modif.getType()+" is not a logic gate type.");
        }
    }

    public LogicGateType getType() {
        return type;
    }

    public String getModificationType() {
        return modificationType;
    }

    public int getPositionIndex() {
        return positionIndex;
    }
}
