/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.crmsfa.search.CrmsfaSearchService;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.gwt.common.client.lookup.configuration.SearchLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.EntityLookupAndSuggestService;
import org.opentaps.gwt.common.server.lookup.JsonResponse;

/**
 * The RPC service used to CRMSFA search results.
 */
public class CrmsfaSearchLookupService extends EntityLookupAndSuggestService {

    /**
     * Creates a new <code>CrmsfaSearchService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public CrmsfaSearchLookupService(InputProviderInterface provider) {
        super(provider, null);
    }

    /**
     * AJAX event to suggest Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String crmsfaSearch(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CrmsfaSearchLookupService service = new CrmsfaSearchLookupService(provider);
        service.search();
        return json.makeSuggestResponse(SearchLookupConfiguration.RESULT_ID, service);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface result) {
        if (result instanceof Party) {
            return makeSuggestDisplayedTextForParty((Party) result);
        } else if (result instanceof SalesOpportunity) {
            return makeSuggestDisplayedTextForOpportunity((SalesOpportunity) result);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> makeExtraSuggestValues(EntityInterface result) {
        if (result instanceof Party) {
            return makeExtraSuggestValuesForParty((Party) result);
        } else if (result instanceof SalesOpportunity) {
            return makeExtraSuggestValuesForOpportunity((SalesOpportunity) result);
        } else {
            return null;
        }
    }

    private Map<String, String> makeExtraSuggestValuesForOpportunity(SalesOpportunity opportunity) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = SalesOpportunity.class.getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + opportunity.getSalesOpportunityId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, opportunity.getSalesOpportunityId());
        extras.put(SearchLookupConfiguration.RESULT_DESCRIPTION, opportunity.getDescription());
        return extras;
    }

    private String makeSuggestDisplayedTextForOpportunity(SalesOpportunity opportunity) {
        StringBuffer sb = new StringBuffer();
        sb.append(opportunity.getOpportunityName());
        sb.append(" (").append(opportunity.getSalesOpportunityId()).append(")");
        return sb.toString();
    }

    private Map<String, String> makeExtraSuggestValuesForParty(Party party) {
        Map<String, String> extras = new HashMap<String, String>();
        String type = party.getClass().getSimpleName();
        extras.put(SearchLookupConfiguration.RESULT_ID, type + "_" + party.getPartyId());
        extras.put(SearchLookupConfiguration.RESULT_TYPE, type);
        extras.put(SearchLookupConfiguration.RESULT_REAL_ID, party.getPartyId());
        return extras;
    }

    private String makeSuggestDisplayedTextForParty(Party party) {
        StringBuffer sb = new StringBuffer();
        sb.append(party.getName());
        sb.append(" (").append(party.getPartyId()).append(")");
        return sb.toString();
    }

    private List<EntityInterface> search() {

        if (getSuggestQuery() == null || getSuggestQuery().trim().isEmpty()) {
            return new ArrayList<EntityInterface>();
        }

        try {
            CrmsfaSearchService crmSearch = new CrmsfaSearchService();
            crmSearch.setInfrastructure(getProvider().getInfrastructure());
            crmSearch.setUser(getProvider().getUser());
            crmSearch.setKeywords(getSuggestQuery());
            crmSearch.setSearchAccounts(true);
            crmSearch.setSearchContacts(true);
            crmSearch.setSearchLeads(true);
            crmSearch.setSearchSalesOpportunities(true);
            crmSearch.search();
            List<Account> accounts = crmSearch.getAccounts();
            List<Contact> contacts = crmSearch.getContacts();
            List<Lead> leads = crmSearch.getLeads();
            List<SalesOpportunity> opportunities = crmSearch.getSalesOpportunities();

            // get paginated results
            List<EntityInterface> r = new ArrayList<EntityInterface>();
            r.addAll(accounts);
            r.addAll(contacts);
            r.addAll(leads);
            r.addAll(opportunities);
            paginateResults(r);

        } catch (ServiceException e) {
            storeException(e);
            return null;
        }

        return getResults();
    }

}
