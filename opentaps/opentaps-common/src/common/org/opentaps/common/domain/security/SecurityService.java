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
package org.opentaps.common.domain.security;

import java.util.List;
import java.util.Locale;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.base.entities.PartyRelationshipAndPermission;
import org.opentaps.base.entities.PartyRelationship;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.security.SecurityRepositoryInterface;
import org.opentaps.domain.security.SecurityServiceInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;


public class SecurityService extends DomainService implements SecurityServiceInterface{
    
    private static final String MODULE = SecurityService.class.getName();
    public String securityModule;
    public List<String> possibleRoleTypeIds;
    
    public SecurityService() {
        super();
    }

    public SecurityService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }
    
    public void setSecurityModule(String securityModule) {
        this.securityModule = securityModule;
    }
    
    public void setPossibleRoleTypeIds(List<String> possibleRoleTypeIds) {
        this.possibleRoleTypeIds = possibleRoleTypeIds;
    }

    /**
     * Not tested yet.
     * 
     * @param securityOperation
     * @param partyIdFor
     * @return
     * @throws org.opentaps.foundation.service.ServiceException
     */
    public boolean hasPermissionForParty(String securityOperation, String partyIdFor) throws ServiceException{
        try {
            if (this.getUser().hasAdminPermissionsForModule(this.securityModule) == true) {
                return true;
            }
            if (this.getUser().hasPermission(this.securityModule, securityOperation) == true) {
                return true;
            }

            //initialize variable needed to seek party permission
            boolean hasPermission = false;
            String partyIdFrom = partyIdFor;
            String partyIdTo = this.getUser().getUserId();
            SecurityRepositoryInterface sec_rep = this.getDomainsDirectory().getSecurityDomain().getSecurityRepository();
            PartyRepositoryInterface party_rep = this.getDomainsDirectory().getPartyDomain().getPartyRepository();

            // check if partyIdFor has proper role
            String roleTypeIdFor = PartyHelper.getFirstValidRoleTypeId(partyIdFor, possibleRoleTypeIds, this.getInfrastructure().getDelegator());
            if (roleTypeIdFor == null) {
                Debug.logError("Failed to check permission for partyId [" + partyIdFor + "] because that party does not have a valid role. I.e., it is not an Account, Contact, Lead, etc.", MODULE);
                return false;
            }

            
            // try to find direct relation with partyIdFor in a list of parties related to current logged in user
            List<PartyRelationshipAndPermission>  permittedRelationships = sec_rep.getPartyRelationshipAndPermission(partyIdTo, this.securityModule, securityOperation);
            int i = 0;
            while((i < permittedRelationships.size())||(hasPermission == false)){
                if(permittedRelationships.get(i).getPartyIdFrom().compareTo(partyIdFrom) == 0){
                    hasPermission = true;
                }
                i++;
            }
            if(hasPermission == true){
                if (Debug.verboseOn()) {
                    Debug.logVerbose(this.getUser() + " has direct permitted relationship for " + partyIdFor, MODULE);
                }
                return true;
            }
            
            //if there's no direct relation, try to find indirect relation
            // Note that here we had to break with convention because
            // of the way PartyRelationship for CONTACT is written
            // (ie, CONTACT_REL_INV is opposite of ASSIGNED_TO, etc.  See comments in CRMSFADemoData.xml )
            i = 0;
            while((i < permittedRelationships.size())||(hasPermission == false)){
                partyIdTo = permittedRelationships.get(i).getPartyIdFrom();
                List<PartyRelationship> indirectPermittedRelationships = party_rep.getPartyRelationship(partyIdFrom, partyIdTo);
                if(indirectPermittedRelationships.size()  > 0){
                    hasPermission = true;
                }
                i++;
            }
            if(hasPermission == true){
                if (Debug.verboseOn()) {
                    Debug.logVerbose(this.getUser() + " has indirect permitted relationship for " + partyIdFor, MODULE);
                }
                return true;
            }
            
            return hasPermission;
        } catch (GenericEntityException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } catch (RepositoryException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } catch (InfrastructureException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        }
    }

    /**
     * add activity domain to implement this method or leave those "delegator.findBy..." ?
     * 
     * @param securityOperation = VIEW|UPDATE
     * @param leadPartyId
     * @return
     * @throws org.opentaps.foundation.service.ServiceException
     */
    public boolean hasPermissionForActivity(String securityOperation, String leadPartyId) throws ServiceException{
        // first check general CRMSFA_ACT_${securityOperation} permission
        /*if (!this.getUser().hasPermission("CRMSFA_ACT", securityOperation)) {
            Debug.logWarning("Checked UserLogin [" + this.getUser() + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] in general but permission was denied.", MODULE);
            return false;
        }*/
        
        /*GenericValue workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        if (workEffort == null) {
            Debug.logWarning("Tried to perform operation [" + securityOperation + "] on an non-existent activity [" + workEffortId + "]", MODULE);
            return false;
        }

        // check for closed activities for actions that are not _VIEW
        if (!"_VIEW".equals(securityOperation) && UtilActivity.activityIsInactive(workEffort)) {
            Debug.logWarning("Tried to perform operation [" + securityOperation + "] on an inactive activity [" + workEffortId + "]", MODULE);
            return false;
        }

        List<GenericValue> parties = UtilActivity.getActivityParties(delegator, workEffortId, PartyHelper.CLIENT_PARTY_ROLES);
        for (Iterator<GenericValue> iter = parties.iterator(); iter.hasNext();) {
            String internalPartyId = iter.next().getString("partyId");
            String securityModule = getSecurityModuleOfInternalParty(internalPartyId, delegator);
            if (!hasPartyRelationSecurity(security, securityModule, securityOperation, userLogin, internalPartyId)) {
                return false;
            }
        }*/
            
        return false;
    }

    

    

    

}
