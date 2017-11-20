package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import java.awt.geom.Point2D;

/**
 * Represents the association or dissociation glyphs. They replace the process glyph in SBGN, and are present along
 * the process glyph in CellDesigner.
 */
public class AssocDissoc extends ReactionNodeModel {

    private static final float ASSOCDISSOC_SIZE = 8;
    private static final float PORT_DISTANCE_RATIO = 1;

    public AssocDissoc(Glyph glyph, String id, StyleInfo styleInfo) {
        super(glyph, id, ASSOCDISSOC_SIZE, PORT_DISTANCE_RATIO, styleInfo);
    }

    public AssocDissoc(Point2D.Float centerCoords, String id, StyleInfo styleInfo) {
        super(new Glyph(
                centerCoords,
                ASSOCDISSOC_SIZE,
                ASSOCDISSOC_SIZE,
                CdShape.CIRCLE,
                SbgnShape.CIRCLE),
                id, ASSOCDISSOC_SIZE, PORT_DISTANCE_RATIO, styleInfo);
    }
}
