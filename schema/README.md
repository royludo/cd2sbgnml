The xsd files in the `schema` directory comes from <https://github.com/funasoul/celldesigner-parser> and underwent the following modifications:
 * for JAXB to be able to correctly set a `@XmlRootElement` annotation, the Sbml type definition in `sbml-level-2-v4-wo-annotation.xsd` was moved to an anonymous inline definition of the `<sbml>` element.
 * the initial schemas were preventing any other annotation to be added to the `<annotation>` elements. This was preventing RDF annotations to be added into a CellDesigner file. To correct this, `<xs:any processContents="skip" minOccurs="0" maxOccurs="unbounded"/>` was added to the following CellDesigner types:
   - `compartmentAnnotationType`
   - `speciesAnnotationType`
   - `reactionAnnotationType`
   - `speciesReferenceAnnotationType`
   - `modelAnnotationType`
 