package com.opensourcestrategies.crmsfa.util;

import java.util.Map;
import java.util.Locale;
import java.util.Iterator;
import javolution.util.FastList;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;


/**
 * Candidate convenience class for handling internationalized messages.
 * The goal of this class is to standardize the way we generate messages
 * in services, servlets and bsh scripts.  In particular, services should
 * return this object or OfbizErrorMessages and the service event handler
 * can then print out the errors in a more convenient way.
 *
 * To use this class first define a set of UI Label error maps for your 
 * method with FTL subsitution strings for context in a properties file,
 *
 * SampleError = Warning, product with ID ${productId} has quantity ${quantity}
 *
 * Then, we can create a simple Message object with one error,
 *
 * Map context = UtilMisc.toMap("productId", productId, "quantity", quantity);
 * OfbizMessages messages = new OfbizMessages("SampleError", context);
 *
 * If you need to add more errors,
 *
 * messages.add("SampleError", context);
 *
 * There are several ways to generate text from this class,
 *
 * messages.toHtmlList();  // generates a UL list of messages
 * messages.toString();    // prints the messages as List.toString();
 *
 * The default style is "ofbizMessagesList" which is given to the
 * UL element.  To change the style,
 *
 * messages.setCssStyle(style);
 *
 * If you use OfbizErrorMessages, the default style is "ofbizErrorMessagesList".
 */
public class OfbizMessages {
    private static String defaultCssStyle = "ofbizMessagesList";

    private FastList messages = null;
    private String resource = null;
    private Locale locale = null;
    protected ResourceBundleMapWrapper resourceBundleMap = null;
    private String cssStyle;

    protected OfbizMessages() {}

    public OfbizMessages(String resource, Locale locale) {
        initMessages(resource, locale, defaultCssStyle);
    }

    public OfbizMessages(String resource, Locale locale, String label, Map context) {
        initMessages(resource, locale, defaultCssStyle);
        resourceBundleMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap(resource, locale);
        add(label, context);
    }

    protected void initMessages(String resource, Locale locale, String style) {
        messages = FastList.newInstance();
        this.resource = resource;
        this.locale = locale;
        cssStyle = style;
    }

    public int size() { return messages.size(); }
    public Iterator iterator() { return messages.iterator(); }

    public void add(String label) {
        String message = UtilProperties.getMessage(resource, label, locale);
        messages.add(message);
    }

    public void add(String label, Map context) {
        String message = UtilProperties.getMessage(resource, label, context, locale);
        messages.add(message);
    }

    public void setCssStyle(String style) { this.cssStyle = style; }

    public String toString() { return messages.toString(); }

    public String toHtmlList() {
        if (messages.size() == 0) return "";

        // don't use \n here because ofbiz transforms them to <br/> somewhere along the line
        StringBuffer buff = new StringBuffer("<ul class=\""+cssStyle+"\">");

        for (Iterator iter = messages.iterator(); iter.hasNext(); ) {
            buff.append("<li>");
            buff.append((String) iter.next());
            buff.append("</li>");
        }

        buff.append("</ul>");
        return buff.toString();
    }
}

