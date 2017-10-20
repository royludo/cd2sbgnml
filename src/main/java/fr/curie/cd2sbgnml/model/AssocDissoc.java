package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import java.awt.geom.Point2D;

public class AssocDissoc extends ReactionNodeModel {

    public static final float ASSOCDISSOC_SIZE = 8;

    public AssocDissoc(GenericReactionModel genericReactionModel, Glyph glyph, String id, StyleInfo styleInfo) {
        super(genericReactionModel, glyph, id, styleInfo);
    }

    public AssocDissoc(GenericReactionModel genericReactionModel, Point2D.Float centerCoords, String id, StyleInfo styleInfo) {
        super(genericReactionModel, new Glyph(
                centerCoords,
                ASSOCDISSOC_SIZE,
                ASSOCDISSOC_SIZE,
                CdShape.CIRCLE,
                SbgnShape.CIRCLE),
                id, styleInfo);
    }
}
