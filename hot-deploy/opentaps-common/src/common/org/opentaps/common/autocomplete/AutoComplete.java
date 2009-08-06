package org.opentaps.common.autocomplete;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import static org.opentaps.common.autocomplete.UtilAutoComplete.*;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.util.UtilCommon;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Catch all location for static auto complete methods.
 */
public class AutoComplete {

    public static final String module = AutoComplete.class.getName();

    /**
     * Retrieves the auto complete clients party IDs with a given keyword.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompleteClientPartyIds(HttpServletRequest request, HttpServletResponse response) {
        // get active clients (but can be related to another user)
        EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                                   EntityUtil.getFilterByDateExpr(),
                                   ac_clientRoleCondition
                                   ), EntityOperator.AND);
        return autocompletePartyIdsByCondition(condition, "PartyFromSummaryByRelationship", request, response);
    }

    /**
     * Retrieves the auto complete accounts party IDs with a given keyword.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompleteAccountPartyIds(HttpServletRequest request, HttpServletResponse response) {
        // get active accounts (but can be related to another user)
        EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                                   EntityUtil.getFilterByDateExpr(),
                                   ac_accountRoleCondition
                                   ), EntityOperator.AND);
        return autocompletePartyIdsByCondition(condition, "PartyFromSummaryByRelationship", request, response);
    }

    /**
     * Retrieves the auto complete suppliers party IDs with a given keyword.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompleteSupplierPartyIds(HttpServletRequest request, HttpServletResponse response) {
        // get suppliers (but can be related to another user)
        EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER")
                ), EntityOperator.AND
            );
        return autocompletePartyIdsByCondition(condition, "PartyRoleNameDetail", request, response);
    }

    /**
     * Retrieves the auto complete any party IDs with a given keyword.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompletePartyIds(HttpServletRequest request, HttpServletResponse response) {
        // get any party (but can be related to another user)
        return autocompletePartyIdsByCondition(null, "PartySummaryCRMView", request, response);
    }

    private static String autocompletePartyIdsByCondition(EntityCondition condition, String entityName, HttpServletRequest request, HttpServletResponse response) {

        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            Debug.logError("Failed to retrieve the login user from the session.", module);
            return "error";
        }

        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        String keyword = UtilCommon.getUTF8Parameter(request, "keyword");
        if (keyword == null) {
            Debug.log("Ignored the empty keyword string.", module);
            return "success";
        }
        keyword = keyword.trim();

        List<GenericValue> parties = FastList.newInstance();
        if (keyword.length() > 0) try {
            // get result as a list iterator (transaction block is to work around a bug in entity engine)
            TransactionUtil.begin();
            EntityListIterator iterator = delegator.findListIteratorByCondition(entityName, condition, null, AC_PARTY_NAME_FIELDS, AP_PARTY_ORDER_BY, AC_FIND_OPTIONS);

            // perform the search
            parties = searchPartyName(iterator, keyword);

            // clean up
            iterator.close();
            TransactionUtil.commit();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }

        // write the JSON data to the response stream
        return makeSelectionJSONResponse(response, parties, "partyId", new PartySelectionBuilder());
    }

    /**
     * Search parties which name is matching the search string.
     * @param iterator an <code>Iterator</code> of parties
     * @param searchString a <code>String</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static List<GenericValue> searchPartyName(Iterator<GenericValue> iterator, String searchString) throws GenericEntityException {
        ArrayList<GenericValue> parties = new ArrayList<GenericValue>();

        // format the search string for matching
        searchString = searchString.toUpperCase();

        int results = 0;
        GenericValue party = null;
        String compositeName;

        while (((party = iterator.next()) != null) && (results <= AC_DEFAULT_RESULTS_SIZE)) {

            compositeName = PartyReader.getPartyCompositeName(party).toUpperCase();

            // search the composite name which matches partyId, groupName, firstName and lastName
            if (compositeName.indexOf(searchString) > -1) {
                parties.add(party);
                results++;
                continue;
            }
        }

        return parties;
    }

    /**
     * Retrieves the auto complete GL Account IDs with a given keyword.
     * It will match the keyword against either the GL Account name or the account code.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompleteGlAccounts(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            Debug.logError("Failed to retrieve the login user from the session.", module);
            return "error";
        }
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        HttpSession session = request.getSession();
        String organizationPartyId = (String) session.getAttribute("organizationPartyId");

        String keyword = UtilCommon.getUTF8Parameter(request, "keyword");
        if (keyword == null) {
            Debug.log("Ignored the empty keyword string.", module);
            return "success";
        }
        keyword = keyword.trim();

        List<GenericValue> accounts = FastList.newInstance();
        if (keyword.length() > 0) try {
            keyword = keyword.toUpperCase();

            // make the condition
            EntityCondition orCondition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("glAccountId", EntityOperator.LIKE, "%" + keyword + "%"),
                    new EntityExpr("accountCode", EntityOperator.LIKE, "%" + keyword + "%"),
                    new EntityExpr("accountName", true, EntityOperator.LIKE, "%" + keyword + "%", false)
                ), EntityOperator.OR
            );
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    orCondition,
                    new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                    EntityUtil.getFilterByDateExpr()
            ), EntityOperator.AND);


            // get result as a list iterator (transaction block is to work around a bug in entity engine)
            TransactionUtil.begin();
            EntityListIterator iterator = delegator.findListIteratorByCondition("GlAccountOrganizationAndClass", condition, null, AC_ACCOUNT_FIELDS, AC_ACCOUNT_ORDER_BY, AC_FIND_OPTIONS);

            // the condition search is sufficient, so we'll just get the entire list
            accounts = iterator.getCompleteList();

            // clean up
            iterator.close();
            TransactionUtil.commit();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }

        // write the JSON data to the response stream
        return makeSelectionJSONResponse(response, accounts, "glAccountId", new GlAccountSelectionBuilder());
    }

    /**
     * Retrieves the auto complete Product IDs with a given keyword.
     * It will match the keyword against either the Product ID or any good ID.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String getAutoCompleteProduct(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            Debug.logError("Failed to retrieve the login user from the session.", module);
            return "error";
        }
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        String keyword = UtilCommon.getUTF8Parameter(request, "keyword");
        if (keyword == null) {
            Debug.log("Ignored the empty keyword string.", module);
            return "success";
        }
        keyword = keyword.trim();

        List<GenericValue> products = new FastList<GenericValue>();
        if (keyword.length() > 0) try {
            keyword = keyword.toUpperCase();

            // make the condition
            EntityCondition keywordCondition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("productId", EntityOperator.LIKE, keyword + "%"),
                    new EntityExpr("idValue", EntityOperator.LIKE, keyword + "%"),
                    new EntityExpr("internalName", true, EntityOperator.LIKE, "%" + keyword + "%", false)
                ), EntityOperator.OR
            );

            EntityCondition dateCondition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.EQUALS, null),
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp())
                ), EntityOperator.OR
            );

            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    keywordCondition,
                    dateCondition
                ), EntityOperator.AND
            );

            // get result as a list iterator (transaction block is to work around a bug in entity engine)
            TransactionUtil.begin();
            EntityListIterator iterator = delegator.findListIteratorByCondition("ProductAndGoodIdentification", condition, null, AC_PRODUCT_FIELDS, AC_PRODUCT_ORDER_BY, AC_FIND_OPTIONS);

            // the condition search is sufficient, so we'll just get the entire list
            products = iterator.getCompleteList();

            // clean up
            iterator.close();
            TransactionUtil.commit();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }

        // write the JSON data to the response stream
        return makeSelectionJSONResponse(response, products, "productId", new ProductSelectionBuilder());
    }

    public static class PartySelectionBuilder implements SelectionBuilder {
        public Map<String, Object> buildRow(Object element) {
            GenericValue party = (GenericValue) element;

            // party ID, full name
            String partyId = party == null ? null : party.getString("partyId");
            String compositeName = PartyReader.getPartyCompositeName(party);

            return UtilMisc.<String, Object>toMap("name", compositeName, "partyId", partyId);
        }
    }

    public static class GlAccountSelectionBuilder implements SelectionBuilder {
        public Map<String, Object> buildRow(Object element) {
            GenericValue account = (GenericValue) element;
            return UtilMisc.<String, Object>toMap("name", account.getString("accountCode") + ":" + account.getString("accountName"), "glAccountId", account.getString("glAccountId"));
        }
    }

    public static class ProductSelectionBuilder implements SelectionBuilder {
        public Map<String, Object> buildRow(Object element) {
            GenericValue product = (GenericValue) element;
            return UtilMisc.<String, Object>toMap("name", product.getString("productId") + ":" + product.getString("internalName"), "productId", product.getString("productId"));
        }
    }

}
