package fr.curie.cd2sbgnml.graphics;

/**
 * All the general shapes a CellDesigner glyph can take. Some are approximation of the real shapes.
 * Things like cut or round corners are not taken into account.
 */
public enum CdShape {
    RECTANGLE, ELLIPSE, PHENOTYPE, LEFT_PARALLELOGRAM, RIGHT_PARALLELOGRAM, TRUNCATED, CIRCLE,
    EMPTY, RECEPTOR
}
