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
package org.opentaps.domain.crmsfa.teams;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * This is the interface of the Team domain.
 */
public interface CrmTeamDomainInterface extends DomainInterface {

    /**
     * Returns the CRM team repository.
     * @return a <code>CrmTeamRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public CrmTeamRepositoryInterface getCrmTeamRepository() throws RepositoryException;

}
