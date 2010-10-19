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

package org.opentaps.tests.crmsfa.teams;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.opentaps.tests.OpentapsTestCase;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;

public class TeamsTests extends OpentapsTestCase {

    protected GenericValue DemoCSR;
    protected GenericValue admin = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DemoCSR = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoCSR"));
        admin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        // test that the object have been retrieved
        assertTrue("DemoCSR not null", DemoCSR != null);
        assertTrue("admin not null", admin != null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        DemoCSR = null;
        admin = null;
    }

    public void testAddTeamMemberWithUnauthorisedLogin() {

        // UserLogin not authorised to add team members
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", DemoCSR);
        input.put("teamMemberPartyId", "DemoCSR");
        input.put("accountTeamPartyId", "DemoAccount1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceError("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberToAccount() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR");
        input.put("accountTeamPartyId", "DemoAccount1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceSuccess("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberToAccountWhichAlreadyExist() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR2");
        input.put("accountTeamPartyId", "DemoAccount1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceSuccess("crmsfa.addTeamMember", input);

        // Adding a team member which already exists
        input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR2");
        input.put("accountTeamPartyId", "DemoAccount1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceError("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberNonCRMPartyToAccount() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCustomer");
        input.put("accountTeamPartyId", "DemoAccount1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceError("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberToTeam() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR");
        input.put("accountTeamPartyId", "DemoSalesTeam1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceSuccess("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberToTeamWhichAlreadyExist() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR2");
        input.put("accountTeamPartyId", "DemoSalesTeam1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceSuccess("crmsfa.addTeamMember", input);

        // Adding a team member which already exists
        input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCSR2");
        input.put("accountTeamPartyId", "DemoSalesTeam1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceError("crmsfa.addTeamMember", input);

    }

    public void testAddTeamMemberNonCRMPartyToTeam() {

        // Adding a team member
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("userLogin", admin);
        input.put("teamMemberPartyId", "DemoCustomer");
        input.put("accountTeamPartyId", "DemoSalesTeam1");
        input.put("securityGroupId", "SALES_MANAGER");
        runAndAssertServiceError("crmsfa.addTeamMember", input);

    }

}
