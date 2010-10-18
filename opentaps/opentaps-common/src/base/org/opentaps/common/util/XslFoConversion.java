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
package org.opentaps.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.w3c.dom.Document;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XslFoConversion {

    private static final String MODULE = XslFoConversion.class.getName();

    public Document convertHtml2XslFo(String html, String stylesheetLocation) {
        String xml = convertHtml2Xhtml(html);
        return convertXml2XslFo(xml, stylesheetLocation);
    }

    private String convertHtml2Xhtml(String html) {
        String xhtml = "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes("UTF-8"));
            Tidy tidy = new Tidy();
            tidy.setXmlOut(true);
            tidy.setXHTML(true);
            tidy.setMakeClean(true);
            tidy.setTidyMark(false);
            tidy.setUpperCaseTags(false);
            tidy.setUpperCaseAttrs(false);
            tidy.setQuoteAmpersand(false);
            tidy.setNumEntities(true);
            tidy.setCharEncoding(Configuration.UTF8);
            tidy.parse(bais, baos);
            xhtml = baos.toString();
            baos.close();
        } catch (Exception ex) {
            Debug.logInfo(ex, MODULE);
        } finally {
            try {
                baos.close();
            } catch (Exception e) {
                Debug.logInfo(e, MODULE);
            }
        }
        return xhtml;
    }

    private Document convertXml2XslFo(String xml, String stylesheetLocation) {
        Document xslfoDocument = null;
        try {
            InputSource insource = new InputSource(new StringReader(xml));
            Document document = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                document = builder.parse(insource);
            } catch (IOException e) {
                Debug.logError(e, "Error while parsing the document.", MODULE);
                String dtd = e.getMessage();
                // second try
                Debug.logWarning("Retrying without DTD validation ...", MODULE);
                insource = new InputSource(new StringReader(xml));
                factory = DocumentBuilderFactory.newInstance();
                builder = factory.newDocumentBuilder();
                builder.setEntityResolver(new EntityResolver() {
                        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                            return new InputSource(new StringReader(""));
                        }
                    });
                document = builder.parse(insource);
                Debug.logWarning("Successfully parsed the document without DTD validation. Please check that you can access: " + dtd + " as it is needed for DTD validation.", MODULE);
            }

            DOMSource xmlDomSource = new DOMSource(document);
            DOMResult domResult = new DOMResult();

            Transformer transformer = getTransformer(stylesheetLocation);

            if (UtilValidate.isEmpty(transformer)) {
                Debug.logInfo("Error creating transformer for " + stylesheetLocation, MODULE);
            }
            transformer.transform(xmlDomSource, domResult);

            xslfoDocument = (Document) domResult.getNode();

        } catch (Exception e) {
            Debug.logInfo(e, MODULE);
        }
        return xslfoDocument;
    }

    private static Transformer getTransformer(String styleSheetLocation) {

        try {

            URL styleSheetURL = FlexibleLocation.resolveLocation(styleSheetLocation);
            if (UtilValidate.isEmpty(styleSheetURL)) {
                throw new IllegalArgumentException("Stylesheet file not found at location: " + styleSheetLocation);
            }
            File stylesheetFile = new File(styleSheetURL.toURI());

            TransformerFactory tFactory = TransformerFactory.newInstance();
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            dFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
            Document xslDoc = dBuilder.parse(stylesheetFile);
            DOMSource xslDomSource = new DOMSource(xslDoc);

            return tFactory.newTransformer(xslDomSource);
        } catch (Exception e) {
            Debug.logInfo(e, MODULE);
            return null;
        }
    }
}
