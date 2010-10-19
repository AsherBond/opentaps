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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.PartyRelationshipTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.PartyFromByRelnAndContactInfoAndPartyClassification;
import org.opentaps.base.entities.PartyRoleNameDetailSupplementalData;
import org.opentaps.base.entities.PartyToSummaryByRelationship;
import org.opentaps.base.entities.SalesOpportunityRole;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
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

    protected static final EntityCondition CONTACT_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.CONTACT);
    protected static final EntityCondition ACCOUNT_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.ACCOUNT);
    protected static final EntityCondition LEAD_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PROSPECT);
    protected static final EntityCondition PARTNER_CONDITIONS = EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PARTNER);
    protected static final EntityCondition SUPPLIER_CONDITIONS = EntityCondition.makeCondition("roleTypeId", RoleTypeConstants.SUPPLIER);
    protected static final EntityCondition CUSTOMER_CONDITIONS = EntityCondition.makeCondition(EntityOperator.OR,
                                                                    EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.ACCOUNT),
                                                                    EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.CONTACT),
                                                                    EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PROSPECT),
                                                                    EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PARTNER));
    protected static final EntityCondition ACCOUNT_OR_LEAD_CONDITIONS = EntityCondition.makeCondition(EntityOperator.OR,
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.ACCOUNT),
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PROSPECT));
    protected static final EntityCondition ACCOUNT_OR_QUALIFIED_LEAD_CONDITIONS = EntityCondition.makeCondition(EntityOperator.OR,
                                                                                      EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.ACCOUNT),
                                                                                      EntityCondition.makeCondition(EntityOperator.AND,
                                                                                            EntityCondition.makeCondition("roleTypeIdFrom", RoleTypeConstants.PROSPECT),
                                                                                            EntityCondition.makeCondition("statusId", StatusItemConstants.PartyLeadStatus.PTYLEAD_QUALIFIED)));


    protected static final List<String> BY_ID_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_PARTY_ID);
    protected static final List<String> BY_NAME_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_GROUP_NAME,
                                                                  PartyLookupConfiguration.INOUT_COMPANY_NAME,
                                                                  PartyLookupConfiguration.INOUT_FIRST_NAME,
                                                                  PartyLookupConfiguration.INOUT_LAST_NAME);
    protected static final List<String> BY_PHONE_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE,
                                                                   PartyLookupConfiguration.INOUT_PHONE_AREA_CODE,
                                                                   PartyLookupConfiguration.INOUT_PHONE_NUMBER);
    protected static final List<String> BY_EMAIL_FILTERS = Arrays.asList(PartyLookupConfiguration.INOUT_EMAIL);
    protected static final List<String> BY_ADVANCED_FILTERS = Arrays.asList(PartyLookupConfiguration.IN_CLASSIFICATION,
                                                                      PartyLookupConfiguration.INOUT_ADDRESS,
                                                                      PartyLookupConfiguration.INOUT_COUNTRY,
                                                                      PartyLookupConfiguration.INOUT_STATE,
                                                                      PartyLookupConfiguration.INOUT_CITY,
                                                                      PartyLookupConfiguration.INOUT_TO_NAME,
                                                                      PartyLookupConfiguration.INOUT_ATTENTION_NAME,
                                                                      PartyLookupConfiguration.INOUT_POSTAL_CODE);
    protected static final List<String> ACCOUNT_CONTACTS_FILTERS = Arrays.asList(PartyLookupConfiguration.IN_PARTY_ID_TO);

    private boolean activeOnly = true;

    /**
     * Creates a new <code>PartyLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     * @exception InfrastructureException if an error occurs
     */
    public PartyLookupService(InputProviderInterface provider) throws InfrastructureException {
        super(provider, PartyLookupConfiguration.LIST_OUT_FIELDS);
        try {
            setRepository(getDomainsDirectory().getPartyDomain().getPartyRepository());
        } catch (RepositoryException e) {
            throw new InfrastructureException(e);
        }
    }

    protected PartyRepositoryInterface getPartyRepository() throws RepositoryException {
        try {
            return PartyRepositoryInterface.class.cast(getRepository());
        } catch (ClassCastException e) {
            setRepository(getDomainsDirectory().getPartyDomain().getPartyRepository());
            return PartyRepositoryInterface.class.cast(getRepository());
        }
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
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, ACCOUNT_CONDITIONS, RoleTypeConstants.ACCOUNT);
    }

    /**
     * Finds a list of <code>Contact</code>.
     * @return the list of <code>Contact</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findContacts() {
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, CONTACT_CONDITIONS, RoleTypeConstants.CONTACT);
    }

    /**
     * Finds a list of <code>Lead</code>.
     * @return the list of <code>Lead</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findLeads() {
        try {

            // Add general leads conditions.
            EntityCondition leadsCond;

            if ("Y".equals(this.getRepository().getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER))) {
                leadsCond = getPartyRepository().makeLookupLeadsUserIsAllowedToViewCondition();
            } else {
                leadsCond = getPartyRepository().makeLookupLeadsCondition();
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

    /**
     * Finds a list of <code>Partner</code>.
     * @return the list of <code>Partner</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> findPartners() {
        return findParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, PARTNER_CONDITIONS, RoleTypeConstants.PARTNER);
    }

    /**
     * Finds a list of <code>Supplier</code>.
     * @return the list of <code>Supplier</code>, or <code>null</code> if an error occurred
     */
    public List<PartyRoleNameDetailSupplementalData> findSuppliers() {
        // suppliers don't have relationships and classifications, so use the basic party lookup entity
        // also note that to be able to change the entity like this, its fields must be coherent with PartyLookupConfiguration
        setActiveOnly(false);
        return findParties(PartyRoleNameDetailSupplementalData.class, SUPPLIER_CONDITIONS, RoleTypeConstants.SUPPLIER);
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
     * Sets if the lookup methods should filter active parties only, defaults to <code>false</code>.
     * @param bool a <code>boolean</code> value
     */
    public void setActiveOnly(boolean bool) {
        this.activeOnly = bool;
    }

    /**
     * Gets if the lookup methods should filter active parties only, defaults to <code>false</code>.
     * @return a <code>boolean</code> value
     */
    public boolean getActiveOnly() {
        return activeOnly;
    }

    protected List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestParties(EntityCondition roleCondition) {
        // use fast list to replace Arrays.asList because it is read only.
        List<EntityCondition> conditions = FastList.newInstance();
        conditions.add(roleCondition);
        if (getSuggestQuery() == null) {
            return findAllParties(PartyFromByRelnAndContactInfoAndPartyClassification.class, EntityCondition.makeCondition(conditions, EntityOperator.AND));
        }

        // add filter by date and status if active only is set
        if (activeOnly) {
            conditions.add(EntityUtil.getFilterByDateExpr());
            conditions.add(EntityCondition.makeCondition(EntityOperator.OR,
                                                         EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                         EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null)));
        }

        try {

            // format the search string for matching
            String searchString = getSuggestQuery().toUpperCase();

            List<PartyFromByRelnAndContactInfoAndPartyClassification> r = getRepository().findList(PartyFromByRelnAndContactInfoAndPartyClassification.class, EntityCondition.makeCondition(conditions, EntityOperator.AND), getFields(), getPager().getSortList());

            List<PartyFromByRelnAndContactInfoAndPartyClassification> parties = new ArrayList<PartyFromByRelnAndContactInfoAndPartyClassification>();

            // counts the number of records found matching the query
            int matchCount = 0;

            String fullName, firstName, lastName, groupName, compositeName, companyName;

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

                // search the company name
                companyName = party.getCompanyName();
                if (companyName == null) {
                    companyName = "";
                }
                companyName = companyName.toUpperCase();
                if (companyName.indexOf(searchString) > -1) {
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

    /**
     * Adds all the field converters to map the base fields to formatted columns.
     * @param service an <code>EntityLookupAndSuggestService</code> value
     */
    public static void prepareFindParties(EntityLookupAndSuggestService service) {

        /** Phone number custom formatter. */
        class PhoneNumberSortable extends ConvertMapToString implements ICompositeValue {

            /** {@inheritDoc} */
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

            /** {@inheritDoc} */
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(3);
                s.add(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE);
                s.add(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE);
                s.add(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                return s;
            }
        }

        /** Party name custom formatter.<br>
         * Format party name as <code>${firstName} ${lastName} (${partyId})</code> for a person and
         * <code>${groupName} (${partyId})</code> for a party group.
         */
        class FriendlyPartyNameSortable extends ConvertMapToString implements ICompositeValue {

            @Override
            public String convert(Map<String, ?> value) {
                if (value == null) {
                    return null;
                }
                StringBuilder name = new StringBuilder();
                if (value.get("groupName") != null) {
                    name.append(value.get("groupName")).append(" ");
                }
                if (value.get("firstName") != null) {
                    name.append(value.get("firstName")).append(" ");
                }
                if (value.get("lastName") != null) {
                    name.append(value.get("lastName")).append(" ");
                }
                name.append("(").append(value.get("partyId")).append(")");
                return name.toString();
            }

            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(3);
                s.add(PartyLookupConfiguration.INOUT_GROUP_NAME);
                s.add(PartyLookupConfiguration.INOUT_FIRST_NAME);
                s.add(PartyLookupConfiguration.INOUT_LAST_NAME);
                return s;
            }

        }

        /** Voip if enable formatter. */
        class VoipLinkEnableSortable extends ConvertMapToString implements ICompositeValue {
            /** {@inheritDoc} */
            @Override
            public String convert(Map<String, ?> value) {
                String voipEnabled = UtilProperties.getPropertyValue("voip.properties", "voip.enabled", "N");
                String phoneNumber = (String) value.get(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                if ("Y".equals(voipEnabled) && (phoneNumber != null && !"".equals(phoneNumber))) {
                    return "Y";
                } else {
                    return "N";
                }

            }

            /** {@inheritDoc} */
            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> s = new LinkedHashSet<String>(1);
                s.add(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                return s;
            }
        }

        // keep rules for calculated fields
        Map<String, ConvertMapToString> calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER, new PhoneNumberSortable());
        service.makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME, new FriendlyPartyNameSortable());
        service.makeCalculatedField(calcField);

        calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(PartyLookupConfiguration.OUT_VOIP_ENABLED, new VoipLinkEnableSortable());
        service.makeCalculatedField(calcField);
    }

    protected <T extends EntityInterface> List<T> findParties(Class<T> entity, EntityCondition roleCondition, String roleTypeId)  {

        prepareFindParties(this);
        EntityCondition condition = roleCondition;

        // select parties assigned to current user or his team according to view preferences.
        if (getProvider().parameterIsPresent(PartyLookupConfiguration.IN_RESPONSIBILTY)) {
            if (getProvider().getUser().getOfbizUserLogin() != null) {
                setActiveOnly(true);
                String userId = getProvider().getUser().getOfbizUserLogin().getString("partyId");
                String viewPref = getProvider().getParameter(PartyLookupConfiguration.IN_RESPONSIBILTY);
                if (PartyLookupConfiguration.MY_VALUES.equals(viewPref)) {
                    // my parties
                    List<String> partyRelationshipTypeIds = Arrays.asList(PartyRelationshipTypeConstants.RESPONSIBLE_FOR);
                    String showOwnLeadOnly = null;
                    try {
                        showOwnLeadOnly = this.getRepository().getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_MYLEADS_SHOW_OWNED_ONLY);
                    } catch (InfrastructureException e) {
                        Debug.logError(e, MODULE);
                    } catch (RepositoryException e) {
                        Debug.logError(e, MODULE);
                    }
                    // default list only owned by current user
                    if (showOwnLeadOnly == null) {
                        showOwnLeadOnly = "Y";
                    }
                    if (RoleTypeConstants.PROSPECT.equals(roleTypeId) && "N".equals(showOwnLeadOnly)) {
                        partyRelationshipTypeIds = Arrays.asList(PartyRelationshipTypeConstants.ASSIGNED_TO, PartyRelationshipTypeConstants.RESPONSIBLE_FOR);
                    }
                    condition = EntityCondition.makeCondition(
                                    condition,
                                    EntityCondition.makeCondition("partyIdTo", userId),
                                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.IN, partyRelationshipTypeIds));
                } else if (PartyLookupConfiguration.TEAM_VALUES.equals(viewPref)) {
                    // my teams parties
                    condition = EntityCondition.makeCondition(
                                    condition,
                                    EntityCondition.makeCondition("partyIdTo", userId),
                                    EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.IN, Arrays.asList(PartyRelationshipTypeConstants.RESPONSIBLE_FOR, PartyRelationshipTypeConstants.ASSIGNED_TO)));
                }
            } else {
                Debug.logError("Current session do not have any UserLogin set.", MODULE);
            }
        }

        if ("Y".equals(getProvider().getParameter(PartyLookupConfiguration.IN_ACTIVE_PARTIES_ONLY))) {
            setActiveOnly(true);
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

        if (getProvider().oneParameterIsPresent(BY_EMAIL_FILTERS)) {
            return findPartiesBy(entity, condition, BY_EMAIL_FILTERS);
        }

        if (getProvider().oneParameterIsPresent(BY_ADVANCED_FILTERS)) {
            return findPartiesBy(entity, condition, BY_ADVANCED_FILTERS);
        }

        /*
         * Find contacts which are assigned to an account.
         * Usually we use Account.getContacts() to retrieve related contacts but
         * here we query PartyFromByRelnAndContactInfoAndPartyClassification entity
         * to minimize database operations and provide to user full set of possible
         * columns. Otherwise we have to retrieve Account, contacts and their contact
         * mech and all these will have a negative impact on performance.
         */
        if (getProvider().parameterIsPresent(PartyLookupConfiguration.IN_PARTY_ID_TO)) {
            setActiveOnly(true);
            return findPartiesBy(entity,
                    EntityCondition.makeCondition(
                            condition,
                            EntityCondition.makeCondition(PartyLookupConfiguration.IN_ROLE_TO, RoleTypeConstants.ACCOUNT),
                            EntityCondition.makeCondition(PartyLookupConfiguration.IN_RELATIONSHIP_TYPE_ID, PartyRelationshipTypeConstants.CONTACT_REL_INV)
                    ),
                    ACCOUNT_CONTACTS_FILTERS);
        }

        /*
         * Find accounts which are parent for particular contact.
         */
        if (getProvider().parameterIsPresent(PartyLookupConfiguration.IN_PARTY_ID_FROM)) {

            setActiveOnly(true);

            // find list of account ID from PartyToSummaryByRelationship entity.
            String partyIdFrom = getProvider().getParameter(PartyLookupConfiguration.IN_PARTY_ID_FROM);
            if (UtilValidate.isEmail(partyIdFrom)) {
                return new ArrayList<T>();
            }

            Set<String> accountIds = null;
            try {
                accountIds = Entity.getDistinctFieldValues(
                        String.class,
                        getRepository().findList(
                                PartyToSummaryByRelationship.class,
                                EntityCondition.makeCondition(
                                                EntityCondition.makeCondition(PartyToSummaryByRelationship.Fields.roleTypeIdFrom.getName(), RoleTypeConstants.CONTACT),
                                                EntityCondition.makeCondition(PartyToSummaryByRelationship.Fields.roleTypeIdTo.getName(), RoleTypeConstants.ACCOUNT),
                                                EntityCondition.makeCondition(PartyToSummaryByRelationship.Fields.partyRelationshipTypeId.getName(), PartyRelationshipTypeConstants.CONTACT_REL_INV),
                                                EntityCondition.makeCondition(PartyToSummaryByRelationship.Fields.partyIdFrom.getName(), partyIdFrom),
                                                EntityUtil.getFilterByDateExpr())),
                        PartyToSummaryByRelationship.Fields.partyId
                );
            } catch (RepositoryException e) {
                storeException(e);
                return null;
            }
            EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(UtilMisc.toList(
                    condition,
                    EntityCondition.makeCondition(PartyLookupConfiguration.INOUT_PARTY_ID, EntityOperator.IN, accountIds)
            ));
            return findAllParties(entity, conditions);
        }

        /*
         * Find accounts assigned to an opportunity.
         */
        if (getProvider().parameterIsPresent(PartyLookupConfiguration.IN_SALES_OPPORTUNITY_ID)) {
            String opportunityId = getProvider().getParameter(PartyLookupConfiguration.IN_SALES_OPPORTUNITY_ID);
            if (UtilValidate.isNotEmpty(opportunityId)) {
                Set<String> contactIds = null;
                try {
                    contactIds = Entity.getDistinctFieldValues(
                            String.class,
                            getRepository().findList(
                                    SalesOpportunityRole.class,
                                    EntityCondition.makeCondition(
                                                    EntityCondition.makeCondition(SalesOpportunityRole.Fields.roleTypeId.getName(), RoleTypeConstants.CONTACT),
                                                    EntityCondition.makeCondition(SalesOpportunityRole.Fields.salesOpportunityId.getName(), opportunityId))),
                            SalesOpportunityRole.Fields.partyId
                    );
                } catch (RepositoryException e) {
                    storeException(e);
                    return null;
                }
                return findAllParties(entity,
                        EntityCondition.makeCondition(
                        condition,
                        EntityCondition.makeCondition(PartyLookupConfiguration.INOUT_PARTY_ID, EntityOperator.IN, contactIds)
                ));
            }
        }

        return findAllParties(entity, condition);
    }

    protected <T extends EntityInterface> List<T> findAllParties(Class<T> entity, EntityCondition roleCondition) {
        List<EntityCondition> conditions = UtilMisc.toList(roleCondition);
        if (activeOnly) {
            // simple lookups (without relationships) do not support date filtering
            if (entity != PartyRoleNameDetailSupplementalData.class) {
                conditions.add(EntityUtil.getFilterByDateExpr());
            }
            conditions.add(EntityCondition.makeCondition(EntityOperator.OR,
                                                         EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                         EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null)));
        }
        return findList(entity, EntityCondition.makeCondition(conditions, EntityOperator.AND));
    }

    protected <T extends EntityInterface> List<T> findPartiesBy(Class<T> entity, EntityCondition roleCondition, List<String> filters) {
        List<EntityCondition> conditions = new ArrayList<EntityCondition>();
        if (activeOnly) {
            // simple lookups (without relationships) do not support date filtering
            if (entity != PartyRoleNameDetailSupplementalData.class) {
                conditions.add(EntityUtil.getFilterByDateExpr());
            }
            conditions.add(EntityCondition.makeCondition(EntityOperator.OR,
                                                         EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                         EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null)));
        }
        conditions.add(roleCondition);
        return findListWithFilters(entity, conditions, filters);
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
        service.setActiveOnly(true);
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
        service.setActiveOnly(true);
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
        service.setActiveOnly(true);
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

    /**
     * AJAX event to suggest Customers.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestCustomers(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.setActiveOnly(true);
        service.suggestCustomers();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * Suggests a list of customers.
     * @return the list of <code>PartyFromByRelnAndContactInfoAndPartyClassification</code>, or <code>null</code> if an error occurred
     */
    public List<PartyFromByRelnAndContactInfoAndPartyClassification> suggestCustomers() {
        return suggestParties(CUSTOMER_CONDITIONS);
    }


    /**
     * AJAX event to suggest Supplier.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestSuppliers(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyLookupService service = new PartyLookupService(provider);
        service.setActiveOnly(true);
        service.suggestSuppliers();
        return json.makeSuggestResponse(PartyLookupConfiguration.INOUT_PARTY_ID, service);
    }

    /**
     * Suggests a list of suppliers.
     * Note: this is mostly the same as <code>suggestParties</code> but queries the <code>PartyRoleNameDetailSupplementalData</code> entity instead.
     * @return the list of <code>PartyRoleNameDetailSupplementalData</code>, or <code>null</code> if an error occurred
     */
    public List<PartyRoleNameDetailSupplementalData> suggestSuppliers() {
        if (getSuggestQuery() == null) {
            return findAllParties(PartyRoleNameDetailSupplementalData.class, SUPPLIER_CONDITIONS);
        }

        List<EntityCondition> conditions = UtilMisc.toList(SUPPLIER_CONDITIONS);

        // add filter by date and status if active only is set
        if (activeOnly) {
            conditions.add(EntityCondition.makeCondition(EntityOperator.OR,
                                                         EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, StatusItemConstants.PartyStatus.PARTY_DISABLED),
                                                         EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null)));
        }

        try {

            // format the search string for matching
            String searchString = getSuggestQuery().toUpperCase();

            List<PartyRoleNameDetailSupplementalData> r = getRepository().findList(PartyRoleNameDetailSupplementalData.class, EntityCondition.makeCondition(conditions, EntityOperator.AND), getFields(), getPager().getSortList());

            List<PartyRoleNameDetailSupplementalData> parties = new ArrayList<PartyRoleNameDetailSupplementalData>();

            // counts the number of records found matching the query
            int matchCount = 0;

            String fullName, firstName, lastName, groupName, compositeName, companyName;

            for (PartyRoleNameDetailSupplementalData party : r) {
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

                // search the company name
                companyName = party.getCompanyName();
                if (companyName == null) {
                    companyName = "";
                }
                companyName = companyName.toUpperCase();
                if (companyName.indexOf(searchString) > -1) {
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

}
