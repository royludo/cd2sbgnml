package fr.curie.cd2sbgnml.graphics;

/**
 * The possible anchor points as defined by CellDesigner (except the CENTER value that is implicit
 * in CellDesigner if no anchor point is defined, or sometimes when it is set to INACTIVE).
 * Each anchor point's relative coordinate depends on the shape of the glyph.
 */
public enum AnchorPoint {
    N, NNE, NE, ENE, E, ESE, SE, SSE, S,
        SSW, SW, WSW, W, WNW, NW, NNW, CENTER
}
