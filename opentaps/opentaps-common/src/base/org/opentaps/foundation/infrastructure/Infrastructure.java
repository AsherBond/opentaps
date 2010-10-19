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
package org.opentaps.foundation.infrastructure;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.base.entities.OpentapsConfiguration;
import org.opentaps.base.entities.OpentapsConfigurationType;
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

    private LocalDispatcher dispatcher = null;
    private Delegator delegator = null;
    private Security security = null;
    private GenericValue systemUserLogin = null;  // Sometimes, the system user must be used by the Factory or Repository that use the ofbiz Infrastructure
    private User systemUser = null;

    // create this map for store all of session factory
    private static Map<String, SessionFactory> sessionFactories = FastMap.newInstance();
    /** Hibernate dialects maps. */
    public static final HashMap<String, String> DIALECTS = new HashMap<String, String>();
    private static final String HELPER_NAME = "org.ofbiz";
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
    /** Hibernate configuration file store path. */
    public static final String HIBERNATE_CFG_PATH = "opentaps/opentaps-common/config/";
    /** Hibernate configuration template path. */
    public static final String HIBERNATE_COMMON_PATH = "opentaps/opentaps-common/config/hibernate.cfg.xml";
    /** Hibernate configuration template path. */
    public static final String HIBERNATE_SEARCH_INDEX_PATH = "runtime/lucene/indexes";
    /** Hibernate configuration file ext. */
    public static final String HIBERNATE_CFG_EXT = ".cfg.xml";
    /** Hibernate entity package name. */
    public static final String ENTITY_PACKAGE = "org.opentaps.base.entities";

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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static synchronized SessionFactory getSessionFactory(String delegatorName) {
        SessionFactory sessionFactory = sessionFactories.get(delegatorName);
        // the sessionFactory haven't init before
        if (sessionFactory == null) {
            Debug.logVerbose("building hibernate SessionFactory ...", MODULE);
            AnnotationConfiguration annotationConfiguration = new AnnotationConfiguration();
            Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
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
            String groupHelperName = EntityConfigUtil.getDelegatorInfo(delegatorName).groupMap.get(getHelperName());
            Debug.logVerbose("groupHelperName : " + groupHelperName, MODULE);
            //get database source information
            DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(groupHelperName);
            Debug.logVerbose("datasourceInfo.fieldTypeName : " + datasourceInfo.fieldTypeName, MODULE);
            //get hibernate dialect by database type
            String dialect = DIALECTS.get(datasourceInfo.fieldTypeName);
            if (dialect.length() == 0) {
                // some rarely used types are not supported
                Debug.logError("No hibernate dialect defined for the type [" + datasourceInfo.fieldTypeName + "]", MODULE);
                return null;
            }
            annotationConfiguration.setProperty("hibernate.dialect", dialect);
            Debug.logVerbose("configuring SessionFactory ...", MODULE);
            //build a sessionFactory
            String datasourceName = EntityConfigUtil.getDelegatorInfo(delegatorName).groupMap.get(getHelperName());
            Debug.logVerbose("init sessionFactory by datasoure " + datasourceName, MODULE);
            sessionFactory = annotationConfiguration.configure(datasourceName + HIBERNATE_CFG_EXT).buildSessionFactory();
            Debug.logVerbose("listing loaded entities ...", MODULE);
            //iterator all classes which are success load of hibernate, it just only for debug use.
            if (Debug.isOn(Debug.VERBOSE)) {
                Map metadata = sessionFactory.getAllClassMetadata();
                for (Iterator<EntityPersister> i = metadata.values().iterator(); i.hasNext();) {
                    EntityPersister persister = i.next();
                    String className = persister.getClassMetadata().getEntityName();
                    Debug.logVerbose("SessionFactory Successfully Loaded AnnotatedClass : " + className, MODULE);
                }
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
     * @exception IllegalArgumentException if the given dispatcher is <code>null</code>
     */
    public Infrastructure(LocalDispatcher dispatcher) throws IllegalArgumentException {
        if (dispatcher == null) {
            throw new IllegalArgumentException("Cannot instantiate Infrastructure from null dispatcher");
        }
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.security = dispatcher.getSecurity();
    }

    /**
     * Constructor.
     * @param delegator a <code>Delegator</code> value
     * @exception IllegalArgumentException if the given dispatcher is <code>null</code>
     */
    public Infrastructure(Delegator delegator) throws IllegalArgumentException {
        if (delegator == null) {
            throw new IllegalArgumentException("Cannot instantiate Infrastructure from null delegator");
        }
        this.delegator = delegator;
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
     * Gets the <code>Delegator</code> object.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator() {
        return this.delegator;
    }

    /**
     * Loads the system user login for Repositories and Factories that use this Infrastructure, plus all their sub-classes.
     */
    private void loadSystemUserLogin() {
        try {
            this.systemUserLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            if (this.systemUserLogin == null) {
                throw new IllegalStateException("Could not find the [system] UserLogin, it was either not loaded yet or missing.");
            }
            this.systemUser = new User(this.systemUserLogin, delegator);
        } catch (GenericEntityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets the system UserLogin for Factories and Repositories that need them.
     * @return the system <code>UserLogin</code> <code>GenericValue</code>
     */
    public GenericValue getSystemUserLogin() {
        if (systemUserLogin == null) {
            loadSystemUserLogin();
        }
        return systemUserLogin;
    }

    /**
     * Gets the system User for Factories and Repositories that need them.
     * @return the system <code>User</code>
     */
    public User getSystemUser() {
        if (systemUser == null) {
            loadSystemUserLogin();
        }
        return systemUser;
    }

    /**
     * Gets a User object by user's login.
     * @return an instance of the <code>User</code>
     */
    public User getUserFromLogin(String login) {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", login));
            if (userLogin == null) {
                throw new IllegalStateException(String.format("Could not find the [%1$s] UserLogin, it was either not loaded yet or missing.", login));
            }
            return new User(userLogin, delegator);
        } catch (GenericEntityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets a configuration description from the database.
     * @param configTypeId the config type to get
     * @return a <code>String</code> value
     * @exception InfrastructureException if an error occurs
     */
    public String getConfigurationDescription(String configTypeId) throws InfrastructureException {
        Session session = getSession();
        try {
            OpentapsConfigurationType configuration = (OpentapsConfigurationType) session.get(OpentapsConfigurationType.class, configTypeId);
            if (configuration == null) {
                return null;
            }
            return configuration.getDescription();
        } finally {
            session.close();
        }
    }

    /**
     * Gets a configuration value from the database with null default value.
     * @param configTypeId the config type to get
     * @return a <code>String</code> value
     * @exception InfrastructureException if an error occurs
     */
    public String getConfigurationValue(String configTypeId) throws InfrastructureException {
        return getConfigurationValue(configTypeId, null);
    }

    /**
     * Gets a configuration value from the database.  If it's not configured, use user supplied defaultValue, then OpentapsConfigurationType.defaultValue
     * @param configTypeId the config type to get
     * @param defaultValue the value to return if no configuration is set
     * @return a <code>String</code> value
     * @exception InfrastructureException if an error occurs
     */
    public String getConfigurationValue(String configTypeId, String defaultValue) throws InfrastructureException {
        Session session = getSession();
        try {
            OpentapsConfiguration configuration = (OpentapsConfiguration) session.get(OpentapsConfiguration.class, configTypeId);
            if (configuration == null) {
                if (defaultValue != null) {
                    // if user supplied a non-null defaultValue, then use it
                    Debug.logWarning("No value found for configuration [" + configTypeId + "] returning a default of ["  + defaultValue + "]", MODULE);
                    return defaultValue;
                } else {
                    // use the default value of the configuration type
                    OpentapsConfigurationType configurationType = (OpentapsConfigurationType) session.get(OpentapsConfigurationType.class, configTypeId);
                    if (configurationType != null) {
                        Debug.logWarning("No value found for configuration [" + configTypeId + "] returning a default of ["  + configurationType.getDefaultValue() + "]", MODULE);
                        return configurationType.getDefaultValue();
                    } else {
                        Debug.logWarning("No configuration type [" + configTypeId + "] returning null", MODULE);
                        return null;
                    }
                }
            }
            return configuration.getValue();
        } finally {
            session.close();
        }
    }

    /**
     * Gets a configuration value as boolean from the database with null default value.
     * This is a convenience method to get the common Y/N flags configuration values.
     * @param configTypeId the config type to get
     * @return <code>TRUE</code> if the config value is Y/y, <code>FALSE</code> otherwise (including for null values)
     * @exception InfrastructureException if an error occurs
     */
    public Boolean getConfigurationValueAsBoolean(String configTypeId) throws InfrastructureException {
        return "Y".equalsIgnoreCase(getConfigurationValue(configTypeId));
    }

    /**
     * Gets a configuration value as boolean from the database with null default value.
     * This is a convenience method to get the common Y/N flags configuration values.
     * @param configTypeId the config type to get
     * @param defaultValue the value to return if no configuration is set
     * @return <code>TRUE</code> if the config value is Y/y, <code>FALSE</code> for any other non empty values, <code>defaultValue</code> if no configuration set
     * @exception InfrastructureException if an error occurs
     */
    public Boolean getConfigurationValueAsBoolean(String configTypeId, Boolean defaultValue) throws InfrastructureException {
         String configValue = getConfigurationValue(configTypeId);
         if (configValue == null || configValue.length() == 0) {
             return defaultValue;
         } else {
             return "Y".equalsIgnoreCase(configValue);
         }
    }

    /**
     * Sets the configuration value for a configTypeId inside its own transaction.
     * @param configTypeId a <code>String</code> value
     * @param value a <code>String</code> value
     * @param comments a <code>String</code> value
     * @exception InfrastructureException if an error occurs
     */
    public void setConfigurationValue(String configTypeId, String value, String comments) throws InfrastructureException {
        Session session = getSession();
        // we should check if exists OpentapsConfiguration firstly
        OpentapsConfiguration configuration = (OpentapsConfiguration) session.get(OpentapsConfiguration.class, configTypeId);
        if (configuration == null) {
            configuration = new OpentapsConfiguration();
        }
        configuration.setConfigTypeId(configTypeId);
        configuration.setValue(value);
        configuration.setComments(comments);
        // saveOrUpdate will try to update when id not null, so we should use save to instead of it.
        session.save(configuration);
        session.flush();
        session.close();
    }

    /**
     * Evict all entries of entityName from the second-level cache.
     * @param entityName a <code>String</code> value
     */
    public void evictHibernateCache(String entityName)  {
        evictHibernateCache(entityName, null);
    }

    /**
     * Evict an entry from the second-level cache.
     * @param entityName the name of the entity
     * @param id a <code>Serializable</code> instance
     */
    public void evictHibernateCache(String entityName, Serializable id) {
        if (entityName.indexOf(".") < 0) {
            // if entity name haven't package, then add it
            entityName = ENTITY_PACKAGE + "." + entityName;
        }
        try {
            Class<?> persistentClass = Class.forName(entityName);
            evictHibernateCache(persistentClass, id);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * Evict all entries of persistentClass from the second-level cache.
     * @param persistentClass a <code>Class</code> instance
     */
    public void evictHibernateCache(Class<?> persistentClass) {
        evictHibernateCache(persistentClass, null);
    }

    /**
     * Evict an entry from the second-level cache.
     * @param persistentClass a <code>Class</code> instance
     * @param id a <code>Serializable</code> instance
     */
    public void evictHibernateCache(Class<?> persistentClass, Serializable id) {
        SessionFactory sessionFactory = sessionFactories.get(delegator.getDelegatorName());
        evictHibernateCache(sessionFactory, persistentClass, id);
    }

    /**
     * Evict an entry from the second-level cache.
     * @param sessionFactory a <code>SessionFactory</code> instance
     * @param persistentClass a <code>Class</code> instance
     * @param id a <code>Serializable</code> instance
     */
    public void evictHibernateCache(SessionFactory sessionFactory, Class<?> persistentClass, Serializable id) {
        if (sessionFactory != null) {
            ClassMetadata classMetadata = sessionFactory.getClassMetadata(persistentClass);
            // ensure the persistentClass is an entity class
            if (classMetadata != null) {
                if (id == null) {
                    sessionFactory.evict(persistentClass);
                } else {
                    sessionFactory.evict(persistentClass, id);
                }
            }
        }
    }

    /**
     * Clear hibernate second-level cache.
     */
    public void evictHibernateCache()  {
        SessionFactory sessionFactory = sessionFactories.get(delegator.getDelegatorName());
        evictHibernateCache(sessionFactory);
    }

    /**
     * Clear hibernate second-level cache.
     * @param sessionFactory a <code>SessionFactory</code> instance
     */
    @SuppressWarnings("unchecked")
    public void evictHibernateCache(SessionFactory sessionFactory) {
        if (sessionFactory != null) {
            Map<String, CollectionMetadata> roleMap = sessionFactory.getAllCollectionMetadata();
            for (String roleName : roleMap.keySet()) {
                sessionFactory.evictCollection(roleName);
            }
            Map<String, ClassMetadata> entityMap = sessionFactory.getAllClassMetadata();
            for (String entityName : entityMap.keySet()) {
                sessionFactory.evictEntity(entityName);
            }
            sessionFactory.evictQueries();
        }
    }
}
