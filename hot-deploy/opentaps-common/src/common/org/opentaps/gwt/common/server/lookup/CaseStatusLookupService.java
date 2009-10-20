/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.gwt.common.server.lookup;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.domain.base.entities.StatusItem;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.CaseStatusLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate Case status autocompleters widgets.
 */
public class CaseStatusLookupService extends EntityLookupAndSuggestService {

    protected CaseStatusLookupService(InputProviderInterface provider) {
        super(provider,
              Arrays.asList(CaseStatusLookupConfiguration.OUT_STATUS_ID,
                      CaseStatusLookupConfiguration.OUT_DESCRIPTION));
    }

    /**
     * AJAX event to suggest Case Priority.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestCaseStatus(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CaseStatusLookupService service = new CaseStatusLookupService(provider);
        service.suggestStatus();
        return json.makeSuggestResponse(CaseStatusLookupConfiguration.OUT_STATUS_ID, service);
    }

    /**
     * Gets all Case status.
     * @return the list of Case status <code>StatusItem</code>
     */
    public List<StatusItem> suggestStatus() {
        return findList(StatusItem.class, new EntityExpr(StatusItem.Fields.statusTypeId.name(), EntityOperator.EQUALS, "CUSTREQ_STTS"));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface suggestStatus) {
        return suggestStatus.getString(CaseStatusLookupConfiguration.OUT_DESCRIPTION);
    }
}
