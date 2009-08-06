package com.opensourcestrategies.crmsfa.util;

import java.util.Map;
import java.util.Locale;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;

public class OfbizErrorMessages extends OfbizMessages {
    private static String defaultErrorCssStyle = "ofbizErrorMessagesList";

    protected OfbizErrorMessages() {}

    public OfbizErrorMessages(String resource, Locale locale) {
        initMessages(resource, locale, defaultErrorCssStyle);
    }

    public OfbizErrorMessages(String resource, Locale locale, String label, Map data) {
        initMessages(resource, locale, defaultErrorCssStyle);
        resourceBundleMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap(resource, locale);
        add(label, data);
    }
}

