/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
