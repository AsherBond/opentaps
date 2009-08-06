/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.foundation.domain;

import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an abstract class of Domain for the ofbiz framework.  It has methods to set and get Infrastructure and User,
 * so that after the first time the domain instantiated, you can set those things and then keep getting everything from
 * the domain without having to reset them for each factory, repository, or service.
 */
public abstract class Domain implements DomainInterface {

    private Infrastructure infrastructure;
    private User user;

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    public void setInfrastructureAndUser(Infrastructure infrastructure, User user) {
        setInfrastructure(infrastructure);
        setUser(user);
    }

    /** {@inheritDoc} */
    public User getUser() {
        return this.user;
    }

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return this.infrastructure;
    }

    /**
     * Standard method for instantiating a Repository object and then setting Infrastructure and User into it.
     * Usage:
     * OrderRepository repository = (OrderRepository) instantiateRepository(OrderRepository.class);
     * Make sure your Repository class has a null constructor
     * @param <T> the repository class to instantiate
     * @param repositoryClass the repository class to instantiate
     * @return a repository instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends RepositoryInterface> T instantiateRepository(Class<T> repositoryClass) throws RepositoryException {
        try {
            T repository = repositoryClass.newInstance();
            repository.setInfrastructure(getInfrastructure());
            repository.setUser(getUser());
            return repository;
        } catch (Exception ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Standard method for instantiating a Service object and then setting Infrastructure, User, and Domains Directory into it.
     * Usage:
     * OrderInventoryService service = (OrderInventoryService) instantiateService(OrderInventoryService.class);
     * Make sure your Service class has a null constructor
     * @param <T> the service class to instantiate
     * @param serviceClass the service class to instantiate
     * @return a service instance
     * @throws ServiceException if an error occurs
     */
    public <T extends ServiceInterface> T instantiateService(Class<T> serviceClass) throws ServiceException {
        try {
            T service = serviceClass.newInstance();
            service.setInfrastructure(getInfrastructure());
            service.setUser(getUser());
            service.loadDomainsDirectory();
            return service;
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }
}
