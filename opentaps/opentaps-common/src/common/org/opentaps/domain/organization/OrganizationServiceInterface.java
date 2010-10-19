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
 */package org.opentaps.domain.organization;

import org.opentaps.foundation.service.ServiceInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * Services for managing organizations
 *
 */
public interface OrganizationServiceInterface extends ServiceInterface {

    /**
     * Sets the organization party Id
     * @param organizationPartyId
     */
    public void setOrganizationPartyId(String organizationPartyId);
    
    /**
     * Sets the party Id for the organization template to copy ledger setup from
     * @param templateOrganizationPartyId
     */
    public void setTemplateOrganizationPartyId(String templateOrganizationPartyId);
    
    /**
     * Copies the general ledger setup from the organization template to an organization.
     * The organization party must already exist, but if it is not set up as an organization yet,
     * this service will do it for you.
     * @throws ServiceException
     */
    public void copyOrganizationLedgerSetup() throws ServiceException;
    
}
