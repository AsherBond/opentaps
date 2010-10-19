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
package org.opentaps.common.domain.security;

import org.opentaps.domain.security.SecurityRepositoryInterface;
import org.opentaps.domain.security.SecurityServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.domain.security.SecurityDomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

public class SecurityDomain extends Domain implements SecurityDomainInterface{

    public SecurityRepositoryInterface getSecurityRepository() throws RepositoryException {
        return instantiateRepository(SecurityRepository.class);
    }

    public SecurityServiceInterface getSecurityService() throws ServiceException {
        return instantiateService(SecurityService.class);
    }

}
