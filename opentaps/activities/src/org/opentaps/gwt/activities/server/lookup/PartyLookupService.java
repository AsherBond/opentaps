/*
 * Copyright (c) 2010 - 2011 Open Source Strategies, Inc.
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

package org.opentaps.gwt.activities.server.lookup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensourcestrategies.activities.reports.ActivitiesChartsService;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.PartyFromByRelnAndContactInfoAndPartyClassification;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.gwt.activities.client.leads.form.ActivityLeadLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.lookup.JsonResponse;

/**
 * Extends the normal PartyLookupConfiguration to add handling of cutoff and Recent/Old/No Activity categories of the activity component.
 */
public class PartyLookupService extends org.opentaps.gwt.common.server.lookup.PartyLookupService {

    private static final String MODULE = PartyLookupService.class.getName();

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     * @exception InfrastructureException if an error occurs
     */
    public PartyLookupService(InputProviderInterface provider) throws InfrastructureException {
        super(provider);
    }

    /**
     * AJAX event to perform lookups on Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findLeads(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.findLeads();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * {@inheritDoc}
     * Overriden to handle the activity related filters.
     */
    @Override
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findLeads() {
        try {

            // Add general leads conditions.
            EntityCondition leadsCond = null;

            // Add activity related filters, cutoff is required to handle to category filters
            if (getProvider().parameterIsPresent(ActivityLeadLookupConfiguration.IN_CUTOFF_DAYS)) {
                Integer cutoffDays = Integer.parseInt(getProvider().getParameter(ActivityLeadLookupConfiguration.IN_CUTOFF_DAYS));
                boolean viewRecent = "Y".equalsIgnoreCase(getProvider().getParameter(ActivityLeadLookupConfiguration.IN_SHOW_RECENT));
                boolean viewOld = "Y".equalsIgnoreCase(getProvider().getParameter(ActivityLeadLookupConfiguration.IN_SHOW_OLD));
                boolean viewNoActivity = "Y".equalsIgnoreCase(getProvider().getParameter(ActivityLeadLookupConfiguration.IN_SHOW_NO_ACTIVITY));

                // do not bother if either cutoffDays was invalid, or the filters are set to view all leads
                // also consider all category filters set to false as vie all leads
                if (cutoffDays != null && (!viewRecent || !viewOld || !viewNoActivity) && (viewRecent || viewOld || viewNoActivity)) {
                    try {
                        ActivitiesChartsService chartService = new ActivitiesChartsService(getProvider().getInfrastructure(), getProvider().getUser(), getProvider().getLocale());
                        // Get the leads the current user is allowed to see
                        chartService.setAllowedLeadPartyIds(getPartyRepository().getLeadIdsUserIsAllowedToView());
                        chartService.setCutoffDays(cutoffDays);
                        chartService.getActivitiesByLeadSnapshot();

                        Set<String> leadPartyIds = new HashSet<String>();
                        if (viewRecent) {
                            leadPartyIds.addAll(chartService.getRecentLeadPartyIds());
                        }
                        if (viewOld) {
                            leadPartyIds.addAll(chartService.getOldLeadPartyIds());
                        }
                        if (viewNoActivity) {
                            leadPartyIds.addAll(chartService.getNoActivityLeadPartyIds());
                        }

                        Debug.logInfo("Filtered lookup to leads [" + leadPartyIds + "]", MODULE);

                        // now make the condition
                        leadsCond = EntityCondition.makeCondition(PartyFromByRelnAndContactInfoAndPartyClassification.Fields.partyIdFrom.name(),
                                                                  EntityOperator.IN,
                                                                  leadPartyIds);

                    } catch (ServiceException e) {
                        storeException(e);
                    }
                }
            }

            // Above block already includes filters on leads the user can view
            // so only do that if it was not executed
            if (leadsCond == null) {
                if ("Y".equals(this.getRepository().getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER))) {
                    leadsCond = getPartyRepository().makeLookupLeadsUserIsAllowedToViewCondition();
                } else {
                    leadsCond = getPartyRepository().makeLookupLeadsCondition();
                }
            }

            // Do leads search according added conditions.
            List<PartyFromByRelnAndContactInfoAndPartyClassification> leads = findParties(
                    PartyFromByRelnAndContactInfoAndPartyClassification.class,
                    leadsCond,
                    RoleTypeConstants.PROSPECT);

            return leads;

        } catch (RepositoryException e) {
            storeException(e);
            return null;
        } catch (InfrastructureException e) {
            storeException(e);
            return null;
        }
    }

}
