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
package org.opentaps.domain.search;

import org.opentaps.foundation.service.ServiceException;

/**
 * Interface for the indexing services.
 * The indexing services are used to generate and keep the search index up-to-date.
 */
public interface IndexingServiceInterface {

    /**
     * Sets the value to be indexed.
     * @param value a <code>Object</code> instance
     */
    public void setValue(Object value);

    /**
     * Service to create the search index for entities.
     * @throws ServiceException if an error occurs
     */
    public void createHibernateSearchIndex() throws ServiceException;

    /**
     * Service to create the search index for GenericEntity (IndexForDelegatorAspects.aj).
     * @throws ServiceException if an error occurs
     */
    public void createIndexForGenericEntity() throws ServiceException;
}
