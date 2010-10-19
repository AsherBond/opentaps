/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

package org.opentaps.gwt.messages;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.ServiceDispatcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import freemarker.template.Configuration;
import freemarker.template.Template;


/**
 * Converts Opentaps labels into GWT compatible labels.
 *
 * Import the labels from the properties files specified in "opentaps/opentaps-common/config/LabelConfiguration.properties" (see <code>sources</code>).
 *
 * For each label we try to extract its placeholders in order to generate an equivalent method.
 * eg: <code>cart.quantity_not_positive_number=Quantity requested: ${quantityReq} Quantity available: ${quantityAvail}</code>
 *     generates the following method:
 *     <code>String cart_quantity_not_positive_number(String quantityReq, String quantityAvail)</code>
 *     and the text:
 *     <code>cart_quantity_not_positive_number = Quantity requested: {0} Quantity available: {1}</code>
 *
 * We currently support two kind of placeholders: indexed, and FreeMarker.
 *
 * In order to conform with the GWT message, labels are reformatted as follow:
 * <ul>
 *  <li>the label key is transformed into a Java method name, so any special character is replaced by an underscore.
 *      eg: <code>cart.quantity_not_positive_number</code> => <code>cart_quantity_not_positive_number</code>
 *  <li>placeholders are replaced by indexed placeholders.
 *      eg: <code>Quantity requested: ${quantityReq} Quantity available: ${quantityAvail}</code> => <code>Quantity requested: {0} Quantity available: {1}</code>
 *  <li>single quotes are escaped, being replaced by two single quotes.
 *      eg: <code>Show This Item's Notes</code> => <code>Show This Item''s Notes</code>
 * </ul>
 *
 * Because we may include labels that are already GWT formatted, we assume their key starts with an underscore. As a result
 * the only processing will be to lookup placeholders in the non quoted part of it's text.
 * But note that this should be limited to a couple of labels used in GWT-Ext widget (such as the pager).
 * eg: <code>_exampleOfMessage = Hello {0} '{0} - {1}'</code>
 *     generates the following method:
 *     <code>String exampleOfMessage(String param_0)</code>
 *     and the text:
 *     <code>exampleOfMessage = Hello {0} '{0} - {1}'</code>
 */
public final class GwtLabelsGeneratorContainer implements Container {

    private static final String MODULE = GwtLabelsGeneratorContainer.class.getName();
    @SuppressWarnings("unused")
    private static final String CONTAINER_NAME = "gwtlabels-generator-container";

    // the default locale is the locale of the properties file that do no specify the local, IE: CommonUiLabels.properties
    // so we do not need to generate a CommonuiLabels_{DEFAULT_LOCALE}.properties
    private static final String DEFAULT_LOCALE = "en";

    /** Config file. */
    @SuppressWarnings("unused")
    private String configFile = null;
    
    static Pattern ftlPlaceholderPattern;
    static Pattern indexedPlaceholderPattern;
    static Pattern fakePlaceholderPattern;
    static {

        // this regex identifies FreeMarker placeholders in the label text
        // eg: Welcome ${firstName} ${lastName}
        // It supports first level recursions like ${amountApplied?currency(${isoCode})}
        //  this matches ${... (?: ${..} ...)? }
        String ftlPlaceholderRegex = "`$`{([^`}`$]+?(?:`$`{[^`}]+?`}[^`}`$]+?)?)`}"; // note: using ` instead of \\ for 'better' readability
        ftlPlaceholderRegex = ftlPlaceholderRegex.replace('`', '\\');
        ftlPlaceholderPattern = Pattern.compile(ftlPlaceholderRegex);

        // this regex identifies indexed placeholders in the label text
        // eg: Welcome {0} {1}
        String indexedPlaceholderRegex = "`{([0-9]+)`}"; // note: using ` instead of \\ for 'better' readability
        indexedPlaceholderRegex = indexedPlaceholderRegex.replace('`', '\\');
        indexedPlaceholderPattern = Pattern.compile(indexedPlaceholderRegex);

        // this regex identifies fake placeholders in the label text, this syntax is used in some rare places
        // eg: Welcome {something}
        // this also match FreeMarker placeholders so we only use it after substituting them by indexed placeholders
        String fakePlaceholderRegex = "`{[^0-9]+`}"; // note: using ` instead of \\ for 'better' readability
        fakePlaceholderRegex = fakePlaceholderRegex.replace('`', '\\');
        fakePlaceholderPattern = Pattern.compile(fakePlaceholderRegex);
    }

    /**
     * Creates a new <code>GwtLabelsGeneratorContainer</code> instance.
     */
    public GwtLabelsGeneratorContainer() {
        super();
    }

    /** {@inheritDoc} */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;
        // disable job scheduler, JMS listener and startup services
        ServiceDispatcher.enableJM(false);
        ServiceDispatcher.enableJMS(false);
        ServiceDispatcher.enableSvcs(false);

        // parse arguments here if needed
    }

    /** {@inheritDoc} */
    public void stop() throws ContainerException { }

    /** {@inheritDoc} */
    public boolean start() throws ContainerException {

        // *** Configuration

        // directory containing the template
        String templatePath = "opentaps/opentaps-common/templates/";
        // template to use
        String templateFile = "BaseGWTUILabel.ftl";

        // the path to the GWT module configuration that defines the target locale, should be common.gwt.xml
        String gwtModuleConfigurationFile = "opentaps/opentaps-common/src/common/org/opentaps/gwt/common/common.gwt.xml";

        // directory where the labels are generated (must end with '/')
        String gwtPropertiesDir = "opentaps/opentaps-common/src/common/org/opentaps/gwt/common/client/messages/";
        // the class name of the GWT message interface to generate in the gwtPropertiesDir
        //  this will generate <gwtPropertiesDir>/<gwtLabelFileName>.java as the interface
        //  <gwtPropertiesDir>/<gwtLabelFileName>.properties as the properties files containing the default label definitions (corresponding to DEFAULT_LOCALE)
        //  <gwtPropertiesDir>/<gwtLabelFileName>_<locale>.properties as the properties files containing the label definitions for other locales specified in the GWT configuration (common.gwt.xml)
        String gwtLabelFileName = "CommonMessages";

        // load the list of UI labels to use as a source
        Properties sources = loadPropertiesFile("opentaps/opentaps-common/config/LabelConfiguration.properties");


        try {

            // read the target locales from the GWT module configuration (common.gwt.xml)
            // by parsing the XML structure
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(gwtModuleConfigurationFile));
            Element rootElement = document.getDocumentElement();
            // each locale is listed as a distinct "extend-property" XML tag, ie: <extend-property name="locale" values="fr"/>
            NodeList list = rootElement.getElementsByTagName("extend-property");
            // the list to store the locale 
            List<String> extendLocales = new ArrayList<String> ();
            // loop through all "extend-property tags
            for (int i = 0; i < list.getLength(); i++) {
                Element element = (Element) list.item(i);
                // check if the tag defines a locale
                if (element.getAttribute("name").equals("locale")) {
                    // each tag only defines one locale
                    String locale = element.getAttribute("values");
                    if (!extendLocales.contains(locale) && !DEFAULT_LOCALE.equals(locale)) {
                        extendLocales.add(locale);
                    }
                }
            }

            // use the default label definitions to generate the Java interface file
            TreeMap<String, GwtLabelDefinition> defaultGwtLabelDefinitionsMaps = new TreeMap<String, GwtLabelDefinition>();
            Set<Entry <Object, Object>> entries = sources.entrySet();
            for (Entry <Object, Object> entry : entries) {
                try {
                    String fileName = (String) entry.getValue();
                    readLabelsFromSourcePropertiesFile(fileName, DEFAULT_LOCALE, defaultGwtLabelDefinitionsMaps);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            // render the GWT message interface Java file from the FTL template
            Writer writer = new StringWriter();
            Configuration ftlConfig = new Configuration();
            ftlConfig.setDirectoryForTemplateLoading(new File(templatePath));
            Template t = ftlConfig.getTemplate(templateFile);
            Map<String, Object> context = new HashMap<String, Object>();
            // label keys are sorted alphabetically by the TreeMap, so the values() collection is sorted automatically
            context.put("functions", defaultGwtLabelDefinitionsMaps.values());
            t.process(context, writer);

            // write the GWT message interface Java file
            File javaFile = new File(gwtPropertiesDir + gwtLabelFileName + ".java");
            FileUtils.writeStringToFile(javaFile, writer.toString(), "UTF-8");

            // write the properties files
            writeGwtLocaleProperties(defaultGwtLabelDefinitionsMaps, DEFAULT_LOCALE, gwtPropertiesDir, gwtLabelFileName);
            for (String locale : extendLocales) {
                // iterator all locale and write gwt labels properties file
                writeGwtLocaleProperties(defaultGwtLabelDefinitionsMaps, locale, gwtPropertiesDir, gwtLabelFileName);
            }
            
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return false;
        }
        return true;
    }

    /**
     * Writes gwt locale properties, generate locale properties files base default GWT label properties
     * @param defaultGwtLabelDefinitionsMaps a <code>Map</code> of gwt label definitions
     * @param locale a <code>String</code> value of Locale
     * @param gwtPropertiesDir a <code>String</code> value
     * @param gwtLabelFileName a <code>String</code> value
     */
    private void writeGwtLocaleProperties(TreeMap<String, GwtLabelDefinition> defaultGwtLabelDefinitionsMaps, String locale, String gwtPropertiesDir, String gwtLabelFileName) throws IOException {
        // write the localized properties files
        StringBuffer stringBuffer = new StringBuffer();
        for (String key : defaultGwtLabelDefinitionsMaps.keySet()) {
            // check the label has the same signature as the default label
            GwtLabelDefinition defaultLabel = defaultGwtLabelDefinitionsMaps.get(key);
            String localizedString = UtilProperties.getMessage(getResourceNameFromPath(defaultLabel.getPropertiesFile()), defaultLabel.getOriginKey(), new Locale(locale));
            if (UtilValidate.isNotEmpty(localizedString)) {
                if (!key.startsWith("_")) {
                    // save the list of parameters that were parsed from the label text
                    // and that will be used as the message interface method parameters
                    List<String> parameters = new ArrayList<String>();
                    localizedString = formatGwtLabel(localizedString, parameters);
                    // replace extra quote character which add by UtilProperties
                    localizedString = localizedString.replace("''{", "'{");
                    localizedString = localizedString.replace("}''", "}'");
                    localizedString = localizedString.replace("'''{", "''{");
                    localizedString = localizedString.replace("}'''", "}''");
                }
                stringBuffer.append(key).append(" = ").append(localizedString).append("\n");
            }
        }
        String outGwtLabelPropertiesFile = gwtPropertiesDir + gwtLabelFileName + ".properties";
        if (!locale.equals(DEFAULT_LOCALE)) {
            outGwtLabelPropertiesFile = gwtPropertiesDir + gwtLabelFileName + "_" + locale + ".properties";
        }
        File propertiesFile = new File(outGwtLabelPropertiesFile);
        FileUtils.writeStringToFile(propertiesFile, stringBuffer.toString(), "UTF-8");
    }
    
    /**
     * Gets resource name from file path.
     * @param path the path of properties file
     * @return the resource name
     */
    private String getResourceNameFromPath(String path) {
        int beginIndex = path.lastIndexOf("/") < 0 ? 0 : path.lastIndexOf("/") + 1;
        int endIndex = path.lastIndexOf(".") < 0 ? path.length() : path.lastIndexOf(".");
        return path.substring(beginIndex, endIndex);
    }
    
    /**
     * Reads a properties file into a <code>Properties</code> object using the DEFAULT_LOCALE.
     * @param fileName the properties file name
     * @return the corresponding <code>Properties</code> object
     */
    private static Properties loadPropertiesFile(String fileName) {
        return loadPropertiesFile(fileName, DEFAULT_LOCALE);
    }

    /**
     * Reads a properties file into a <code>Properties</code> object.
     * @param fileName the properties file name
     * @param locale the locale that properties file use
     * @return the corresponding <code>Properties</code> object
     */
    private static Properties loadPropertiesFile(String fileName, String locale) {
        Properties props = new Properties();
        String propertiesFileName = getPropertiesFileName(fileName, locale);
        try {
            File file = new File(propertiesFileName);
            if (file.exists()) {
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                props.load(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Gets locale properties file name.
     * @param fileName the properties file name
     * @param locale the locale that properties file use
     * @return the locale properties file name
     */
    private static String getPropertiesFileName(String fileName, String locale) {
        String propertiesFileName = fileName;
        if (!locale.equals(DEFAULT_LOCALE)) {
            propertiesFileName =
                propertiesFileName.substring(0, propertiesFileName.indexOf(".properties")) + "_" + locale + ".properties";
        }
        return propertiesFileName;
    }

    /**
     * Reads a source properties file, and adds all its labels.
     * @param fileName the properties file name to load
     * @param functionNames a <code>List</code> of label names
     * @param gwtLabelDefinitionsMap a <code>Map</code> of label names to <code>GwtLabelDefinition</code>
     * @param locale the locale that properties file use
     * @exception IOException if an error occurs
     * @exception FileNotFoundException if an error occurs
     */
    private static void readLabelsFromSourcePropertiesFile(String fileName, String locale, Map<String, GwtLabelDefinition> gwtLabelDefinitionsMap) throws IOException, FileNotFoundException {
        // load the file
        Properties properties = null;
        String propertiesFileName = null;
        if (fileName.endsWith(".xml")) {
            // read labels from xml file
            properties = UtilProperties.xmlToProperties(new FileInputStream(fileName), new Locale(locale), null);
            propertiesFileName = fileName;
        } else {
            properties = loadPropertiesFile(fileName, locale);
            propertiesFileName = getPropertiesFileName(fileName, locale);
        }
        if (properties == null) {
            Debug.logError("Null properties for fileName [" + fileName + "] and locale [" + locale + "]", MODULE);
            return;
        }


        // loop over each labels from the loaded file
        Set<Entry<Object, Object>> entries = properties.entrySet();
        for (Entry<Object, Object> entry : entries) {

            try {
                String originKey = (String) entry.getKey();
                String labelKey = (String) entry.getKey();
                String labelText = (String) entry.getValue();

                // Note on GWT formatted labels:
                //  since some labels may need to in GWT format already, we distinguish them from other label in the properties file by
                //  having their key starts with an underscore.
                //  GWT formatted labels may contain indexed placeholder in quoted string for which the parameters must not appear
                //  in the message interface as they are substituted dynamically in the widgets code.
                //  they do not need to be sanitized and can be imported as-is.
                //  for those label we only check normal non-quoted GWT placeholders in order to generate the corresponding message interface parameters
                boolean isGwtFormatted = false;

                // check if the key starts with an underscore and we consider it as already GWT formatted
                if (labelKey.startsWith("_")) {
                    labelKey = labelKey.substring(1);
                    isGwtFormatted = true;
                } else {
                    labelKey = makeValidLabelKey(labelKey);
                }

                // if a label with the same key was already loaded from a different file
                //  just log the duplicate
                if (gwtLabelDefinitionsMap.containsKey(labelKey)) {
                    Debug.logError("Found duplicate label key [" + labelKey + "] in " + propertiesFileName, MODULE);
                    Debug.logError("key [" + labelKey + "] already defined in " + gwtLabelDefinitionsMap.get(labelKey).getPropertiesFile(), MODULE);
                    continue;
                }

                // save the list of parameters that were parsed from the label text
                // and that will be used as the message interface method parameters
                List<String> parameters = new ArrayList<String>();

                // keep track of the first numeric index available
                // this is incremented if we find indexed parameters in the label text so that they cannot conflict with FreeMarker
                // placeholders when they get indexed
                int indexStart = 0;

                if (isGwtFormatted) {
                    // loop through the un-quoted part of the label text
                    String[] quotedTexts = labelText.split("'");
                    for (int i = 0; i < quotedTexts.length; i += 2) {
                        // find each indexed placeholder the text string
                        indexStart += parseIndexedPlaceholders(quotedTexts[i], indexedPlaceholderPattern, parameters);
                    }
                } else {
                    labelText = formatGwtLabel(labelText, parameters);
                }

                // trim the label text
                labelText = labelText.trim();

                // loop through the found placeholders and make the corresponding GwtLabelDefinition objects
                if (parameters.size() > 0) {
                    gwtLabelDefinitionsMap.put(labelKey, new GwtLabelDefinition(propertiesFileName, labelKey, originKey, labelText, parameters));
                } else {
                    gwtLabelDefinitionsMap.put(labelKey, new GwtLabelDefinition(propertiesFileName, labelKey, originKey, labelText));
                }

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Format gwt label function
     * @param origin a <code>String</code> value
     * @return formatted gwt label
     */    
    private static String formatGwtLabel(String origin, List<String> parameters) {
        String labelText = origin;

        // keep track of the first numeric index available
        // this is incremented if we find indexed parameters in the label text so that they cannot conflict with FreeMarker
        // placeholders when they get indexed
        int indexStart = 0;
        // find each indexed placeholder the text string
        indexStart += parseIndexedPlaceholders(labelText, indexedPlaceholderPattern, parameters);

        // find empty freemarker placeholders (actually used in very few labels) and index them
        while (labelText.contains("${}")) {
            labelText = labelText.replaceFirst("\\$\\{\\}", "{" + indexStart + "}");
            indexStart++;
        }

        // save the list of FreeMarker placeholders strings
        // we will replace them by indexed parameters
        List<String> vars = new ArrayList<String>();

        // find each FreeMarker placeholder in the text string
        Matcher matchFtlPlaceholder = ftlPlaceholderPattern.matcher(labelText);
        while (matchFtlPlaceholder.find()) {
            // get the placeholder from the regex
            // eg: ${firstName}
            String placeholder = matchFtlPlaceholder.group(); // ${firstName}
            String placeholderName = matchFtlPlaceholder.group(1); // firstName

            // check the raw placeholder content against what we already parsed
            if (vars.contains(placeholder)) {
                continue;
            }

            // sanitize the placeholderName so that it can be used as a Java variable name
            //  handle special FTL like amountApplied?currency(${isoCode})
            Matcher matchFtlInnerPlaceholder = ftlPlaceholderPattern.matcher(placeholderName);
            if (matchFtlInnerPlaceholder.find()) {
                placeholderName = matchFtlInnerPlaceholder.group(1);
            }

            // in case of two level recursions, or special FTL construct that we did not handle
            // we need to cut the string at the first ? char (used in FTL to check the variable, like in ${firstName?default("Joe")}
            int idx = placeholderName.indexOf("?");
            if (idx > 0) {
                placeholderName = placeholderName.substring(0, idx);
            }

            // some FTL constructs use ${variable()}
            idx = placeholderName.indexOf("(");
            if (idx > 0) {
                placeholderName = placeholderName.substring(0, idx);
            }

            // remove characters not allowed in Java but used in FTL to access object attributes
            // eg: The order ${order.orderId} has been shipped.
            placeholderName = placeholderName.replace('.', '_');

            // check that we did not accidentally make a duplicate
            // else rename it to placeholderName + index
            // eg: firstName1, firstName2 ...
            if (parameters.contains(placeholderName)) {
                String placeholderNameOrig = placeholderName;
                int i = 1;
                while (parameters.contains(placeholderName)) {
                    placeholderName = placeholderNameOrig + i;
                    i++;
                }
            }

            // add the placeholder to the list of parameters for the message interface
            vars.add(placeholder);
            parameters.add(placeholderName);
        }

        // replace the FTL placeholders by GWT placeholders, note that this supports the case where
        //  the same placeholder is used multiple times
        // eg: Welcome ${firstName} ${lastName} ${firstName}  => Welcome {0} {1} {0}
        for (int i = 0; i < vars.size(); i++) {
            labelText = labelText.replace(vars.get(i), "{" + i + "}");
        }

        // find remaining fake placeholders in the text string
        Matcher matchFakePlaceholder = fakePlaceholderPattern.matcher(labelText);
        vars.clear();
        while (matchFakePlaceholder.find()) {
            String placeholder = matchFakePlaceholder.group();

            if (vars.contains(placeholder)) {
                continue;
            }

            vars.add(placeholder);
        }

        // alter fake placeholders
        // eg: {some text} => [some text]
        for (String s : vars) {
            labelText = labelText.replace(s, "[" + s.substring(1, s.length() - 1) + "]");
        }

        // sanitize white space chars like \n \r \t ...
        labelText = labelText.replace('\n', ' ');
        labelText = labelText.replace('\t', ' ');
        labelText = labelText.replace("\r", "");

        // in GWT labels quotes need to be doubled, quoted text has a special meaning.
        labelText = labelText.replace("'", "''");
    
        return labelText.trim();
    }

    private static String makeValidLabelKey(String labelKey) {
        // enforce a convention that the first character of the label key is lower case
        labelKey = labelKey.substring(0, 1).toLowerCase() + labelKey.substring(1);

        // convert characters that are invalid in Java into underscores so that we can use the label key as a Java method name properly
        labelKey = labelKey.replace('.', '_');
        labelKey = labelKey.replace('-', '_');
        labelKey = labelKey.replace('+', '_');
        labelKey = labelKey.replace('/', '_');
        labelKey = labelKey.replace(' ', '_');

        return labelKey;
    }

    private static int parseIndexedPlaceholders(String labelText, Pattern indexedPlaceholderPattern, List<String> parameters) {
        int counter = 0;
        Matcher matchIndexedPlaceholder = indexedPlaceholderPattern.matcher(labelText);
        while (matchIndexedPlaceholder.find()) {

            String placeholder = matchIndexedPlaceholder.group(1); // get the index: 0, 1, ...
            String placeholderName = "param_" + placeholder; // give it a name to be used as the interface method parameter

            // check that it was not already parsed
            if (parameters.contains(placeholderName)) {
                continue;
            }

            // add the placeholder to the list of parameters for the message interface
            parameters.add(placeholderName);
            // increment the counter of indexed parameter
            counter++;
        }
        return counter;
    }

}
