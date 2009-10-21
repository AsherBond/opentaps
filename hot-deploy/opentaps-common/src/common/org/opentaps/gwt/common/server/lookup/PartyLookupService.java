/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.domain.base.entities.PartyFromByRelnAndContactInfoAndPartyClassification;
import org.opentaps.domain.base.entities.PartyRoleNameDetailSupplementalData;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the PartyListView and Party autocompleters widgets.
 */
public class PartyLookupService extends EntityLookupAndSuggestService {

    private static final String MODULE = PartyLookupService.class.getName();

    private static final EntityCondition CONTACT_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", "CONTACT");
    private static final EntityCondition ACCOUNT_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", "ACCOUNT");
    private static final EntityCondition LEAD_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", "PROSPECT");
    private static final EntityCondition PARTNER_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", "PARTNER");
    private static final EntityCondition SUPPLIER_CONDITIONS = EntityCondition.makeCondition("roleTypeId", "SUPPLIER");
    private static final EntityCondition ACCOUNT_OR_LEAD_CONDITIONS = EntityCondition.makeCondition(EntityOperator.OR,
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", "ACCOUNT"),
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", "PROSPECT"));
    private static final EntityCondition ACCOUNT_OR_QUALIFIED_LEAD_CONDITIONS = EntityCondition.makeCondition(EntityOperator.OR,
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", "ACCOUNT"),
                                                                                      EntityCondition.makeCondition(EntityOperator.AND,
                                                                                            EntityCondition.makeCondition("roleTypeIdFrom", "PROSPECT"),
                                                                                            EntityCondition.makeCondition("statusId", "PTYLEAD_QUALIFIED")));


    private static List<String> BY_ID_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_PARTY_ID);
    private static List<String> BY_NAME_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_GROUP_NAME,
                                                                  PartyLookupConfiguration.INOUT_COMPANY_NAME,
                                                                  PartyLookupConfiguration.INOUT_FIRST_NAME,
                                                                  PartyLookupConfiguration.INOUT_LAST_NAME);
    private static List<String> BY_PHONE_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE,
                                                                   PartyLookupConfiguration.INOUT_PHONE_AREA_CODE,
                                                                   PartyLookupConfiguration.INOUT_PHONE_NUMBER);
    private static List<String> BY_ADVANCED_FILTERS = Arrays.asList(PartyLookupConfiguration.IN_CLASSIFICATION,
                                                                      PartyLookupConfiguration.INOUT_ADDRESS,
                                                                      PartyLookupConfiguration.INOUT_COUNTRY,
                                                                      PartyLookupConfiguration.INOUT_STATE,
                                                                      PartyLookupConfiguration.INOUT_CITY,
                                                                      PartyLookupConfiguration.INOUT_POSTAL_CODE);

    private boolean activeOnly = false;

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public PartyLookupService(InputProviderInterface provider) {
        super(provider, PartyLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups on Contacts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findContacts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.findContacts();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to suggest Contacts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestContacts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.suggestContacts();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * AJAX event to perform lookups on Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findAccounts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.findAccounts();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to suggest Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestAccounts(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.suggestAccounts();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
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
     * AJAX event to perform lookups on Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findPartners(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.findPartners();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * AJAX event to perform lookups on Suppliers.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findSuppliers(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.findSuppliers();
        return json.makeLookupResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Account</code>.
     * @return the list of <code>Account</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findAccounts() {
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, ACCOUNT_CONDITIONS);
    }

    /**
     * Finds a list of <code>Contact</code>.
     * @return the list of <code>Contact</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findContacts() {
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, CONTACT_CONDITIONS);
    }

    /**
     * Finds a list of <code>Lead</code>.
     * @return the list of <code>Lead</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findLeads() {
        return findParties(
                PartyFromByRelnAndContactInfoAndPartyClassification.class,
                EntityCondition.makeCondition(
                        UtilMisc.toList(
                                LEAD_CONDITIONS,
                                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PTYLEAD_CONVERTED")
                        )
                )
        );
    }

    /**
     * Finds a list of <code>Partner</code>.
     * @return the list of <code>Partner</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findPartners() {
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, PARTNER_CONDITIONS);
    }

    /**
     * Finds a list of <code>Supplier</code>.
     * @return the list of <code>Supplier</code>, or <code>null</code> if an error occurred
     */
    public List<PartyRoleNameDetailSupplementalData> findSuppliers() {
        // suppliers don't have relationships and classifications, so use the basic party lookup entity
        // also note that to be able to change the entity like this, its fields must be coherent with PartyLookupConfiguration
        setActiveOnly(false);
        return findParties(PartyRoleNameDetailSupplementalData.class, SUPPLIER_CONDITIONS);
    }

    /**
     * Suggests a list of <code>Account</code>.
     * @return the list of <code>Account</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestAccounts() {
        return suggestParties(ACCOUNT_CONDITIONS);
    }

    /**
     * Suggests a list of contacts.
     * @return the list of <code>Contact</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestContacts() {
        return suggestParties(CONTACT_CONDITIONS);
    }

    /**
     * Sets if the lookup methods should filter active parties only, defaults to <code>true</code>.
     * @param bool a <code>boolean</code> value
     */
    public void setActiveOnly(boolean bool) {
        this.activeOnly = bool;
    }

    private List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestParties(EntityCondition roleCondition) {

        List<EntityCondition> conditions = Arrays.asList(roleCondition);
        // add filter by date is active only is set
        if (activeOnly) {
            conditions.add(EntityUtil.getFilterByDateExpr());
        }

        if (getSuggestQuery() == null) {
            return findAllParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, EntityCondition.makeCondition(conditions, EntityOperator.AND));
        }

        try {

            // format the search string for matching
            String searchString = getSuggestQuery().toUpperCase();

            List<PartyFromByRelnAndContactInfoAndPartyClassification> r = getRepository().findList(PartyFromByRelnAndContactInfoAndPartyClassification.class, EntityCondition.makeCondition(conditions, EntityOperator.AND), getFields(), getPager().getSortList());

            List<PartyFromByRelnAndContactInfoAndPartyClassification> parties = new ArrayList<PartyFromByRelnAndContactInfoAndPartyClassification>();

            // counts the number of records found matching the query
            int matchCount = 0;

            String fullName, firstName, lastName, groupName, compositeName;

            for (PartyFromByRelnAndContactInfoAndPartyClassification party : r) {
                if (matchCount > UtilLookup.SUGGEST_MAX_RESULTS) {
                    break;
                }

                // search the full name
                fullName = "";
                firstName = party.getFirstName();
                if (firstName != null) {
                    fullName = firstName;
                }

                lastName = party.getLastName();
                if (lastName != null) {
                    fullName = fullName + " " + lastName;
                }

                fullName = fullName.toUpperCase();
                if (fullName.indexOf(searchString) > -1) {
                    parties.add(party);
                    matchCount++;
                    continue;
                }

                // search the group name
                groupName = party.getGroupName();
                if (groupName == null) {
                    groupName = "";
                }
                groupName = groupName.toUpperCase();
                if (groupName.indexOf(searchString) > -1) {
                    parties.add(party);
                    matchCount++;
                    continue;
                }

                // search the composite name (incidentally, this also matches partyId)
                compositeName = groupName;
                if (fullName.trim().length() > 0) {
                    compositeName = compositeName + " " + fullName;
                }
                compositeName = compositeName + " (" + party.getPartyId().toUpperCase() + ")";
                if (compositeName.indexOf(searchString) > -1) {
                    parties.add(party);
                    matchCount++;
                    continue;
                }
            }

            // get paginated results
            paginateResults(parties);

        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            return null;
        }

        return getResults();
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface party) {
        StringBuffer sb = new StringBuffer();
        String firstName = party.getString("firstName");
        String middleName = party.getString("middleName");
        String lastName = party.getString("lastName");
        String groupName = party.getString("groupName");
        String partyId = party.getString("partyId");
        if (UtilValidate.isNotEmpty(groupName)) {
            sb.append(groupName);
        } else {
            sb.append(firstName);
            if (UtilValidate.isNotEmpty(middleName)) {
                sb.append(" ").append(middleName);
            }
            if (UtilValidate.isNotEmpty(lastName)) {
                sb.append(" ").append(lastName);
            }
        }
        sb.append(" (").append(partyId).append(")");

        return sb.toString();
    }

    private <T extends EntityInterface> List<T> findParties(Class<T> entity, EntityCondition roleCondition) {
        // add rule that causes formated primary phone to be added to result
        class PhoneNumberSortable extends ConvertMapToString implements ICompositeValue {

            /* (non-Javadoc)
             * @see org.opentaps.common.util.ConvertMapToString#convert(java.util.Map)
             */
            @Override
            public String convert(Map<String, ?> value) {
                StringBuilder sb = new StringBuilder();
                String s = (String) value.get(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE);
                if (UtilValidate.isNotEmpty(s)) {
                    sb.append(s);
                }
                s = (String) value.get(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE);
                if (UtilValidate.isNotEmpty(s)) {
                    sb.append(" ").append(s);
                }
                s = (String) value.get(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                if (UtilValidate.isNotEmpty(s)) {
                    sb.append(" ").append(s);
                }
                String phoneNumber = sb.toString();
                if (UtilValidate.isNotEmpty(phoneNumber)) {
                    return phoneNumber.trim();
                } else {
                    return "";
                }
            }

            /* (non-Javadoc)
             * @see org.opentaps.common.util.ICompositeValue#getFields()
             */
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(3);
                s.add(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE);
                s.add(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE);
                s.add(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                return s;
            }
        }

        Map<String, ConvertMapToString> calcField = FastMap.newInstance();
        calcField.put("formatedPrimaryPhone", new PhoneNumberSortable());
        makeCalculatedField(calcField);

        EntityCondition condition = roleCondition;

        // select parties assigned to current user or his team according to view preferences.
        if (getProvider().parameterIsPresent(PartyLookupConfiguration.IN_RESPONSIBILTY)) {
            if (getProvider().getUser().getOfbizUserLogin() != null) {
                String userId = getProvider().getUser().getOfbizUserLogin().getString("partyId");
                String viewPref = getProvider().getParameter(PartyLookupConfiguration.IN_RESPONSIBILTY);
                if (PartyLookupConfiguration.MY_VALUES.equals(viewPref)) {
                    // my parties
                    condition = EntityCondition.makeCondition(
                            Arrays.asList(
                                    condition,
                                    EntityCondition.makeCondition("partyIdTo", userId),
                                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.IN, Arrays.asList("RESPONSIBLE_FOR"))
                            ),
                            EntityOperator.AND
                    );
                } else if (PartyLookupConfiguration.TEAM_VALUES.equals(viewPref)) {
                    // my teams parties
                    condition = EntityCondition.makeCondition(
                            Arrays.asList(
                                    condition,
                                    EntityCondition.makeCondition("partyIdTo", userId),
                                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.IN, Arrays.asList("RESPONSIBLE_FOR", "ASSIGNED_TO"))
                            ),
                            EntityOperator.AND
                    );
                }
            } else {
                Debug.logError("Current session do not have any UserLogin set.", MODULE);
            }
        }

        if (getProvider().oneParameterIsPresent(BY_ID_FILTERS)) {
            return findPartiesBy(entity, condition, BY_ID_FILTERS);
        }

        if (getProvider().oneParameterIsPresent(BY_NAME_FILTERS)) {
            return findPartiesBy(entity, condition, BY_NAME_FILTERS);
        }

        if (getProvider().oneParameterIsPresent(BY_PHONE_FILTERS)) {
            return findPartiesBy(entity, condition, BY_PHONE_FILTERS);
        }

        if (getProvider().oneParameterIsPresent(BY_ADVANCED_FILTERS)) {
            return findPartiesBy(entity, condition, BY_ADVANCED_FILTERS);
        }

        return findAllParties(entity, condition);
    }

    private <T extends EntityInterface> List<T> findAllParties(Class<T> entity, EntityCondition roleCondition) {
        List<EntityCondition> conditions = Arrays.asList(roleCondition);
        if (activeOnly) {
            conditions.add(EntityUtil.getFilterByDateExpr());
        }
        return findList(entity, EntityCondition.makeCondition(conditions, EntityOperator.AND));
    }

    private <T extends EntityInterface> List<T> findPartiesBy(Class<T> entity, EntityCondition roleCondition, List<String> filters) {
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        if (activeOnly) {
            conds.add(EntityUtil.getFilterByDateExpr());
        }
        conds.add(roleCondition);
        return findListWithFilters(entity, conds, filters);
    }

    /**
     * AJAX event to suggest Leads.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestLeads(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.suggestLeads();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * Suggests a list of leads.
     * @return the list of <code>Lead</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestLeads() {
        return suggestParties(LEAD_CONDITIONS);
    }

    /**
     * AJAX event to suggest Accounts or Leads.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestAccountsOrLeads(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.suggestAccountsOrLeads();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * Suggests a list of accounts or leads.
     * @return the list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestAccountsOrLeads() {
        return suggestParties(ACCOUNT_OR_LEAD_CONDITIONS);
    }

    /**
     * AJAX event to suggest Accounts or Qualified Leads.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestAccountsOrQualifiedLeads(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.suggestAccountsOrQualifiedLeads();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * Suggests a list of accounts or leads.
     * @return the list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestAccountsOrQualifiedLeads() {
        return suggestParties(ACCOUNT_OR_QUALIFIED_LEAD_CONDITIONS);
    }

}

