/*
 * Copyright (c) 2008 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.analytics.gwt.query.server;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opentaps.analytics.gwt.query.client.ConditionDef;
import org.opentaps.analytics.gwt.query.client.QueryBuilderService;
import org.opentaps.analytics.gwt.query.client.ValueDef;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * RPC service to read the configured filters available for each dimension and autocomplete values.
 */
public class QueryBuilderServiceImpl extends RemoteServiceServlet implements QueryBuilderService {

    private static final long serialVersionUID = -1551396794796995954L;
    private static final String CONF_FILE = "query-builder.xml";
    private static final String dataSourceName = "analytics";

    /**
     * Load configuration file from classpath and parse it to document.
     * @return XML document instance
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private Document readXmlDocument() throws ParserConfigurationException, SAXException, IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(CONF_FILE);
        if (url == null) {
            return null;
        }

        Document doc = null;
        DocumentBuilderFactory factory = new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(url.openStream());

        return doc;
    }

    private DataSource getDatasourceFromJndi() throws NamingException {
        InitialContext ctxt = new InitialContext();
        DataSource ds = null;
        try {
            ds = (DataSource) ctxt.lookup(dataSourceName);
            if (ds != null) {
                return ds;
            }
        } catch (NamingException ignored) {
        }
        try {
            ds = (DataSource) ctxt.lookup("java:" + dataSourceName);
            if (ds != null) {
                return ds;
            }
        } catch (NamingException ignored) {
        }
        try {
            ds = (DataSource) ctxt.lookup("java:comp/env/jdbc/" + dataSourceName);
            if (ds != null) {
                return ds;
            }
        } catch (NamingException ignored) {
        }
        try {
            ds = (DataSource) ctxt.lookup("jdbc/" + dataSourceName);
            if (ds != null) {
                return ds;
            }
        } catch (NamingException ignored) {
        }
        return ds;
    }

    /** {@inheritDoc} */
    public List<ConditionDef> getAvailableConditions(String report) {
        List<ConditionDef> result = new ArrayList<ConditionDef>();
        Map<String, Map<String, List<String>>> filter = new HashMap<String, Map<String, List<String>>>();

        try {
            //TODO: session locale should be used
            ResourceBundle bundle = ResourceBundle.getBundle("org.opentaps.analytics.locale.messages", Locale.getDefault());

            Document document = readXmlDocument();

            // looks if there are any report filters
            NodeList reports = document.getElementsByTagName("report");
            if (reports != null && reports.getLength() > 0) {
                int reportsSize = reports.getLength();
                for (int i = 0; i < reportsSize; i++) {
                    Node reportNode = reports.item(i);
                    if ("report".equals(reportNode.getNodeName()) && reportNode.hasChildNodes()) {
                        String reportName = reportNode.getAttributes().getNamedItem("name").getNodeValue();
                        NodeList parameters = reportNode.getChildNodes();
                        int parametersSize = parameters.getLength();
                        for (int n = 0; n < parametersSize; n++) {
                            Node parametersNode = parameters.item(n);
                            if ("parameters".equals(parametersNode.getNodeName())) {
                                NamedNodeMap parametersAttr = parametersNode.getAttributes();
                                Node dimensionFilterItem = parametersAttr.getNamedItem("dimension");
                                if (dimensionFilterItem == null) {
                                    continue;
                                }
                                List<String> includeConditionList = new ArrayList<String>();
                                if (parametersNode.hasChildNodes()) {
                                    NodeList inclusions = parametersNode.getChildNodes();
                                    int incSize = inclusions.getLength();
                                    for (int m = 0; m < incSize; m++) {
                                        Node inclusionNode = inclusions.item(m);
                                        if ("include-condition".equals(inclusionNode.getNodeName())) {
                                            NamedNodeMap inclusionsAttr = inclusionNode.getAttributes();
                                            Node conditionNode = inclusionsAttr.getNamedItem("id");
                                            if (conditionNode != null) {
                                                includeConditionList.add(conditionNode.getNodeValue());
                                            }
                                        }
                                    }
                                }
                                Map<String, List<String>> reportFilter = new HashMap<String, List<String>>();
                                reportFilter.put(dimensionFilterItem.getNodeValue(), includeConditionList);
                                filter.put(reportName, reportFilter);
                            }
                        }
                    }
                }
            }

            NodeList dimensions = document.getElementsByTagName("dimension");
            if (dimensions == null || dimensions.getLength() == 0) {
                return result;
            }

            int size = dimensions.getLength();
            for (int i = 0; i < size; i++) {

                // dimension attributes
                String name = null;

                Node dimension = dimensions.item(i);
                if (dimension != null) {
                    NamedNodeMap dimensionAttributes = dimension.getAttributes();
                    name = label(bundle, dimensionAttributes.getNamedItem("name").getNodeValue());

                    if (!dimension.hasChildNodes()) {
                        continue;
                    }

                    //retrieve child nodes
                    NodeList conditions = dimension.getChildNodes();
                    int csize = conditions.getLength();
                    for (int n = 0; n < csize; n++) {
                        Node condition = conditions.item(n);
                        if ("condition".equals(condition.getNodeName())) {
                            NamedNodeMap attributes = condition.getAttributes();
                            ConditionDef def = new ConditionDef();
                            String attrValue = null;
                            Node nodeItem = attributes.getNamedItem("id");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setId(attrValue != null ? attrValue : "Error! Condition ID undefined.");
                            }
                            nodeItem = attributes.getNamedItem("selected");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setSelected(attrValue != null ? Boolean.valueOf(attrValue) : Boolean.FALSE);
                            }
                            nodeItem = attributes.getNamedItem("fieldName");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setFieldName(attrValue != null ? attrValue : "Error! Field name undefined.");
                            }
                            nodeItem = attributes.getNamedItem("label");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setLabel(attrValue != null ? label(bundle, attrValue) : "Error! Label undefined.");
                            }
                            nodeItem = attributes.getNamedItem("value");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setValue(attrValue != null ? attrValue : "");
                            }
                            nodeItem = attributes.getNamedItem("javaType");
                            if (nodeItem != null) {
                                attrValue = nodeItem.getNodeValue();
                                def.setJavaType(attrValue != null ? attrValue : "Error! Java type undefined.");
                            }
                            def.setDimension(name);
                            result.add(def);
                        }
                    }
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /** {@inheritDoc} */
    public List<ValueDef> getConditionValues(String conditionId) {
        List<ValueDef> lst = new ArrayList<ValueDef>();
        DataSource ds = null;
        Connection connection = null;

        try {

            ds = getDatasourceFromJndi();
            connection = ds.getConnection();

            String dimensionTable = null;
            String fieldName = null;

            Document document = readXmlDocument();
            if (document == null) {
                return lst;
            }

            NodeList dimensions = document.getElementsByTagName("dimension");
            if (dimensions == null || dimensions.getLength() == 0) {
                return lst;
            }

            int size = dimensions.getLength();
            for (int i = 0; i < size; i++) {

                Node dimension = dimensions.item(i);
                if (dimension != null) {
                    NamedNodeMap dimensionAttributes = dimension.getAttributes();
                    Node tableNode = dimensionAttributes.getNamedItem("table");
                    if (tableNode == null) {
                        continue;
                    }

                    dimensionTable = tableNode.getNodeValue();

                    if (!dimension.hasChildNodes()) {
                        continue;
                    }

                    NodeList conditions = dimension.getChildNodes();
                    int csize = conditions.getLength();
                    for (int n = 0; n < csize; n++) {
                        Node condition = conditions.item(n);
                        if ("condition".equals(condition.getNodeName())) {
                            NamedNodeMap attributes = condition.getAttributes();
                            Node idNode = attributes.getNamedItem("id");
                            if (conditionId != null && idNode != null && conditionId.equals(idNode.getNodeValue())) {
                                Node fieldNameNode = attributes.getNamedItem("fieldName");
                                if (fieldNameNode == null) {
                                    continue;
                                }

                                fieldName = fieldNameNode.getNodeValue();
                                break;
                            }
                        }
                    }
                }
                if (fieldName != null) {
                    break;
                }
            }

            String sql = String.format("SELECT %1$s FROM %2$s GROUP BY %1$s ORDER BY %1$s", fieldName, dimensionTable);

            Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if ((sql != null && sql.length() > 0) && stmt.execute(sql)) {
                ResultSet rs = stmt.getResultSet();
                while (rs.next()) {
                    String value = rs.getString(1);
                    if (value != null)
                        lst.add(new ValueDef(conditionId, rs.getString(1)));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
		}

        return lst;
    }

    /**
     * Load string labels from external resource.
     *
     * @param bundle
     *  Resource bundle
     * @param str
     *  String that may be either properties key in format %key or final label
     * @return
     *  If specified string starts with % return corresponding string from resource bundle.
     *  Returns unchanged <code>str</code> if error or key isn't found.
     */
    private String label(ResourceBundle bundle, String str) {
        try {
            if (str.startsWith("%") && bundle != null) {
                return bundle.getString(str.substring(1));
            }
        } catch (MissingResourceException e) {
            return str;
        }
        return str;
    }

}
