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
/* Copyright (c) Open Source Strategies, Inc. */

package org.opentaps.tests.crmsfa;

import org.ofbiz.base.util.UtilMisc;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityFactory;

import junit.framework.TestCase;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * This is a class of unit tests for CRM/SFA application's security permissions.
 * To run these, from the command line do
 * $ java -jar ofbiz.jar -test
 *
 * or
 * $ ant run-tests
 *
 * This will be run along with other test suites, such as those for the entity engine or the service engine
 *
 * This test suite requires crmsfa/data/CRMSFADemoData.xml and CRMSFASecurityData.xml to be installed
 *
 * Also, make sure that your "test" delegator is set to the correct datasource in framework/entity/config/entityengine.xml
 *
 * @author sichen
 *
 */
public class SecurityTests extends TestCase {

    public static final String module = SecurityTests.class.getName();
    public static final String DELEGATOR = "test";
    private Delegator delegator = null;
    private Security security = null;

    public SecurityTests (String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        this.delegator = DelegatorFactory.getDelegator(DELEGATOR);
        this.security = SecurityFactory.getInstance(delegator);
    }

    // DemoSalesManager for DemoAccount1

    /**
     * This is a template for a security unit case.  This tests if DemoSalesManager userLogin has permission to update
     * DemoAccount1
     * @throws Exception
     */
    public void testDemoAccount1DemoSalesManagerUpdate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has update permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesManagerView() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_VIEW", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has view permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesManagerDeactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has deactivate permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesManagerReassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has reassign permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesRep1 for DemoAccount1
    public void testDemoAccount1DemoSalesRep1View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_VIEW", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has view permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep1Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has update permission for DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep1Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has deactivate permission for DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep1Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has reassign permission for DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesRep2 for DemoAccount1
    public void testDemoAccount1DemoSalesRep2View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_VIEW", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has view permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep2Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has update permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep2Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has deactivate permission for DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep2Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has reassign permission for DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesRep3 for DemoAccount1
    public void testDemoAccount1DemoSalesRep3View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_VIEW", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has view permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep3Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has update permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep3Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has deactivate permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoAccount1DemoSalesRep3Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, "DemoAccount1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has reassign permission for DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesManager for DemoContact1
    public void testDemoContact1DemoSalesManagerView() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_VIEW", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has view permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesManagerUpdate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_UPDATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has update permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesManagerDeactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_DEACTIVATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has deactivate permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesManagerReassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has reassign permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact2DemoSalesManagerReassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager has reassign permission for DemoContact2", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesRep1 for DemoContact1
    public void testDemoContact1DemoSalesRep1View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_VIEW", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has view permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep1Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_UPDATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has update permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep1Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_DEACTIVATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has deactivate permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep1Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has reassign permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact2DemoSalesRep1Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact2")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 has reassign permission for DemoContact2", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    // DemoSalesRep2 for DemoContact1
    public void testDemoContact1DemoSalesRep2View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_VIEW", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has view permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep2Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_UPDATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has update permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep2Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_DEACTIVATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has deactivate permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep2Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has reassign permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact2DemoSalesRep2Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact2")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep2 has reassign permission for DemoContact2", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    // DemoSalesRep3 for DemoContact1
    public void testDemoContact1DemoSalesRep3View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_VIEW", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has view permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep3Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_UPDATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has update permission for DemoContact1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep3Deactivate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_DEACTIVATE", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has deactivate permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact1DemoSalesRep3Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has reassign permission for DemoContact1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoContact2DemoSalesRep3Reassign() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep3"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, "DemoContact2")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep3 has reassign permission for DemoContact2", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    /**
     * Test the CrmsfaSecurity.hasActivityPermission() method.
     */
    public void testDemoTask1DemoSalesManagerView() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_VIEW", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can view DemoTask1 which is associated with DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoTask1DemoSalesManagerUpdate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can update DemoTask1 which is associated with DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoTask1DemoSalesManagerClose() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_CLOSE", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can close DemoTask1 which is associated with DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoTask1DemoSalesRep1View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_VIEW", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can view DemoTask1 which is associated with DemoAccount1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoTask1DemoSalesRep1Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can update DemoTask1 which is associated with DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoTask1DemoSalesRep1Close() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasActivityPermission(security, "_CLOSE", userLogin, "DemoTask1", "DemoAccount1", null, null)) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can close DemoTask1 which is associated with DemoAccount1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    /**
     * Test the CrmsfaSecurity.hasCasePermission() method.
     */
    public void testDemoCase1DemoSalesManagerView() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_VIEW", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can view DemoCase1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoCase1DemoSalesManagerUpdate() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can update DemoCase1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testDemoCase1DemoSalesManagerClose() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_CLOSE", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesManager can close DemoCase1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoCase1DemoSalesRep1View() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_VIEW", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can view DemoCase1", true, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoCase1DemoSalesRep1Update() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can update DemoCase1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    public void testDemoCase1DemoSalesRep1Close() throws Exception {
        try {
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
            boolean hasPermission = false;
            if (CrmsfaSecurity.hasCasePermission(security, "_CLOSE", userLogin, "DemoCase1")) {
                hasPermission = true;
            }
            TestCase.assertEquals("DemoSalesRep1 can close DemoCase1", false, hasPermission);
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
}
