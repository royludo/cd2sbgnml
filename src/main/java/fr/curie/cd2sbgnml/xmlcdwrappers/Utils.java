package fr.curie.cd2sbgnml.xmlcdwrappers;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

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
                if(notes != null && !isNoteEmpty(notes)) {
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

        return (title == null || title.getChildNodes().getLength() == 0)
                && (body == null && body.getChildNodes().getLength() == 0);
    }

    /**
     * Resolve some special encoded strings in CellDesigner as UTF8 chars.
     * @param s
     * @return
     */
    public static String interpretToUTF8(String s) {
        // special chars
        String newString = s.replaceAll("_br_", "\n")
                .replaceAll("_plus_", "+")
                .replaceAll("_minus_", "-")
                .replaceAll("_slash_", "/")
                .replaceAll("_underscore_", "_")
                .replaceAll("_space_", " ");

        // greek small letters
        newString = newString
                .replaceAll("_alpha_", "α")
                .replaceAll("_beta_", "β")
                .replaceAll("_gamma_", "γ")
                .replaceAll("_delta_", "δ")
                .replaceAll("_epsilon_", "ε")
                .replaceAll("_zeta_", "ζ")
                .replaceAll("_eta_", "η")
                .replaceAll("_theta_", "θ")
                .replaceAll("_iota_", "ι")
                .replaceAll("_kappa_", "κ")
                .replaceAll("_lambda_", "λ")
                .replaceAll("_mu_", "μ")
                .replaceAll("_nu_", "ν")
                .replaceAll("_xi_", "ξ")
                .replaceAll("_omicron_", "ο")
                .replaceAll("_pi_", "π")
                .replaceAll("_rho_", "ρ")
                .replaceAll("_sigma_", "σ")
                .replaceAll("_tau_", "τ")
                .replaceAll("_upsilon_", "υ")
                .replaceAll("_phi_", "φ")
                .replaceAll("_chi_", "χ")
                .replaceAll("_psi_", "ψ")
                .replaceAll("_omega_", "ω");

        // greek capital letters
        newString = newString
                .replaceAll("_Alpha_", "Α")
                .replaceAll("_Beta_", "Β")
                .replaceAll("_Gamma_", "Γ")
                .replaceAll("_Delta_", "Δ")
                .replaceAll("_Epsilon_", "Ε")
                .replaceAll("_Zeta_", "Ζ")
                .replaceAll("_Eta_", "Η")
                .replaceAll("_Theta_", "Θ")
                .replaceAll("_Iota_", "Ι")
                .replaceAll("_Kappa_", "Κ")
                .replaceAll("_Lambda_", "Λ")
                .replaceAll("_Mu_", "Μ")
                .replaceAll("_Nu_", "Ν")
                .replaceAll("_Xi_", "Ξ")
                .replaceAll("_Omicron_", "Ο")
                .replaceAll("_Pi_", "Π")
                .replaceAll("_Rho_", "Ρ")
                .replaceAll("_Sigma_", "Σ")
                .replaceAll("_Tau_", "Τ")
                .replaceAll("_Upsilon_", "Υ")
                .replaceAll("_Phi_", "Φ")
                .replaceAll("_Chi_", "Χ")
                .replaceAll("_Psi_", "Ψ")
                .replaceAll("_Omega_", "Ω");

        // get rid of super and subscript, for lack of management possibility
        newString = newString
                .replaceAll("_super_","")
                .replaceAll("_endsuper_","")
                .replaceAll("_sub_","")
                .replaceAll("_endsub_","");

        return newString;
    }

    /**
     * Get the RDF element from an annotation element, if present
     * @param annotationsXml
     * @return
     */
    public static Element getRDFAnnotations(XmlObject annotationsXml) {
        for(int i=0; i < annotationsXml.getDomNode().getChildNodes().getLength(); i++) {
            Node n = annotationsXml.getDomNode().getChildNodes().item(i);
            System.out.println("n name: "+n.getNodeName());
            if(n.getNodeName().equals("rdf:RDF")) {
                Element rdf = (Element) n;
                return rdf;
            }
        }
        return null;
    }

    /**
     * This is used to tell jaxb to use 'celldesigner' as namespace prefix, when marshalling
     */
    public static class DefaultNamespacePrefixMapper extends NamespacePrefixMapper {

        private Map<String, String> namespaceMap = new HashMap<>();

        public DefaultNamespacePrefixMapper() {
            namespaceMap.put("http://www.sbml.org/2001/ns/celldesigner", "celldesigner");
            namespaceMap.put("http://www.w3.org/1998/Math/MathML", "mathml");

        }

        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            return namespaceMap.getOrDefault(namespaceUri, suggestion);
        }
    }

}
