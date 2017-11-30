package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.Modification;

import java.util.List;

public class LogicGateWrapper extends ReactantWrapper {

    public enum LogicGateType {AND, OR, NOT, UNKNOWN}

    private LogicGateType type;
    private String modifiers;
    private String aliases;
    private ModificationLinkType logicGateModificationType;

    public LogicGateWrapper(Modification modif, int i) {
        super(modif, null, i);

        this.setPositionIndex(i);
        this.modifiers = modif.getModifiers();
        this.aliases = modif.getAliases();
        this.logicGateModificationType = ModificationLinkType.valueOf(modif.getModificationType());
        switch (ModificationLinkType.valueOf(modif.getType())) {
            case BOOLEAN_LOGIC_GATE_OR: this.type = LogicGateType.OR; break;
            case BOOLEAN_LOGIC_GATE_AND: this.type = LogicGateType.AND; break;
            case BOOLEAN_LOGIC_GATE_NOT: this.type = LogicGateType.NOT; break;
            case BOOLEAN_LOGIC_GATE_UNKNOWN: this.type = LogicGateType.UNKNOWN; break;
            default:
                throw new IllegalArgumentException("Modification type: "+modif.getType()+" is not a logic gate type.");
        }
    }

    /**
     * Can be used to transform a ReactantWrapper into a LogicGateWrapper
     * @param w
     */
    public LogicGateWrapper(ReactantWrapper w, LogicGateType type,
                            List<String> modifiers, List<String> aliases, ModificationLinkType linkType) {
        super(w);
        this.type = type;
        switch(type) {
            case AND:
                this.setModificationLinkType(ModificationLinkType.BOOLEAN_LOGIC_GATE_AND);
                break;
            case UNKNOWN:
                this.setModificationLinkType(ModificationLinkType.BOOLEAN_LOGIC_GATE_UNKNOWN);
                break;
            case OR:
                this.setModificationLinkType(ModificationLinkType.BOOLEAN_LOGIC_GATE_OR);
                break;
            case NOT:
                this.setModificationLinkType(ModificationLinkType.BOOLEAN_LOGIC_GATE_NOT);
                break;
        }
        this.logicGateModificationType = linkType;
        this.modifiers = String.join(",", modifiers);
        this.aliases = String.join(",", aliases);
    }

    public Modification getCDElement() {
        Modification modification = new Modification();
        modification.setModifiers(this.getModifiers());
        modification.setAliases(this.getAliases());
        modification.setTargetLineIndex(this.getTargetLineIndex());
        modification.setType(this.getModificationLinkType().toString());
        modification.setModificationType(this.getLogicGateModificationType().toString());

        modification.setConnectScheme(this.getLineWrapper().getCDConnectScheme());
        modification.setLine(this.getLineWrapper().getCDLine());
        if(this.getLineWrapper().getEditPoints().size() > 0) {
            for(String s: this.getLineWrapper().editPointsAsStringList()){
                modification.getEditPoints().add(s);
            }
        }

        return modification;
    }

    public LogicGateType getType() {
        return type;
    }

    public String getModificationType() {
        return this.logicGateModificationType.toString();
    }

    public void setType(LogicGateType type) {
        this.type = type;
    }

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public void setLogicGateModificationType(ModificationLinkType logicGateModificationType) {
        this.logicGateModificationType = logicGateModificationType;
    }

    public ModificationLinkType getLogicGateModificationType() {
        return logicGateModificationType;
    }
}
