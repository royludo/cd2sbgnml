package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.LogicGateWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.LogicGateWrapper.LogicGateType;

import java.awt.geom.Point2D;

public class LogicGate extends ReactionNodeModel{

    public static final float LOGICGATE_SIZE = 20;

    private LogicGateType type;

    public LogicGate(GenericReactionModel genericReactionModel, Glyph glyph, String id, LogicGateType type) {
        super(genericReactionModel, glyph, id);
        this.type = type;
    }

    public LogicGate(GenericReactionModel genericReactionModel, Point2D.Float centerCoords, String id, LogicGateType type) {
        super(genericReactionModel, new Glyph(
                        centerCoords,
                        LOGICGATE_SIZE,
                        LOGICGATE_SIZE,
                        CdShape.CIRCLE,
                        SbgnShape.CIRCLE),
                id);
        this.type = type;
    }

    public static String getSbgnClass(LogicGateType logicType) {
        switch(logicType) {
            case OR: return "or";
            case AND: return "and";
            case NOT: return "not";
            //case UNKNOWN: break; // TODO what to do ?
        }
        throw new IllegalArgumentException("Could not infer SBGN class from logic type: "+logicType);
    }

    public LogicGateType getType() {
        return type;
    }
}
