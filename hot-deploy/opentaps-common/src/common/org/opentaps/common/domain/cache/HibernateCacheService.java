/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.common.domain.cache;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.domain.cache.HibernateCacheServiceInterface;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

/**
 * Class for the Hibernate Cache Service implementations.
 */
public class HibernateCacheService extends Service implements HibernateCacheServiceInterface {
    private String entityName;
    private GenericEntity pk;
    private EntityCondition condition;
    
    /**
     * Default constructor.
     */
    public HibernateCacheService() {
        super();
    }

    /** {@inheritDoc} */
    public void evictHibernateCache() throws ServiceException {
        try {
            if (entityName == null) {
                getInfrastructure().evictHibernateCache();
            } else if (condition != null) {
                List<GenericValue> removedEntities = getInfrastructure().getDelegator().findList(entityName, condition, null, null, null, false);
                for (GenericValue entity : removedEntities) {
                    getInfrastructure().evictHibernateCache(entity.getEntityName(), HibernateUtil.genericPkToEntityPk(entity.getPrimaryKey()));
                }
            } else if (pk != null) {
                getInfrastructure().evictHibernateCache(pk.getEntityName(), HibernateUtil.genericPkToEntityPk(pk));
            } else {
                getInfrastructure().evictHibernateCache(entityName);
            }
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e);
        } catch (InstantiationException e) {
            throw new ServiceException(e);
        } catch (IllegalAccessException e) {
            throw new ServiceException(e);
        } catch (ClassNotFoundException e) {
            throw new ServiceException(e);
        } catch (NoSuchMethodException e) {
            throw new ServiceException(e);
        } catch (InvocationTargetException e) {
            throw new ServiceException(e);
        }
       
    }

    /** {@inheritDoc} */
    public void setCondition(EntityCondition condition) {
       this.condition = condition;
    }

    /** {@inheritDoc} */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /** {@inheritDoc} */
    public void setPk(GenericEntity pk) {
        this.pk = pk;
    }

}
