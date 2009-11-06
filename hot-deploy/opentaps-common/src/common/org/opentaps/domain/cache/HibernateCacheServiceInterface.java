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
package org.opentaps.domain.cache;

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.foundation.service.ServiceException;
/**
 * Interface for hibernate cache services.
 */
public interface HibernateCacheServiceInterface {
    /**
     * Sets the entity name to be evict.
     * @param entityName a <code>String</code> value
     */
    public void setEntityName(String entityName);

    /**
     * Sets the entity pk to be evict.
     * @param pk a <code>GenericEntity</code> value
     */
    public void setPk(GenericEntity pk);
    
    /**
     * Sets the condition for entity to be evict.
     * @param condition a <code>EntityCondition</code> value
     */
    public void setCondition(EntityCondition condition);

    /**
     * Service to evict hibernate cache.
     * @throws ServiceException if an error occurs
     */
    public void evictHibernateCache() throws ServiceException;

}
