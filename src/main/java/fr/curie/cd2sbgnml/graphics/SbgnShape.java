package fr.curie.cd2sbgnml.graphics;

/**
 * General shapes used by SBGN. Some are approximation of the real shapes.
 * Things like cut or round corners are not taken into account.
 * The special shape of perturbing agent isn't here as it cannot be used due to CellDesigner not having this concept.
 */
public enum SbgnShape {
    RECTANGLE, ELLIPSE, PHENOTYPE, CIRCLE
}
