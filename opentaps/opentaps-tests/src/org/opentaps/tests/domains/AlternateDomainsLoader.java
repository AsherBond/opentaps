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
package org.opentaps.tests.domains;

import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * This is an example of an alternate domains loader, which loads domains from an alternate domains directory XML file.
 */
public class AlternateDomainsLoader extends DomainsLoader {

    /** The file defining the domains directory beans. */
    public static final String ALT_DOMAINS_DIRECTORY = "alternate-domains-directory.xml";

    /**
     * Default constructor.
     */
    public AlternateDomainsLoader() {
        super();
    }

    /**
     * Creates a new <code>AlternateDomainsLoader</code> instance.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     */
    public AlternateDomainsLoader(Infrastructure infrastructure, User user) {
        super(infrastructure, user);
    }

    @Override
    protected String getDefaultDomainsDirectoryFile() {
        return ALT_DOMAINS_DIRECTORY;
    }

}
