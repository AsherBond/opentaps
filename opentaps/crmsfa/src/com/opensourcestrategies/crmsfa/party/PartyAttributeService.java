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
package com.opensourcestrategies.crmsfa.party;

import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.base.entities.PartyAttribute;
import org.opentaps.base.entities.PartyAttributePk;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainService;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.service.ServiceException;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Service to create/update/remove PartyAttribute 
 */
public class PartyAttributeService extends DomainService {
    private String partyId;
    private String attrName;
    private String attrValue;
    private static final String MODULE = PartyAttributeService.class.getName();
    public static List<String> VALID_PARTY_ROLES = Arrays.asList("ACCOUNT", "CONTACT", "PROSPECT"); 
    

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }
    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }
    
    /**
     * Create a party attribute entity.
     * @exception ServiceException if an error occurs
     */
    public void createPartyAttribute() throws ServiceException {
        try {
            String roleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, VALID_PARTY_ROLES, getInfrastructure().getDelegator());
            if (roleTypeId == null) {
                throw new ServiceException("CrmError_InvalidPartyRoleOnCustomFields", UtilMisc.toMap("partyId", partyId));
            }
            String securityModule = null;
            if ("PROSPECT".equals(roleTypeId)) {
                securityModule = "CRMSFA_LEAD";
            } else if ("ACCOUNT".equals(roleTypeId)) {
                securityModule = "CRMSFA_ACCOUNT";
            } else if ("CONTACT".equals(roleTypeId)) {
                securityModule = "CRMSFA_CONTACT";
            }

        	if (!CrmsfaSecurity.hasPartyRelationSecurity(security, securityModule, "_CUST_CREATE", getUser().getOfbizUserLogin(), partyId)) {
        		String err = UtilMessage.getPermissionDeniedError(locale) + ": user [" + getUser().getUserId() + "] does not have permission " + securityModule + "_CUST_CREATE";
                throw new ServiceException(err);
        	}

            Session session = getInfrastructure().getSession();
            
            // check if existing same party attribute alreay
            String hql = "from PartyAttribute eo where eo.id.partyId = :partyId and eo.id.attrName = :attrName";
            Query query = session.createQuery(hql);
            query.setString("partyId", partyId);
            query.setString("attrName", attrName);
            List<PartyAttribute> attributes = query.list();
            if (attributes.size() > 0) {
                throw new ServiceException("CrmError_ExistingSameCustomFieldAlready", UtilMisc.toMap("partyId", partyId, "attrName", attrName));
            }
            PartyAttribute attribute = new PartyAttribute();
            PartyAttributePk attributeId = new PartyAttributePk();
            attributeId.setPartyId(partyId);
            attributeId.setAttrName(attrName);
            attribute.setId(attributeId);
            attribute.setAttrValue(attrValue);
            attribute.setCreatedByUserLoginId(this.getUser().getUserId());
            session.save(attribute);
            session.flush();
            session.close();
            
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        }
    }
    
    /**
     * Update a party attribute entity.
     * @exception ServiceException if an error occurs
     */
    public void updatePartyAttribute() throws ServiceException {
        try {
            String roleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, VALID_PARTY_ROLES, getInfrastructure().getDelegator());
            if (roleTypeId == null) {
                throw new ServiceException("CrmError_InvalidPartyRoleOnCustomFields", UtilMisc.toMap("partyId", partyId));
            }
            String securityModule = null;
            if ("PROSPECT".equals(roleTypeId)) {
                securityModule = "CRMSFA_LEAD";
            } else if ("ACCOUNT".equals(roleTypeId)) {
                securityModule = "CRMSFA_ACCOUNT";
            } else if ("CONTACT".equals(roleTypeId)) {
                securityModule = "CRMSFA_CONTACT";
            }
            Session session = getInfrastructure().getSession();
            
            // check if existing same party attribute alreay
            String hql = "from PartyAttribute eo where eo.id.partyId = :partyId and eo.id.attrName = :attrName";
            Query query = session.createQuery(hql);
            query.setString("partyId", partyId);
            query.setString("attrName", attrName);
            List<PartyAttribute> attributes = query.list();
            if (attributes.size() == 0) {
                throw new ServiceException("CrmError_NotExistingTheCustomField", UtilMisc.toMap("partyId", partyId, "attrName", attrName));
            }
            PartyAttribute attribute = attributes.get(0);
            // UPDATE permission is required to update the custom fields if the user was not the original creator of the custom field.
            if (attribute.getCreatedByUserLoginId().equals(this.getUser().getUserId()) || CrmsfaSecurity.hasPartyRelationSecurity(security, securityModule, "_CUST_UPDATE", getUser().getOfbizUserLogin(), partyId)) {
                attribute.setAttrValue(attrValue);
                session.save(attribute);
                session.flush();
            } else {
                String err = UtilMessage.getPermissionDeniedError(locale) + ": user [" + getUser().getUserId() + "] does not have permission " + securityModule + "_CUST_UPDATE";
                throw new ServiceException(err);
            }
            session.close();
            
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Remove a party attribute entity.
     * @exception ServiceException if an error occurs
     */
    public void removePartyAttribute() throws ServiceException {
        try {
            String roleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, VALID_PARTY_ROLES, getInfrastructure().getDelegator());
            if (roleTypeId == null) {
                throw new ServiceException("CrmError_InvalidPartyRoleOnCustomFields", UtilMisc.toMap("partyId", partyId));
            }
            String securityModule = null;
            if ("PROSPECT".equals(roleTypeId)) {
                securityModule = "CRMSFA_LEAD";
            } else if ("ACCOUNT".equals(roleTypeId)) {
                securityModule = "CRMSFA_ACCOUNT";
            } else if ("CONTACT".equals(roleTypeId)) {
                securityModule = "CRMSFA_CONTACT";
            }
            
            Session session = getInfrastructure().getSession();
            
            // check if existing same party attribute alreay
            String hql = "from PartyAttribute eo where eo.id.partyId = :partyId and eo.id.attrName = :attrName";
            Query query = session.createQuery(hql);
            query.setString("partyId", partyId);
            query.setString("attrName", attrName);
            List<PartyAttribute> attributes = query.list();
            if (attributes.size() == 0) {
                throw new ServiceException("CrmError_NotExistingTheCustomField", UtilMisc.toMap("partyId", partyId, "attrName", attrName));
            }
            PartyAttribute attribute = attributes.get(0);
            // DELETE permission is required to delete the custom fields if the user was not the original creator of the custom field.
            if (attribute.getCreatedByUserLoginId().equals(this.getUser().getUserId()) || CrmsfaSecurity.hasPartyRelationSecurity(security, securityModule, "_CUST_DELETE", getUser().getOfbizUserLogin(), partyId)) {
                session.delete(attribute);
                session.flush();
            } else {
                String err = UtilMessage.getPermissionDeniedError(locale) + ": user [" + getUser().getUserId() + "] does not have permission " + securityModule + "_CUST_DELETE";
                throw new ServiceException(err);
            }
            session.close();
            
        } catch (GenericEntityException e) {
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        }
    }
}
