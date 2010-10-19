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
package org.opentaps.common.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
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

import org.hibernate.SessionFactory;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * HibernateContainer - Container implementation for loading the hibernate <code>SessionFactory</code>.
 *
 * This container generates the hibernates configuration files for all the defined data sources from framework/entity/config/entityengine.xml
 * and then loads the <code>SessionFactory</code> instance for the delegator configured in <code>framework/base/config/ofbiz-containers.xml</code>.
 *
 */
public class HibernateContainer implements Container {

    private static final String MODULE = HibernateContainer.class.getName();
    private String delegatorName;

    /**
     * hibernate session factory instance.
     */
    private SessionFactory sessionFactory = null;

    /**
     * Initialize the container.
     *
     * @param args       args from calling class
     * @param configFile Location of configuration file
     * @throws org.ofbiz.base.container.ContainerException if an error occurs
     *
     */
    public void init(String[] args, String configFile) throws ContainerException {
        ContainerConfig.Container cc = ContainerConfig.getContainer("hibernate-container", configFile);
        if (cc == null) {
            throw new ContainerException("No hibernate-container configuration found in container config, please check framework/base/config/ofbiz-containers.xml.");
        }
        // retrieve delegator name
        delegatorName = ContainerConfig.getPropertyValue(cc, "delegator-name", "default");
    }

    /**
     * This method is always run when ofbiz loads the container.  It will generate a hibernate cfg.xml for each
     * data source defined in the entity engine XML file and then initiate the hibernate session factory for the delegator
     * defined in ofbiz-containers.xml's for the hibernate-container Container.  The hibernate.cfg.xml will be used
     * as a template (see generateHibernateCfg()).
     *
     * @return true if server started
     * @throws org.ofbiz.base.container.ContainerException if an error occurs
     *
     */
    public boolean start() throws ContainerException {
        try {
            // generate hibernate config file for each data source
            generateHibernateCfg();
        } catch (ParserConfigurationException e) {
            Debug.logInfo(e, MODULE);
        } catch (SAXException e) {
            Debug.logInfo(e, MODULE);
        } catch (IOException e) {
            Debug.logInfo(e, MODULE);
        } catch (TransformerFactoryConfigurationError e) {
            Debug.logInfo(e, MODULE);
        } catch (TransformerException e) {
            Debug.logInfo(e, MODULE);
        }
        // initial sessionFactory
        sessionFactory = Infrastructure.getSessionFactory(delegatorName);
        if (sessionFactory == null) {
            Debug.logError("Cannot create Hibernate Session Factory for [" + delegatorName + "] delegator", MODULE);
            return false;
        }
        Debug.logInfo("Sucessfully Created Hibernate Session Factory for [" + delegatorName + "] delegator", MODULE);
        return true;
    }

    /**
     * Stops the container and closes the <code>SessionFactory</code>.
     * @throws org.ofbiz.base.container.ContainerException  if an error occurs
     */
    public void stop() throws ContainerException {
        // close the sessionFactory
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

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
    @SuppressWarnings("unchecked")
    public static void generateHibernateCfg() throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {

        // get the current configuration as defined in framework/entity/config/entityengine.xml
        // and create the configuration for each defined datasource
        Iterator iter = EntityConfigUtil.getDatasourceInfos().entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry<String, DatasourceInfo> entry = (Map.Entry<String, DatasourceInfo>) iter.next();
            DatasourceInfo datasourceInfo = entry.getValue();

            // get jdbc parameters from datasourceInfo
            String fieldTypeName = datasourceInfo.fieldTypeName;
            String dialect = Infrastructure.DIALECTS.get(fieldTypeName);
            String indexBase = (new File(Infrastructure.HIBERNATE_SEARCH_INDEX_PATH)).getAbsolutePath();

            // new DocumentBuilderFactory instance
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // use hibernate.cfg.xml as template
            Document document = builder.parse(new File(Infrastructure.HIBERNATE_COMMON_PATH));
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
                String targetFile = Infrastructure.HIBERNATE_CFG_PATH + datasourceInfo.name + Infrastructure.HIBERNATE_CFG_EXT;
                Debug.logInfo("Building hibernate configuration: " + targetFile, MODULE);
                StreamResult result = new StreamResult(new FileOutputStream(new File(targetFile)));
                trans.transform(new DOMSource(document), result);
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }
    }
}
