package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Utils {

    /**
     * Get the html Element notes of a celldesigner/sbml xml element
     * @param xml
     * @return
     */
    public static Element getNotes(XmlObject xml) {
        for(int i =0; i < xml.getDomNode().getChildNodes().getLength(); i++) {
            Node n = xml.getDomNode().getChildNodes().item(i);
             if((n.getNodeName().equals("notes") || n.getNodeName().equals("celldesigner_notes")) &&
                    ((Element) n).getElementsByTagName("html") != null) {
                Element notes = (Element)((Element) n).getElementsByTagName("html").item(0);
                if(!isNoteEmpty(notes)) {
                    return notes;
                }
            }
        }
        return null;
    }

    /**
     * Celldesigner saves empty notes of the form
     *
     * <html>
     *     <head>
     *         <title/>
     *     </head>
     *     <body/>
     * </html>
     *
     * We don't want them. This function will return true if the <title> and <body> are empty.
     * @param note
     * @return
     */
    public static boolean isNoteEmpty(Element note) {
        //System.out.println("note is empty?");
        Element title = (Element) note.getElementsByTagName("title").item(0);
        Element body = (Element) note.getElementsByTagName("body").item(0);

        /*System.out.println(title.getChildNodes().getLength());
        System.out.println(body.getChildNodes().getLength());

        if(body.getChildNodes().getLength() != 1) {
            System.out.println(">>>>>>>>>>>>>>>>");
        }*/

        return title.getChildNodes().getLength() == 0
                && body.getChildNodes().getLength() == 0;
    }

}
