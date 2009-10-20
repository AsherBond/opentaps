/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

/*
 *  $Id$
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
package com.opensourcestrategies.crmsfa.activities;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import com.opensourcestrategies.crmsfa.party.PartyHelper;

/**
 * Activities helper methods
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */

public class ActivitiesHelper {

    public static final String module = ActivitiesHelper.class.getName();
    public static final List ACTIVITY_WORKEFFORT_IDS = UtilMisc.toList( "TASK", "EVENT" ) ;
    public static final String crmsfaProperties = "crmsfa";

    /**
     * Retrieve the internal partyIds involved with a workEffort
     * @param workEffortId
     * @param delegator
     * @return List of partyIds
     */
    public static List findInternalWorkeffortPartyIds(String workEffortId, GenericDelegator delegator) {
        List workEffortRoles = UtilMisc.toList("CAL_OWNER", "CAL_ATTENDEE");
        List internalPartyIds = new ArrayList();
        try {
            List assignedParties = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toList(
                                                new EntityExpr("workEffortId", EntityOperator.EQUALS, workEffortId),
                                                new EntityExpr("roleTypeId", EntityOperator.IN, workEffortRoles),
                                                new EntityExpr("statusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED")));
            assignedParties = EntityUtil.filterByDate(assignedParties);
            List assignedPartyIds = EntityUtil.getFieldListFromEntityList(assignedParties, "partyId", true);
            if (assignedPartyIds != null) {
                Iterator apit = assignedPartyIds.iterator();
                while (apit.hasNext()) {
                    String partyId = (String) apit.next();
                    List conditions = UtilMisc.toList(new EntityExpr("partyId", EntityOperator.EQUALS, partyId), new EntityExpr("roleTypeId", EntityOperator.IN, PartyHelper.TEAM_MEMBER_ROLES));
                    List roles = delegator.findByCondition("PartyRole", new EntityConditionList(conditions, EntityOperator.AND), null, null);
                    if (roles != null && roles.size() > 0) {
                        internalPartyIds.add(partyId);
                    }
                }
            }
        } catch ( GenericEntityException e ) {
            Debug.logError(e, "Unable to retrieve internal workEffort roles for workEffort: " + workEffortId, module);
        }
        return internalPartyIds;
    }
    
    public static String getEmailSubjectCaseFormatRegExp() {
        String emailSubjectCaseFormatRegExp = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.case.emailSubjectCaseFormat.regExp", "\\\\[Case:(.*?)\\\\]");
        return emailSubjectCaseFormatRegExp.replaceAll("\\\\\\\\", "\\");
    }

    public static String getEmailSubjectOrderFormatRegExp() {
        String emailSubjectOrderFormatRegExp = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.order.emailSubjectOrderFormat.regExp", "\\\\[Order:(.*?)\\\\]");
        return emailSubjectOrderFormatRegExp.replaceAll("\\\\\\\\", "\\");
    }

    public static String getEmailSubjectCaseString(String caseId) {
        String emailSubjectCaseString = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.case.emailSubjectCaseFormat", "[Case:${caseId}]");
        return emailSubjectCaseString.replaceAll("\\$\\{caseId\\}", caseId);
    }

    public static String getEmailSubjectOrderString(String orderId) {
        String emailSubjectOrderString = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.order.emailSubjectOrderFormat", "[Order:${orderId}]");
        return emailSubjectOrderString.replaceAll("\\$\\{orderId\\}", orderId);
    }

    public static List getCustRequestIdsFromCommEvent( GenericValue communicationEvent, GenericDelegator delegator ) throws GenericEntityException {
        return getCustRequestIdsFromString(communicationEvent.getString("subject"), delegator);
    }
        
    public static List getCustRequestIdsFromString( String parseString, GenericDelegator delegator ) throws GenericEntityException {
        String getEmailSubjectCaseFormatRegExp = getEmailSubjectCaseFormatRegExp();
        Set custRequestIds = new TreeSet();

        if ( UtilValidate.isNotEmpty(parseString)) {
            Pattern pattern = Pattern.compile(getEmailSubjectCaseFormatRegExp);
            Matcher matcher = pattern.matcher(parseString);
            while (matcher.find()) {
                if (matcher.group(1) != null) custRequestIds.add(matcher.group(1));
            }
        }
        
        if (UtilValidate.isEmpty(custRequestIds)) return new ArrayList();
        
        // Filter the retrieved custRequestIds against existing CustRequest entities
        List custRequests = delegator.findByCondition("CustRequest", new EntityConditionList(UtilMisc.toList(new EntityExpr("custRequestId", EntityOperator.IN, custRequestIds)), EntityOperator.AND), null, null);
        List validCustRequestIds = EntityUtil.getFieldListFromEntityList(custRequests, "custRequestId", true);

        if (UtilValidate.isEmpty(custRequests)) return new ArrayList();

        return validCustRequestIds;
    }
        
    public static List getOrderIdsFromString(String parseString, GenericDelegator delegator) throws GenericEntityException {
        Set orderIds = new TreeSet();
        if ( UtilValidate.isNotEmpty(parseString)) {
            Pattern pattern = Pattern.compile(getEmailSubjectOrderFormatRegExp());
            Matcher matcher = pattern.matcher(parseString);
            while (matcher.find()) {
                if (matcher.group(1) != null) orderIds.add(matcher.group(1));
            }
        }
        
        if (UtilValidate.isEmpty(orderIds)) return new ArrayList();
        
        // Filter the retrieved orderIds against existing CustRequest entities
        List orderHeaders = delegator.findByCondition("OrderHeader", new EntityConditionList(UtilMisc.toList(new EntityExpr("orderId", EntityOperator.IN, orderIds)), EntityOperator.AND), null, null);
        List validOrderIds = EntityUtil.getFieldListFromEntityList(orderHeaders, "orderId", true);

        if (UtilValidate.isEmpty(orderHeaders)) return new ArrayList();

        return validOrderIds;
    }
}
