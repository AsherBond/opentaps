/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.tests.crmsfa.activities;

import java.util.HashMap;
import java.util.Map;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.tests.OpentapsTestCase;

public class ActivitiesTests extends OpentapsTestCase {
    
    public void testOwnerPartyId() throws GeneralException {
        GenericValue activityOwner = UtilActivity.getActivityOwner("DemoMeeting1", delegator);
        assertEquals("DemoSalesRep1 is the owner of Demo Meeting 1", activityOwner.getString("partyId"), "DemoSalesRep1");
    }
    
    public void testChangeOwnerPartyId() throws GeneralException {
        
        GenericValue DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        GenericValue DemoSalesRep1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep1"));
        GenericValue DemoSalesRep2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesRep2"));
        
        /*
         * DemoSalesRep1 changes owner of activity DemoMeeting1 to DemoSalesManager.
         * Should be success. Calendar owner about to change owner to superuser. 
         */
        Map<String, Object> callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesRep1);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesManager.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);
        
        /*
         * DemoSalesRep1 changes owner of activity DemoMeeting to DemoSalesRep2.
         * Should be fail. DemoSalesRep1 isn't owner any more and isn't superuser. 
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesRep1);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep2.getString("partyId"));
        
        runAndAssertServiceError("crmsfa.changeActivityOwner", callContext);
        
        /*
         * DemoSalesManager changes owner of activity DemoMeeting1 to DemoSalesRep2.
         * Should be success. DemoSalesManager is owner and superuser. 
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesManager);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep2.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);

        /*
         * DemoSalesManager changes owner of activity DemoMeeting1 to DemoSalesRep1.
         * Should be success. DemoSalesManager isn't owner of the activity but superuser.
         */
        callContext = new HashMap<String, Object>();
        callContext.put("userLogin", DemoSalesManager);
        callContext.put("workEffortId", "DemoMeeting1");
        callContext.put("newOwnerPartyId", DemoSalesRep1.getString("partyId"));
        
        runAndAssertServiceSuccess("crmsfa.changeActivityOwner", callContext);
        
    }
}