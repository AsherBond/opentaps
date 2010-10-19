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
package org.opentaps.domain.activities;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * This is the interface of the Activities domain.
 */
public interface ActivitiesDomainInterface extends DomainInterface {

    /**
     * Returns the ActivityFact repository instance.
     * @return a <code>ActivityFactRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public ActivityFactRepositoryInterface getActivityFactRepository() throws RepositoryException;

    /**
     * Returns the Activity repository instance.
     * @return a <code>ActivityRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public ActivityRepositoryInterface getActivityRepository() throws RepositoryException;

}
