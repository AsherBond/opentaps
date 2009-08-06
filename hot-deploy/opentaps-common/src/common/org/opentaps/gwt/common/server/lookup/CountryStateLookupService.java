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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.gwt.common.client.lookup.configuration.CountryStateLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.domain.base.entities.Geo;
import org.opentaps.domain.base.entities.GeoAssocAndGeoTo;

/**
 * The RPC service used to populate the Country and State autocompleters widgets.
 */
public class CountryStateLookupService extends EntityLookupAndSuggestService {

    private static final EntityCondition COUNTRY_CONDITIONS = new EntityExpr("geoTypeId", EntityOperator.EQUALS, "COUNTRY");
    private static final EntityCondition STATE_CONDITIONS = new EntityExpr("geoTypeId", EntityOperator.EQUALS, "STATE");

    private String geoIdFrom;

    protected CountryStateLookupService(InputProviderInterface provider) {
        super(provider, Arrays.asList(CountryStateLookupConfiguration.OUT_GEO_ID, CountryStateLookupConfiguration.OUT_GEO_NAME));

        geoIdFrom = provider.getParameter(CountryStateLookupConfiguration.IN_COUNTRY_FOR_STATE);
    }

    /**
     * AJAX event to suggest Countries.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestCountries(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CountryStateLookupService service = new CountryStateLookupService(provider);
        service.suggestCountries();
        return json.makeSuggestResponse(CountryStateLookupConfiguration.OUT_GEO_ID, service);
    }

    /**
     * AJAX event to suggest States / Regions.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestStates(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CountryStateLookupService service = new CountryStateLookupService(provider);
        service.suggestStates();
        return json.makeSuggestResponse(CountryStateLookupConfiguration.OUT_GEO_ID, service);
    }

    /**
     * Suggests a list of countries <code>Geo</code>.
     */
    public void suggestStates() {
        if (geoIdFrom != null) {
            suggestGeoAssocAndGeoTo(STATE_CONDITIONS);
        } else {
            suggestGeo(STATE_CONDITIONS);
        }
    }

    /**
     * Suggests a list of states <code>Geo</code>.
     */
    public void suggestCountries() {
        if (geoIdFrom != null) {
            suggestGeoAssocAndGeoTo(COUNTRY_CONDITIONS);
        } else {
            suggestGeo(COUNTRY_CONDITIONS);
        }
    }

    private List<Geo> suggestGeo(EntityCondition geoCondition) {

        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(geoCondition);
        if (getSuggestQuery() != null) {
            List<EntityCondition> suggestConds = new ArrayList<EntityCondition>();
            suggestConds.add(new EntityExpr("geoName", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            suggestConds.add(new EntityExpr("geoCode", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            suggestConds.add(new EntityExpr("abbreviation", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            conds.add(new EntityConditionList(suggestConds, EntityOperator.OR));
        }
        return findList(Geo.class, new EntityConditionList(conds, EntityOperator.AND));
    }

    private List<GeoAssocAndGeoTo> suggestGeoAssocAndGeoTo(EntityCondition geoCondition) {

        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(new EntityExpr("geoAssocTypeId", EntityOperator.EQUALS, "REGIONS"));
        conds.add(new EntityExpr("geoIdFrom", EntityOperator.EQUALS, geoIdFrom));
        if (getSuggestQuery() != null) {
            List<EntityCondition> suggestConds = new ArrayList<EntityCondition>();
            suggestConds.add(new EntityExpr("geoName", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            suggestConds.add(new EntityExpr("geoCode", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            suggestConds.add(new EntityExpr("abbreviation", true, EntityOperator.LIKE, "%" + getSuggestQuery() + "%", true));
            conds.add(new EntityConditionList(suggestConds, EntityOperator.OR));
        }
        return findList(GeoAssocAndGeoTo.class, new EntityConditionList(conds, EntityOperator.AND));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface geo) {
        return geo.getString(CountryStateLookupConfiguration.OUT_GEO_NAME);
    }

}
