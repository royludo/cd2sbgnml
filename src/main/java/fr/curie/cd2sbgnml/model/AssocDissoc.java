package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;

import java.awt.geom.Point2D;

public class AssocDissoc extends ReactionNodeModel {

    public static final float ASSOCDISSOC_SIZE = 8;

    public AssocDissoc(GenericReactionModel genericReactionModel, Glyph glyph, String id) {
        super(genericReactionModel, glyph, id);
    }

    public AssocDissoc(GenericReactionModel genericReactionModel, Point2D.Float centerCoords, String id) {
        super(genericReactionModel, new Glyph(
                centerCoords,
                ASSOCDISSOC_SIZE,
                ASSOCDISSOC_SIZE,
                CdShape.CIRCLE,
                SbgnShape.CIRCLE),
                id);
    }
}
