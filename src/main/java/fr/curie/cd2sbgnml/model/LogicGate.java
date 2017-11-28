package fr.curie.cd2sbgnml.model;

import com.sun.javafx.font.LogicalFont;
import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.LogicGateWrapper.LogicGateType;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import org.sbgn.GlyphClazz;

import java.awt.geom.Point2D;

/**
 * A generic logic gate model.
 */
public class LogicGate extends ReactionNodeModel{

    private static final float LOGICGATE_SIZE = 20;
    private static final float PORT_DISTANCE_RATIO = 0.5f;

    private LogicGateType type;

    public LogicGate(Glyph glyph, String id, LogicGateType type, StyleInfo styleInfo) {
        super(glyph, id, LOGICGATE_SIZE, PORT_DISTANCE_RATIO, styleInfo);
        this.type = type;
    }

    public LogicGate(Point2D.Float centerCoords, String id, LogicGateType type, StyleInfo styleInfo) {
        super(new Glyph(
                        centerCoords,
                        LOGICGATE_SIZE,
                        LOGICGATE_SIZE,
                        CdShape.CIRCLE,
                        SbgnShape.CIRCLE),
                id, LOGICGATE_SIZE, PORT_DISTANCE_RATIO, styleInfo);
        this.type = type;
    }

    public static String getSbgnClass(LogicGateType logicType) {
        switch(logicType) {
            case OR: return "or";
            case AND: return "and";
            case NOT: return "not";
            //case UNKNOWN: break; // they are removed
        }
        throw new IllegalArgumentException("Could not infer SBGN class from logic type: "+logicType);
    }

    public static LogicGateType getLogicGateType(GlyphClazz clazz) {
        switch(clazz) {
            case OR: return LogicGateType.OR;
            case AND: return LogicGateType.AND;
            case NOT: return LogicGateType.NOT;
        }
        throw new IllegalArgumentException("Could not infer logic gate type from SBGN class: "+clazz);
    }

    public LogicGateType getType() {
        return type;
    }
}
