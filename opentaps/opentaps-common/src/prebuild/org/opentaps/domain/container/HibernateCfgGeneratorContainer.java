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

/* This file has been modified by Open Source Strategies, Inc. */
package org.opentaps.domain.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.service.ServiceDispatcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Some utility routines for loading seed data.
 */
public class HibernateCfgGeneratorContainer implements Container {

    private static final String MODULE = HibernateCfgGeneratorContainer.class.getName();
    // hibernate configuration file store path, It also define in Infrastructure, but we cannot access it in this class.
    private final static String HIBERNATE_CFG_PATH = "opentaps/opentaps-common/config/";
    // hibernate configuration template path
    private final static String HIBERNATE_COMMON_PATH = "opentaps/opentaps-common/config/hibernate.cfg.xml";
    // hibernate configuration template path
    private final static String HIBERNATE_SEARCH_INDEX_PATH = "runtime/lucene/indexes";
    // hibernate configuration file ext
    private final static String HIBERNATE_CFG_EXT = ".cfg.xml";
    // hibernate dialects maps
    private static final HashMap<String, String> DIALECTS = new HashMap<String, String>();
    static {
        DIALECTS.put("hsql", "org.hibernate.dialect.HSQLDialect");
        DIALECTS.put("derby", "org.hibernate.dialect.DerbyDialect");
        DIALECTS.put("mysql", "org.opentaps.foundation.entity.hibernate.OpentapsMySQLDialect");
        DIALECTS.put("postgres", "org.hibernate.dialect.PostgreSQLDialect");
        DIALECTS.put("postnew", "org.hibernate.dialect.PostgreSQLDialect");
        DIALECTS.put("oracle", "org.hibernate.dialect.OracleDialect");
        DIALECTS.put("sapdb", "org.hibernate.dialect.SAPDBDialect");
        DIALECTS.put("sybase", "org.hibernate.dialect.SybaseDialect");
        DIALECTS.put("firebird", "org.hibernate.dialect.FirebirdDialect");
        DIALECTS.put("mssql", "org.hibernate.dialect.SQLServerDialect");

        DIALECTS.put("cloudscape", ""); //not exist mapping Dialect
        DIALECTS.put("daffodil", "");   //not exist mapping Dialect
        DIALECTS.put("axion", "");      //not exist mapping Dialect
        DIALECTS.put("advantage", "");  //not exist mapping Dialect
    }

    /** Config file. */
    @SuppressWarnings("unused")
    private String configFile = null;

    /**
     * Creates a new <code>PojoGeneratorContainer</code> instance.
     */
    public HibernateCfgGeneratorContainer() {
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
    public boolean start() throws ContainerException {
        try {
            File hibernateFile = new File(HIBERNATE_COMMON_PATH);
            if (!hibernateFile.exists()) {
                throw new ContainerException("Cannot find hibernate configuration template [" + HIBERNATE_COMMON_PATH + "], please run make-base-entities first.");
            }
            generateHibernateCfg();
        } catch (IOException e) {
            Debug.logError(e, "Aborting, error writing hibernate configuration files.", MODULE);
        } catch (ParserConfigurationException e) {
            Debug.logError(e, "Aborting, occur error on generate hibernate configuration for datasource.", MODULE);
        } catch (SAXException e) {
            Debug.logError(e, "Aborting, occur error on generate hibernate configuration for datasource.", MODULE);
        } catch (TransformerFactoryConfigurationError e) {
            Debug.logError(e, "Aborting, occur error on generate hibernate configuration for datasource.", MODULE);
        } catch (TransformerException e) {
            Debug.logError(e, "Aborting, occur error on generate hibernate configuration for datasource.", MODULE);
        }
        return true;
    }

    /** {@inheritDoc} */
    public void stop() throws ContainerException { }

    /**
     * Generates the hibernate configuration file for each data source using the <code>hibernate.cfg.xml</code> as a template
     * and inserts the database connection settings from <code>entityengine.xml</code>.
     *
     * @throws ParserConfigurationException if an error occurs
     * @throws IOException if an error occurs
     * @throws SAXException if an error occurs
     * @throws TransformerFactoryConfigurationError if an error occurs
     * @throws TransformerException if an error occurs
     *
     */
    public static void generateHibernateCfg() throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {

        // get the current configuration as defined in framework/entity/config/entityengine.xml
        // and create the configuration for each defined datasource
        for (Map.Entry<String, DatasourceInfo> entry : EntityConfigUtil.getDatasourceInfos().entrySet()) {

            DatasourceInfo datasourceInfo = entry.getValue();

            // get jdbc parameters from datasourceInfo
            String fieldTypeName = datasourceInfo.fieldTypeName;
            String dialect = DIALECTS.get(fieldTypeName);
            String indexBase = (new File(HIBERNATE_SEARCH_INDEX_PATH)).getAbsolutePath();

            // new DocumentBuilderFactory instance
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // cancel validate dtd on parse xml
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            // use hibernate.cfg.xml as template
            Document document = builder.parse(new File(HIBERNATE_COMMON_PATH));
            Element rootElement = document.getDocumentElement();
            Node sessionFactoryElement = rootElement.getElementsByTagName("session-factory").item(0);
            Node firstChildElement = sessionFactoryElement.getFirstChild();

            // insert dialect node
            Element helperNameElement = document.createElement("property");
            sessionFactoryElement.insertBefore(helperNameElement, firstChildElement);
            helperNameElement.setAttribute("name", "ofbiz.helperName");
            helperNameElement.setTextContent(datasourceInfo.name);

            // insert dialect node
            Element dialectElement = document.createElement("property");
            sessionFactoryElement.insertBefore(dialectElement, firstChildElement);
            dialectElement.setAttribute("name", "dialect");
            dialectElement.setTextContent(dialect);

            // insert hibernate search indexBase node
            Element indexBaseElement = document.createElement("property");
            sessionFactoryElement.insertBefore(indexBaseElement, firstChildElement);
            indexBaseElement.setAttribute("name", "hibernate.search.default.indexBase");
            indexBaseElement.setTextContent(indexBase);

            // save the configuration file, use datasource as filename, such as localpostgres.cfg.xml
            // NOTICE: proper doctype is required else hibernate would return "Document is invalid: no grammar found".
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Hibernate/Hibernate Configuration DTD 3.0//EN");
            trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            // write the resulting configuration file
            try {
                String targetFile = HIBERNATE_CFG_PATH + datasourceInfo.name + HIBERNATE_CFG_EXT;
                Debug.logInfo("Building hibernate configuration: " + targetFile, MODULE);
                StreamResult result = new StreamResult(new FileOutputStream(new File(targetFile)));
                trans.transform(new DOMSource(document), result);
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }
    }
}
