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
package org.opentaps.domain;

import java.util.Locale;

import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

public class DomainService extends Service {

    private DomainsLoader domainsLoader;
    private DomainsDirectory domainsDirectory;

    /**
     * Default constructor.
     */
    public DomainService() {
        super();
    }

    /**
     * Domain constructor.  Also sets the <code>security</code> object from <code>Infrastructure</code>
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param locale a <code>Locale</code> value
     * @exception ServiceException if an error occurs
     */
    public DomainService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    public DomainsLoader getDomainsLoader() {
        if (domainsLoader == null) {
            domainsLoader = new DomainsLoader(getInfrastructure(), getUser());
        }
        return domainsLoader;
    }

    public DomainsDirectory getDomainsDirectory() {
        if (domainsDirectory == null) {
            domainsDirectory = getDomainsLoader().getDomainsDirectory();
        }
        return domainsDirectory;
    }
}
