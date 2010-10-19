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
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.OpentapsTestCase;

/**
 * These tests validate functionality of the DomainsLoader and DomainsDirectory.
 */
public class DomainsTests extends OpentapsTestCase {

    private TestDomainsLoader testDomainsLoader;
    private Object testDomain;
    private Class<?> testDomainClass = org.opentaps.tests.domains.TestDomain.class;
    private Class<?> testBillingDomainClass = org.opentaps.tests.domains.TestBillingDomain.class;
    private Class<?> regularBillingDomainClass = org.opentaps.financials.domain.billing.BillingDomain.class;
    private AlternateDomainsLoader alternateDomainsLoader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDomainsLoader = new TestDomainsLoader(new Infrastructure(dispatcher), new User(admin));
        alternateDomainsLoader = new AlternateDomainsLoader(new Infrastructure(dispatcher), new User(admin));
        testDomain = null;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verify that the TestDomainsLoader returns the test domain successfully.
     */
    public void testExtendedDomainLoader() {
        testDomain = testDomainsLoader.getDomainsDirectory().getDomain(TestDomainsLoader.TEST_DOMAIN);
        assertNotNull("Test domain was null from the TestDomainsLoader", testDomain);
        assertTrue("Test domain was not an instance of [" + testDomainClass.getName() + "]", testDomainClass.isInstance(testDomain));
    }

    /**
     * Verify that the main DomainsLoader will also return the test domain.
     */
    public void testExtendedDomainFromMainLoader() {
        testDomain = domainsLoader.getDomainsDirectory().getDomain(TestDomainsLoader.TEST_DOMAIN);
        assertNotNull("Test domain could not be retrieved from main DomainsLoader", testDomain);
        assertTrue("Test domain was not an instance of [" + testDomainClass.getName() + "]", testDomainClass.isInstance(testDomain));
    }

    /**
     * Verify that the alternate domains loader is loading the alternate domain, and that the main and test domains loaders
     * are still loading the regular (opentaps) billing domain.
     */
    public void testAlternateDomainLoader() {
        testDomain = alternateDomainsLoader.getDomainsDirectory().getDomain(DomainsDirectory.BILLING_DOMAIN);
        assertNotNull("Domain could not be retrieved from alternate DomainsLoader", testDomain);
        assertTrue("Domain from alternate DomainsLoader was not an instance of [" + testBillingDomainClass.getName() + "] but is actually [" + testDomain.getClass().getName() + "]", testBillingDomainClass.isInstance(testDomain));

        testDomain = testDomainsLoader.getDomainsDirectory().getDomain(DomainsDirectory.BILLING_DOMAIN);
        assertTrue("Domain from test domains loader was not [" + regularBillingDomainClass.getName() + "] but is actually [" + testDomain.getClass().getName() + "]", regularBillingDomainClass.isInstance(testDomain));

        testDomain = domainsLoader.getDomainsDirectory().getDomain(DomainsDirectory.BILLING_DOMAIN);
        assertTrue("Domain from main domains loader was not [" + regularBillingDomainClass.getName() + "] but is actually [" + testDomain.getClass().getName() + "]", regularBillingDomainClass.isInstance(testDomain));
    }

}
