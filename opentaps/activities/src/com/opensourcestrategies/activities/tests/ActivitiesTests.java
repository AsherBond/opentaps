/*
 * Copyright (c) 2010 - 2011 Open Source Strategies, Inc.
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
package com.opensourcestrategies.activities.tests;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.constants.WorkEffortTypeConstants;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.DateDim;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.common.reporting.etl.UtilEtl;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Tests for the activities component and activities domain.
 */
public class ActivitiesTests extends OpentapsTestCase {

    private static final String MODULE = ActivitiesTests.class.getName();

    private DomainsLoader domainLoader = null;
    PartyDomainInterface act_domain = null;
    PartyRepositoryInterface act_rep = null;

    //private String workEffortId1 = "testWorkEffort1";
    //private String workEffortId2 = "testWorkEffort2";
    private Timestamp testTimestamp1 = Timestamp.valueOf("2010-06-14 12:56:00");
    private Timestamp testTimestamp2 = Timestamp.valueOf("2010-06-15 12:56:00");
    private String internalPartyId1 = "DemoSalesManager";
    private String internalPartyId2 = "DemoSalesRep1";
    private String externalPartyId1 = "DemoLead1";
    private String externalPartyId2 = "DemoLead2";
    private String testWorkEffortName = "testWorkEffortName";

    private DateFormat dayOfMonthFmt = new SimpleDateFormat("dd");
    private DateFormat monthOfYearFmt = new SimpleDateFormat("MM");
    private DateFormat yearNumberFmt = new SimpleDateFormat("yyyy");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(this.admin));
        act_domain = domainLoader.getDomainsDirectory().getPartyDomain();
        act_rep = act_domain.getPartyRepository();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        domainLoader = null;
        act_domain = null;
        act_rep = null;
    }

    /**
     * Performs data transformition to ActivityFacts table.
     * @exception Exception if an error occurs
     */
    public void testTransformToActivityFacts() throws Exception {

        //Get date dimention ID of work effort start date .

        String dayOfMonth = dayOfMonthFmt.format(testTimestamp2);
        String monthOfYear = monthOfYearFmt.format(testTimestamp2);
        String yearNumber = yearNumberFmt.format(testTimestamp2);

        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());

        // Add the first visit work effor data to tranform from.

        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_MEETING,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp1,
                "estimatedCompletionDate", testTimestamp2
                );
        Map results = this.runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId1 = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  externalPartyId1
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  externalPartyId2
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "partyId",  internalPartyId2
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId1,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_COMPLETED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_MEETING,
                "actualCompletionDate", testTimestamp2
                );
        this.runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation

        EntityCondition partiesCond1 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore1 = act_rep.findList(ActivityFact.class, partiesCond1);

        EntityCondition partiesCond2 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore2 = act_rep.findList(ActivityFact.class, partiesCond2);

        EntityCondition partiesCond3 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_ATTENDEE),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore3 = act_rep.findList(ActivityFact.class, partiesCond3);

        EntityCondition partiesCond4 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_ATTENDEE),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore4 = act_rep.findList(ActivityFact.class, partiesCond4);

        // Execute tranformition.

        args = UtilMisc.toMap("workEffortId", workEffortId1);
        this.runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts1 = act_rep.findList(ActivityFact.class, partiesCond1);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts1.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore1.size() > 0) {
            emailActivityCountBefore = factsBefore1.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore1.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore1.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore1.get(0).getOtherActivityCount();
        }

        ActivityFact fact1 = facts1.get(0);
        assertEquals("Email activity count is not good.", fact1.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact1.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact1.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact1.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts2 = act_rep.findList(ActivityFact.class, partiesCond2);

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts2.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore2.size() > 0) {
            emailActivityCountBefore = factsBefore2.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore2.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore2.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore2.get(0).getOtherActivityCount();
        }

        ActivityFact fact2 = facts2.get(0);
        assertEquals("Email activity count is not good.", fact2.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact2.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact2.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact2.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts3 = act_rep.findList(ActivityFact.class, partiesCond3);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, facts3.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore3.size() > 0) {
            emailActivityCountBefore = factsBefore3.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore3.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore3.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore3.get(0).getOtherActivityCount();
        }

        ActivityFact fact3 = facts3.get(0);
        assertEquals("Email activity count is not good.", fact3.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact3.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact3.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact3.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        List<ActivityFact> facts4 = act_rep.findList(ActivityFact.class, partiesCond4);

        assertEquals("Record with target party [" + externalPartyId2 + "], team party [" + internalPartyId2 + "] not added to ActivityFact entity.", 1, facts4.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore4.size() > 0) {
            emailActivityCountBefore = factsBefore4.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore4.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore4.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore4.get(0).getOtherActivityCount();
        }

        ActivityFact fact4 = facts4.get(0);
        assertEquals("Email activity count is not good.", fact4.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact4.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact4.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact4.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

        //Add the second work effor data to tranform from.

        args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        results = this.runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId2 = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId2,
                "partyId",  externalPartyId1
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId2,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_COMPLETED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        this.runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation
        EntityCondition partiesCond5 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore5 = act_rep.findList(ActivityFact.class, partiesCond5);

        //Execute tranformition.
        args = UtilMisc.toMap("workEffortId", workEffortId2);
        this.runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts5 = act_rep.findList(ActivityFact.class, partiesCond5);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts5.size());

        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if (factsBefore5.size() > 0) {
            emailActivityCountBefore = factsBefore5.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore5.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore5.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore5.get(0).getOtherActivityCount();
        }

        ActivityFact fact5 = facts5.get(0);
        assertEquals("Email activity count is not good.", fact5.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact5.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact5.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact5.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

        //==================================

    }

    /**
     * Testing transformation of cancelled activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformCancelledActivities() throws Exception {

        //Get date dimention ID of work effort start date .

        String dayOfMonth = dayOfMonthFmt.format(testTimestamp2);
        String monthOfYear = monthOfYearFmt.format(testTimestamp2);
        String yearNumber = yearNumberFmt.format(testTimestamp2);

        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());

        //Add the second work effor data to tranform from.

        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_STARTED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        Map results = this.runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "partyId",  externalPartyId1
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_CANCELLED,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        this.runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);

        //Execute tranformition.
        args = UtilMisc.toMap("workEffortId", workEffortId);
        this.runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        //==================================

        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " +
                     "because work effor is chancelled.", factsBefore.size(), facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

    }

    /**
     * Testing transformation of pending activities.
     * @exception Exception if an error occurs
     */
    public void testNotTransformPendingActivities() throws Exception {

        //Get date dimention ID of work effort start date .

        String dayOfMonth = dayOfMonthFmt.format(testTimestamp2);
        String monthOfYear = monthOfYearFmt.format(testTimestamp2);
        String yearNumber = yearNumberFmt.format(testTimestamp2);

        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());

        //Add the second work effor data to tranform from.

        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap("userLogin", userLogin,
                "availabilityStatusId", StatusItemConstants.WepaAvailability.WEPA_AV_AVAILABLE,
                "forceIfConflicts",  "Y",
                "workEffortName",  testWorkEffortName,
                "workEffortTypeId", WorkEffortTypeConstants.TASK,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_SCHEDULED,
                "estimatedStartDate", testTimestamp2,
                "estimatedCompletionDate", testTimestamp2
                );
        Map results = this.runAndAssertServiceSuccess("crmsfa.createActivity", args);
        String workEffortId = (String) results.get(WorkEffort.Fields.workEffortId.name());

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "partyId",  externalPartyId1
                );
        this.runAndAssertServiceSuccess("crmsfa.addWorkEffortPartyAssignment", args);

        args = UtilMisc.toMap("userLogin", userLogin,
                "workEffortId", workEffortId,
                "currentStatusId", StatusItemConstants.TaskStatus.TASK_ON_HOLD,
                "workEffortPurposeTypeId", WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL,
                "actualCompletionDate", testTimestamp2
                );
        this.runAndAssertServiceSuccess("crmsfa.updateActivityWithoutAssoc", args);

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);

        //Execute tranformition.
        args = UtilMisc.toMap("workEffortId", workEffortId);
        this.runAndAssertServiceSuccess("activities.transformToActivityFacts", args);

        // Check if proper records was found.

        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] must be not added to ActivityFact entity, " + "because work effort is pending.", factsBefore.size(), facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));

    }

    /**
     * Testing transformation after loging task.
     * @exception Exception if an error occurs
     */
    public void testTransformLogTaskActivity() throws Exception {

        //Get date dimention ID of work effort start date .

        String dayOfMonth = dayOfMonthFmt.format(UtilDateTime.nowTimestamp());
        String monthOfYear = monthOfYearFmt.format(UtilDateTime.nowTimestamp());
        String yearNumber = yearNumberFmt.format(UtilDateTime.nowTimestamp());

        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);

        // Call logTask service.

        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap("userLogin", userLogin,
                "internalPartyId", this.externalPartyId1,
                "fromPartyId", this.internalPartyId1 ,
                "outbound", "N" ,
                "workEffortName", testWorkEffortName);
        this.runAndAssertServiceSuccess("crmsfa.logTask", args);

        // Check if proper records was found.

        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(1 + otherActivityCountBefore));

    }

    /**
     * Testing transformation after sending an email.
     * @exception Exception if an error occurs
     */
    public void testTransformSendEmailActivity() throws Exception {

        //Get date dimention ID of work effort start date .

        String dayOfMonth = dayOfMonthFmt.format(UtilDateTime.nowTimestamp());
        String monthOfYear = monthOfYearFmt.format(UtilDateTime.nowTimestamp());
        String yearNumber = yearNumberFmt.format(UtilDateTime.nowTimestamp());

        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());

        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.LEAD),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.CAL_OWNER),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);

        // Call sen email service.

        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap("userLogin", userLogin,
                "contactMechIdFrom", "DemoMgrEmail1",
                "toEmail", "demo@demolead1.com",
                "content", "test content",
                "contentMimeTypeId", "text/plain",
                "subject", "test subject"
                );
        Map<String, Object> results = this.runAndAssertServiceSuccess("crmsfa.sendActivityEmail", args);

        String workEffortId = (String) results.get("workEffortId");
        Debug.logImportant("send email workEffortId = " + workEffortId, MODULE);

        // Check if proper records was found.

        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);

        assertEquals("Record with target party [" + externalPartyId1 + "], team party [" + internalPartyId1 + "] not added to ActivityFact entity.", 1, facts.size());

        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if (factsBefore.size() > 0) {
            emailActivityCountBefore = factsBefore.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore.get(0).getOtherActivityCount();
        }

        ActivityFact fact = facts.get(0);
        assertEquals("Email activity count is not good.", fact.getEmailActivityCount(), Long.valueOf(1 + emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
    }


}
