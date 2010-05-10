/*
 * Copyright (c) 2010 Open Source Strategies, Inc.
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

import org.opentaps.domain.DomainsLoader;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * A domains loader to test loading additional domains from an XML file
 * The domains should be retrievable from a DomainsDirectory retrieved from TestDomainsLoader
 *
 */
public class TestDomainsLoader extends DomainsLoader {
	
	public static String TEST_DOMAINS_DIRECTORY = "test-domains-directory.xml";
	public static String TEST_DOMAIN = "testDomain";
	
	public TestDomainsLoader() {
		super();
	}
	
	public TestDomainsLoader(Infrastructure infrastructure, User user) {
		super(infrastructure, user);
		super.registerDomains(TEST_DOMAINS_DIRECTORY);	
	}

}
