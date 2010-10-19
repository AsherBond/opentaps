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
package org.opentaps.asterisk.tests;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.ExternalUser;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.OpentapsTestCase;


/**
 * Asterisk related tests.
 */
public class AsteriskTests  extends OpentapsTestCase {

    private static final String MODULE = AsteriskTests.class.getName();

    private GenericValue admin;
    
    private static final String ASTERISK_USERTYPE_ID = "ASTERISK";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
    }
    
    /**
     * Tests testGetUserByExtension for lookup an asterisk extension for a user.
     * @throws Exception if an error occurs
     */
    public void testGetExternalUserForUser() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();
        GenericValue demoSalesRep1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
        ExternalUser externalUser = repo.getExternalUserForUser(ASTERISK_USERTYPE_ID, new User(demoSalesRep1));
        assertNotNull("Could not find any ExternalUser", externalUser);
    }

    /**
     * Tests getExternalUserForUser for lookup the user who not have asterisk extension.
     * @throws Exception if an error occurs
     */
    public void testGetExternalUserForUserForNonExisting() throws Exception {
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin));
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repo = partyDomain.getPartyRepository();
        GenericValue purchentry = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "purchentry"));
        ExternalUser externalUser = repo.getExternalUserForUser(ASTERISK_USERTYPE_ID, new User(purchentry));
        assertNull("There should be no any ExternalUser matched.", externalUser);
    }
    
    /**
     * Tests getExternalUserForUser for lookup the user who not have expired asterisk extension.
     * @throws Exception if an error occurs
     */
    public void testGetExternalUserForUserForExpiredNumber() throws Exception { 
       DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(admin)); 
       PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain(); 
       PartyRepositoryInterface repo = partyDomain.getPartyRepository();
       GenericValue demoSalesRep3 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
       ExternalUser externalUser = repo.getExternalUserForUser(ASTERISK_USERTYPE_ID, new User(demoSalesRep3));
       assertNull("There should be no any ExternalUser matched.", externalUser);
   } 

}
