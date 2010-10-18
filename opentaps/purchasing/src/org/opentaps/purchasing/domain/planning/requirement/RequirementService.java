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
package org.opentaps.purchasing.domain.planning.requirement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.Requirement;
import org.opentaps.base.entities.RequirementRole;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.base.services.PurchasingUpdateRequirementSupplierService;
import org.opentaps.base.services.UpdateRequirementService;
import org.opentaps.domain.purchasing.PurchasingDomainInterface;
import org.opentaps.domain.purchasing.planning.PlanningRepositoryInterface;
import org.opentaps.domain.purchasing.planning.requirement.RequirementServiceInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class RequirementService extends DomainService implements RequirementServiceInterface {

    private static final String MODULE = RequirementService.class.getName();

    protected String requirementId;
    protected String partyId;
    protected String newPartyId;
    protected String roleTypeId;
    protected Timestamp fromDate;
    protected BigDecimal quantity;

    /**
     * Default constructor.
     */
    public RequirementService() {
        super();
    }

    /** {@inheritDoc} */
    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    /** {@inheritDoc} */
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    /** {@inheritDoc} */
    public void setNewPartyId(String newPartyId) {
        this.newPartyId = newPartyId;
    }

    /** {@inheritDoc} */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /** {@inheritDoc} */
    public void closeRequirement() throws ServiceException {

        try {
            PurchasingDomainInterface purchasingDomain = getDomainsDirectory().getPurchasingDomain();
            PlanningRepositoryInterface planningRepository = purchasingDomain.getPlanningRepository();

            Requirement requirement = planningRepository.getRequirementById(requirementId);
            requirement.setStatusId("REQ_CLOSED");
            planningRepository.update(requirement);

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void cancelRequirement() throws ServiceException {

        try {
            PurchasingDomainInterface purchasingDomain = getDomainsDirectory().getPurchasingDomain();
            PlanningRepositoryInterface planningRepository = purchasingDomain.getPlanningRepository();

            // this force the check that the requirement exists
            planningRepository.getRequirementById(requirementId);
            // update the status to REQ_REJECTED using the updateRequirement service in order to allow SECAs to fire
            UpdateRequirementService service = new UpdateRequirementService();
            service.setInRequirementId(requirementId);
            service.setInStatusId("REQ_REJECTED");
            runSync(service);

        } catch (FoundationException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void updateRequirementSupplierAndQuantity() throws ServiceException {
        try {
            PurchasingDomainInterface purchasingDomain = getDomainsDirectory().getPurchasingDomain();
            PlanningRepositoryInterface planningRepository = purchasingDomain.getPlanningRepository();

            Requirement requirement = planningRepository.getRequirementById(requirementId);
            // if the requirement is in a closed state, then the user should not be able to update it.
            if (Arrays.asList("REQ_CLOSED", "REQ_REJECTED").contains(requirement.getStatusId())) {
                throw new ServiceException("Requirement [" + requirementId + "] is " + requirement.getStatusId() + " and not modifiable.");
            }
            // update the quantity if needed
            if (quantity != null && requirement.getQuantity().compareTo(quantity) != 0) {
                Debug.logInfo("Updating the quantity of Requirement [" + requirementId + "] from [" + requirement.getQuantity() + "] to [" + quantity + "]" , MODULE);
                UpdateRequirementService service = new UpdateRequirementService();
                service.setInRequirementId(requirementId);
                service.setInQuantity(quantity);
                runSync(service);
            }
            // change the supplier, only for a PRODUCT_REQUIREMENT (purchasing requirement)
            Debug.logInfo("Should update the supplier of Requirement [" + requirement + "] from [" + partyId + "] to [" + newPartyId + "] ???" , MODULE);
            if ("PRODUCT_REQUIREMENT".equals(requirement.getRequirementTypeId()) && newPartyId != null && partyId != null && !partyId.equals(newPartyId)) {
                Debug.logInfo("Updating the supplier of Requirement [" + requirementId + "] from [" + partyId + "] to [" + newPartyId + "]" , MODULE);
                PurchasingUpdateRequirementSupplierService service = new PurchasingUpdateRequirementSupplierService();
                service.setInRequirementId(requirementId);
                service.setInPartyId(partyId);
                service.setInNewPartyId(newPartyId);
                runSync(service);
            }
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void updateRequirementSupplier() throws ServiceException {
        if (newPartyId.equals(partyId)) {
            // nothing to do then
            return;
        }

        try {
            PurchasingDomainInterface purchasingDomain = getDomainsDirectory().getPurchasingDomain();
            PlanningRepositoryInterface planningRepository = purchasingDomain.getPlanningRepository();

            Requirement requirement = planningRepository.getRequirementById(requirementId);
            // change the supplier only for a PRODUCT_REQUIREMENT (purchasing requirement)
            if (!"PRODUCT_REQUIREMENT".equals(requirement.getRequirementTypeId())) {
                throw new ServiceException("Can only update the supplier of a purchasing requirement (PRODUCT_REQUIREMENT), Requirement [" + requirementId + "] is of type [" + requirement.getRequirementTypeId() + "]");
            }

            // only allow to update the supplier if the new supplier has the product
            List<SupplierProduct> products = planningRepository.findList(SupplierProduct.class, planningRepository.map(SupplierProduct.Fields.productId, requirement.getProductId(), SupplierProduct.Fields.partyId, newPartyId));
            if (products.isEmpty()) {
                throw new ServiceException("Cannot update this requirement to supplier [" + newPartyId + "], this supplier does not supply the required product [" + requirement.getProductId() + "]");
            }


            // find the active Supplier RequirementRole for the given partyId
            List<RequirementRole> roles = planningRepository.getSupplierRequirementRoles(requirement, partyId);

            // expire them
            Timestamp now = UtilDateTime.nowTimestamp();
            for (RequirementRole role : roles) {
                role.setThruDate(now);
                planningRepository.update(role);
            }

            // create the new supplier role, with newPartyId
            RequirementRole role = new RequirementRole();
            role.setRequirementId(requirementId);
            role.setPartyId(newPartyId);
            role.setRoleTypeId("SUPPLIER");
            role.setFromDate(now);
            planningRepository.createOrUpdate(role);
            Debug.logInfo("updateRequirementSupplier from [" + partyId + "] to [" + newPartyId + "] succeeded.", MODULE);
        } catch (GeneralException ex) {
            Debug.logError("updateRequirementSupplier from [" + partyId + "] to [" + newPartyId + "] failed.", MODULE);
            throw new ServiceException(ex);
        }
    }
}
