/**
 * Higher abstraction of xml wrappers.
 *
 * Classes of the package are used to deal mainly with reactions parsed from CellDesigner:
 * links and points management, creation of the non-existent process and logic gates glyphs, reducing branch type
 * reactions defined by CellDesigner into a more generic model.
 *
 * General remarks on CellDesigner way of storing reactions information:
 *
 * CellDesigner uses a special coordinate system to define the edit points of a link.
 * The x axis goes along a direct line passing through the center of both involved elements.
 * Origin of the x axis is the center of the start element, 1 is the center of the end element.
 * The y axis is orthogonal, its origin is the same as x, and the 1 coordinate is at the same distance as x.
 * Y axis is oriented on the right of x, following the global coordinate system of the map.
 *
 * It means that points going beyond the center of elements can have coordinates > 1 or < 0.
 *
 */
package fr.curie.cd2sbgnml.model;