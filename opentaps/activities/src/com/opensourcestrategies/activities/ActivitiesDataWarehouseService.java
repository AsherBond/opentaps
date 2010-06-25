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
package com.opensourcestrategies.activities;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortPartyAssignment;
import org.opentaps.common.reporting.etl.UtilEtl;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.entities.DateDim;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.service.Service;

/**
 * Do data warehouse operations for activities.
 */
public class ActivitiesDataWarehouseService extends Service{
    
    private static final String MODULE = ActivitiesDataWarehouseService.class.getName();
    private DomainsLoader domainLoader = new DomainsLoader(this.getInfrastructure(), this.getUser());
    public static List<String> CLIENT_PARTY_ROLES = UtilMisc.toList(RoleTypeConstants.ACCOUNT, 
                                                                    RoleTypeConstants.CONTACT, 
                                                                    RoleTypeConstants.PROSPECT, 
                                                                    RoleTypeConstants.PARTNER);
    private String workEffortId;
    
    public ActivitiesDataWarehouseService() {
        super();
    }

    public ActivitiesDataWarehouseService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    /**
     * Sets the required input parameter for service {@link #transformToAcitivityFacts}.
     * @param workEffortId the ID of work effort
     */
    public void setWorkEffortId(String workEffortId) {
        this.workEffortId = workEffortId;
    }
    
    /**
     * Transformation which transforms data from WorkEffort, WorkEffortPartyAssign entities to ActivitiesFact entity.
     * It expands the WorkEffortPartyAssign to covers all target parties for all team members.
     * The counts that is in ActivitiesFacts is based on the purpose of the WorkEffort.
     * @throws org.opentaps.foundation.service.ServiceException
     */
    public void transformToAcitivityFacts() throws ServiceException{
        try {
            if(workEffortId == null){
                throw new ServiceException("Variable workEffortId is empty. Use set method to initialize it.");
            }

            PartyRepositoryInterface act_rep = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository();

            //Get WorkEffortPartyAssign and WorkEffort data by workEffortId.
            
            WorkEffort workEffort = act_rep.findOne(WorkEffort.class, act_rep.map(WorkEffort.Fields.workEffortId, workEffortId));
            List<WorkEffortPartyAssignment> assgmnts = act_rep.findList(WorkEffortPartyAssignment.class, 
                                act_rep.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId));
            
            // Pass only completed workEfforts to do transformation.
            
            EntityCondition completedCond = EntityCondition.makeCondition(EntityOperator.OR, 
                    EntityCondition.makeCondition(WorkEffort.Fields.currentStatusId.name(), EntityOperator.EQUALS, StatusItemConstants.TaskStatus.TASK_COMPLETED),
                     EntityCondition.makeCondition(WorkEffort.Fields.currentStatusId.name(), EntityOperator.EQUALS, StatusItemConstants.EventStatus.EVENT_COMPLETED));
            EntityCondition isWfCompleted = EntityCondition.makeCondition(EntityOperator.AND,
                    completedCond,
                    EntityCondition.makeCondition(WorkEffort.Fields.workEffortId.name(), EntityOperator.EQUALS, workEffort.getWorkEffortId()));
            List<WorkEffort> completed = act_rep.findList(WorkEffort.class, isWfCompleted);
            if(completed.size() == 0){
                return;
            }
            
            //Fill 2 lists according to assigment of work effort to team members (internal parties) and clients (external parties).
            
            List<WorkEffortPartyAssignment> internal_parties_assgmn = new ArrayList<WorkEffortPartyAssignment>();
            List<WorkEffortPartyAssignment> external_parties_assgmn = new ArrayList<WorkEffortPartyAssignment>();
            for(int i = 0; i < assgmnts.size(); i++){
                WorkEffortPartyAssignment ass = assgmnts.get(i);
                boolean is_external = false;
                
                int j = 0;
                while((is_external == false)&(j < CLIENT_PARTY_ROLES.size())){
                    if(CLIENT_PARTY_ROLES.get(j).compareTo(ass.getRoleTypeId()) == 0){
                        is_external = true;
                    }
                    j++;
                }
                
                if(is_external == true){
                    external_parties_assgmn.add(ass);
                }else{
                    internal_parties_assgmn.add(ass);
                }
            }

            //Get date dimension ID according to the work effort start date.
            
            UtilEtl.setupDateDimension(act_rep.getInfrastructure().getDelegator(), timeZone, locale);
            Timestamp workEffortDate = workEffort.getActualCompletionDate();
            
            DateFormat dayOfMonthFmt = new SimpleDateFormat("dd");
            DateFormat monthOfYearFmt = new SimpleDateFormat("MM");
            DateFormat yearNumberFmt = new SimpleDateFormat("yyyy");
            String dayOfMonth = dayOfMonthFmt.format(workEffortDate);
            String monthOfYear = monthOfYearFmt.format(workEffortDate);
            String yearNumber = yearNumberFmt.format(workEffortDate);

            EntityCondition dateDimConditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition(DateDim.Fields.dayOfMonth.name(), dayOfMonth),
            EntityCondition.makeCondition(DateDim.Fields.monthOfYear.name(), monthOfYear),
            EntityCondition.makeCondition(DateDim.Fields.yearNumber.name(), yearNumber));

            Long dateDimId = UtilEtl.lookupDimension(DateDim.class.getSimpleName(), DateDim.Fields.dateDimId.getName(), dateDimConditions, act_rep.getInfrastructure().getDelegator());
            
            // Associate all team member with clients (add this association if it is not in the place) 
            // and increase count according to WorkEffor workEffortPurposeTypeId.
            
            for(int i = 0; i< external_parties_assgmn.size(); i++){
                WorkEffortPartyAssignment external = external_parties_assgmn.get(i);
                for(int j = 0; j < internal_parties_assgmn.size(); j++){
                    WorkEffortPartyAssignment internal = internal_parties_assgmn.get(j);
                    
                    Debug.logInfo(i+" service internal = "+internal, MODULE);
                    
                    //Try to find ActivityFact with such target party id and member party id and date dimension combination. 
                    // If not such, then create it.
                    
                    EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.EQUALS, external.getPartyId()),
                    EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), EntityOperator.EQUALS, internal.getPartyId()),
                    EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), EntityOperator.EQUALS, dateDimId));
                    List<ActivityFact> activityFacts = act_rep.findList(ActivityFact.class, partiesCond);
                    
                    ActivityFact activityFact = null;
                    if(activityFacts.size() > 0){
                        activityFact = activityFacts.get(0);
                    }else{
                        activityFact = new ActivityFact();
                        activityFact.setActivityFactId(act_rep.getInfrastructure().getSession().getNextSeqId(ActivityFact.class.getSimpleName()));
                        activityFact.setTargetPartyId(external.getPartyId());
                        activityFact.setTeamMemberPartyId(internal.getPartyId());
                        activityFact.setDateDimId(dateDimId);
                        activityFact.setEmailActivityCount(Long.valueOf(0));
                        activityFact.setPhoneCallActivityCount(Long.valueOf(0));
                        activityFact.setVisitActivityCount(Long.valueOf(0));
                        activityFact.setOtherActivityCount(Long.valueOf(0));
                    }
                    
                    // Increase count according to WorkEffor workEffortPurposeTypeId.
                    
                    String purpose = workEffort.getWorkEffortPurposeTypeId();                    
                    if(purpose == null){
                        activityFact.setOtherActivityCount(activityFact.getOtherActivityCount()+1);
                    }else
                    if(purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_EMAIL) == 0){
                        activityFact.setEmailActivityCount(activityFact.getEmailActivityCount()+1);
                    }else
                    if(purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL) == 0){
                        activityFact.setPhoneCallActivityCount(activityFact.getPhoneCallActivityCount()+1);
                    }else
                    if(purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_MEETING) == 0){
                        activityFact.setVisitActivityCount(activityFact.getVisitActivityCount()+1);
                    }else{
                        activityFact.setOtherActivityCount(activityFact.getOtherActivityCount()+1);
                    }
                    
                    act_rep.createOrUpdate(activityFact);
                    
                    Debug.logInfo("ActivityFact entity record ["+activityFact.getActivityFactId()+"] created/updated.", MODULE);
                }
            }
            
        } catch (RepositoryException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } catch (GenericEntityException ex){
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } catch (InfrastructureException ex){
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        }
    }

}



















