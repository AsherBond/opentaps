/*
 * Copyright (c) opentaps Group LLC
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

import java.util.ArrayList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.services.ActivitiesTransformToActivityFactsService;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.ofbiz.base.util.UtilValidate;

/**
 * Do data warehouse operations for activities.
 */
public class ActivitiesDataWarehouseService extends DomainService {

    private static final String MODULE = ActivitiesDataWarehouseService.class.getName();

    private String workEffortId;

    /**
     * Sets the required input parameter for service {@link #transformToActivityFacts}.
     * @param workEffortId the ID of work effort
     */
    public void setWorkEffortId(String workEffortId) {
        this.workEffortId = workEffortId;
    }


    /**
     * Transform all Activities which are TASK_COMPLETED or EVENT_COMPLETED
     * into ActivityFact
     *
     * @throws ServiceException
     */
    public void transformAllActivities() throws ServiceException {
        ActivitiesTransformToActivityFactsService activitiesTransform = null;

        try {
            ActivityRepositoryInterface activityRepository = getDomainsDirectory().getActivitiesDomain().getActivityRepository();
            PartyRepositoryInterface patryRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();

            // Remove all ActivityFacts
            List<ActivityFact> activityFacts = patryRepository.findAll(ActivityFact.class);
            patryRepository.remove(activityFacts);

            // Find all Activities which are TASK_COMPLETED or EVENT_COMPLETED
            List<Activity> activityList = activityRepository.getCompletedActivities();

            // Each found activity transform into ActivityFact entities.
            for(Activity activity : activityList) {
                String workEffortId = activity.getWorkEffortId();

                activitiesTransform = new ActivitiesTransformToActivityFactsService();
                activitiesTransform.setInWorkEffortId(workEffortId);
                activitiesTransform.setUser(getUser());
                activitiesTransform.runSync(infrastructure);
            }

        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /**
     * Transformation wich transforms data from WorkEffort, WorkEffortPartyAssign entities to ActivityFact entities.
     *  It expands the WorkEffortPartyAssign to cover all target parties for all team members.
     *  The counts that is in ActivityFacts is based on the purpose of the WorkEffort.
     * @throws ServiceException if an error occurs
     */
    public void transformToActivityFacts() throws ServiceException {
        try {
            PartyRepositoryInterface repository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            ActivityRepositoryInterface activityRepository = getDomainsDirectory().getActivitiesDomain().getActivityRepository();

            // Get Activity
            Activity activity = activityRepository.getActivityById(workEffortId);

            List<Party> parties = activity.getParticipants();
            List<Party> externalParty = new ArrayList<Party>();
            List<Party> internalParty = new ArrayList<Party>();

            if(parties != null) {
                for(Party party : parties) {
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
                            activityRepository.createActivityFact(internal.getPartyId(), external.getPartyId(),internal.getDescription(), targetPartyRoleTypeId, activity);
                        }
                    }

                } else {
                    Debug.logError("Missing internal or external assignments for Activity [" + activity.getWorkEffortId() + "] (found: " + internalParty.size() + " internal and " + externalParty.size() + " external)", MODULE);
                }

            } else {
                Debug.logInfo("Activity [" + activity.getWorkEffortId() + "] not has participants ", MODULE);
            }

        } catch (RepositoryException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex);
        } catch (EntityNotFoundException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex);
        }
    }

}

