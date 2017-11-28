The xsd files in the `schema` directory comes from <https://github.com/funasoul/celldesigner-parser> and underwent the following modifications:
 * for JAXB to be able to correctly set a `@XmlRootElement` annotation, the Sbml type definition in `sbml-level-2-v4-wo-annotation.xsd` was moved to an anonymous inline definition of the `<sbml>` element.
 * the initial schemas were preventing any other annotation to be added to the `<annotation>` elements. This was preventing RDF annotations to be added into a CellDesigner file. To correct this, `<xs:any processContents="skip" minOccurs="0" maxOccurs="unbounded"/>` was added to the following CellDesigner types:
   - `compartmentAnnotationType`
   - `speciesAnnotationType`
   - `reactionAnnotationType`
   - `speciesReferenceAnnotationType`
   - `modelAnnotationType`
 * `font` was added to:
   - `speciesAlias`
   - `complesSpeciesAlias`
 * `<editPoints>` and `<connectScheme>` were added to:
   - `reactantLink`
   - `productLink`
 * For `modification` elements who are logic gates, the `modification` attribute was changed to `modificationType`
 * Following values were added as possible base reaction types:
   - PHYSICAL_STIMULATION
   - POSITIVE_INFLUENCE
   - NEGATIVE_INFLUENCE
   - REDUCED_MODULATION
   - REDUCED_TRIGGER
 * `<listOfGateMembers>` and `<GateMember>` elements were added to be able to manage reactions of type BOOLEAN_LOGIC_GATE.
 The `BOOLEAN_LOGIC_GATE` value was added to the reaction type enum.
