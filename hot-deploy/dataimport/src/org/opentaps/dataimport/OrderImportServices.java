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
package org.opentaps.dataimport;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.constants.ContactMechTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.*;
import org.opentaps.common.party.PartyContactHelper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Import orders via intermediate DataImportOrderHeader and DataImportOrderItem entities.
 *
 * @author <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version $Rev$
 */
public class OrderImportServices {

    private static String MODULE = OrderImportServices.class.getName();
    public static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    // this constant value is used in various places
    protected static final String defaultShipGroupSeqId = "00001";

    /**
     * Describe <code>importOrders</code> method here.
     *
     * @param dctx    a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map importOrders(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String companyPartyId = (String) context.get("companyPartyId");
        Boolean readProductStoreFromTable = (Boolean) context.get("readProductStoreFromTable");
        String productStoreId = (String) context.get("productStoreId");
        String prodCatalogId = (String) context.get("prodCatalogId");
        // AG24012008: purchaseOrderShipToContactMechId is needed to get the MrpOrderInfo.shipGroupContactMechId used by the MRP run
        String purchaseOrderShipToContactMechId = (String) context.get("purchaseOrderShipToContactMechId");
        Boolean importEmptyOrders = (Boolean) context.get("importEmptyOrders");
        Boolean calculateGrandTotal = (Boolean) context.get("calculateGrandTotal");
        Boolean reserveInventory = (Boolean) context.get("reserveInventory");
        Boolean readShippingAddressFromTable = (Boolean) context.get("readShippingAddressFromTable");
        if (reserveInventory == null) {
            reserveInventory = Boolean.FALSE;
        }

        int imported = 0;

        // main try/catch block that traps errors related to obtaining data from delegator
        try {
            GenericValue productStore = null;
            if (!readProductStoreFromTable) {
                // if We take the product store form service parameters we make sure the productStore exists
                productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));

                if (UtilValidate.isEmpty(productStore)) {
                    String errMsg = "Error in importOrders service: product store [" + productStoreId + "] does not exist";
                    Debug.logError(errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }


            // Make sure the productCatalog exists
            if (UtilValidate.isNotEmpty(prodCatalogId)) {
                GenericValue productCatalog = delegator.findByPrimaryKey("ProdCatalog", UtilMisc.toMap("prodCatalogId", prodCatalogId));

                if (UtilValidate.isEmpty(productCatalog)) {
                    String errMsg = "Error in importOrders service: product catalog [" + productCatalog + "] does not exist";
                    Debug.logError(errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }

            // Make sure the company party exists
            GenericValue companyParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", companyPartyId));

            if (UtilValidate.isEmpty(companyParty)) {
                String errMsg = "Error in importOrders service: company party [" + companyPartyId + "] does not exist";
                Debug.logError(errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }

            // Ensure the party role for the company
            List billFromRoles = companyParty.getRelatedByAnd("PartyRole", UtilMisc.toMap("roleTypeId", "BILL_FROM_VENDOR"));
            if (billFromRoles.size() == 0) {
                delegator.create("PartyRole", UtilMisc.toMap("partyId", companyPartyId, "roleTypeId", "BILL_FROM_VENDOR"));
            }

            // need to get an ELI because of possibly large number of records.  productId <> null will get all records
            EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                    EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null),
                    statusCond   // leave out previously processed orders
            );
            TransactionUtil.begin();   // since the service is not inside a transaction, this needs to be in its own transaction, or you'll get a harmless exception
            EntityListIterator importOrderHeaders = delegator.findListIteratorByCondition("DataImportOrderHeader", conditions, null, null);
            TransactionUtil.commit();

            GenericValue orderHeader = null;
            while ((orderHeader = importOrderHeaders.next()) != null) {

                try {

                    List<GenericValue> toStore = OrderImportServices.decodeOrder(orderHeader, companyPartyId, productStore, prodCatalogId, purchaseOrderShipToContactMechId, importEmptyOrders.booleanValue(), calculateGrandTotal.booleanValue(), readShippingAddressFromTable.booleanValue(), reserveInventory, delegator, dispatcher, userLogin);
                    if (toStore == null) {
                        Debug.logWarning("Import of orderHeader[" + orderHeader.get("orderId") + "] was unsuccessful.", MODULE);
                    }

                    TransactionUtil.begin();

                    delegator.storeAll(toStore);

                    // make reservation if requested
                    if (reserveInventory && !orderHeader.getBoolean("orderClosed") && "SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                        Debug.logInfo("Starting product reservation against order [" + orderHeader.getString("orderId") + "]", MODULE);

                        String reserveOrderEnumId = productStore.getString("reserveOrderEnumId");
                        if (UtilValidate.isEmpty(reserveOrderEnumId)) {
                            reserveOrderEnumId = "INVRO_FIFO_REC";
                        }

                        for (GenericValue currentEntity : toStore) {
                            String entityName = currentEntity.getEntityName();
                            if (!"OrderItem".equals(entityName)) {
                                continue;
                            }

                            // we have order item
                            Debug.logInfo("Reserve order item [" + currentEntity.getString("orderItemSeqId") + "]", MODULE);
                            Map<String, Object> callCtxt = FastMap.newInstance();
                            callCtxt.put("productStoreId", productStoreId);
                            callCtxt.put("productId", currentEntity.getString("productId"));
                            callCtxt.put("orderId", currentEntity.getString("orderId"));
                            callCtxt.put("orderItemSeqId", currentEntity.getString("orderItemSeqId"));
                            callCtxt.put("shipGroupSeqId", defaultShipGroupSeqId);
                            callCtxt.put("quantity", currentEntity.getDouble("quantity"));
                            callCtxt.put("userLogin", userLogin);

                            Map<String, Object> callResult = dispatcher.runSync("reserveStoreInventory", callCtxt);
                            if (ServiceUtil.isError(callResult)) {
                                Debug.logWarning("reserveStoreInventory returned error " + ServiceUtil.getErrorMessage(callResult), MODULE);
                                TransactionUtil.rollback();
                            }

                            Debug.logWarning("The order item is reserved successfully", MODULE);
                        }
                    }

                    //change status of the payment
                    // we have the payment we now change the status to PMNT_RECEIVED to do ledger posting

                    for (GenericValue currentEntity : toStore) {
                        String entityName = currentEntity.getEntityName();
                        if (!"Payment".equals(entityName)) {
                            continue;
                        }

                        //we hav the payment
                        String paymentId = currentEntity.getString("paymentId");
                        Debug.logInfo("Changing payment status for [" + paymentId + "]", MODULE);
                        Map results = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("userLogin", userLogin, "paymentId", paymentId, "statusId", "PMNT_RECEIVED"));
                    }


                    Debug.logInfo("Successfully imported orderHeader [" + orderHeader.get("orderId") + "].", MODULE);
                    imported++;

                    TransactionUtil.commit();

                } catch (GenericEntityException e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, "Failed to import orderHeader[" + orderHeader.get("orderId") + "]. Error stack follows.", MODULE);
                    // store the import error
                    String message = "Failed to import Order " + orderHeader.getPkShortValueString() + ": " + e.getMessage();
                    storeImportError(orderHeader, message, delegator);
                } catch (Exception e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, "Import of orderHeader[" + orderHeader.get("orderId") + "] was unsuccessful. Error stack follows.", MODULE);
                    // store the import error
                    String message = "Failed to import Order " + orderHeader.getPkShortValueString() + ": " + e.getMessage();
                    storeImportError(orderHeader, message, delegator);
                }
            }
            importOrderHeaders.close();

        } catch (GenericEntityException e) {
            String errMsg = "Error in importOrders service: " + e.getMessage();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        Map results = ServiceUtil.returnSuccess();
        results.put("ordersImported", new Integer(imported));
        return results;
    }

    /**
     * Helper method to store import error to DataImportOrderHeader/DataImportOrderItem entities.
     *
     * @param dataImportOrderHeader a <code>GenericValue</code> value
     * @param message               a <code>String</code> value
     * @param delegator             a <code>GenericDelegator</code> value
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void storeImportError(GenericValue dataImportOrderHeader, String message, GenericDelegator delegator) throws GenericEntityException {
        // OrderItems
        EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
        List orderItemConditions = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, dataImportOrderHeader.getString("orderId")), statusCond);
        List<GenericValue> dataImportOrderItems = delegator.findByCondition("DataImportOrderItem", EntityCondition.makeCondition(orderItemConditions, EntityOperator.AND), null, null);
        for (GenericValue dataImportOrderItem : dataImportOrderItems) {
            // store the exception and mark as failed
            dataImportOrderItem.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_FAILED);
            dataImportOrderItem.set("processedTimestamp", UtilDateTime.nowTimestamp());
            dataImportOrderItem.set("importError", message);
            dataImportOrderItem.store();
        }
        // store the exception and mark as failed
        dataImportOrderHeader.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_FAILED);
        dataImportOrderHeader.set("processedTimestamp", UtilDateTime.nowTimestamp());
        dataImportOrderHeader.set("importError", message);
        dataImportOrderHeader.store();
    }

    /**
     * Helper method to decode a DataImportOrderHeader/DataImportOrderItem into a List of GenericValues modeling that product in the OFBiz schema.
     * If for some reason obtaining data via the delegator fails, this service throws that exception.
     * Note that everything is done with the delegator for maximum efficiency.
     *
     * @param externalOrderHeader a <code>GenericValue</code> value
     * @param companyPartyId      a <code>String</code> value
     * @param productStore        a <code>GenericValue</code> value
     * @param prodCatalogId       a <code>String</code> value
     * @param purchaseOrderShipToContactMechId
     *
     * @param importEmptyOrders   a <code>boolean</code> value
     * @param calculateGrandTotal a <code>boolean</code> value
     * @param reserveInventory    a <code>boolean</code> value
     * @param delegator           a <code>GenericDelegator</code> value
     * @param userLogin           a <code>GenericValue</code> value
     * @return a <code>List</code> value
     * @throws GenericEntityException if an error occurs
     * @throws Exception              if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List decodeOrder(GenericValue externalOrderHeader, String companyPartyId, GenericValue productStore, String prodCatalogId, String purchaseOrderShipToContactMechId, boolean importEmptyOrders, boolean calculateGrandTotal, boolean reserveInventory, boolean readShippingAddressFromTable, GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException, Exception {
        //todo move this at the beginning of the class
        DataImportOrderHeader dataImportOrderHeader = new DataImportOrderHeader();
        dataImportOrderHeader.fromMap(externalOrderHeader);

        //if the productStore is null we try to load it from the table if it does not exist we throw an error
        if (productStore == null) {
            String productStoreId = (String) externalOrderHeader.getString("productStoreId");
            productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));

            if (UtilValidate.isEmpty(productStore)) {
                String errMsg = "Error in importOrders service: product store [" + productStoreId + "] does not exist";
                Debug.logError(errMsg, MODULE);
                return FastList.newInstance();
            }
        }
        String orderId = externalOrderHeader.getString("orderId");
        String orderTypeId = externalOrderHeader.getString("orderTypeId");
        if (UtilValidate.isEmpty(orderTypeId)) {
            orderTypeId = "SALES_ORDER";
        }
        Timestamp orderDate = externalOrderHeader.getTimestamp("orderDate");
        if (UtilValidate.isEmpty(orderDate)) {
            orderDate = UtilDateTime.nowTimestamp();
        }
        boolean isSalesOrder = "SALES_ORDER".equals(orderTypeId);
        boolean isPurchaseOrder = "PURCHASE_ORDER".equals(orderTypeId);

        Debug.logInfo("Importing orderHeader[" + orderId + "]", MODULE);

        // Check to make sure that an order with this ID doesn't already exist
        GenericValue existingOrderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        if (existingOrderHeader != null) {
            Debug.logError("Ignoring duplicate orderHeader[" + orderId + "]", MODULE);
            return FastList.newInstance();
        }

        String orderStatusId = externalOrderHeader.getBoolean("orderClosed").booleanValue() ? "ORDER_COMPLETED" : "ORDER_APPROVED";

        // OrderHeader

        Map orderHeaderInput = FastMap.newInstance();
        orderHeaderInput.put("orderId", orderId);
        orderHeaderInput.put("orderTypeId", orderTypeId);
        orderHeaderInput.put("orderName", orderId);
        orderHeaderInput.put("externalId", orderId);
        String salesChannelEnumId = (String) externalOrderHeader.getString("salesChannelEnumId");
        if (UtilValidate.isNotEmpty(salesChannelEnumId)) {
            orderHeaderInput.put("salesChannelEnumId", salesChannelEnumId); //todo we should validate the enum code against enumTypeId="ORDER_SALES_CHANNEL"
        } else {
            orderHeaderInput.put("salesChannelEnumId", "UNKNWN_SALES_CHANNEL");
        }

        orderHeaderInput.put("orderDate", orderDate);
        orderHeaderInput.put("entryDate", UtilDateTime.nowTimestamp());
        orderHeaderInput.put("statusId", orderStatusId);
        orderHeaderInput.put("currencyUom", externalOrderHeader.get("currencyUomId"));
        orderHeaderInput.put("remainingSubTotal", new Double(0));
        orderHeaderInput.put("productStoreId", isPurchaseOrder ? "PURCHASING" : productStore.getString("productStoreId"));

        // main customer and bill to party
        String customerPartyId = externalOrderHeader.getString("customerPartyId");
        String supplierPartyId = externalOrderHeader.getString("supplierPartyId");
        orderHeaderInput.put("billFromPartyId", isPurchaseOrder ? supplierPartyId : companyPartyId);
        orderHeaderInput.put("billToPartyId", isPurchaseOrder ? companyPartyId : customerPartyId);

        List orderAdjustments = new ArrayList();

        // todo:Make orderAdjustments from adjustmentsTotal and taxTotal
        // Record an OrderStatus
        Map orderStatusInput = UtilMisc.toMap("orderStatusId", delegator.getNextSeqId("OrderStatus"), "orderId", orderId, "statusId", orderStatusId, "statusDatetime", orderDate, "statusUserLogin", userLogin.getString("userLoginId"));

        // purchase orders must be assigned to a ship group
        GenericValue oisg = null;
        if (isPurchaseOrder) {
            oisg = delegator.makeValue("OrderItemShipGroup");
            oisg.put("orderId", orderId);
            oisg.put("shipGroupSeqId", defaultShipGroupSeqId);
            oisg.put("carrierPartyId", "_NA_");
            oisg.put("carrierRoleTypeId", "CARRIER");
            oisg.put("maySplit", "N");
            oisg.put("isGift", "N");
            if (UtilValidate.isNotEmpty(purchaseOrderShipToContactMechId)) {
                oisg.put("contactMechId", purchaseOrderShipToContactMechId);
            }

        }

        // create a ship group for the sales order
         List postalAddressEntities = FastList.newInstance();
        if (isSalesOrder) {
            // get requested shipping method
            String productStoreShipMethId = externalOrderHeader.getString("productStoreShipMethId");

            GenericValue shipMeth = delegator.findByPrimaryKeyCache("ProductStoreShipmentMeth", UtilMisc.toMap("productStoreShipMethId", productStoreShipMethId));
            if (shipMeth == null) {
                //todo we should throw and error and not assume that we don't have shipping associated.
                Debug.logWarning("Customer [" + customerPartyId + "] has no shipping method specified.  Assuming No Shipping.", MODULE);
                shipMeth = delegator.makeValue("ProductStoreShipmentMeth", UtilMisc.toMap("partyId", "_NA_", "roleTypeId", "CARRIER", "shipmentMethodTypeId", "NO_SHIPPING"));
            }
            String shipmentMethodTypeId = shipMeth.getString("shipmentMethodTypeId");
            String carrierPartyId = shipMeth.getString("partyId");
            String carrierRoleTypeId = shipMeth.getString("roleTypeId");

            //
            String contactMechId = null;

            if (readShippingAddressFromTable) {
                //get info from the table
                ContactMech contactMech = new ContactMech();
                String contactMechNextId = delegator.getNextSeqId(contactMech.getBaseEntityName());
                contactMech.setContactMechId(contactMechNextId);
                contactMech.setContactMechTypeId(ContactMechTypeConstants.POSTAL_ADDRESS);
                GenericValue contactMechEntity = delegator.makeValue(contactMech.getBaseEntityName(), contactMech.toMap());
                //todo should we check if the contactMech is empty?
                PostalAddress postalAddress = new PostalAddress();
                postalAddress.setContactMechId(contactMech.getContactMechId());
                String toName = dataImportOrderHeader.getShippingFirstName() + " " + dataImportOrderHeader.getShippingLastName();
                postalAddress.setToName(toName);
                postalAddress.setAttnName(dataImportOrderHeader.getShippingCompanyName());
                postalAddress.setAddress1(dataImportOrderHeader.getShippingStreet());
                postalAddress.setCity(dataImportOrderHeader.getShippingCity());
                postalAddress.setCountryGeoId(dataImportOrderHeader.getShippingCountry());
                postalAddress.setPostalCode(dataImportOrderHeader.getShippingPostcode());
                postalAddress.setStateProvinceGeoId(dataImportOrderHeader.getShippingRegion());
                GenericValue postalAddressEntity = delegator.makeValue(postalAddress.getBaseEntityName(), postalAddress.toMap());
                if (UtilValidate.isEmpty(postalAddressEntity)) {
                    Debug.logError("Error creating PostalAddress Entity", MODULE);
                    return FastList.newInstance();
                }
                postalAddressEntities.add(contactMechEntity);
                postalAddressEntities.add(postalAddressEntity);
                contactMechId = contactMech.getContactMechId();

            } else {
                List<GenericValue> shippingAddresses = PartyContactHelper.getContactMechsByPurpose(customerPartyId, "POSTAL_ADDRESS", "SHIPPING_LOCATION", true, delegator);
                if (shippingAddresses.size() > 1) {
                    Debug.logWarning("Customer [" + customerPartyId + "] has more than one shipping address.  Using first one.", MODULE);
                }
                if (shippingAddresses.size() == 0) {
                    Debug.logInfo("No shipping address found for customer [" + customerPartyId + "].  Not creating ship group for the order.", MODULE);
                } else {
                    contactMechId = EntityUtil.getFirst(shippingAddresses).getString("contactMechId");
                }
            }

            if (contactMechId != null) {
                oisg = delegator.makeValue("OrderItemShipGroup");
                oisg.put("orderId", orderId);
                oisg.put("shipGroupSeqId", defaultShipGroupSeqId);
                oisg.put("carrierPartyId", carrierPartyId);

                oisg.put("carrierRoleTypeId", carrierRoleTypeId);
                oisg.put("shipmentMethodTypeId", shipmentMethodTypeId);
                oisg.put("maySplit", "N");
                oisg.put("isGift", "N");
                oisg.put("contactMechId", contactMechId);
                Debug.logInfo("Created ship group for order at PostalAddress [" + contactMechId + "]", MODULE);
            }

        }

        // handle the shipping total as a whole order one
        Double shippingTotal = externalOrderHeader.getDouble("shippingTotal");
        if (shippingTotal != null && shippingTotal > 0.0) {
            GenericValue adj = delegator.makeValue("OrderAdjustment");
            adj.put("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
            adj.put("orderAdjustmentTypeId", "SHIPPING_CHARGES");
            adj.put("orderId", orderId);
            adj.put("orderItemSeqId", "_NA_");
            adj.put("shipGroupSeqId", defaultShipGroupSeqId);
            adj.put("amount", shippingTotal);
            orderAdjustments.add(adj);
        }

        // whole order tax, which must have orderTax and taxAuthPartyId defined
        Double orderTax = externalOrderHeader.getDouble("orderTax");
        String taxAuthPartyId = externalOrderHeader.getString("taxAuthPartyId");
        if (orderTax != null && orderTax > 0.0 && taxAuthPartyId != null) {
            GenericValue taxAuth = EntityUtil.getFirst(delegator.findByAndCache("TaxAuthority", UtilMisc.toMap("taxAuthPartyId", taxAuthPartyId)));
            if (taxAuth == null) {
                Debug.logWarning("Order [" + orderId + "] has a tax to an unknown tax authority.  No entry for taxAuthPartyId [" + taxAuthPartyId + "] found in TaxAuthority.", MODULE);
            } else {
                GenericValue adj = delegator.makeValue("OrderAdjustment");
                adj.put("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                adj.put("orderAdjustmentTypeId", "SALES_TAX");
                adj.put("orderId", orderId);
                adj.put("orderItemSeqId", "_NA_");
                adj.put("shipGroupSeqId", defaultShipGroupSeqId);
                adj.put("taxAuthPartyId", taxAuth.get("taxAuthPartyId"));
                adj.put("taxAuthGeoId", taxAuth.get("taxAuthGeoId"));
                adj.put("amount", orderTax);
                orderAdjustments.add(adj);
            }
        }


        // OrderItems
        EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
                EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));

        List orderItemConditions = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId), statusCond);

        //todo the following part will be moved into a payment module and decoupled form the order import
        List<GenericValue> externalOrderPayments = delegator.findByCondition("DataImportOrderPayment", EntityCondition.makeCondition(orderItemConditions, EntityOperator.AND), null, null);
        List orderPayments = new LinkedList();
        if (UtilValidate.isNotEmpty(externalOrderPayments)) {
            //we process the payments; we should have only one payment for order
            for (GenericValue externalOrderPayment : externalOrderPayments) {
                DataImportOrderPayment dataImportOrderPayment = new DataImportOrderPayment();
                dataImportOrderPayment.fromMap(externalOrderPayment);
                OrderPaymentPreference orderPaymentPreference = new OrderPaymentPreference();
                orderPaymentPreference.setOrderId(dataImportOrderPayment.getOrderId());
                orderPaymentPreference.setOrderPaymentPreferenceId(dataImportOrderPayment.getOrderPaymentPreferenceId());
                orderPaymentPreference.setPaymentMethodTypeId(dataImportOrderPayment.getPaymentMethodTypeId());
                orderPaymentPreference.setMaxAmount(dataImportOrderPayment.getMaxAmount());
                orderPaymentPreference.setStatusId(dataImportOrderPayment.getStatusId());

                GenericValue orderPaymentPreferenceEntity = delegator.makeValue(orderPaymentPreference.getBaseEntityName(), orderPaymentPreference.toMap());
                if (UtilValidate.isEmpty(orderPaymentPreferenceEntity)) {
                    return FastList.newInstance();
                }

                Payment payment = new Payment();
                String paymentId = delegator.getNextSeqId(payment.getBaseEntityName());
                payment.setPaymentId(paymentId);
                payment.setPaymentTypeId(dataImportOrderPayment.getPaymentTypeId());
                payment.setPaymentMethodTypeId(dataImportOrderPayment.getPaymentMethodTypeId());
                payment.setPaymentPreferenceId(orderPaymentPreference.getOrderPaymentPreferenceId());
                // Make sure the customer party exists
                GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));
                Party customerParty = new Party();
                customerParty.fromMap(party);

                if (UtilValidate.isEmpty(party)) {
                    Debug.logError("CustomerPartyId [" + customerPartyId + "] not found - not creating Payment for orderId [" + orderId + "]", MODULE);
                }
                //todo this cover only the case of sales order
                payment.setPartyIdFrom(customerParty.getPartyId());
                payment.setPartyIdTo(companyPartyId);
                payment.setStatusId(dataImportOrderPayment.getStatusId());
                payment.setEffectiveDate(dataImportOrderPayment.getEffectiveDate());
                payment.setPaymentRefNum(dataImportOrderPayment.getPaymentRefNum());
                payment.setAmount(dataImportOrderPayment.getAmount());
                payment.setCurrencyUomId(dataImportOrderPayment.getCurrencyUomId());
                payment.setComments(dataImportOrderPayment.getComments());
                payment.setStatusId("PMNT_NOT_PAID");
                payment.setAppliedAmount(new BigDecimal(0));
                payment.setOpenAmount(new BigDecimal(0));
                GenericValue paymentEntity = delegator.makeValue(payment.getBaseEntityName(), payment.toMap());

                if (UtilValidate.isEmpty(paymentEntity)) {
                    //todo add log here
                    return FastList.newInstance();
                }


                orderPayments.add(orderPaymentPreferenceEntity);
                orderPayments.add(paymentEntity);
            }
        }


        List externalOrderItems = delegator.findByCondition("DataImportOrderItem", EntityCondition.makeCondition(orderItemConditions, EntityOperator.AND), null, null); //getExternalOrderItems(externalOrderHeader.getString("orderId"), delegator);

        // If orders without orderItems should not be imported, return now without doing anything
        if (UtilValidate.isEmpty(externalOrderItems) && !importEmptyOrders) {
            return FastList.newInstance();
        }

        // item status depends on order status
        String itemStatus = "ORDER_COMPLETED".equals(orderStatusId) ? "ITEM_COMPLETED" : "ITEM_APPROVED";

        List orderItems = new ArrayList();
        List oisgAssocs = new ArrayList();
        for (int count = 0; count < externalOrderItems.size(); count++) {
            GenericValue externalOrderItem = (GenericValue) externalOrderItems.get(count);

            String orderItemSeqId = UtilFormatOut.formatPaddedNumber(count + 1, 5);
            Double quantity = UtilValidate.isEmpty(externalOrderItem.get("quantity")) ? new Double(0) : externalOrderItem.getDouble("quantity");

            Map orderItemInput = FastMap.newInstance();
            orderItemInput.put("orderId", orderId);
            orderItemInput.put("orderItemSeqId", orderItemSeqId);
            orderItemInput.put("orderItemTypeId", "PRODUCT_ORDER_ITEM");
            if (UtilValidate.isNotEmpty(externalOrderItem.get("productId"))) {
                orderItemInput.put("productId", externalOrderItem.getString("productId"));
                //todo we remove foreign key association to product so this won't work
                // GenericValue product = externalOrderItem.getRelatedOneCache("Product");
                GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", externalOrderItem.getString("productId")));
                if (UtilValidate.isNotEmpty(product)) {
                    orderItemInput.put("itemDescription", product.getString("productName"));
                } else {
                    Debug.logError("Product [" + externalOrderItem.getString("productId") + "] does not exist", MODULE);
                    return FastList.newInstance();
                }
            }
            if (isSalesOrder && UtilValidate.isNotEmpty(prodCatalogId)) {
                orderItemInput.put("prodCatalogId", prodCatalogId);
            }
            orderItemInput.put("isPromo", "N");
            orderItemInput.put("quantity", quantity);
            orderItemInput.put("selectedAmount", new Double(0));
            if (UtilValidate.isNotEmpty(externalOrderItem.get("price"))) {
                orderItemInput.put("unitPrice", externalOrderItem.getDouble("price"));
                orderItemInput.put("unitListPrice", externalOrderItem.getDouble("price"));
            } else {
                orderItemInput.put("unitPrice", new Double(0));
                orderItemInput.put("unitListPrice", new Double(0));
            }
            orderItemInput.put("isModifiedPrice", "N");
            if (UtilValidate.isNotEmpty(externalOrderItem.get("comments"))) {
                orderItemInput.put("comments", externalOrderItem.getString("comments"));
            }
            if (UtilValidate.isNotEmpty(externalOrderItem.get("customerPo"))) {
                orderItemInput.put("correspondingPoId", externalOrderItem.getString("customerPo"));
            }
            orderItemInput.put("statusId", itemStatus);
            orderItems.add(delegator.makeValue("OrderItem", orderItemInput));

            // purchase orders must assign all their order items to the oisg
            if ((isPurchaseOrder && oisg != null) || (isSalesOrder && reserveInventory && oisg != null)) {
                Debug.logInfo("Begin to create OrderItemShipGroupAssoc", MODULE);
                Map oisgAssocInput = FastMap.newInstance();
                oisgAssocInput.put("orderId", orderId);
                oisgAssocInput.put("orderItemSeqId", orderItemSeqId);
                oisgAssocInput.put("shipGroupSeqId", defaultShipGroupSeqId);
                oisgAssocInput.put("quantity", quantity);
                oisgAssocs.add(delegator.makeValue("OrderItemShipGroupAssoc", oisgAssocInput));
                Debug.logInfo("OrderItemShipGroupAssoc is created", MODULE);
            }

            // line item tax, which must have itemTax and taxAuthPartyId defined
            Double itemTax = externalOrderItem.getDouble("itemTax");
            taxAuthPartyId = externalOrderItem.getString("taxAuthPartyId");
            if (itemTax != null && itemTax > 0.0 && taxAuthPartyId != null) {
                GenericValue taxAuth = EntityUtil.getFirst(delegator.findByAndCache("TaxAuthority", UtilMisc.toMap("taxAuthPartyId", taxAuthPartyId)));
                if (taxAuth == null) {
                    Debug.logWarning("Order Item [" + orderId + "," + orderItemSeqId + "] has a tax to an unknown tax authority.  No entry for taxAuthPartyId [" + taxAuthPartyId + "] found in TaxAuthority.", MODULE);
                } else {
                    GenericValue adj = delegator.makeValue("OrderAdjustment");
                    adj.put("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                    adj.put("orderAdjustmentTypeId", "SALES_TAX");
                    adj.put("orderId", orderId);
                    adj.put("orderItemSeqId", orderItemSeqId);
                    adj.put("shipGroupSeqId", defaultShipGroupSeqId);
                    adj.put("taxAuthPartyId", taxAuth.get("taxAuthPartyId"));
                    adj.put("taxAuthGeoId", taxAuth.get("taxAuthGeoId"));
                    adj.put("amount", itemTax);
                    orderAdjustments.add(adj);
                }
            }
            externalOrderItem.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_IMPORTED);
            externalOrderItem.set("importError", null);
            externalOrderItem.set("processedTimestamp", UtilDateTime.nowTimestamp());
            externalOrderItem.set("orderItemSeqId", orderItemSeqId);
        }

        BigDecimal orderGrandTotal;
        if (calculateGrandTotal) {

            // Get the grand total from the order items and order adjustments
            orderGrandTotal = getOrderGrandTotal(orderAdjustments, orderItems);
            if (orderGrandTotal.compareTo(BigDecimal.ZERO) == 0) {
                Debug.logWarning("Order [" + orderId + "] had a zero calculated total, so we are using the DataImportOrderHeader grand total of [" + externalOrderHeader.getBigDecimal("grandTotal") + "]", MODULE);
                orderGrandTotal = externalOrderHeader.getBigDecimal("grandTotal").setScale(decimals, rounding);
            }
        } else {
            orderGrandTotal = externalOrderHeader.getBigDecimal("grandTotal").setScale(decimals, rounding);
        }

        // updade order header for total
        orderHeaderInput.put("grandTotal", new Double(orderGrandTotal.doubleValue()));

        // OrderRoles

        // create the bill to party order role
        List roles = FastList.newInstance();
        if (isSalesOrder && UtilValidate.isNotEmpty(customerPartyId)) {

            // Make sure the customer party exists
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", customerPartyId));

            if (UtilValidate.isEmpty(party)) {
                Debug.logError("CustomerPartyId [" + customerPartyId + "] not found - not creating BILL_TO_CUSTOMER order role for orderId [" + orderId + "]", MODULE);
            } else {
                // Ensure the party roles for the customer
                roles.addAll(UtilImport.ensurePartyRoles(customerPartyId, UtilMisc.toList("PLACING_CUSTOMER", "BILL_TO_CUSTOMER", "SHIP_TO_CUSTOMER", "END_USER_CUSTOMER"), delegator));

                // Create the customer order roles
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", customerPartyId, "roleTypeId", "PLACING_CUSTOMER")));
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", customerPartyId, "roleTypeId", "BILL_TO_CUSTOMER")));
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", customerPartyId, "roleTypeId", "SHIP_TO_CUSTOMER")));
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", customerPartyId, "roleTypeId", "END_USER_CUSTOMER")));
            }
        }
        if (isPurchaseOrder) {
            roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", companyPartyId, "roleTypeId", "BILL_TO_CUSTOMER")));
        }

        // Create the bill from vendor order role
        if (isSalesOrder) {
            roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", companyPartyId, "roleTypeId", "BILL_FROM_VENDOR")));
        }
        if (isPurchaseOrder) {
            // Make sure the supplier party exists
            GenericValue supplier = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", supplierPartyId));
            if (supplier == null) {
                Debug.logError("SupplierPartyId [" + supplierPartyId + "] not found - not creating BILL_FROM_VENDOR order role for orderId [" + orderId + "]", MODULE);
            } else {
                roles.addAll(UtilImport.ensurePartyRoles(supplierPartyId, UtilMisc.toList("SUPPLIER", "SUPPLIER_AGENT", "BILL_FROM_VENDOR", "SHIP_FROM_VENDOR"), delegator));

                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", supplierPartyId, "roleTypeId", "BILL_FROM_VENDOR")));
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", supplierPartyId, "roleTypeId", "SHIP_FROM_VENDOR")));
                roles.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", supplierPartyId, "roleTypeId", "SUPPLIER_AGENT")));
            }
        }

        // Order Notes
        List notes = FastList.newInstance();
        String comments = externalOrderHeader.getString("comments");
        if (UtilValidate.isNotEmpty(comments)) {
            String noteId = delegator.getNextSeqId("NoteData");
            notes.add(delegator.makeValue("NoteData", UtilMisc.toMap("noteId", noteId, "noteInfo", comments, "noteDateTime", UtilDateTime.nowTimestamp(), "noteParty", userLogin.getString("partyId"))));
            notes.add(delegator.makeValue("OrderHeaderNote", UtilMisc.toMap("orderId", orderId, "noteId", noteId, "internalNote", "Y")));
        }

        // Set the processedTimestamp on the externalOrderHeader
        externalOrderHeader.set("processedTimestamp", UtilDateTime.nowTimestamp());
        externalOrderHeader.set("importStatusId", StatusItemConstants.Dataimport.DATAIMP_IMPORTED);
        externalOrderHeader.set("importError", null); // clear this out in case it had an exception originally

        // Add everything to the store list here to avoid problems with having to derive more values for order header, etc.
        List toStore = FastList.newInstance();
        toStore.add(delegator.makeValue("OrderHeader", orderHeaderInput));
        toStore.add(delegator.makeValue("OrderStatus", orderStatusInput));
        if (oisg != null) {
            toStore.add(oisg);
        }
        toStore.addAll(orderItems);
        toStore.addAll(externalOrderItems);
        toStore.addAll(orderAdjustments);
        toStore.addAll(oisgAssocs);
        toStore.addAll(roles);
        toStore.addAll(notes);
        toStore.addAll(orderPayments);
        toStore.addAll(postalAddressEntities);
        toStore.add(externalOrderHeader);


        return toStore;
    }

    @SuppressWarnings("unchecked")
    private static BigDecimal getOrderGrandTotal(List orderAdjustments, List orderItems) {
        BigDecimal grandTotal = ZERO;

        Iterator oajit = orderAdjustments.iterator();
        while (oajit.hasNext()) {
            GenericValue orderAdjustment = (GenericValue) oajit.next();
            grandTotal = grandTotal.add(orderAdjustment.getBigDecimal("amount").setScale(decimals, rounding));
        }

        Iterator oiit = orderItems.iterator();
        while (oiit.hasNext()) {
            GenericValue orderItem = (GenericValue) oiit.next();
            BigDecimal quantity = orderItem.getBigDecimal("quantity").setScale(decimals, rounding);
            BigDecimal price = orderItem.getBigDecimal("unitPrice").setScale(decimals, rounding);

            // Order item adjustments are included in orderAdjustments above - no need to consider them here
            BigDecimal orderItemSubTotal = quantity.multiply(price);
            grandTotal = grandTotal.add(orderItemSubTotal);
        }
        return grandTotal;
    }

}
