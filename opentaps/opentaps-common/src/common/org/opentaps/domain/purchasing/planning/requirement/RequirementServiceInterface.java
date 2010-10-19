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
package org.opentaps.domain.purchasing.planning.requirement;

import java.math.BigDecimal;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Interface for Requirement services.
 */
public interface RequirementServiceInterface extends ServiceInterface {

    /**
     * Sets the requirement ID, required parameter for all requirement services.
     * @param requirementId the requirement ID
     */
    public void setRequirementId(String requirementId);

    /**
     * Sets the party ID, required parameter for {@link #updateRequirementSupplier} and {@link #updateRequirementSupplierAndQuantity}.
     * @param partyId the party ID
     */
    public void setPartyId(String partyId);

    /**
     * Sets the new party ID, required parameter for {@link #updateRequirementSupplier} and {@link #updateRequirementSupplierAndQuantity}.
     * @param newPartyId the new party ID
     */
    public void setNewPartyId(String newPartyId);

    /**
     * Sets the quantity, required parameter for {@link #updateRequirementSupplierAndQuantity}.
     * @param quantity the fromDate
     */
    public void setQuantity(BigDecimal quantity);

    /**
     * Service to close a requirement. This should only be called as an ECA when a requirement status
     * is set to REQ_ORDERED.
     * @throws ServiceException if an error occurs
     * @see #setRequirementId required input <code>requirementId</code>
     */
    public void closeRequirement() throws ServiceException;

    /**
     * Service to cancel a requirement. Sets its status to REQ_REJECTED.
     * @throws ServiceException if an error occurs
     * @see #setRequirementId required input <code>requirementId</code>
     */
    public void cancelRequirement() throws ServiceException;

    /**
     * Service to update a requirement supplier from <code>partyId</code> to <code>newPartyId</code>.
     * @throws ServiceException if an error occurs
     * @see #setRequirementId required input <code>requirementId</code>
     * @see #setPartyId required input <code>partyId</code>
     * @see #setNewPartyId required input <code>newPartyId</code>
     */
    public void updateRequirementSupplier() throws ServiceException;

    /**
     * Service to update the supplier and or the quantity of a requirement.
     * @throws ServiceException if an error occurs
     * @see #setRequirementId required input <code>requirementId</code>
     * @see #setPartyId required input <code>partyId</code>
     * @see #setNewPartyId required input <code>newPartyId</code>
     * @see #setQuantity required input <code>quantity</code>
     */
    public void updateRequirementSupplierAndQuantity() throws ServiceException;
}
