import org.sbml.x2001.ns.celldesigner.CelldesignerConnectSchemeDocument.CelldesignerConnectScheme;
import org.sbml.x2001.ns.celldesigner.CelldesignerLineDirectionDocument.CelldesignerLineDirection;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;

public class ReactionWrapper {

    private List<ReactantWrapper> reactantList;
    private String id;
    private CelldesignerConnectScheme baseConnectScheme;
    private List<ReactantWrapper> baseReactants;
    private List<ReactantWrapper> baseProducts;
    private List<ReactantWrapper> additionalReactants;
    private List<ReactantWrapper> additionalProducts;
    private List<ReactantWrapper> modifiers;
    private Reaction reaction;
    private Process process;
    private List<LinkWrapper> baseLinks; // 1 if no branches, else 3
    private int processSegmentIndex;

    public ReactionWrapper (Reaction reaction, ModelWrapper modelW) {
        this.id = reaction.getId();
        this.reaction = reaction;
        this.reactantList = ReactantWrapper.fromReaction(reaction, modelW);
        this.baseConnectScheme = reaction.getAnnotation().getCelldesignerConnectScheme();

        this.baseReactants = new ArrayList<>();
        this.baseProducts = new ArrayList<>();
        this.additionalReactants = new ArrayList<>();
        this.additionalProducts = new ArrayList<>();
        this.modifiers = new ArrayList<>();

        this.baseLinks = new ArrayList<>();

        // fill the corresponding lists
        for(ReactantWrapper reactW: this.reactantList) {
            switch(reactW.getReactantType()){
                case BASE_REACTANT: this.baseReactants.add(reactW); break;
                case BASE_PRODUCT:this.baseProducts.add(reactW); break;
                case ADDITIONAL_REACTANT:this.additionalReactants.add(reactW); break;
                case ADDITIONAL_PRODUCT: this.additionalProducts.add(reactW); break;
                case MODIFICATION: this.modifiers.add(reactW); break;
            }

            //System.out.println("Reactant link start point: "+ reaction.getId()+" "+reactW.getAliasW().getId()+" "+reactW.getLinkStartingPoint());
        }

        System.out.println("REACTION ID: "+this.getId());
        if(!this.isBranchType()) {
            ReactantWrapper startR = this.baseReactants.get(0);
            ReactantWrapper endR = this.baseProducts.get(0);

            int segmentCount = baseConnectScheme.getCelldesignerListOfLineDirection().sizeOfCelldesignerLineDirectionArray();
            this.processSegmentIndex = getProcessSegment(reaction);
            //System.out.println("segment count: "+segmentCount+" rectangleIndex: "+processSegmentIndex);

            Point2D.Float baseLinkStartPoint = startR.getLinkStartingPoint();
            Point2D.Float baseLinkEndPoint = endR.getLinkStartingPoint();

            List<Point2D.Float> editPoints = getBaseEditPoints();
            List<AffineTransform> transformList = GeometryUtils.getTransformsToGlobalCoords(baseLinkStartPoint, baseLinkEndPoint);
            List<Point2D.Float> absoluteEditPoints = convertPoints(editPoints, transformList);

            // TODO refactor this, overcomplicated
            Point2D.Float currentStartPoint = baseLinkStartPoint;
            Point2D.Float currenEndPoint;
            List<Point2D.Float> subLinkPoints1 = new ArrayList<>();
            List<Point2D.Float> subLinkPoints2 = new ArrayList<>();
            List<Point2D.Float> currentSubLink = subLinkPoints1;
            currentSubLink.add(currentStartPoint);
            System.out.println("Segment count: "+segmentCount);
            for(int i=0; i < segmentCount; i++) {
                System.out.println("i: "+i);
                if(i == segmentCount - 1) { // we're at the end
                    System.out.println("END");
                    currenEndPoint = baseLinkEndPoint;
                }
                else {
                    currenEndPoint = absoluteEditPoints.get(i);
                }

                if(i == processSegmentIndex) { // split this segment in 2
                    System.out.println("process segment");
                    Point2D.Float processCoords = new Point2D.Float(
                            (float) (currentStartPoint.getX()  + (currenEndPoint.getX() - currentStartPoint.getX()) / 2),
                            (float) (currentStartPoint.getY() + (currenEndPoint.getY() - currentStartPoint.getY()) / 2));
                    this.process = new Process(processCoords);
                    currentSubLink.add(processCoords);
                    currentSubLink = subLinkPoints2;
                    currentSubLink.add(processCoords);
                }
                currentSubLink.add(currenEndPoint);

                currentStartPoint = currenEndPoint;
            }
            System.out.println(subLinkPoints1+" //// "+subLinkPoints2);

            this.baseLinks.add(new LinkWrapper(startR, this.process, subLinkPoints1));
            this.baseLinks.add(new LinkWrapper(this.process, endR, subLinkPoints2));
            this.baseLinks.get(0).setSbgnSpacePointList(
                    this.baseLinks.get(0).getNormalizedEndPoints(
                            startR.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                    ));
            this.baseLinks.get(1).setSbgnSpacePointList(
                    this.baseLinks.get(1).getNormalizedEndPoints(
                            GeometryUtils.AnchorPoint.CENTER, endR.getAnchorPoint()
                    ));
        }
        else {
            if(isBranchTypeLeft()) { // more reactant, association
                ReactantWrapper startR1 = this.baseReactants.get(0);
                ReactantWrapper startR2 = this.baseReactants.get(1);
                ReactantWrapper endR = this.baseProducts.get(0);

                List<Integer> segmentCount = new ArrayList<>();
                segmentCount.add(0);
                segmentCount.add(0);
                segmentCount.add(0);
                List<CelldesignerLineDirection> listOfLineDirection =
                        Arrays.asList(baseConnectScheme.getCelldesignerListOfLineDirection().getCelldesignerLineDirectionArray());
                for(CelldesignerLineDirection lineDirection: listOfLineDirection){
                    int arm = Integer.parseInt(lineDirection.getArm());
                    segmentCount.set(arm, segmentCount.get(arm) + 1);
                }

                System.out.println("branch segments count: "+segmentCount);

                // list edit points
                List<Point2D.Float> editPoints = this.getBaseEditPoints();

                // process association point
                Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1);
                Point2D.Float assocGlyphGlobalCoords = getAssocDissocPoint(
                        startR1.getCenterPoint(),
                        startR2.getCenterPoint(),
                        endR.getCenterPoint(), assocGlyphLocalCoords);
                System.out.println("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

                this.process = new Process(assocGlyphGlobalCoords);

                Point2D.Float startR1coordPoint;
                Point2D.Float startR2coordPoint;
                Point2D.Float endRcoordPoint;
                if(this.baseConnectScheme.getConnectPolicy().equals("square")) {
                    startR1coordPoint = startR1.getLinkStartingPoint();
                    startR2coordPoint = startR2.getLinkStartingPoint();
                    endRcoordPoint = endR.getLinkStartingPoint();
                }
                else {
                    startR1coordPoint = startR1.getLinkStartingPoint();
                    startR2coordPoint = startR2.getLinkStartingPoint();
                    endRcoordPoint = endR.getLinkStartingPoint();
                }

                // careful here, edit points goes from origin (process glyph) to reactant
                // but we want the opposite, as a production arc it goes from reactant to process
                List<Point2D.Float> absoluteEditPoints0 = this.getBranchPoints(process.getCoords(), startR1coordPoint, 0);
                Collections.reverse(absoluteEditPoints0);
                LinkWrapper link0 = new LinkWrapper(startR1, process, absoluteEditPoints0);
                link0.setSbgnSpacePointList(
                        link0.getNormalizedEndPoints(
                                startR1.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                        ));
                this.baseLinks.add(link0);

                List<Point2D.Float> absoluteEditPoints1 = this.getBranchPoints(process.getCoords(), startR2coordPoint, 1);
                Collections.reverse(absoluteEditPoints1);
                LinkWrapper link1 = new LinkWrapper(startR2, process, absoluteEditPoints1);
                link1.setSbgnSpacePointList(
                        link1.getNormalizedEndPoints(
                                startR2.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                        ));
                this.baseLinks.add(link1);

                List<Point2D.Float> absoluteEditPoints2 = this.getBranchPoints(process.getCoords(), endRcoordPoint, 2);
                LinkWrapper link2 = new LinkWrapper(process, endR, absoluteEditPoints2);
                link2.setSbgnSpacePointList(
                        link2.getNormalizedEndPoints(
                                GeometryUtils.AnchorPoint.CENTER, endR.getAnchorPoint()
                        ));
                this.baseLinks.add(link2);
            }
            else { // more products, dissociation
                ReactantWrapper startR = this.baseReactants.get(0);
                ReactantWrapper endR1 = this.baseProducts.get(0);
                ReactantWrapper endR2 = this.baseProducts.get(1);

                List<Integer> segmentCount = new ArrayList<>();
                segmentCount.add(0);
                segmentCount.add(0);
                segmentCount.add(0);
                List<CelldesignerLineDirection> listOfLineDirection =
                        Arrays.asList(baseConnectScheme.getCelldesignerListOfLineDirection().getCelldesignerLineDirectionArray());
                for(CelldesignerLineDirection lineDirection: listOfLineDirection){
                    int arm = Integer.parseInt(lineDirection.getArm());
                    segmentCount.set(arm, segmentCount.get(arm) + 1);
                }

                System.out.println("branch segments count: "+segmentCount);

                // list edit points
                List<Point2D.Float> editPoints = this.getBaseEditPoints();

                // process association point
                Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1);
                Point2D.Float assocGlyphGlobalCoords = getAssocDissocPoint(
                        startR.getCenterPoint(),
                        endR1.getCenterPoint(),
                        endR2.getCenterPoint(), assocGlyphLocalCoords);
                System.out.println("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

                this.process = new Process(assocGlyphGlobalCoords);

                List<Point2D.Float> absoluteEditPoints0 = this.getBranchPoints(process.getCoords(), startR.getLinkStartingPoint(), 0);
                Collections.reverse(absoluteEditPoints0);
                LinkWrapper link0 = new LinkWrapper(startR, process, absoluteEditPoints0);
                link0.setSbgnSpacePointList(
                        link0.getNormalizedEndPoints(
                                startR.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                                ));
                this.baseLinks.add(link0);

                List<Point2D.Float> absoluteEditPoints1 = this.getBranchPoints(process.getCoords(), endR1.getLinkStartingPoint(), 1);
                //Collections.reverse(absoluteEditPoints1);
                LinkWrapper link1 = new LinkWrapper(process, endR1, absoluteEditPoints1);
                link1.setSbgnSpacePointList(
                        link1.getNormalizedEndPoints(
                                GeometryUtils.AnchorPoint.CENTER, endR1.getAnchorPoint()
                                ));
                this.baseLinks.add(link1);

                List<Point2D.Float> absoluteEditPoints2 = this.getBranchPoints(process.getCoords(), endR2.getLinkStartingPoint(), 2);
                LinkWrapper link2 = new LinkWrapper(process, endR2, absoluteEditPoints2);
                link2.setSbgnSpacePointList(
                        link2.getNormalizedEndPoints(
                                GeometryUtils.AnchorPoint.CENTER, endR2.getAnchorPoint()
                        ));
                this.baseLinks.add(link2);
            }

        }

        // process modifiers
        for(ReactantWrapper reactantW: this.getModifiers()) {
            // simple case, no logic gate
            System.out.println("modifier: "+reactantW.getAliasW().getId());
            int modifIndex = reactantW.getLink().getIndex();
            List<Point2D.Float> editPoints = this.getEditPointsForModifier(modifIndex);
            CelldesignerModification modif = reaction.getAnnotation().
                    getCelldesignerListOfModification().getCelldesignerModificationArray(modifIndex);
            Point2D.Float processAnchorPoint = process.getAbsoluteAnchorCoords(ReactantWrapper.getProcessAnchorIndex(modif));

            System.out.println("edit points: "+editPoints);

            List<AffineTransform> transformList =
                    GeometryUtils.getTransformsToGlobalCoords(reactantW.getLinkStartingPoint(), processAnchorPoint);
            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(reactantW.getLinkStartingPoint());
            absoluteEditPoints.addAll(convertPoints(editPoints, transformList));
            absoluteEditPoints.add(processAnchorPoint);

            String linkCdClass = reaction.getAnnotation().getCelldesignerListOfModification().
                    getCelldesignerModificationArray(modifIndex).getType();

            LinkWrapper link = new LinkWrapper(reactantW, process, absoluteEditPoints,
                    modifIndex, linkCdClass);
            link.setSbgnSpacePointList(
                    link.getNormalizedEndPoints(
                            reactantW.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                            ));

            reactantW.setLink(link);


        }
    }

    public boolean isBranchTypeLeft() {
        if(this.baseReactants.size() > 1 && this.baseProducts.size() > 1) {
            throw new RuntimeException("Multiple branches on both sides of reaction: "+this.getId()+" unforeseen case.");
        }
        return this.baseReactants.size() > 1 && this.baseProducts.size() == 1;
    }

    public boolean isBranchTypeRight() {
        if(this.baseReactants.size() > 1 && this.baseProducts.size() > 1) {
            throw new RuntimeException("Multiple branches on both sides of reaction: "+this.getId()+" unforeseen case.");
        }
        return this.baseReactants.size() == 1 && this.baseProducts.size() > 1;
    }

    // we assume there can never be multiple branches on the left AND right at the same time
    // branch amount != reactant amount
    // branching is defined by base products and reactant only
    public boolean isBranchType() {
        return this.isBranchTypeLeft() || this.isBranchTypeRight();
    }

    public Point2D getProcessCoords (ModelWrapper modelW) {

        // TODO also take complexSpeciesAlias into account <- should be ok

        //System.out.println("reactant "+reactantBbox.getX()+" "+reactantBbox.getY()+" "+reactantBbox.getW()+" "+reactantBbox.getH());
        //System.out.println("product "+productBbox.getX()+" "+productBbox.getY()+" "+productBbox.getW()+" "+productBbox.getH());

        Point2D processCenter = null;
        if(!this.isBranchType()) {
            // TODO take anchor point into account
            // for now, only from center
            Point2D reactantStart = this.baseReactants.get(0).getLinkStartingPoint();
            Point2D productEnd = this.baseProducts.get(0).getLinkStartingPoint();

            LinkWrapper baseLink = this.baseLinks.get(0);

            if(baseLink.getPointList().size() == 0) {
                //System.out.println(reactantStart+" "+productEnd);

                processCenter = new Point2D.Double(
                        reactantStart.getX() + (productEnd.getX() - reactantStart.getX()) / 2,
                        reactantStart.getY() + (productEnd.getY() - reactantStart.getY()) / 2
                );
            }
            else {
                Point2D segmentStart;
                Point2D segmentEnd;
                int segmentCount = baseLink.getPointList().size() + 1;
                if(this.processSegmentIndex == 0) { // on first segment
                    segmentStart = reactantStart;
                    segmentEnd = baseLink.getPointList().get(0);
                }
                else if(this.processSegmentIndex == segmentCount - 1) { // on last segment
                    segmentStart = baseLink.getPointList().get(segmentCount - 2);
                    segmentEnd = productEnd;
                }
                else {
                    segmentStart = baseLink.getPointList().get(this.processSegmentIndex - 1);
                    segmentEnd = baseLink.getPointList().get(this.processSegmentIndex);
                }

                processCenter = new Point2D.Double(
                        segmentStart.getX() + (segmentEnd.getX() - segmentStart.getX()) / 2,
                        segmentStart.getY() + (segmentEnd.getY() - segmentStart.getY()) / 2
                );

            }
        }
        else {
            processCenter = new Point2D.Float(0,0);
        }

        return processCenter;

    }

    /**
     * CellDesigner uses a special coordinate system to define the edit points of a link.
     * The x axis goes along a direct line passing through the center of both involved elements.
     * Origin of the x axis is the center of the start element, 1 is the center of the end element.
     * The y axis is orthogonal, its origin is the same as x, and the 1 coordinate is at the same distance as x.
     * Y axis is oriented on the right of x, following the global coordinate system of the map.
     *
     * It means that points going beyond the center of elements can have coordinates > 1 or < 0.
     *
     */
    /*public Point2f localToAbsoluteCoord(Point2f cStart, Point2f cEnd, Point2f p) {
        return new Point2f();
    }*/


    public List<Point2D.Float> getBaseEditPoints (){
        if(!reaction.getAnnotation().isSetCelldesignerEditPoints()) {
            return new ArrayList<>();
        }

        String editPointString = reaction.getAnnotation().getCelldesignerEditPoints().
                getDomNode().getFirstChild().getNodeValue();
       return parseEditPointsString(editPointString);
    }

    public List<Point2D.Float> getEditPointsForBranch(int b) {
        List<Point2D.Float> editPoints = this.getBaseEditPoints();
        int num0 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum0());
        int num1 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum1());
        int num2 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum2());

        List<Point2D.Float> finalEditPoints = new ArrayList<>();
        switch(b) {
            case 0:
                for(int i=0; i < num0; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            case 1:
                for(int i=num0; i < num0 + num1; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            case 2:
                // don't go to the end of edit points list, last one may be
                // for association/dissociation point or for logic gate
                for(int i=num0 + num1; i < num0 + num1 + num2; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            default:
                throw new RuntimeException("Value: "+b+" not allowed for branch index. Authorized values: 0, 1, 2.");
        }
        return finalEditPoints;
    }

    public List<Point2D.Float> getEditPointsForModifier(int index) {
        CelldesignerModification modif = reaction.getAnnotation().
                getCelldesignerListOfModification().getCelldesignerModificationArray(index);

        if(!modif.isSetEditPoints()) {
            return new ArrayList<>();
        }

        String editPointString = modif.getEditPoints().getStringValue();
        return parseEditPointsString(editPointString);
    }

    public List<Point2D.Float> parseEditPointsString(String editPointString) {
        List<Point2D.Float> editPoints = new ArrayList<>();
        Arrays.stream(editPointString.split(" ")).
                forEach(e -> {
                    String[] tmp = e.split(",");
                    editPoints.add(new Point2D.Float(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1])));
                });
        return editPoints;
    }

    /**
     * Get the segment index on which the process glyph of a reaction is located.
     * If no process is present (direct connection), 0 (1st segment) is returned.
     * If reaction has branches (association/dissociation), segment index is taken from tshapeIndex
     * @param reaction sbml reaction element
     * @return index as int starting from 0
     */
    public static int getProcessSegment(Reaction reaction) {
        CelldesignerConnectScheme connectScheme = reaction.getAnnotation().getCelldesignerConnectScheme();
        if(connectScheme.getDomNode().
                getAttributes().getNamedItem("rectangleIndex") != null) {
            return Integer.parseInt(connectScheme.getDomNode().
                    getAttributes().getNamedItem("rectangleIndex").getNodeValue());
        }
        else {
            if(reaction.getAnnotation().isSetCelldesignerEditPoints()
                    && reaction.getAnnotation().getCelldesignerEditPoints().isSetTShapeIndex()) {
                return Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getTShapeIndex());
            }
            else {
                // default to 1st segment, if nothing is specified
                return 0;
            }
        }
    }

    /**
     * Apply a list of affine transforms to a list of points.
     * Used to change the coordinate system of a set of points.
     * @param points a list of 2D coordinates
     * @param transforms a list of transforms to be applied
     * @return a new list of points
     */
    public List<Point2D.Float> convertPoints(List<Point2D.Float> points, List<AffineTransform> transforms) {

        List<Point2D.Float> convertedPoints = new ArrayList<>();
        for (Point2D editP : points) {
            Point2D p = new Point2D.Double(editP.getX(), editP.getY());

            for(AffineTransform t: transforms) {
                t.transform(p, p);
            }

            System.out.println("result: " + editP + " -> " + p.toString());
            convertedPoints.add(new Point2D.Float((float) p.getX(), (float) p.getY()));

        }
        return convertedPoints;
    }

    public static Point2D.Float getAssocDissocPoint(Point2D.Float origin, Point2D.Float pX, Point2D.Float pY, Point2D.Float editPoint) {
        //System.out.println("local coord system: "+origin+" "+pX+" "+pY);
        //System.out.println("local edit points: "+editPoint);

        // transform association glyph point
        Point2D.Float absolutePoint = new Point2D.Float((float) editPoint.getX(), (float) editPoint.getY());
        for(AffineTransform t: GeometryUtils.getTransformsToGlobalCoords(origin, pX, pY)) {
            t.transform(absolutePoint, absolutePoint);
        }

        return absolutePoint;
    }

    public LinkedList<Point2D.Float> getBranchPoints(Point2D.Float origin, Point2D.Float pX, int branch) {

        LinkedList<Point2D.Float> absoluteEditPoints = new LinkedList<>();
        absoluteEditPoints.add(origin);
        System.out.println("local system: "+origin+" "+pX);
        System.out.println("points for BRANCH "+branch+" "+ this.getEditPointsForBranch(branch));

        for (Point2D editP : this.getEditPointsForBranch(branch)) {
            Point2D p = new Point2D.Double(editP.getX(), editP.getY());

            for(AffineTransform t: GeometryUtils.getTransformsToGlobalCoords(origin, pX)) {
                t.transform(p, p);
            }

            System.out.println("BRANCH "+branch+" result: " + editP + " -> " + p.toString());
            absoluteEditPoints.add(new Point2D.Float((float) p.getX(), (float) p.getY()));

        }
        absoluteEditPoints.add(pX);
        System.out.println("BRANCH "+branch+" stack: "+absoluteEditPoints);

        return absoluteEditPoints;
    }


    public List<ReactantWrapper> getReactantList() {
        return reactantList;
    }

    public String getId() {
        return id;
    }

    public CelldesignerConnectScheme getBaseConnectScheme() {
        return baseConnectScheme;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public Process getProcess() {
        return process;
    }

    public List<ReactantWrapper> getBaseReactants() {
        return baseReactants;
    }

    public List<ReactantWrapper> getBaseProducts() {
        return baseProducts;
    }

    public List<ReactantWrapper> getAdditionalReactants() {
        return additionalReactants;
    }

    public List<ReactantWrapper> getAdditionalProducts() {
        return additionalProducts;
    }

    public List<ReactantWrapper> getModifiers() {
        return modifiers;
    }

    public List<LinkWrapper> getBaseLink() {
        return baseLinks;
    }

    public int getProcessSegmentIndex() {
        return processSegmentIndex;
    }


}
