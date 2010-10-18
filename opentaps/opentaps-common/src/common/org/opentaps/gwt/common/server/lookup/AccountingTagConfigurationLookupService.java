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

package org.opentaps.gwt.common.server.lookup;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.AccountingTagLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to retrieve the accounting tag configuration for an organization and usage type.
 */
public class AccountingTagConfigurationLookupService extends EntityLookupService {

    private static final String MODULE = AccountingTagConfigurationLookupService.class.getName();

    private String organizationPartyId;
    private String accountingTagUsageTypeId;
    private OrganizationRepositoryInterface repository;

    protected AccountingTagConfigurationLookupService(InputProviderInterface provider) throws RepositoryException {
        super(provider, AccountingTagLookupConfiguration.LIST_OUT_FIELDS);
        organizationPartyId = getProvider().getParameter(AccountingTagLookupConfiguration.IN_ORGANIZATION_PARTY_ID);
        accountingTagUsageTypeId = getProvider().getParameter(AccountingTagLookupConfiguration.IN_TAG_USAGE_TYPE_ID);
        // auto set the invoice repository as the service repository
        repository = getDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
        setRepository(repository);
    }

    /**
     * AJAX event to fetch the accounting tags configuration.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws FoundationException if an error occurs
     */
    public static String findAccountingTagsConfiguration(HttpServletRequest request, HttpServletResponse response)  throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        AccountingTagConfigurationLookupService service = new AccountingTagConfigurationLookupService(provider);
        service.findTagsConfiguration();
        return json.makeLookupResponse(AccountingTagLookupConfiguration.OUT_TAG_INDEX, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds the tag configuration that applies to the given organization and usage type.
     * @return a list of <code>Map</code> representing the tags configuration
     */
    private List<AccountingTagConfigurationForOrganizationAndUsage> findTagsConfiguration() {
        if (organizationPartyId == null) {
            Debug.logError("Missing required parameter organizationPartyId", MODULE);
            return null;
        }
        if (accountingTagUsageTypeId == null) {
            Debug.logError("Missing required parameter accountingTagUsageTypeId", MODULE);
            return null;
        }

        try {
            List<AccountingTagConfigurationForOrganizationAndUsage> conf = repository.getAccountingTagConfiguration(organizationPartyId, accountingTagUsageTypeId);
            setResultTotalCount(conf.size());
            setResults(conf);
            return conf;
        } catch (FoundationException e) {
            storeException(e);
            return null;
        }
    }
}
