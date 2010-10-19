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
package org.opentaps.common.domain.party;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.model.ModelViewEntity;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.common.security.OpentapsSecurity;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.party.PartyMergeServiceInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * Party merge services implementation.
 */
public class PartyMergeService extends DomainService implements PartyMergeServiceInterface {

    public static final String MODULE = PartyMergeService.class.getName();

    private String partyIdFrom = null;
    private String partyIdTo = null;
    private boolean validate = true;

    /** {@inheritDoc} */
    public void setPartyIdFrom(String partyId) {
        partyIdFrom = partyId;
    }

    /** {@inheritDoc} */
    public void setPartyIdTo(String partyId) {
        partyIdTo = partyId;
    }

    /** {@inheritDoc} */
    public void setValidate(String s) {
        validate = "Y".equalsIgnoreCase(s) ? true : "N".equalsIgnoreCase(s) ? false : true;
    }

    /** {@inheritDoc} */
    public void validateMergeParties() throws ServiceException {

        Delegator delegator = getInfrastructure().getDelegator();

        try {
            // ensure that merging parties are the same type (ACCOUNT, CONTACT, PROSPECT, SUPPLIER)
            String fromPartyType = PartyHelper.getFirstValidRoleTypeId(partyIdFrom, PartyHelper.MERGE_PARTY_ROLES, delegator);
            String toPartyType = PartyHelper.getFirstValidRoleTypeId(partyIdTo, PartyHelper.MERGE_PARTY_ROLES, delegator);
            if ((fromPartyType == null) || !fromPartyType.equals(toPartyType)) {
                throw new ServiceException(String.format("Cannot merge party [%1$s] of type [%2$s] with party [%3$s] of type [%4$s] because they are not the same type. %5$s", partyIdFrom, fromPartyType, partyIdTo, toPartyType, UtilMessage.expandLabel("OpentapsError_MergePartiesFail", locale)));
            }

            if (partyIdFrom.equals(partyIdTo)) {
                throw new ServiceException(String.format("Cannot merge party [%1$s] to itself! %2$s", partyIdFrom, UtilMessage.expandLabel("CrmErrorMergeParties", locale)));
            }

            // convert ACCOUNT/CONTACT/PROSPECT/SUPPLIER to ACCOUNT/CONTACT/LEAD/SUPPLIER
            String partyType = (fromPartyType.equals(RoleTypeConstants.PROSPECT) ? RoleTypeConstants.LEAD : fromPartyType);

            // make sure user has CRMSFA_${partyType}_UPDATE (or PRCH_SPLR_UPDATE for SUPPLIER) permission for both parties
            // TODO: and delete, check security config
            if (RoleTypeConstants.SUPPLIER.equals(partyType)) {
                OpentapsSecurity s = new OpentapsSecurity(getSecurity(), getUser().getOfbizUserLogin());
                if (!s.hasPartyRelationSecurity("PRCH_SPLR", "_UPDATE", partyIdFrom)
                        || !s.hasPartyRelationSecurity("PRCH_SPLR", "_UPDATE", partyIdTo)) {
                    throw new ServiceException(UtilMessage.expandLabel("CrmErrorPermissionDenied", locale) + ": PRCH_SPLR_UPDATE");
                }
            } else {
                OpentapsSecurity s = new OpentapsSecurity(getSecurity(), getUser().getOfbizUserLogin()); 
                if (!s.hasPartyRelationSecurity(String.format("CRMSFA_%1$s", partyType), "_UPDATE", partyIdFrom)
                        || !s.hasPartyRelationSecurity(String.format("CRMSFA_%1$s", partyType), "_UPDATE", partyIdTo)) {
                    throw new ServiceException(UtilMessage.expandLabel("CrmErrorPermissionDenied", locale) + ": CRMSFA_" + partyType + "_UPDATE");
                }
            }
        } catch (GenericEntityException e) {
            throw new ServiceException("OpentapsError_MergePartiesFail", null);
        }
     
    }

    /** {@inheritDoc} */
    public void mergeParties() throws ServiceException {

        Delegator delegator = getInfrastructure().getDelegator();

        try {

            if (validate) {
                // validate again
                validateMergeParties();
            }

            // merge the party objects
            mergeTwoValues("PartySupplementalData", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("Person", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("PartyGroup", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("Party", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);

            List<GenericValue> toRemove = new ArrayList<GenericValue>();

            // Get a list of entities related to the Party entity, in descending order by relation
            List<ModelEntity> relatedEntities = getRelatedEntities("Party", delegator);

            // Go through the related entities in forward order - this makes sure that parent records are created before child records
            Iterator<ModelEntity> reit = relatedEntities.iterator();
            while (reit.hasNext()) {
                ModelEntity modelEntity = reit.next();

                // Examine each field of the entity
                Iterator<ModelField> mefit = modelEntity.getFieldsIterator();
                while (mefit.hasNext()) {
                    ModelField modelField = mefit.next();
                    if (modelField.getName().matches(".*[pP]artyId.*")) {

                        // If the name of the field has something to do with a partyId, get all the existing records from that entity which have the
                        //  partyIdFrom in that particular field
                        List<GenericValue> existingRecords = delegator.findByAnd(modelEntity.getEntityName(), UtilMisc.toMap(modelField.getName(), partyIdFrom));
                        if (existingRecords.size() > 0) {
                            Iterator<GenericValue> eit = existingRecords.iterator();
                            while (eit.hasNext()) {
                                GenericValue existingRecord = eit.next();
                                if (modelField.getIsPk()) {

                                    // If the partyId field is part of a primary key, create a new record with the partyIdTo in place of the partyIdFrom
                                    GenericValue newRecord = delegator.makeValue(modelEntity.getEntityName(), existingRecord.getAllFields());
                                    newRecord.set(modelField.getName(), partyIdTo);

                                    // Create the new record if a record with the same primary key doesn't already exist
                                    if (delegator.findOne(newRecord.getPrimaryKey().getEntityName(), newRecord.getPrimaryKey(), false) == null) {
                                        newRecord.create();
                                    }

                                    // Add the old record to the list of records to remove
                                    toRemove.add(existingRecord);
                                } else {

                                    // If the partyId field is not party of a primary key, simply update the field with the new value and store it
                                    existingRecord.set(modelField.getName(), partyIdTo);
                                    existingRecord.store();
                                }
                            }
                        }
                    }
                }
            }

            // Go through the list of records to remove in REVERSE order! Since they're still in descending order of relation to the Party
            //  entity, reversing makes sure that child records are removed before parent records, all the way back to the original Party record
            ListIterator<GenericValue> rit = toRemove.listIterator(toRemove.size());
            while (rit.hasPrevious()) {
                GenericValue existingRecord = (GenericValue) rit.previous();
                Debug.logError(existingRecord.toString(), MODULE);
                existingRecord.remove();
            }

        } catch (GenericEntityException e) {
            new ServiceException(UtilMessage.expandLabel("OpentapsError_MergePartiesFail", locale) + e.getMessage());
        }
    }

    /**
     * Merging function for two unique <code>GenericValues</code>.
     * @param entityName the name of the <code>GenericValue</code> entity
     * @param fromKeys <code>Map</code> representing the primary key of the entity to merge from
     * @param toKeys <code>Map</code> representing the primary key of the entity to merge to
     * @param delegator a <code>Delegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static void mergeTwoValues(String entityName, Map<String, String> fromKeys, Map<String, String> toKeys, Delegator delegator) throws GenericEntityException {
        GenericValue from = delegator.findByPrimaryKey(entityName, fromKeys);
        GenericValue to = delegator.findByPrimaryKey(entityName, toKeys);
        if (from == null || to == null) {
            return;
        }
        from.setNonPKFields(to.getAllFields());
        to.setNonPKFields(from.getAllFields());
        to.store();
    }

    private static List<ModelEntity> getRelatedEntities(String parentEntityName, Delegator delegator) {
        ModelEntity parentEntity = delegator.getModelEntity(parentEntityName);
        // Start the recursion
        return getRelatedEntities(new ArrayList<ModelEntity>(), parentEntity, delegator);
    }

    /**
     * Recursive method to map relations from a single entity.
     * @param relatedEntities List of related ModelEntity objects in descending order of relation from the parent entity
     * @param parentEntity Root ModelEntity for deriving relations
     * @param delegator Delegator
     * @return List of ModelEntity objects in descending order of relation from the original parent entity
     */
    private static List<ModelEntity> getRelatedEntities(List<ModelEntity> relatedEntities, ModelEntity parentEntity, Delegator delegator) {

        // Do nothing if the parent entity has already been mapped
        if (relatedEntities.contains(parentEntity)) {
            return relatedEntities;
        }

        relatedEntities.add(parentEntity);
        Iterator<ModelRelation> reit = parentEntity.getRelationsIterator();

        // Recurse for each relation from the parent entity that doesn't refer to a view-entity
        while (reit.hasNext()) {
            ModelRelation relation = reit.next();
            String relatedEntityName = relation.getRelEntityName();
            ModelEntity relatedEntity = delegator.getModelEntity(relatedEntityName);
            if (!(relatedEntity instanceof ModelViewEntity)) {
                relatedEntities = getRelatedEntities(relatedEntities, relatedEntity, delegator);
            }
        }
        return relatedEntities;
    }

}
