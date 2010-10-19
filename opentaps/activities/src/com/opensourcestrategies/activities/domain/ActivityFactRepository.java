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
package com.opensourcestrategies.activities.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.common.reporting.etl.UtilEtl;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class ActivityFactRepository extends Repository implements ActivityFactRepositoryInterface {

    private static final String MODULE = ActivityFactRepository.class.getName();

    private String targetRoleTypeId = null;
    private Set<String> allowedTargetPartyIds = null;
    private long dateDimId = 0;
    private String teamMemberPartyId = null;
    private String targetPartyId = null;
    private String teamMemberRoleTypeId = null;

    private List<ActivityFact> listActivityFact = null;

    /**
     * Default constructor.
     */
    public ActivityFactRepository() {
        super();
    }

    /** {@inheritDoc} */
    public void setTargetRoleTypeId(String roleTypeId) {
        targetRoleTypeId = roleTypeId;
    }

    /** {@inheritDoc} */
    public void setAllowedTargetPartyIds(Set<String> partyIds) {
        allowedTargetPartyIds = partyIds;
    }

    /** {@inheritDoc} */
    public void setDateDimensionId(long dateDimId) {
        this.dateDimId = dateDimId;
    }

    /** {@inheritDoc} */
    public void setTeamMemberPartyId(String partyId) {
        teamMemberPartyId = partyId;
    }

    /** {@inheritDoc} */
    public void setTargetPartyId(String partyId) {
        targetPartyId = partyId;
    }

    /** {@inheritDoc} */
    public void setTeamMemeberRoleTypeId(String memberRoleTypeId) {
        this.teamMemberRoleTypeId = memberRoleTypeId;
    }

    /** {@inheritDoc} */
    public List<ActivityFact> findActivityFacts() throws RepositoryException {
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());

        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), targetPartyId),
                EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), teamMemberPartyId),
                EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), targetRoleTypeId),
                EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), teamMemberRoleTypeId)
                //EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId)
                );
        listActivityFact = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository().findList(ActivityFact.class, condition);

        return listActivityFact;
    }

    /** {@inheritDoc} */
    public Map<String, List<ActivityFact>> findLeadsActivitiesGroupedBy(ActivityFact.Fields groupedByField) throws RepositoryException {
        List<ActivityFact> prospectActivityFacts = null;
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());

        EntityCondition condition = EntityCondition.makeCondition(
                ActivityFact.Fields.targetPartyRoleTypeId.name(), targetRoleTypeId);
        if (allowedTargetPartyIds != null) {
            condition = EntityCondition.makeCondition(condition,
                    EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.IN, allowedTargetPartyIds));
        }

        prospectActivityFacts = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository().findList(
                ActivityFact.class, condition, Arrays.asList(ActivityFact.Fields.targetPartyId.name(),
                    ActivityFact.Fields.dateDimId.name()), Arrays.asList(ActivityFact.Fields.targetPartyId.asc(),
                            ActivityFact.Fields.dateDimId.desc()));

        return Entity.groupByFieldValues(String.class, prospectActivityFacts, groupedByField);
    }

    /** {@inheritDoc} */
    public void createActivityFact(String teamMemberPartyId, String targetPartyId, String teamMemberRoleTypeId, String targetRoleTypeId, Activity activity, int count) throws RepositoryException {
        ActivityFact activityFact = new ActivityFact();
        try {

            DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());
            PartyRepositoryInterface partyRepository = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository();

            Long dateDimId = null;
            dateDimId = UtilEtl.lookupDateDimensionForTimestamp(UtilDateTime.nowTimestamp(), partyRepository.getInfrastructure().getDelegator());

            activityFact.setActivityFactId(this.getNextSeqId(activityFact));
            activityFact.setTargetPartyId(targetPartyId);
            activityFact.setTeamMemberPartyId(teamMemberPartyId);
            activityFact.setDateDimId(dateDimId);
            activityFact.setTargetPartyRoleTypeId(targetRoleTypeId);
            activityFact.setTeamMemberPartyRoleTypeId(teamMemberRoleTypeId);

            activityFact.setEmailActivityCount(Long.valueOf(0));
            activityFact.setPhoneCallActivityCount(Long.valueOf(0));
            activityFact.setVisitActivityCount(Long.valueOf(0));
            activityFact.setOtherActivityCount(Long.valueOf(0));
            activityFact.setActivityCompletedDatetime(activity.getActualCompletionDate());

            // Increase count according to WorkEffort workEffortPurposeTypeId.
            String purpose = activity.getWorkEffortPurposeTypeId();

            if (purpose == null) {
                activityFact.setOtherActivityCount(Long.valueOf(count));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_EMAIL) == 0) {
                activityFact.setEmailActivityCount(Long.valueOf(count));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL) == 0) {
                activityFact.setPhoneCallActivityCount(Long.valueOf(count));
            } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_MEETING) == 0) {
                activityFact.setVisitActivityCount(Long.valueOf(count));
            } else {
                activityFact.setOtherActivityCount(Long.valueOf(count));
            }

            partyRepository.createOrUpdate(activityFact);

            Debug.logInfo("ActivityFact entity record [" + activityFact.getActivityFactId() + "] created/updated.", MODULE);

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new RepositoryException(e);
        }

    }

    /** {@inheritDoc} */
    public long getEmailActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        for(ActivityFact activityFact : listActivityFact ) {
            count += activityFact.getEmailActivityCount();
        }
        return count;
    }

    /** {@inheritDoc} */
    public long getPhoneCallActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        for(ActivityFact activityFact : listActivityFact ) {
            count += activityFact.getPhoneCallActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getVisitActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        for(ActivityFact activityFact : listActivityFact ) {
            count += activityFact.getVisitActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getOtherActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        for(ActivityFact activityFact : listActivityFact ) {
            count += activityFact.getOtherActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getTotalActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        for(ActivityFact activityFact : listActivityFact ) {
            count += activityFact.getEmailActivityCount()+
                     activityFact.getPhoneCallActivityCount()+
                     activityFact.getVisitActivityCount()+
                     activityFact.getOtherActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public void transformToActivityFacts(Activity activity, List<Party> participants, int count) throws RepositoryException {
        try {
            DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());
            PartyRepositoryInterface repository = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository();

            List<Party> externalParty = new ArrayList<Party>();
            List<Party> internalParty = new ArrayList<Party>();

            if(participants != null) {
                for(Party party : participants) {
                    // Note: a party can be both internal and external
                    //   in case of multi-tenant setup there is a case
                    //   where A B X Y are involved in a WorkEffort; A and B being supposed to be
                    //   internal (as in two sales rep) but B would be considered external if
                    //   he is a contact somewhere else.
                    //   All parties could be both have the contact role and be an internal user.
                    boolean isInternal = false; // is the party a user of the system
                    boolean isExternal = false; // is the party a CRM party

                    // always consider the current user as internal
                    if (party.getPartyId().equals(getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name()))) {
                        isInternal = true;
                    } else {
                        // if the party as a userLogin it is internal
                        if (UtilValidate.isNotEmpty(repository.findList(UserLogin.class, repository.map(UserLogin.Fields.partyId, party.getPartyId())))) {
                            isInternal = true;
                        }
                    }

                    if (party.isAccount()) {
                        isExternal = true;
                    } else if (party.isContact()) {
                        isExternal = true;
                    } else if (party.isLead()) {
                        isExternal = true;
                    } else if (party.isPartner()) {
                        isExternal = true;
                    }

                    Debug.logInfo("External = " + isExternal + " / Internal = " + isInternal + " for Activity [" + activity.getWorkEffortId() + "] with party [" + party.getPartyId() + "]", MODULE);

                    if (isExternal) {
                        externalParty.add(party);
                    }
                    if (isInternal) {
                        internalParty.add(party);
                    }
                }

                if (externalParty.size() > 0 && internalParty.size() > 0) {

                    for (Party external : externalParty) {

                        // Find out what type is external party: is it lead, is it account, ...

                        String targetPartyRoleTypeId = null;
                        Party assignedParty = repository.getPartyById(external.getPartyId());
                        if (assignedParty.isAccount()) {
                            targetPartyRoleTypeId = RoleTypeConstants.ACCOUNT;
                        } else if (assignedParty.isContact()) {
                            targetPartyRoleTypeId = RoleTypeConstants.CONTACT;
                        } else if (assignedParty.isLead()) {
                            targetPartyRoleTypeId = RoleTypeConstants.LEAD;
                        } else if (assignedParty.isPartner()) {
                            targetPartyRoleTypeId = RoleTypeConstants.PARTNER;
                        }

                        for (Party internal : internalParty) {

                            // skip if it is the same party as the external one
                            if (external.getPartyId().equals(internal.getPartyId())) {
                                continue;
                            }

                            // Create ActivityFact
                            // internal party description contains WorkEffortPartyAssignment roleTypeId
                            createActivityFact(internal.getPartyId(), external.getPartyId(),internal.getDescription(), targetPartyRoleTypeId, activity, count);
                        }
                    }
                } else {
                    Debug.logError("Missing internal or external assignments for Activity [" + activity.getWorkEffortId() + "] (found: " + internalParty.size() + " internal and " + externalParty.size() + " external)", MODULE);
                }
            } else {
                Debug.logInfo("Activity [" + activity.getWorkEffortId() + "] not has participants ", MODULE);
            }
        } catch (EntityNotFoundException ex) {
            Debug.logError(ex, MODULE);
            throw new RepositoryException(ex);
        }
    }
}