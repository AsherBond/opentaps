/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.foundation.infrastructure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;

import org.hibernate.event.DeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.foundation.entity.hibernate.EcaDeleteEventListener;
import org.opentaps.foundation.entity.hibernate.EcaLoadEventListener;
import org.opentaps.foundation.entity.hibernate.EcaPersistEventListener;
import org.opentaps.foundation.entity.hibernate.EcaSaveEventListener;
import org.opentaps.foundation.entity.hibernate.EcaSaveOrUpdateEventListener;
import org.opentaps.foundation.entity.hibernate.Session;


/**
 * These are the fundamental infrastructure Resources used by opentaps, including the ofbiz dispatcher, delegator,
 * security objects and Hibernate sessions for each delegator.  Typically, all the OFBiz business tier
 * infrastructure can be obtained from their GenericDispatcher.
 *
 */
public class Infrastructure {

    private static final String MODULE = Infrastructure.class.getName();

    protected LocalDispatcher dispatcher = null;
    protected GenericDelegator delegator = null;
    protected Security security = null;
    protected GenericValue systemUserLogin = null;  // Sometimes, the system user must be used by the Factory or Repository that use the ofbiz Infrastructure
    // create this map for store all of session factory
    private static Map<String, SessionFactory> sessionFactories = FastMap.newInstance();
    // hibernate dialets maps
    public static final HashMap<String, String> DIALECTS = new HashMap<String, String>();
    private static final String HELPER_NAME = "org.ofbiz";
    static {
        DIALECTS.put("hsql", "org.hibernate.dialect.HSQLDialect");
        DIALECTS.put("derby", "org.hibernate.dialect.DerbyDialect");
        DIALECTS.put("mysql", "org.opentaps.foundation.entity.hibernate.OpentapsMySQLDialect");
        DIALECTS.put("postgres", "org.hibernate.dialect.PostgreSQLDialect");
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
    /** Hibernate configuration file store path. */
    public final static String HIBERNATE_CFG_PATH = "hot-deploy/opentaps-common/config/";
    /** Hibernate configuration template path. */
    public final static String HIBERNATE_COMMON_PATH = "hot-deploy/opentaps-common/config/hibernate.cfg.xml";
    /** Hibernate configuration template path. */
    public final static String HIBERNATE_SEARCH_INDEX_PATH = "runtime/lucene/indexes";
    /** Hibernate configuration file ext. */
    public final static String HIBERNATE_CFG_EXT = ".cfg.xml";

    /**
     * Gets the Hibernate <code>SessionFactory</code> object for the corresponding delegator.
     * A Map of delegatorName to SessionFactory is maintained, and the first time a SessionFactory is requested for.
     * A delegatorName, it is created in the the following way:
     * <ul>
     * <li>First, get the data source for the group helper (ie, "org.ofbiz" in entitygroup.xml) and that delegator</li>
     * <li>Then, get the hibernate cfg xml for the data source</li>
     * <li>Finally, use that hibernate cfg xml to create a SessionFactory</li>
     * </ul>
     * @param delegatorName a <code>String</code> value
     * @return a Hibernate <code>SessionFactory</code> value
     */
    @SuppressWarnings("unchecked")
    public static synchronized SessionFactory getSessionFactory(String delegatorName) {
        SessionFactory sessionFactory = sessionFactories.get(delegatorName);
        // the sessionFactory haven't init before
        if (sessionFactory == null) {
            Debug.logVerbose("building hibernate SessionFactory ...", MODULE);
            AnnotationConfiguration annotationConfiguration = new AnnotationConfiguration();
            GenericDelegator delegator = GenericDelegator.getGenericDelegator(delegatorName);
            //for support eccas, construct persist event listener arrays
            PersistEventListener[] persistEventListeners = {new EcaPersistEventListener(delegator)};
            //for support eccas, construct load event listener arrays
            LoadEventListener[] loadEventListeners = {new EcaLoadEventListener(delegator)};
            //for support eccas, construct delete event listener arrays
            DeleteEventListener[] deleteEventListeners = {new EcaDeleteEventListener(delegator)};
            //for support eccas, construct saveOrUpdate event listener arrays
            SaveOrUpdateEventListener[] saveOrUpdateEventListeners = {new EcaSaveOrUpdateEventListener(delegator)};
            //for support eccas, construct saveOrUpdate event listener arrays
            SaveOrUpdateEventListener[] saveEventListeners = {new EcaSaveEventListener(delegator)};
            //register our event listener arrays
            annotationConfiguration.getEventListeners().setPersistEventListeners(persistEventListeners);
            annotationConfiguration.getEventListeners().setSaveOrUpdateEventListeners(saveOrUpdateEventListeners);
            annotationConfiguration.getEventListeners().setDeleteEventListeners(deleteEventListeners);
            annotationConfiguration.getEventListeners().setLoadEventListeners(loadEventListeners);
            annotationConfiguration.getEventListeners().setSaveEventListeners(saveEventListeners);
            annotationConfiguration.getEventListeners().setUpdateEventListeners(saveOrUpdateEventListeners);
            //get groupHelpName for retrieve database connection information.
            String groupHelperName = (String) EntityConfigUtil.getDelegatorInfo(delegatorName).groupMap.get(getHelperName());
            Debug.logVerbose("groupHelperName : " + groupHelperName, MODULE);
            //get database source information
            DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(groupHelperName);
            Debug.logVerbose("datasourceInfo.fieldTypeName : " + datasourceInfo.fieldTypeName, MODULE);
            //get hibernate dialect by database type
            annotationConfiguration.setProperty("hibernate.dialect", DIALECTS.get(datasourceInfo.fieldTypeName));
            Debug.logVerbose("configuring SessionFactory ...", MODULE);
            //build a sessionFactory
            String datasourceName = (String) EntityConfigUtil.getDelegatorInfo(delegatorName).groupMap.get(getHelperName());
            Debug.logVerbose("init sessionFactory by datasoure " + datasourceName, MODULE);
            sessionFactory = annotationConfiguration.configure(HIBERNATE_CFG_PATH + datasourceName + HIBERNATE_CFG_EXT).buildSessionFactory();
            Debug.logVerbose("listing loaded entities ...", MODULE);
            Map metadata = sessionFactory.getAllClassMetadata();
            //iterator all classess which are success load of hibernate, it just only for debug use.
            for (Iterator i = metadata.values().iterator(); i.hasNext();) {
                EntityPersister persister = (EntityPersister) i.next();
                String className = persister.getClassMetadata().getEntityName();
                Debug.logVerbose("SessionFactory Successfully Loaded AnnotatedClass : " + className, MODULE);
            }
            sessionFactories.put(delegatorName, sessionFactory);
        }
        return sessionFactory;
    }

    /**
     * Gets the Hibernate <code>Session</code> object, it return an open session from the getSessionFactory
     * using the delegator already in this Infrastructure object.
     * @return a Hibernate <code>Session</code> value
     * @throws InfrastructureException if an error occurs
     */
    public Session getSession() throws InfrastructureException {
        org.hibernate.Session hibernateSession = getSessionFactory(delegator.getDelegatorName()).openSession();
        Session session = new Session(hibernateSession, delegator);
        return session;
    }
    /**
     * Gets the helper name, which is used to map a datasource to a set of entities in the ofbiz entity engine.
     * Currently this just returns the static HELPER_NAME defined in the class,
     * but it's here in case later we need more sophisticated ways to get the helper name.
     * @return a <code>String</code> value
     */
    public static String getHelperName() {
        return HELPER_NAME;
    }

    /**
     * Constructor.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @exception InfrastructureException if an error occurs
     */
    public Infrastructure(LocalDispatcher dispatcher) throws InfrastructureException {
        if (dispatcher == null) {
            throw new InfrastructureException("Cannot instantiate Infrastructure from null dispatcher");
        }
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.security = dispatcher.getSecurity();
        this.loadSystemUserLogin();
    }

    /**
     * Gets the <code>Security</code> object.
     * @return a <code>Security</code> value
     */
    public Security getSecurity() {
        return this.security;
    }

    /**
     * Gets the <code>LocalDispatcher</code> object.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * Gets the <code>GenericDelegator</code> object.
     * @return a <code>GenericDelegator</code> value
     */
    public GenericDelegator getDelegator() {
        return this.delegator;
    }

    /**
     * Loads the system user login for Repositories and Factories that use this Infrastructure, plus all their sub-classes.
     * @throws InfrastructureException if an error occurs
     */
    private void loadSystemUserLogin() throws InfrastructureException {
        try {
            this.systemUserLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
            throw new InfrastructureException(e);
        }
    }

    /**
     * Gets the system UserLogin for Factories and Repositories that need them.
     * @return the system <code>UserLogin</code> <code>GenericValue</code>
     */
    public GenericValue getSystemUserLogin() {
        return this.systemUserLogin;
    }

}
