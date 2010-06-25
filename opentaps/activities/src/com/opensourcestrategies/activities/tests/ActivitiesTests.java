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
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.DateDim;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortPartyAssignment;
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
public class ActivitiesTests extends OpentapsTestCase{
    
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
    private String internalPartyId3 = "DemoSalesRep2";
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
        /*
        //Clean up work effor data.        
        String id_suffix = "testWorkEffort%";
        act_rep.remove(act_rep.findList(WorkEffortKeyword.class, EntityCondition.makeCondition(WorkEffortKeyword.Fields.workEffortId.name(), EntityOperator.LIKE, id_suffix)));
        act_rep.remove(act_rep.findList(WorkEffortPartyAssignment.class, EntityCondition.makeCondition(WorkEffortPartyAssignment.Fields.workEffortId.name(), EntityOperator.LIKE, id_suffix)));
        act_rep.remove(act_rep.findList(WorkEffort.class, EntityCondition.makeCondition(WorkEffort.Fields.workEffortId.name(), EntityOperator.LIKE, id_suffix)));        
        
        //Clean up activity fact data.
        id_suffix = "Demo%";
        act_rep.remove(act_rep.findList(ActivityFact.class, EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.LIKE, id_suffix)));
        */
        
        super.tearDown();
        domainLoader = null;
        act_domain = null;
        act_rep = null;
    }
    
    /**
     * Performs data transformition to ActivityFacts table.
     * @throws java.lang.Exception
     */
    public void testTransformToAcitivityFacts() throws Exception{
        
        //Get date dimention ID of work effort start date .
        
        String dayOfMonth = dayOfMonthFmt.format(testTimestamp1);
        String monthOfYear = monthOfYearFmt.format(testTimestamp1);
        String yearNumber = yearNumberFmt.format(testTimestamp1);
        
        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));
        
        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());    
        
        //Add the first work effor data to tranform from.
        
        // FIXME: use crmsfa.createActivity and addWorkEffortPartyAssignment service
        String workEffortId1 = act_rep.getNextSeqId(WorkEffort.class.getSimpleName());
        
        WorkEffort weffort1 = new WorkEffort();
        weffort1.setWorkEffortId(workEffortId1);
        weffort1.setActualCompletionDate(testTimestamp1);
        weffort1.setWorkEffortPurposeTypeId(WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL);
        weffort1.setCurrentStatusId(StatusItemConstants.TaskStatus.TASK_COMPLETED);
        act_rep.createOrUpdate(weffort1);
        
        WorkEffortPartyAssignment weassgn11 = new WorkEffortPartyAssignment();
        weassgn11.setWorkEffortId(workEffortId1);
        weassgn11.setPartyId(externalPartyId1);
        weassgn11.setRoleTypeId(RoleTypeConstants.ACCOUNT);
        weassgn11.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn11);
        
        WorkEffortPartyAssignment weassgn12 = new WorkEffortPartyAssignment();
        weassgn12.setWorkEffortId(workEffortId1);
        weassgn12.setPartyId(internalPartyId1);
        weassgn12.setRoleTypeId(RoleTypeConstants.CAL_OWNER);
        weassgn12.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn12);
        
        WorkEffortPartyAssignment weassgn13 = new WorkEffortPartyAssignment();
        weassgn13.setWorkEffortId(workEffortId1);
        weassgn13.setPartyId(internalPartyId2);
        weassgn13.setRoleTypeId(RoleTypeConstants.CAL_ATTENDEE);
        weassgn13.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn13);
        
        WorkEffortPartyAssignment weassgn15 = new WorkEffortPartyAssignment();
        weassgn15.setWorkEffortId(workEffortId1);
        weassgn15.setPartyId(externalPartyId2);
        weassgn15.setRoleTypeId(RoleTypeConstants.PROSPECT);
        weassgn15.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn15);

        // Look up activity fact's before transformation
        
        EntityCondition partiesCond1 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore1 = act_rep.findList(ActivityFact.class, partiesCond1);
        
        EntityCondition partiesCond2 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore2 = act_rep.findList(ActivityFact.class, partiesCond2);
        
        EntityCondition partiesCond3 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore3 = act_rep.findList(ActivityFact.class, partiesCond3);
        
        EntityCondition partiesCond4 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId2),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore4 = act_rep.findList(ActivityFact.class, partiesCond4);
        
        // Execute tranformition.
        
        Map args = UtilMisc.toMap("workEffortId", workEffortId1);
        this.runAndAssertServiceSuccess("activities.transformToAcitivityFacts", args);  
        
        // Check if proper records was found.
        
        //==================================
        
        List<ActivityFact> facts1 = act_rep.findList(ActivityFact.class, partiesCond1);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts1.size());
        
        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if(factsBefore1.size() > 0){
            emailActivityCountBefore = factsBefore1.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore1.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore1.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore1.get(0).getOtherActivityCount();
        }
        
        ActivityFact fact1 = facts1.get(0);
        assertEquals("Email activity count is not good.", fact1.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact1.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact1.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact1.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        
        //==================================
        
        List<ActivityFact> facts2 = act_rep.findList(ActivityFact.class, partiesCond2);
        
        assertEquals("Record with target party ["+externalPartyId2+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts2.size());
        
        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if(factsBefore2.size() > 0){
            emailActivityCountBefore = factsBefore2.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore2.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore2.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore2.get(0).getOtherActivityCount();
        }
        
        ActivityFact fact2 = facts2.get(0);
        assertEquals("Email activity count is not good.", fact2.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact2.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact2.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact2.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        
        //==================================
        
        List<ActivityFact> facts3 = act_rep.findList(ActivityFact.class, partiesCond3);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId2+"] not added to ActivityFact entity.", 1, facts3.size());
        
        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if(factsBefore3.size() > 0){
            emailActivityCountBefore = factsBefore3.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore3.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore3.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore3.get(0).getOtherActivityCount();
        }
        
        ActivityFact fact3 = facts3.get(0);
        assertEquals("Email activity count is not good.", fact3.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact3.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact3.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact3.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        
        //==================================
        
        List<ActivityFact> facts4 = act_rep.findList(ActivityFact.class, partiesCond4);
        
        assertEquals("Record with target party ["+externalPartyId2+"], team party ["+internalPartyId2+"] not added to ActivityFact entity.", 1, facts4.size());
        
        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if(factsBefore4.size() > 0){
            emailActivityCountBefore = factsBefore4.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore4.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore4.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore4.get(0).getOtherActivityCount();
        }
        
        ActivityFact fact4 = facts4.get(0);
        assertEquals("Email activity count is not good.", fact4.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact4.getPhoneCallActivityCount(), Long.valueOf(1 + phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact4.getVisitActivityCount(), Long.valueOf(visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact4.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        
        //==================================
        
        //Add the second work effor data to tranform from.
     
        // FIXME: use crmsfa.createActivity and addWorkEffortPartyAssignment service
        String workEffortId2 = act_rep.getNextSeqId(WorkEffort.class.getSimpleName());
        
        WorkEffort weffort2 = new WorkEffort();
        weffort2.setWorkEffortId(workEffortId2);
        weffort2.setActualCompletionDate(testTimestamp1);
        weffort2.setWorkEffortPurposeTypeId(WorkEffortPurposeTypeConstants.WEPT_MEETING);
        weffort2.setCurrentStatusId(StatusItemConstants.EventStatus.EVENT_COMPLETED);
        act_rep.createOrUpdate(weffort2);
        
        WorkEffortPartyAssignment weassgn21 = new WorkEffortPartyAssignment();
        weassgn21.setWorkEffortId(workEffortId2);
        weassgn21.setPartyId(externalPartyId1);
        weassgn21.setRoleTypeId(RoleTypeConstants.PROSPECT);
        weassgn21.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn21);
        
        WorkEffortPartyAssignment weassgn22 = new WorkEffortPartyAssignment();
        weassgn22.setWorkEffortId(workEffortId2);
        weassgn22.setPartyId(internalPartyId1);
        weassgn22.setRoleTypeId(RoleTypeConstants.VISITOR);
        weassgn22.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn22);
        
        // Look up activity fact's before transformation
        EntityCondition partiesCond5 = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore5 = act_rep.findList(ActivityFact.class, partiesCond5);
        
        //Execute tranformition.
        args = UtilMisc.toMap("workEffortId", workEffortId2);
        this.runAndAssertServiceSuccess("activities.transformToAcitivityFacts", args);
        
        // Check if proper records was found.
        
        //==================================
        
        List<ActivityFact> facts5 = act_rep.findList(ActivityFact.class, partiesCond5);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts5.size());
        
        emailActivityCountBefore = 0;
        phoneCallActivityCountBefore = 0;
        visitActivityCountBefore = 0;
        otherActivityCountBefore = 0;
        if(factsBefore5.size() > 0){
            emailActivityCountBefore = factsBefore5.get(0).getEmailActivityCount();
            phoneCallActivityCountBefore = factsBefore5.get(0).getPhoneCallActivityCount();
            visitActivityCountBefore = factsBefore5.get(0).getVisitActivityCount();
            otherActivityCountBefore = factsBefore5.get(0).getOtherActivityCount();
        }
        
        ActivityFact fact5 = facts5.get(0);
        assertEquals("Email activity count is not good.", fact5.getEmailActivityCount(), Long.valueOf(emailActivityCountBefore));
        assertEquals("Phone activity count is not good.", fact5.getPhoneCallActivityCount(), Long.valueOf(phoneCallActivityCountBefore));
        assertEquals("Visit activity count is not good.", fact5.getVisitActivityCount(), Long.valueOf(1 + visitActivityCountBefore));
        assertEquals("Other activity count is not good.", fact5.getOtherActivityCount(), Long.valueOf(otherActivityCountBefore));
        
        //==================================
         
    }
    
    /**
     * Testing transformation of cancelled activities.
     * @throws java.lang.Exception
     */
    public void testNotTransformCancelledActivities() throws Exception{
        
        //Get date dimention ID of work effort start date .
        
        String dayOfMonth = dayOfMonthFmt.format(testTimestamp1);
        String monthOfYear = monthOfYearFmt.format(testTimestamp1);
        String yearNumber = yearNumberFmt.format(testTimestamp1);
        
        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));
        
        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());    
        
        //Add the second work effor data to tranform from.
        
        String workEffortId = act_rep.getNextSeqId(WorkEffort.class.getSimpleName());
        
        WorkEffort weffort = new WorkEffort();
        weffort.setWorkEffortId(workEffortId);
        weffort.setActualCompletionDate(testTimestamp1);
        weffort.setWorkEffortPurposeTypeId(WorkEffortPurposeTypeConstants.WEPT_TASK_EMAIL);
        weffort.setCurrentStatusId(StatusItemConstants.TaskStatus.TASK_CANCELLED);
        act_rep.createOrUpdate(weffort);
        
        WorkEffortPartyAssignment weassgn1 = new WorkEffortPartyAssignment();
        weassgn1.setWorkEffortId(workEffortId);
        weassgn1.setPartyId(externalPartyId1);
        weassgn1.setRoleTypeId(RoleTypeConstants.PROSPECT);
        weassgn1.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn1);
        
        WorkEffortPartyAssignment weassgn2 = new WorkEffortPartyAssignment();
        weassgn2.setWorkEffortId(workEffortId);
        weassgn2.setPartyId(internalPartyId1);
        weassgn2.setRoleTypeId(RoleTypeConstants.VISITOR);
        weassgn2.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn2);
        
        // Look up activity fact's before transformation
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);
        
        //Execute tranformition.
        Map args = UtilMisc.toMap("workEffortId", workEffortId);
        this.runAndAssertServiceSuccess("activities.transformToAcitivityFacts", args);
        
        // Check if proper records was found.
        
        //==================================
        
        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts.size());
        
        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if(factsBefore.size() > 0){
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
     * @throws java.lang.Exception
     */
    public void testNotTransformPendingActivities() throws Exception{
        
        //Get date dimention ID of work effort start date .
        
        String dayOfMonth = dayOfMonthFmt.format(testTimestamp1);
        String monthOfYear = monthOfYearFmt.format(testTimestamp1);
        String yearNumber = yearNumberFmt.format(testTimestamp1);
        
        EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
        EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
        EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));
        
        Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_domain.getInfrastructure().getDelegator());    
        
        //Add the second work effor data to tranform from.
        
        String workEffortId = act_rep.getNextSeqId(WorkEffort.class.getSimpleName());
        
        WorkEffort weffort = new WorkEffort();
        weffort.setWorkEffortId(workEffortId);
        weffort.setActualCompletionDate(testTimestamp1);
        weffort.setWorkEffortPurposeTypeId(WorkEffortPurposeTypeConstants.WEPT_MEETING);
        weffort.setCurrentStatusId(StatusItemConstants.ComEventStatus.COM_PENDING);
        act_rep.createOrUpdate(weffort);
        
        WorkEffortPartyAssignment weassgn1 = new WorkEffortPartyAssignment();
        weassgn1.setWorkEffortId(workEffortId);
        weassgn1.setPartyId(externalPartyId1);
        weassgn1.setRoleTypeId(RoleTypeConstants.PROSPECT);
        weassgn1.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn1);
        
        WorkEffortPartyAssignment weassgn2 = new WorkEffortPartyAssignment();
        weassgn2.setWorkEffortId(workEffortId);
        weassgn2.setPartyId(internalPartyId1);
        weassgn2.setRoleTypeId(RoleTypeConstants.VISITOR);
        weassgn2.setFromDate(testTimestamp2);
        act_rep.createOrUpdate(weassgn2);
        
        // Look up activity fact's before transformation.
        EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
        EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, externalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internalPartyId1),
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);
        
        //Execute tranformition.
        Map args = UtilMisc.toMap("workEffortId", workEffortId);
        this.runAndAssertServiceSuccess("activities.transformToAcitivityFacts", args);
        
        // Check if proper records was found.
        
        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts.size());
        
        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if(factsBefore.size() > 0){
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
     *  Testing transformation after loging task.
     * @throws java.lang.Exception
     */
    public void testTransformLogTaskActivity() throws Exception{
        
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
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);
        
        // Call logTask service.
        
        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());
        
        Map args = UtilMisc.toMap( "userLogin", userLogin,
                "internalPartyId", this.externalPartyId1, 
                "fromPartyId", this.internalPartyId1 , 
                "outbound", "N" , 
                "workEffortName", testWorkEffortName);
        this.runAndAssertServiceSuccess("crmsfa.logTask", args);
        
        // Check if proper records was found.
        
        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts.size());
        
        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if(factsBefore.size() > 0){
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
     * @throws java.lang.Exception
     */
    public void testTransformSendEmailActivity() throws Exception{
        
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
        EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
        List<ActivityFact> factsBefore = act_rep.findList(ActivityFact.class, partiesCond);

        // Call sen email service.
        
        UserLogin user = act_rep.findOne(UserLogin.class, act_rep.map(UserLogin.Fields.userLoginId, this.internalPartyId1));
        GenericValue userLogin = act_rep.getInfrastructure().getDelegator().makeValue(UserLogin.class.getSimpleName(), user.toMap());

        Map args = UtilMisc.toMap( "userLogin", userLogin,
                "contactMechIdFrom", "DemoMgrEmail1",
                "toEmail", "demo@demolead1.com",
                "content", "test content",
                "contentMimeTypeId", "text/plain",
                "subject", "test subject"
                );
        Map<String, Object> results = this.runAndAssertServiceSuccess("crmsfa.sendActivityEmail", args);
        
        String workEffortId = (String) results.get("workEffortId");
        Debug.logImportant("send email workEffortId = "+workEffortId, MODULE);
        
        // Check if proper records was found.
        
        List<ActivityFact> facts = act_rep.findList(ActivityFact.class, partiesCond);
        
        assertEquals("Record with target party ["+externalPartyId1+"], team party ["+internalPartyId1+"] not added to ActivityFact entity.", 1, facts.size());
        
        long emailActivityCountBefore = 0;
        long phoneCallActivityCountBefore = 0;
        long visitActivityCountBefore = 0;
        long otherActivityCountBefore = 0;
        if(factsBefore.size() > 0){
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
