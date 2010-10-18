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
package org.opentaps.dataimport;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

/**
 * DataImportActions - Java Actions for DataImport.
 */
public final class DataImportActions {

    /**
     * Action for prepare the data for copyLedgerSetup screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void copyLedgerSetup(Map<String, Object> context) throws GeneralException {
        final ActionContext ac = new ActionContext(context);
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();
        List<PartyGroup> fromOrganizationTemplates = organizationRepository.getOrganizationTemplates();
        List<PartyGroup> toOrganizations = organizationRepository.getOrganizationWithoutLedgerSetup();
        ac.put("fromOrganizationTemplates", fromOrganizationTemplates);
        ac.put("toOrganizations", toOrganizations);
    }
}
