package org.opentaps.domain;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

/**
 * This class helps with loading of domains using the Spring framework
 */
public class DomainsLoader {

    private static String DOMAINS_DIRECTORY_FILE = "domains-directory.xml";
    private static String DOMAINS_DIRECTORY_BEAN_ID = "domainsDirectory";

    private Infrastructure infrastructure = null;
    private User user = null;
        
    public DomainsLoader() {
        // default constructor
    }

    public DomainsLoader(Infrastructure infrastructure, User user) {
        this();
        setInfrastructure(infrastructure);
        setUser(user);
    }

    /**
     * Constructs a domain loader from an application's request context.
     */
    public DomainsLoader(HttpServletRequest request) throws InfrastructureException {
        this();

        // in OFBiz, we can get the dispatcher and user login as follows
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        if (userLogin == null) {
            HttpSession session = request.getSession();
            if (session != null) userLogin = (GenericValue) session.getAttribute("userLogin");
        }
        setInfrastructure(new Infrastructure(dispatcher));
        if (userLogin != null) setUser(new User(userLogin));
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public User getUser() {
        return this.user;
    }

    public Infrastructure getInfrastructure() {
        return this.infrastructure;
    }

    /**
     * the domains directory by using the Spring framework to load the
     * domains directory file (by default called) "domains-directory.xml") and return its DomainsDirectory bean (by default called
     * "domainsDirectory")
     * @return
     */
    public DomainsDirectory loadDomainsDirectory() {
        Resource resource = new ClassPathResource(DOMAINS_DIRECTORY_FILE);
        ListableBeanFactory bf = new XmlBeanFactory(resource);
        DomainsDirectory domains = (DomainsDirectory) bf.getBean(DOMAINS_DIRECTORY_BEAN_ID);
        domains.setInfrastructure(infrastructure);
        domains.setUser(user);
        return domains;
    }
}
