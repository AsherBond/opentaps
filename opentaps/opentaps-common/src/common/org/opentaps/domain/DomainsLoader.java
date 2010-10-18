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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


/**
 * This class helps with loading of domains using the Spring framework.
 */
public class DomainsLoader implements DomainContextInterface {

    private static String DOMAINS_DIRECTORY_FILE = "domains-directory.xml";
    private static String DOMAINS_DIRECTORY_BEAN_ID = "domainsDirectory";
    private static String MODULE = DomainsLoader.class.getName();

    /* The domainsDirectory is static because the XML definitions should not
     * change until the system restarts. Also, the directory will accept
     * registrations from various modules.  By having a single, centralized
     * directory all domains can access all other domains.
     *
     * To support separate domains directories for each component, we maintain
     * a Map of source file -> DomainsDirectory
     */
    private static Map<String, DomainsDirectory> DOMAINS_DIRECTORIES = FastMap.newInstance();
    private static Set<String> REGISTERED_LOADERS = new HashSet<String>();
 
    private Infrastructure infrastructure = null;
    private User user = null;

    /**
     * Default constructor.
     */
    public DomainsLoader() { }

    /**
     * Creates a new <code>DomainsLoader</code> instance.
     *
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     */
    public DomainsLoader(Infrastructure infrastructure, User user) {
        this();
        setInfrastructure(infrastructure);
        setUser(user);
    }

    /**
     * Creates a new <code>DomainsLoader</code> instance.
     *
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param domainsDirectoryFile a <code>String</code> value
     */
    public DomainsLoader(Infrastructure infrastructure, User user, String domainsDirectoryFile) {
        this();
        setInfrastructure(infrastructure);
        setUser(user);
    }

    /**
     * Constructs a domain loader from an application's request context.
     * @param request a <code>HttpServletRequest</code> value
     * @exception InfrastructureException if an error occurs
     */
    public DomainsLoader(HttpServletRequest request) throws InfrastructureException {
        this();

        // in OFBiz, we can get the dispatcher and user login as follows
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        if (userLogin == null) {
            HttpSession session = request.getSession();
            if (session != null) {
                userLogin = (GenericValue) session.getAttribute("userLogin");
            }
        }
        setInfrastructure(new Infrastructure(dispatcher));
        if (userLogin != null) {
            setUser(new User(userLogin));
        }
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    /** {@inheritDoc} */
    public User getUser() {
        return this.user;
    }

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return this.infrastructure;
    }

    /** {@inheritDoc} */
    public void setDomainContext(DomainContextInterface context) {
        this.setDomainContext(context.getInfrastructure(), context.getUser());
    }

    /** {@inheritDoc} */
    public void setDomainContext(Infrastructure infrastructure, User user) {
        this.setInfrastructure(infrastructure);
        this.setUser(user);
    }

    /**
     * Gets the default domains directory file, defining the beans for this domains loader.
     * Override in custom domains loader that need to redefine the beans.
     * @return a <code>String</code> value
     */
    protected String getDefaultDomainsDirectoryFile() {
        return DOMAINS_DIRECTORY_FILE;
    }

    private DomainsDirectory getCachedDomainsDirectory(String domainsDirectoryFile) {
        DomainsDirectory directory = DOMAINS_DIRECTORIES.get(domainsDirectoryFile);
        if (directory == null) {
            initializeDomainsDirectory(domainsDirectoryFile);
            directory = DOMAINS_DIRECTORIES.get(domainsDirectoryFile);
        }
        return directory;
    }

    /**
     * Returns the domains directory from the default domains directory file.
     * @return a <code>DomainsDirectory</code> value
     */
    public final DomainsDirectory getDomainsDirectory() {
        return getDomainsDirectory(getDefaultDomainsDirectoryFile());
    }

    /**
     * Returns DomainsDirectory from the given domainsDirectoryFile.
     * Use this method to create overloaded versions in custom domains loaders.
     * @param domainsDirectoryFile name of the domain definition xml file
     * @return a <code>DomainsDirectory</code> value
     */
    public final DomainsDirectory getDomainsDirectory(String domainsDirectoryFile) {
        DomainsDirectory directory = getCachedDomainsDirectory(domainsDirectoryFile);

        // clone the cached domains directory so we can set the proper User / Infrastructure
        directory = new DomainsDirectory(directory);
        directory.setUser(this.user);
        directory.setInfrastructure(this.infrastructure);
        return directory;
    }

    /**
     * Initialize the DomainsDirectory for domainsDirectoryFile.  This is internally
     * synchronized to ensure two threads do not attempt to initialize two
     * copies of the domainsDirectory from the same domainsDirectoryFile.  The domains
     * directory is saved in a Map with the domains directory file as a key.
     *
     * @param domainsDirectoryFile  name of the domain definition xml file
     */
    private static synchronized void initializeDomainsDirectory(String domainsDirectoryFile) {
        /* If this method was not synchronized it could lead to a temporary
         * state of multiple domainsDirectories.  Registrations could randomly
         * occur against either.  When the earlier of two was garbage collected
         * the registrations within that instance would be lost.
         */
        if (DOMAINS_DIRECTORIES.get(domainsDirectoryFile) != null) {
            Debug.logWarning("Domains directory for [" + domainsDirectoryFile + "] is not null, will not be reinitializing", MODULE);
            return;
        }

        // use default domains directory file unless another one has been set
        if (domainsDirectoryFile == null) {
            Debug.logFatal("No domains directory file found, using default value [" + DOMAINS_DIRECTORY_FILE + "]", MODULE);
            domainsDirectoryFile = DOMAINS_DIRECTORY_FILE;
        }

        Debug.logInfo("Using domains directory file [" + domainsDirectoryFile + "]", MODULE);
        Resource resource = new ClassPathResource(domainsDirectoryFile);
        ListableBeanFactory bf = new XmlBeanFactory(resource);
        DomainsDirectory myDomainsDirectory = (DomainsDirectory) bf.getBean(DOMAINS_DIRECTORY_BEAN_ID);

        // save using the domains directory file as key
        DOMAINS_DIRECTORIES.put(domainsDirectoryFile, myDomainsDirectory);
    }

    /**
     * Registers the domains configured in the specified domainsDirectoryFile.
     * Extending DomainsLoaders should invoke this method on instantiation. This
     * method will ignore attempts to re-register domains that have already
     * registered.
     *
     * Note since this method is protected, only classes which extend DomainsLoaders,
     * i.e. custom DomainsLoaders, can register additional domains.
     *
     * @param domainsDirectoryFile name of the domain definition xml file
     */
    protected void registerDomains(String domainsDirectoryFile) {
        /* Figure out the name of the DomainLoader attempting to register. If it
         * has already registered, do not re-register
         */
        final Throwable t = new Throwable();
        final StackTraceElement methodCaller = t.getStackTrace()[1];
        final String domainLoaderName = methodCaller.getClassName();
        if (REGISTERED_LOADERS.contains(domainLoaderName)) {
            // TODO: should throw an exception here
            Debug.logWarning("Domain loader [" + domainLoaderName + "] has already been registered.  Will not be registering again.", MODULE);
            return;
        } else {
            Debug.logInfo("Now registering domain loader [" + domainLoaderName + "]", MODULE);
        }

        Resource resource = new ClassPathResource(domainsDirectoryFile);
        XmlBeanFactory bean = new XmlBeanFactory(resource);
        String[] domainsToRegister = bean.getBeanNamesForType(DomainInterface.class);
        DomainsDirectory directory = getCachedDomainsDirectory(getDefaultDomainsDirectoryFile());
        for (String domainToRegister : domainsToRegister) {
            directory.addDomain(domainToRegister, (DomainInterface) bean.getBean(domainToRegister));
        }

        // Register the calling class so that it may not re-register
        REGISTERED_LOADERS.add(domainLoaderName);
    }

    public static void registerDomainDirectory(String domainsDirectoryFile) {
        if (UtilValidate.isEmpty(domainsDirectoryFile)) {
            throw new IllegalArgumentException();
        }

        Resource resource = new ClassPathResource(domainsDirectoryFile);
        XmlBeanFactory bean = new XmlBeanFactory(resource);
        String[] domainsToRegister = bean.getBeanNamesForType(DomainInterface.class);
        DomainsDirectory directory = DOMAINS_DIRECTORIES.get(DOMAINS_DIRECTORY_FILE);
        if (directory == null) {
            initializeDomainsDirectory(DOMAINS_DIRECTORY_FILE);
            directory = DOMAINS_DIRECTORIES.get(DOMAINS_DIRECTORY_FILE);
        }
        for (String domainToRegister : domainsToRegister) {
            directory.addDomain(domainToRegister, (DomainInterface) bean.getBean(domainToRegister));
        }
    }

    /**
     * Same as getDomainsDirectory().
     *
     * @return a <code>DomainsDirectory</code> value
     * @deprecated {@link #getDomainsDirectory()}
     */
    public DomainsDirectory loadDomainsDirectory() {
        return getDomainsDirectory();
    }
}
