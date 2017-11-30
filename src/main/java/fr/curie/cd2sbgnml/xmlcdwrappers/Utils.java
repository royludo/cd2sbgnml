package fr.curie.cd2sbgnml.xmlcdwrappers;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.sbml._2001.ns.celldesigner.Bounds;
import org.sbml._2001.ns.celldesigner.Notes;
import org.sbml.sbml.level2.version4.SBase;
import org.w3c.dom.Element;

import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    /**
     * Get the html Element notes of a celldesigner/sbml xml element
     * @param xml
     * @return
     */
    public static Element getNotes(List<Element> xml) {
        for(Element e: xml) {
            if(e.getElementsByTagName("html") != null && !isNoteEmpty(e)) {
                return e;
            }
        }
        return null;
    }

    public static Element getNotes(Notes xml) {
        if(xml != null) {
            return getNotes(xml.getAny());
        }
        return null;
    }

    public static Element getNotes(SBase.Notes xml) {
        if(xml != null) {
            return getNotes(xml.getAny());
        }
        return null;
    }

    /**
     * Will merge 2 notes defined by an html Element into one html element, by merging the body
     * @param h1
     * @param h2
     * @return
     */
    public static Element mergeHtmls(Element h1, Element h2) {
        if(h1 == null) {
            return h2;
        }

        if(h2 == null) {
            return h1;
        }

        Element body1 = (Element) h1.getElementsByTagName("body").item(0);
        Element body2 = (Element) h2.getElementsByTagName("body").item(0);
        Element newElement = (Element) h1.cloneNode(true);
        Element newBody = (Element) newElement.getElementsByTagName("body").item(0);
        newBody.setTextContent(
                body1.getTextContent()
                + "\n\n----- content merged by Celldesigner to SBGN-ML translation ------\n\n"
                + body2.getTextContent()
        );

        return newElement;
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
        Element title = (Element) note.getElementsByTagName("title").item(0);
        Element body = (Element) note.getElementsByTagName("body").item(0);

        return (title == null || title.getChildNodes().getLength() == 0)
                && (body == null || body.getChildNodes().getLength() == 0);
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
     * Resolve some special chars as CellDesigner specific encoding.
     * @param s
     * @return
     */
    public static String UTF8charsToCD(String s) {
        // special chars
        String newString =
                s.replaceAll("\\n", "_br_") // actual line break
                .replaceAll("\n", "_br_") // literal \n
                .replaceAll("&#10;", "_br_") // encoded line break
                .replaceAll("\\+", "_plus_")
                .replaceAll("-", "_minus_")
                .replaceAll("/", "_slash_")
                //.replaceAll("_", "_underscore_")
                .replaceAll(" ", "_space_");

        // greek small letters
        newString = newString
                .replaceAll("α","_alpha_")
                .replaceAll("β","_beta_")
                .replaceAll("γ","_gamma_")
                .replaceAll("δ","_delta_")
                .replaceAll("ε","_epsilon_")
                .replaceAll("ζ","_zeta_")
                .replaceAll("η","_eta_")
                .replaceAll("θ","_theta_")
                .replaceAll("ι","_iota_")
                .replaceAll("κ","_kappa_")
                .replaceAll("λ","_lambda_")
                .replaceAll("μ","_mu_")
                .replaceAll("ν","_nu_")
                .replaceAll("ξ","_xi_")
                .replaceAll("ο","_omicron_")
                .replaceAll("π","_pi_")
                .replaceAll("ρ","_rho_")
                .replaceAll("σ","_sigma_")
                .replaceAll("τ","_tau_")
                .replaceAll("υ","_upsilon_")
                .replaceAll("φ","_phi_")
                .replaceAll("χ","_chi_")
                .replaceAll("ψ","_psi_")
                .replaceAll("ω","_omega_");

        // greek capital letters
        newString = newString
                .replaceAll("Α", "_Alpha_")
                .replaceAll("Β", "_Beta_")
                .replaceAll("Γ", "_Gamma_")
                .replaceAll("Δ", "_Delta_")
                .replaceAll("Ε", "_Epsilon_")
                .replaceAll("Ζ", "_Zeta_")
                .replaceAll("Η", "_Eta_")
                .replaceAll("Θ", "_Theta_")
                .replaceAll("Ι", "_Iota_")
                .replaceAll("Κ", "_Kappa_")
                .replaceAll("Λ", "_Lambda_")
                .replaceAll("Μ", "_Mu_")
                .replaceAll("Ν", "_Nu_")
                .replaceAll("Ξ", "_Xi_")
                .replaceAll("Ο", "_Omicron_")
                .replaceAll("Π", "_Pi_")
                .replaceAll("Ρ", "_Rho_")
                .replaceAll("Σ", "_Sigma_")
                .replaceAll("Τ", "_Tau_")
                .replaceAll("Υ", "_Upsilon_")
                .replaceAll("Φ", "_Phi_")
                .replaceAll("Χ", "_Chi_")
                .replaceAll("Ψ", "_Psi_")
                .replaceAll("Ω", "_Omega_");

        return newString;
    }

    /**
     * Get the RDF element from an annotation element, if present
     * @param annotationsXml
     * @return
     */
    public static Element getRDFAnnotations(List<Element> annotationsXml) {
        for(Element e: annotationsXml) {
            if(e.getTagName().equals("rdf:RDF")) {
                return e;
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

    /**
     * @param b a CellDesigner Bound xml element
     * @return the corresponding Rectangle2D
     */
    public static Rectangle2D bounds2Rect(Bounds b) {
        return new Rectangle2D.Float(
                b.getX().floatValue(),
                b.getY().floatValue(),
                b.getW().floatValue(),
                b.getH().floatValue());
    }

    /**
     * @param r a Rectangle2D
     * @return the corresponding CellDesigner Bounds xml element
     */
    public static Bounds rect2Bounds(Rectangle2D r) {
        Bounds b = new Bounds();
        b.setX(BigDecimal.valueOf(r.getX()));
        b.setY(BigDecimal.valueOf(r.getY()));
        b.setW(BigDecimal.valueOf(r.getWidth()));
        b.setH(BigDecimal.valueOf(r.getHeight()));
        return b;
    }

}
