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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.opensourcestrategies.crmsfa.cases;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * Case utility methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public class UtilCase {

    public static final String module = UtilCase.class.getName();

    // list the case statuses in one place in case the model changes
    public static List CASE_STATUSES_COMPLETED = null;
    static {
        CASE_STATUSES_COMPLETED = UtilMisc.toList("CRQ_COMPLETED", "CRQ_CANCELLED", "CRQ_REJECTED");
    }

    /**
     * Get the active account and contacts for a case.
     * @return List of PartyRelationshipAndCaseRoles with the requested partyId's.
     */
    public static List getCaseAccountsAndContacts(GenericDelegator delegator, String custRequestId) throws GenericEntityException {
        return getCasePartiesByRole(delegator, custRequestId, UtilMisc.toList("ACCOUNT", "CONTACT"));
    }

    /**
     * Gets the first active contact party ID for a case.
     * @return partyId of contact or null
     */
    public static String getCasePrimaryContactPartyId(GenericDelegator delegator, String custRequestId) throws GenericEntityException {
        List candidates = getCasePartiesByRole(delegator, custRequestId, UtilMisc.toList("CONTACT"));
        if (candidates.size() > 0) {
            return ((GenericValue) candidates.get(0)).getString("partyId");
        }
        return null;
    }

    /**
     * Gets the first active account party ID for a case.
     * @return partyId of account or null
     */
    public static String getCasePrimaryAccountPartyId(GenericDelegator delegator, String custRequestId) throws GenericEntityException {
        List candidates = getCasePartiesByRole(delegator, custRequestId, UtilMisc.toList("ACCOUNT"));
        if (candidates.size() > 0) {
            return ((GenericValue) candidates.get(0)).getString("partyId");
        }
        return null;
    }

    /**
     * Helper method to get active party relationships related to a given case via the cust request roles. This is used, for instance, to
     * get unexpired ACCOUNTS or CONTACTS related to the case. This method is to be used for logic, not presentation. Presentation 
     * requires ordering and fields from other joined entities. Don't use this directly, use one of the more convenient helper methods.
     * @return  A list of PartyRelationshipAndCaseRole with partyId's of the requested parties
     */
    public static List getCasePartiesByRole(GenericDelegator delegator, String custRequestId, List roleTypeIds) 
        throws GenericEntityException {

        // add each role type id to an OR condition list
        List roleCondList = new ArrayList();
        for (Iterator iter = roleTypeIds.iterator(); iter.hasNext(); ) {
            String roleTypeId = (String) iter.next();
            roleCondList.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, roleTypeId));
        }
        EntityConditionList roleEntityCondList = new EntityConditionList(roleCondList, EntityOperator.OR);

        // roleEntityCondList AND custRequestId = ${custRequestID} AND filterByDateExpr
        EntityConditionList mainCondList = new EntityConditionList(UtilMisc.toList(
                    roleEntityCondList,
                    new EntityExpr("custRequestId", EntityOperator.EQUALS, custRequestId), 
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);

        TransactionUtil.begin();
        EntityListIterator partiesIt = delegator.findListIteratorByCondition("PartyRelationshipAndCaseRole", mainCondList, null,
                UtilMisc.toList("partyId"),  // fields to select (right now we just want the partyId)
                null, // fields to order by (unimportant here)
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));
        List list = partiesIt.getCompleteList();
        partiesIt.close();
        TransactionUtil.commit();
        return list;
    }
    
    /**
     * Finds all CustRequest which are open for a given party and role combination.  CustRequest must not be CANCELLED, REJECTED, or COMPLETED.
     * @param delegator
     * @param partyId
     * @param roleTypeId
     * @param casesOrderBy field to order by.  Defaults to "priority DESC"
     * @return list iterator of the cases
     * @throws GenericEntityException
     */
    public static EntityListIterator getCasesForParty(GenericDelegator delegator, String partyId, String roleTypeId, String casesOrderBy) throws GenericEntityException {
        if (casesOrderBy == null) {
            casesOrderBy = "priority DESC";
        }
        
        EntityConditionList casesCond = getCasesForPartyCond(partyId, roleTypeId);
        
        EntityListIterator myCases = delegator.findListIteratorByCondition("PartyRelationshipAndCaseRole", casesCond, null, 
                UtilMisc.toList("custRequestId", "custRequestName", "priority", "statusId", "custRequestTypeId", "custRequestCategoryId"),  // fields to select
                UtilMisc.toList(casesOrderBy), // fields to order by
                // the first true here is for "specifyTypeAndConcur"
                // the second true is for a distinct select.  Apparently this is the only way the entity engine can do a distinct query
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));
        
        return myCases;
    }
    
    /**
     * Returns the EntityConditionList that getCasesForParty constructs for finding all CustRequest which are open for
     * a given party and role combination
     * @param partyId
     * @param roleTypeId
     * @return EntityConditionList
     */
    public static EntityConditionList getCasesForPartyCond(String partyId, String roleTypeId) {
    	
        EntityConditionList casesCond = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_COMPLETED"),
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_REJECTED"),
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_CANCELLED"),
                new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, roleTypeId), 
                new EntityExpr("partyIdFrom", EntityOperator.EQUALS, partyId)
                ), EntityOperator.AND);    	

        return casesCond;
    }
  
    
    /**
     * Determine if the case has a status which should be considered "finished" or "history" or "done with".
     * It is recommended to use this instead of checking statuses by hand, because those can change.
     */
    public static boolean caseIsInactive(GenericValue custRequest) {
        for (Iterator iter = CASE_STATUSES_COMPLETED.iterator(); iter.hasNext(); ) {
            if (iter.next().equals(custRequest.getString("statusId"))) {
                return true;
            }
        }
        return false;
    }
}
