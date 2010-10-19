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

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Helper class which is able to cache the DomainLoader.
 */
public class DomainRepository extends Repository {

    private DomainsLoader domainsLoader;
    private DomainsDirectory domainsDirectory;

    /**
     * Default constructor.
     */
    public DomainRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public DomainRepository(Delegator delegator) {
        super(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @throws RepositoryException if an error occurs
     */
    public DomainRepository(Infrastructure infrastructure) throws RepositoryException {
        super(infrastructure);
    }

    /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public DomainRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param userLogin the Ofbiz <code>UserLogin</code> generic value
     * @throws RepositoryException if an error occurs
     */
    public DomainRepository(Infrastructure infrastructure, GenericValue userLogin) throws RepositoryException {
        super(infrastructure, userLogin);
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
