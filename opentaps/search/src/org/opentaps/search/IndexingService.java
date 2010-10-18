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
package org.opentaps.search;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.model.ModelEntity;
import org.opentaps.domain.search.IndexingServiceInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

/**
 * Implementation of the IndexingServiceInterface.
 * <ul>
 *  <li>the <code>createHibernateSearchIndex</code> service regenerates the search index for all entities configured in <code>entitysearch.properties</code>
 *  <li>the <code>createIndexForGenericEntity</code> service updates the search index with the values of an entity set by <code>setValue</code>
 * </ul>
 */
public class IndexingService extends Service implements IndexingServiceInterface {

    private static final String MODULE = IndexingService.class.getName();

    private GenericEntity value = null;

    /**
     * Default constructor.
     */
    public IndexingService() {
        super();
    }

    /** {@inheritDoc} */
    public void setValue(Object value) {
        if (value instanceof GenericEntity) {
            this.value = (GenericEntity) value;
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void createHibernateSearchIndex() throws ServiceException {
        try {
            Properties entitySearchProperties = UtilProperties.getProperties("entitysearch.properties");
            Enumeration enumeration = entitySearchProperties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                String value = entitySearchProperties.getProperty(key);
                // find and create index for each entity define in entitysearch.properties
                if ("index".equals(value)) {
                    if (!key.contains(".")) {
                        key = "org.opentaps.base.entities." + key;
                    }
                    Debug.logInfo("creating index for entity [" + key + "]", MODULE);
                    Class cls = Class.forName(key);
                    createHibernateSearchIndex(new Class[] {cls});
                }
            }
        } catch (ClassNotFoundException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /**
     * Creates the hibernate search index for the given none-view entity classes.
     * @param classes a <code>Class[]</code> instance
     * @throws ServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void createHibernateSearchIndex(Class[] classes) throws ServiceException {
        Session session = null;
        FullTextSession fullTextSession = null;
        Transaction tx = null;
        try {
            // open a session
            session = getInfrastructure().getSession();
            // create FullTextSession by original hibernate session.
            fullTextSession = Search.getFullTextSession(session.getHibernateSession());
            // begin a transaction
            tx = fullTextSession.beginTransaction();
            for (int i = 0; i < classes.length; i++) {
                Class cls = classes[i];
                Entity object = (Entity) cls.newInstance();
                // only can create index for none-view entities
                if (!object.isView()) {
                    Debug.logVerbose("refresh index for [" + cls.getCanonicalName() + "] begin.", MODULE);
                    // refresh every entities hibernate search index
                    createIndexForEntity(fullTextSession, cls);
                    Debug.logVerbose("refresh index for [" + cls.getCanonicalName() + "] end.", MODULE);
                }
            }
            // commit the transaction
            tx.commit();
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            // rollback the transaction
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e2) {
                    Debug.logWarning(e2, "Could not rollback the hibernate transaction on error.", MODULE);
                }
            }
            throw new ServiceException(e);
        } finally {
            // close the session
            try {
                if (fullTextSession != null) {
                    fullTextSession.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the FullTextSession.", MODULE);
            }

            try {
                if (session != null && session.isOpen()) {
                    Debug.logWarning("Session still open, closing.", MODULE);
                    session.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the Session.", MODULE);
            }
        }
    }

    /**
     * Creates the hibernate search index for a given Entity class.
     * @param fullTextSession a <code>FullTextSession</code> value
     * @param entityClass a <code>Class</code> value
     */
    @SuppressWarnings("unchecked")
    private void createIndexForEntity(FullTextSession fullTextSession, Class entityClass) {
        Criteria query = fullTextSession.createCriteria(entityClass)
        //load necessary associations
        .setFetchMode("distributor", FetchMode.JOIN)
        //distinct them (due to collection load)
        .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
        //set flush mode, ensure it will write to disk on commit the transaction
        .setFlushMode(FlushMode.COMMIT)
        //minimize cache interaction
        .setCacheMode(CacheMode.IGNORE)
        .setFetchSize(Session.FETCH_SIZE);
        //scroll in forward only
        ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY);
        int batch = 0;
        while (scroll.next()) {
            batch++;
            fullTextSession.index(scroll.get(0));
            if (batch % Session.FETCH_SIZE == 0) {
                // batch flush index into session per FETCH_SIZE
                fullTextSession.flushToIndexes();
                fullTextSession.clear();
            }
        }
        // flush last changes
        fullTextSession.flushToIndexes();
        fullTextSession.getSearchFactory().optimize(entityClass); 
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void createIndexForGenericEntity() throws ServiceException {

        if (value == null) {
            Debug.logError("Cannot create index for null value.", MODULE);
            return;
        }

        Session session = null;
        FullTextSession fullTextSession = null;
        Transaction tx = null;

        try {
            // open a session
            session = getInfrastructure().getSession();
            // create FullTextSession by original hibernate session.
            fullTextSession = Search.getFullTextSession(session.getHibernateSession());
            fullTextSession.setFlushMode(FlushMode.COMMIT);
            fullTextSession.setCacheMode(CacheMode.IGNORE);

            tx = fullTextSession.beginTransaction();

            String entityName = value.getEntityName();
            ModelEntity modelEntity = value.getModelEntity();
            List<String> pkFieldNames = modelEntity.getPkFieldNames();

            Class cls = Class.forName("org.opentaps.base.entities." + entityName);
            Serializable id = null;

            if (pkFieldNames.size() > 1) {
                // multi fields pk
                Class pkCls = Class.forName("org.opentaps.base.entities." + entityName + "Pk");
                id = (Serializable) pkCls.newInstance();
                for (String pkFieldName : pkFieldNames) {
                    HibernateUtil.setFieldValue(id, pkFieldName, value.get(pkFieldName));
                }
            } else if (pkFieldNames.size() ==  1) {
                // simple field pk
                id = (Serializable) value.get(pkFieldNames.get(0));
            }

            Debug.logInfo("createIndexForGenericEntity: got id [" + id + "] for entity: " + entityName, MODULE);
            if (id != null) {
                Entity entity = (Entity) fullTextSession.get(cls, id);
                if (entity != null) {
                    fullTextSession.index(entity);
                } else {
                    fullTextSession.purge(cls, id);
                }
            }

            // flush last changes
            fullTextSession.flushToIndexes();
            fullTextSession.clear();
            tx.commit();
            Debug.logInfo("createIndexForGenericEntity: index committed", MODULE);
            if (session.isDirty()) {
                Debug.logWarning("Session still dirty ??", MODULE);
            }

        } catch (Exception e) {
            Debug.logError(e, MODULE);
            // rollback the transaction
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e2) {
                    Debug.logWarning(e2, "Could not rollback the hibernate transaction on error.", MODULE);
                }
            }
            throw new ServiceException(e);
        } finally {
            // close the sessions
            try {
                if (fullTextSession != null) {
                    fullTextSession.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the FullTextSession.", MODULE);
            }

            try {
                if (session != null && session.isOpen()) {
                    Debug.logWarning("Session still open, closing.", MODULE);
                    session.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the Session.", MODULE);
            }
        }
    }
}
