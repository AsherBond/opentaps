package org.opentaps.warehouse.inventory;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.util.UtilCommon;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.*;

/**
 * Inventory events.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 */
public class InventoryEvents {

    public static final String module = InventoryEvents.class.getName();

    /**
     * 
     * @param request
     * @param statusId
     * @param inventoryTransferId
     * @return isError
     */
    public static boolean checkInventoryForTransfer(HttpServletRequest request, String statusId, String inventoryTransferId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        
        // Ignore updates to statuses other than completed
        if (! statusId.equals("IXF_COMPLETE")) return false;
        
        try {
            GenericValue inventoryTransfer = delegator.findByPrimaryKey("InventoryTransfer", UtilMisc.toMap("inventoryTransferId", inventoryTransferId));
            GenericValue inventoryItem = inventoryTransfer.getRelatedOne("InventoryItem");

            // Ignore intra-facility transfers
            if (UtilValidate.isNotEmpty(inventoryTransfer.getString("facilityId")) && inventoryTransfer.getString("facilityId").equals(inventoryTransfer.getString("facilityIdTo"))) return false;
            
            String productId = inventoryItem.getString("productId");
            if (UtilValidate.isEmpty(productId)) return false;

            BigDecimal quantity = inventoryItem.getBigDecimal("quantityOnHandTotal");
            if (UtilValidate.isEmpty(quantity)) return false;
            
            Map serviceResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", inventoryTransfer.getString("facilityId"), "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                UtilMessage.addError(request, ServiceUtil.getErrorMessage(serviceResult));
                Debug.logError(ServiceUtil.getErrorMessage(serviceResult), module);
                return true;
            }
            BigDecimal quantityOnHandTotal = (BigDecimal) serviceResult.get("quantityOnHandTotal");
            if ((UtilValidate.isNotEmpty(quantityOnHandTotal)) && quantityOnHandTotal.compareTo(quantity) < 0 /* quantityOnHandTotal < quantity */) {
                UtilMessage.addError(request, "WarehouseErrorInventoryItemProductQOHUnderZero", UtilMisc.toMap("productId", productId));
                return true;
            }
        } catch (GenericEntityException e) {
            UtilMessage.addError(request, e.getMessage());
            Debug.logError(e, module);
            return true;
        } catch (GenericServiceException e) {
            UtilMessage.addError(request, e.getMessage());
            Debug.logError(e, module);
            return true;
        }
        return false;
    }
    
    public static String checkInventoryForUpdateTransfer(HttpServletRequest request, HttpServletResponse response) {
        boolean forceComplete = "true".equals(request.getParameter("forceComplete"));
        if (forceComplete) return "success";
        
        String statusId = (String) request.getParameter("statusId");
        String inventoryTransferId = (String) request.getParameter("inventoryTransferId");
        String result = checkInventoryForTransfer(request, statusId, inventoryTransferId) ? "error" : "success";
        if (result.equals("error")) request.setAttribute("forceComplete", "true");
        return result;
    }
    
    public static String checkInventoryForCompleteRequestedTransfers(HttpServletRequest request, HttpServletResponse response) {
        boolean forceComplete = "true".equals(request.getParameter("forceComplete"));
        if (forceComplete) return "success";
        
        String result = "success";
        Collection data = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));
        for (Iterator iter = data.iterator(); iter.hasNext(); ) {
            Map row = (Map) iter.next();
            String statusId = (String) row.get("statusId");
            String inventoryTransferId = (String) row.get("inventoryTransferId");
            if (checkInventoryForTransfer(request, statusId, inventoryTransferId)) result = "error";
        }
        if (result.equals("error")) request.setAttribute("forceComplete", "true");
        return result;
    }

    /**
     * Provides a forceComplete behavior when issuing material to a production run task.  It will check to see
     * if there is enough QOH to meet the issuance requirements, otherwise it will print errors and return forceComplete
     * so that the user can verify the action.
     *
     * This event accepts either a multi form or a single form.
     */
    public static String checkInventoryForMaterialIssuances(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);

        // special flag to force a complete, otherwise we will check the inventory
        boolean forceComplete = "true".equals(request.getParameter("forceComplete"));
        if (forceComplete) {
            request.setAttribute("forceComplete", "false"); // this prevents forceComplete from triggering if the destination is the same page
            return "success";
        }

        Boolean doForceComplete = false;
        Collection<Map<String, Object>> issuanceData = null;
        String workEffortId = UtilCommon.getParameter(request, "workEffortId");
        if (workEffortId != null) {
            Map<String, Object> fields = FastMap.newInstance();
            fields.put("workEffortId", workEffortId);
            fields.put("productId", UtilCommon.getParameter(request, "productId"));
            fields.put("quantity", UtilCommon.getParameter(request, "quantity"));
            fields.put("reasonEnumId", UtilCommon.getParameter(request, "reasonEnumId"));
            fields.put("description", UtilCommon.getParameter(request, "description"));

            issuanceData = FastList.newInstance();
            issuanceData.add(fields);
        } else {
            issuanceData = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));
        }

        try {
            // the getInventoryAvailableByFacility service uses an EntityListIterator which will throw an ugly exception if no transaction is in place.
            TransactionUtil.begin();
            for (Map<String, Object> issuance : issuanceData) {
                Map<String, Object> results = hasInventoryForIssuance(request, issuance);
                if (ServiceUtil.isError(results)) {
                    return "error";
                }
                if (! doForceComplete) {
                    doForceComplete = (Boolean) results.get("doForceComplete");
                }
            }
            TransactionUtil.commit();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, module);
        }

        request.setAttribute("forceComplete", doForceComplete.toString());
        return (doForceComplete) ? "forceComplete" : "success";
    }

    private static Map<String, Object> hasInventoryForIssuance(HttpServletRequest request, Map<String, Object> issuance) throws GeneralException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        boolean doForceComplete = false;

        String workEffortId = (String) issuance.get("workEffortId");
        String productId = (String) issuance.get("productId");
        String quantityStr = (String) issuance.get("quantity");
        if (UtilValidate.isEmpty(quantityStr)) {
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("doForceComplete", doForceComplete);
            return results;
        }

        GenericValue task = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));

        double quantity = 0.0;
        try {
            quantity = Double.valueOf(quantityStr);
        } catch (NumberFormatException e) {
            Integer row = (Integer) issuance.get("row"); // the row number created by UtilHttp.parseMultiFormData()
            UtilMessage.addFieldError(request, row == null ? "qauntity" : "quantity_o_" + row, "OpentapsFieldError_BadDoubleFormat");
            return ServiceUtil.returnError("");
        }

        if (quantity <= 0.0) {
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("doForceComplete", false);
            return results;
        }

        Map<String, Object> serviceResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", task.getString("facilityId"), "userLogin", userLogin));

        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult;
        }

        BigDecimal quantityOnHandTotal = (BigDecimal) serviceResult.get("quantityOnHandTotal");
        // if quantityOnHandTotal < quantity
        if ((UtilValidate.isNotEmpty(quantityOnHandTotal)) && quantityOnHandTotal.compareTo(new BigDecimal(quantity))< 0) {
            UtilMessage.addError(request, "WarehouseErrorInventoryItemProductQOHUnderZero", UtilMisc.toMap("productId", productId));
            doForceComplete = true;
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("doForceComplete", doForceComplete);
        return results;
    }

}
