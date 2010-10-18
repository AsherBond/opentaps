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
package org.opentaps.domain.party;

import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * Merge parties services.
 */
public interface PartyMergeServiceInterface extends ServiceInterface {

    /**
     * Sets source party identifier.
     */
    public void setPartyIdFrom(String partyId);

    /**
     * Sets target party identifier.
     */
    public void setPartyIdTo(String partyId);

    /**
     * This attribute may disallow preliminary parties validation if equals to <code>N</code>.
     * @param s Text flag having a boolean meaning, can equals to Y or N. 
     */
    public void setValidate(String s);

    /**
     * Validate two parties if they can participate in merger.
     * Ensures two parties can be merged. Returns service error if they cannot. A merge requires *_${type}_UPDATE permission 
     * where type is the roleTypeId of the party, such as ACCOUNT, CONTACT, LEAD or SUPPLIER. Also, the input must be two 
     * different partyIds with the same roleTypeId.
     */
    public void validateMergeParties() throws ServiceException;

    /**
     * Merge two parties. Checks <code>crmsfa.validateMergeParties</code> as a precaution if the validate 
     * parameter is not set to N. The From party will be deleted after the merge.
     * @throws ServiceException
     */
    public void mergeParties() throws ServiceException;
}
