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

package org.opentaps.amazon.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.amazon.AmazonConstants;
import org.opentaps.amazon.AmazonUtil;
import org.opentaps.amazon.soap.axis.MerchantDocumentInfo;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Services for Amazon integration order management.
 */
public final class AmazonOrderServices {

    private AmazonOrderServices() { }

    private static final String MODULE = AmazonOrderServices.class.getName();

    /**
     * Stores any pending order documents from Amazon that don't exist in the AmazonOrderDocument entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> storePendingOrderDocuments(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            List<MerchantDocumentInfo> pendingDocuments = null;
            try {
                pendingDocuments = AmazonConstants.soapClient.getPendingOrderDocuments();
            } catch (RemoteException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }

            for (MerchantDocumentInfo pendingDocument : pendingDocuments) {

                // Check if record of a successful download exists in the AmazonOrderDocument entity
                GenericValue existingDocument = delegator.findByPrimaryKey("AmazonOrderDocument", UtilMisc.toMap("documentId", pendingDocument.getDocumentID()));
                if (UtilValidate.isNotEmpty(existingDocument) && AmazonConstants.statusDocDownloaded.equals(existingDocument.getString("statusId"))) {
                    Debug.logWarning(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoreExistingOrderDocument", UtilMisc.toMap("documentId", pendingDocument.getDocumentID(), "downloadTimestamp", existingDocument.getTimestamp("downloadTimestamp")), locale), MODULE);
                    continue;
                }

                // Get the document itself
                boolean successfulDownload = true;
                String amazonDocumentXml = null;
                String downloadErrorMessage = null;
                String errorLog = null;
                String errorLabel = null;
                try {
                    amazonDocumentXml = AmazonConstants.soapClient.getDocumentById(pendingDocument.getDocumentID());
                } catch (SOAPException e) {
                    successfulDownload = false;
                    downloadErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ErrorGettingDocumentAttachment";
                } catch (RemoteException e) {
                    successfulDownload = false;
                    downloadErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ErrorGettingDocumentAttachment";
                } catch (IOException e) {
                    successfulDownload = false;
                    downloadErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ErrorGettingDocument";
                }

                try {
                    // Make sure the XML is valid
                    UtilXml.readXmlDocument(amazonDocumentXml);
                } catch (Exception e) {
                    successfulDownload = false;
                    downloadErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ParseError";
                }

                // Create or store the document recordim
                GenericValue amazonOrderDocument = null;
                if (UtilValidate.isNotEmpty(existingDocument)) {
                    amazonOrderDocument = existingDocument;
                }
                if (UtilValidate.isEmpty(amazonOrderDocument)) {
                    amazonOrderDocument = delegator.makeValue("AmazonOrderDocument", UtilMisc.toMap("documentId", pendingDocument.getDocumentID()));
                }
                amazonOrderDocument.set("generatedTimestamp", new Timestamp(pendingDocument.getGeneratedDateTime().getTimeInMillis()));
                amazonOrderDocument.set("statusId", successfulDownload ? AmazonConstants.statusDocDownloaded : AmazonConstants.statusDocDownloadError);
                amazonOrderDocument.set("ackStatusId", AmazonConstants.statusDocNotAcknowledged);
                amazonOrderDocument.set("documentXml", successfulDownload ? amazonDocumentXml : null);
                amazonOrderDocument.set("downloadTimestamp", UtilDateTime.nowTimestamp());
                amazonOrderDocument.set("downloadErrorMessage", successfulDownload ? null : downloadErrorMessage);
                amazonOrderDocument.set("extractionFailures", 0);
                delegator.createOrStore(amazonOrderDocument);

                if (!successfulDownload) {
                    Map<String, Object> errorMap = UtilMisc.<String, Object>toMap("documentId", pendingDocument.getDocumentID(), "errorMessage", downloadErrorMessage);
                    errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, errorLabel, errorMap, locale);
                    Debug.logError(errorLog, MODULE);
                    if (AmazonConstants.sendErrorEmails) {
                        AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_StoreOrderDoc", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                    }
                }
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Extracts order data from Amazon order documents and stores it in transitional import tables.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> extractOrdersForImport(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            // Get any stored order documents from which orders haven't been extracted, or for which extraction failed the last time
            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.OR,
                                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, AmazonConstants.statusDocDownloaded),
                                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, AmazonConstants.statusDocExtractedError));
            TransactionUtil.begin();
            EntityListIterator amazonOrderDocIt = delegator.findListIteratorByCondition("AmazonOrderDocument", cond, null, null);
            TransactionUtil.commit();

            GenericValue amazonOrderDocument = null;
            while ((amazonOrderDocument = amazonOrderDocIt.next()) != null) {

                if (AmazonConstants.docExtractRetryThreshold <= amazonOrderDocument.getLong("extractionFailures").intValue()) {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ExtractAttemptsOverThreshold", UtilMisc.<String, Object>toMap("documentId", amazonOrderDocument.getString("documentId"), "threshold", AmazonConstants.docExtractRetryThreshold), locale);
                    Debug.logInfo(errorLog, MODULE);
                    continue;
                }

                boolean importSuccess = true;
                String importErrorMessage = null;
                String errorLabel = null;
                List<GenericValue> toStore = new ArrayList<GenericValue>();

                try {

                    Document doc = UtilXml.readXmlDocument(amazonOrderDocument.getString("documentXml"));
                    String documentId = amazonOrderDocument.getString("documentId");

                    List<? extends Element> messages = UtilXml.childElementList(doc.getDocumentElement(), "Message");
                    for (Element message : messages) {

                        Element orderReport = UtilXml.firstChildElement(message, "OrderReport");
                        Element billingData = UtilXml.firstChildElement(orderReport, "BillingData");
                        Element fulfillmentData = UtilXml.firstChildElement(orderReport, "FulfillmentData");
                        Element address = UtilXml.firstChildElement(fulfillmentData, "Address");

                        String messageId = UtilXml.childElementValue(message, "MessageID");
                        String amazonOrderId = UtilXml.childElementValue(orderReport, "AmazonOrderID");

                        // Check to see that this order hasn't yet been extracted
                        GenericValue amazonOrder = delegator.findByPrimaryKey("AmazonOrder", UtilMisc.toMap("amazonOrderId", amazonOrderId));
                        if (UtilValidate.isNotEmpty(amazonOrder)) {
                            Debug.logWarning(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_IgnoreExistingOrder", UtilMisc.toMap("amazonOrderId", amazonOrderId, "createdTimestamp", amazonOrder.getTimestamp("createdStamp")), locale), MODULE);
                            continue;
                        }

                        Timestamp orderTimestamp = AmazonUtil.convertAmazonXSDateToLocalTimestamp(UtilXml.childElementValue(orderReport, "OrderDate"));
                        Timestamp orderPostedTimestamp = AmazonUtil.convertAmazonXSDateToLocalTimestamp(UtilXml.childElementValue(orderReport, "OrderPostedDate"));

                        amazonOrder = delegator.makeValue("AmazonOrder", UtilMisc.toMap("documentId", documentId, "documentMessageId", messageId));
                        amazonOrder.set("amazonOrderId", amazonOrderId);
                        amazonOrder.set("amazonSessionId", UtilXml.childElementValue(orderReport, "AmazonSessionID"));
                        amazonOrder.set("orderDate", orderTimestamp);
                        amazonOrder.set("orderPostedDate", orderPostedTimestamp);
                        amazonOrder.set("buyerEmailAddress", UtilXml.childElementValue(billingData, "BuyerEmailAddress"));
                        amazonOrder.set("buyerName", UtilXml.childElementValue(billingData, "BuyerName"));
                        amazonOrder.set("buyerPhoneNumber", UtilXml.childElementValue(billingData, "BuyerPhoneNumber"));
                        amazonOrder.set("fulfillmentMethod", UtilXml.childElementValue(fulfillmentData, "FulfillmentMethod"));
                        amazonOrder.set("fulfillmentServiceLevel", UtilXml.childElementValue(fulfillmentData, "FulfillmentServiceLevel"));
                        amazonOrder.set("addressName", UtilXml.childElementValue(address, "Name"));
                        amazonOrder.set("addressFieldOne", UtilXml.childElementValue(address, "AddressFieldOne"));
                        amazonOrder.set("addressFieldTwo", UtilXml.childElementValue(address, "AddressFieldTwo"));
                        amazonOrder.set("addressFieldThree", UtilXml.childElementValue(address, "AddressFieldThree"));
                        amazonOrder.set("addressCity", UtilXml.childElementValue(address, "City"));
                        amazonOrder.set("addressStateOrRegion", UtilXml.childElementValue(address, "StateOrRegion"));
                        amazonOrder.set("addressPostalCode", UtilXml.childElementValue(address, "PostalCode"));
                        amazonOrder.set("addressCountryCode", UtilXml.childElementValue(address, "CountryCode"));
                        amazonOrder.set("addressPhoneNumber", UtilXml.childElementValue(address, "PhoneNumber"));
                        amazonOrder.set("statusId", AmazonConstants.statusOrderCreated);
                        amazonOrder.set("ackStatusId", AmazonConstants.statusOrderNotAcknowledged);
                        amazonOrder.set("importFailures", 0);
                        toStore.add(amazonOrder);

                        List<? extends Element> items = UtilXml.childElementList(orderReport, "Item");
                        for (Element item : items) {
                            String amazonOrderItemCode = UtilXml.childElementValue(item, "AmazonOrderItemCode");
                            GenericValue amazonOrderItem = delegator.makeValue("AmazonOrderItem", UtilMisc.toMap("amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode));
                            amazonOrderItem.set("sku", UtilXml.childElementValue(item, "SKU"));
                            amazonOrderItem.set("title", UtilXml.childElementValue(item, "Title"));
                            amazonOrderItem.set("quantity", Double.parseDouble(UtilXml.childElementValue(item, "Quantity")));
                            amazonOrderItem.set("productTaxCode", UtilXml.childElementValue(item, "ProductTaxCode"));
                            toStore.add(amazonOrderItem);

                            Element itemPrice = UtilXml.firstChildElement(item, "ItemPrice");
                            List<? extends Element> itemPriceComponents = UtilXml.childElementList(itemPrice, "Component");
                            for (Element itemPriceComponent : itemPriceComponents) {
                                Element amountEl = UtilXml.firstChildElement(itemPriceComponent, "Amount");
                                GenericValue amazonOrderItemPriceComp = delegator.makeValue("AmazonOrderItemPriceComp", UtilMisc.toMap("amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode));
                                amazonOrderItemPriceComp.set("componentType", UtilXml.childElementValue(itemPriceComponent, "Type"));
                                amazonOrderItemPriceComp.set("componentAmount", Double.parseDouble(UtilXml.elementValue(amountEl)));
                                amazonOrderItemPriceComp.set("componentCurrency", amountEl.getAttribute("currency"));
                                toStore.add(amazonOrderItemPriceComp);
                            }

                            Element itemFees = UtilXml.firstChildElement(item, "ItemFees");
                            List<? extends Element> fees = UtilXml.childElementList(itemFees, "Fee");
                            for (Element fee : fees) {
                                Element feeAmountEl = UtilXml.firstChildElement(fee, "Amount");
                                GenericValue amazonOrderItemFee = delegator.makeValue("AmazonOrderItemFee", UtilMisc.toMap("amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode));
                                amazonOrderItemFee.set("feeType", UtilXml.childElementValue(fee, "Type"));
                                amazonOrderItemFee.set("feeAmount", Double.parseDouble(UtilXml.elementValue(feeAmountEl)));
                                amazonOrderItemFee.set("feeCurrency", feeAmountEl.getAttribute("currency"));
                                toStore.add(amazonOrderItemFee);
                            }

                            for (String taxType : AmazonConstants.taxTypes) {
                                List<? extends Element> itemTaxDataList = UtilXml.childElementList(item, taxType);
                                for (Element taxData : itemTaxDataList) {
                                    Element taxJurisdiction = UtilXml.firstChildElement(taxData, "TaxJurisdictions");
                                    String itemTaxJurisTypeId = delegator.getNextSeqId("AmazonOrderItemTaxJurisdtn");
                                    GenericValue amazonOrderItemTaxJurisdtn = delegator.makeValue("AmazonOrderItemTaxJurisdtn", UtilMisc.toMap("itemTaxJurisTypeId", itemTaxJurisTypeId));
                                    amazonOrderItemTaxJurisdtn.set("taxType", taxType);
                                    amazonOrderItemTaxJurisdtn.set("amazonOrderId", amazonOrderId);
                                    amazonOrderItemTaxJurisdtn.set("amazonOrderItemCode", amazonOrderItemCode);
                                    amazonOrderItemTaxJurisdtn.set("taxLocationCode", UtilXml.childElementValue(taxJurisdiction, "TaxLocationCode"));
                                    amazonOrderItemTaxJurisdtn.set("taxJurisDistrict", UtilXml.childElementValue(taxJurisdiction, "District"));
                                    amazonOrderItemTaxJurisdtn.set("taxJurisCity", UtilXml.childElementValue(taxJurisdiction, "City"));
                                    amazonOrderItemTaxJurisdtn.set("taxJurisCounty", UtilXml.childElementValue(taxJurisdiction, "County"));
                                    amazonOrderItemTaxJurisdtn.set("taxJurisState", UtilXml.childElementValue(taxJurisdiction, "State"));
                                    toStore.add(amazonOrderItemTaxJurisdtn);

                                    Map<String, Object> amountData = UtilMisc.<String, Object>toMap("itemTaxJurisTypeId", itemTaxJurisTypeId, "amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode);
                                    for (String taxAmountType : AmazonConstants.taxAmountTypes) {
                                        Element taxAmountParent = UtilXml.firstChildElement(taxData, taxAmountType);
                                        for (String taxJurisdictionType : AmazonConstants.taxJurisdictionTypes) {
                                            Element taxAmountElement = UtilXml.firstChildElement(taxAmountParent, taxJurisdictionType);
                                            GenericValue taxValue = delegator.makeValue("AmazonOrderItemTaxAmount", amountData);
                                            taxValue.set("taxAmountType", taxAmountType);
                                            taxValue.set("taxJurisdictionType", taxJurisdictionType);
                                            taxValue.set("taxAmount", Double.parseDouble(UtilXml.elementValue(taxAmountElement)));
                                            taxValue.set("taxCurrency", taxAmountElement.getAttribute("currency"));
                                            toStore.add(taxValue);
                                        }
                                    }

                                    Element taxRates = UtilXml.firstChildElement(taxData, "TaxRates");
                                    for (String taxJurisdictionType : AmazonConstants.taxJurisdictionTypes) {
                                        Element taxRateElement = UtilXml.firstChildElement(taxRates, taxJurisdictionType);
                                        GenericValue taxRateValue = delegator.makeValue("AmazonOrderItemTaxRate", amountData);
                                        taxRateValue.set("taxJurisdictionType", taxJurisdictionType);
                                        taxRateValue.set("taxRate", Double.parseDouble(UtilXml.elementValue(taxRateElement)));
                                        toStore.add(taxRateValue);
                                    }
                                }
                            }

                            List<? extends Element> promotions = UtilXml.childElementList(item, "Promotion");
                            for (Element promotion : promotions) {
                                List<? extends Element> components = UtilXml.childElementList(promotion, "Component");
                                for (Element component : components) {
                                    Element promoAmount = UtilXml.firstChildElement(component, "Amount");
                                    GenericValue amazonOrderItemPromo = delegator.makeValue("AmazonOrderItemPromo", UtilMisc.toMap("amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode));
                                    amazonOrderItemPromo.set("promotionClaimCode", UtilXml.childElementValue(promotion, "PromotionClaimCode"));
                                    amazonOrderItemPromo.set("merchantPromotionId", UtilXml.childElementValue(promotion, "MerchantPromotionID"));
                                    amazonOrderItemPromo.set("promoAmountType", UtilXml.childElementValue(component, "Type"));
                                    amazonOrderItemPromo.set("promoAmount", Double.parseDouble(UtilXml.elementValue(promoAmount)));
                                    amazonOrderItemPromo.set("promoAmountCurrency", promoAmount.getAttribute("currency"));
                                    toStore.add(amazonOrderItemPromo);
                                }
                            }

                        }
                    }
                } catch (ParserConfigurationException e) {
                    importSuccess = false;
                    importErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ParseError";
                } catch (SAXException e) {
                    importSuccess = false;
                    importErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ParseError";
                } catch (IOException e) {
                    importSuccess = false;
                    importErrorMessage = e.getMessage();
                    errorLabel = "AmazonError_ParseError";
                }
                if (importSuccess) {
                    try {
                        TransactionUtil.begin();
                        delegator.storeAll(toStore);
                        TransactionUtil.commit();
                    } catch (GenericEntityException e) {
                        importSuccess = false;
                        importErrorMessage = e.getMessage();
                        String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_DecodeOrderError", UtilMisc.toMap("documentId", amazonOrderDocument.getString("documentId"), "errorMessage", importErrorMessage), locale);
                        Debug.logError(errorLog, MODULE);
                        TransactionUtil.rollback();
                    }
                } else {
                    String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, errorLabel, UtilMisc.toMap("documentId", amazonOrderDocument.getString("documentId"), "errorMessage", importErrorMessage), locale);
                    Debug.logError(errorLog, MODULE);
                    TransactionUtil.rollback();
                }
                amazonOrderDocument.set("statusId", importSuccess ? AmazonConstants.statusDocExtracted : AmazonConstants.statusDocExtractedError);
                amazonOrderDocument.set("importTimestamp", UtilDateTime.nowTimestamp());
                amazonOrderDocument.set("importErrorMessage", importSuccess ? null : importErrorMessage);
                amazonOrderDocument.set("extractionFailures", importSuccess ? 0 : amazonOrderDocument.getLong("extractionFailures") + 1);
                amazonOrderDocument.store();
                if (AmazonConstants.sendErrorEmails && !importSuccess) {
                    Map<String, Object> errorMap = UtilMisc.<String, Object>toMap("documentId", amazonOrderDocument.getString("documentId"), "errorMessage", importErrorMessage);
                    AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_ExtractOrders", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                }
            }
            amazonOrderDocIt.close();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates the constellation of order-related data from any unimported Amazon orders.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> importOrders(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");

        try {

            // Make sure the productStore and organizationParty really exist
            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId));
            if (UtilValidate.isEmpty(productStore)) {
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoProductStore", UtilMisc.toMap("productStoreId", AmazonConstants.productStoreId), locale);
                return ServiceUtil.returnError(errorLog);
            }
            GenericValue organizationParty = productStore.getRelatedOne("Party");
            if (UtilValidate.isEmpty(organizationParty)) {
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_NoOrgParty", UtilMisc.toMap("organizationPartyId", productStore.getString("payToPartyId")), locale);
                return ServiceUtil.returnError(errorLog);
            }

            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.OR,
                                            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, AmazonConstants.statusOrderCreated),
                                            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, AmazonConstants.statusOrderImportedError));
            TransactionUtil.begin();
            EntityListIterator amazonOrderIt = delegator.findListIteratorByCondition("AmazonOrder", cond, null, null);
            TransactionUtil.commit();

            GenericValue amazonOrder = null;
            while ((amazonOrder = amazonOrderIt.next()) != null) {
                String errorMessage = null;
                boolean importSuccess = true;
                try {
                    TransactionUtil.begin();

                    // Check to see that the order *really* hasn't been imported
                    GenericValue amazonOrderImport = amazonOrder.getRelatedOne("AmazonOrderImport");
                    if (UtilValidate.isNotEmpty(amazonOrderImport)) {
                        errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_OrderAlreadyImported", UtilMisc.toMap("orderId", amazonOrderImport.getString("orderId")), locale);
                        throw new Exception(errorMessage);
                    }

                    // Check to see that importing of the order hasn't failed too many times
                    if (AmazonConstants.orderImportRetryThreshold <= amazonOrder.getLong("importFailures").intValue()) {
                        String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ImportAttemptsOverThreshold", UtilMisc.<String, Object>toMap("amazonOrderId", amazonOrder.getString("amazonOrderId"), "threshold", AmazonConstants.orderImportRetryThreshold), locale);
                        Debug.logInfo(errorLog, MODULE);
                        continue;
                    }

                    String orderId = importOrder(dispatcher, delegator, amazonOrder, locale, userLogin, productStore, timeZone);

                    String successMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_OrderImportSuccess", UtilMisc.toMap("amazonOrderId", amazonOrder.getString("amazonOrderId"), "orderId", orderId), locale);
                    Debug.logInfo(successMessage, MODULE);

                    TransactionUtil.commit();

                } catch (Exception e) {
                    TransactionUtil.rollback();
                    Map<String, String> errorMap = UtilMisc.toMap("amazonOrderId", amazonOrder.getString("amazonOrderId"), "errorMessage", e.getMessage());
                    errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_OrderImportError", errorMap, locale);
                    Debug.logError(errorMessage, MODULE);
                    importSuccess = false;
                    if (AmazonConstants.sendErrorEmails) {
                        AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_ImportOrder", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                    }
                }
                amazonOrder.set("statusId", importSuccess ? AmazonConstants.statusOrderImported : AmazonConstants.statusOrderImportedError);
                amazonOrder.set("importTimestamp", UtilDateTime.nowTimestamp());
                amazonOrder.set("importErrorMessage", errorMessage);
                amazonOrder.set("importFailures", importSuccess ? 0 : amazonOrder.getLong("importFailures") + 1);
                amazonOrder.store();
            }
            amazonOrderIt.close();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    private static String importOrder(LocalDispatcher dispatcher, Delegator delegator, GenericValue amazonOrder, Locale locale, GenericValue userLogin, GenericValue productStore, TimeZone timeZone) throws Exception {

        String errorMessage = "";

        // Retrieve and assemble the related orderItem records
        List<GenericValue> amazonOrderItems = amazonOrder.getRelated("AmazonOrderItem");
        if (UtilValidate.isEmpty(amazonOrderItems)) {
            errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_OrderNoItems", locale);
            throw new Exception(errorMessage);
        }
        LinkedHashMap<String, List<GenericValue>> itemPriceComponents = new LinkedHashMap<String, List<GenericValue>>();
        LinkedHashMap<String, List<GenericValue>> itemPromos = new LinkedHashMap<String, List<GenericValue>>();
        LinkedHashMap<String, List<GenericValue>> itemFees = new LinkedHashMap<String, List<GenericValue>>();
        for (GenericValue amazonOrderItem : amazonOrderItems) {
            itemPriceComponents.put(amazonOrderItem.getString("amazonOrderItemCode"), amazonOrderItem.getRelated("AmazonOrderItemPriceComp"));
            itemPromos.put(amazonOrderItem.getString("amazonOrderItemCode"), amazonOrderItem.getRelated("AmazonOrderItemPromo"));
            itemFees.put(amazonOrderItem.getString("amazonOrderItemCode"), amazonOrderItem.getRelated("AmazonOrderItemFee"));
        }

        // Determine the currency from the first price component of the first order item
        String currencyUomId = ((List<GenericValue>) itemPriceComponents.values().toArray()[0]).get(0).getString("componentCurrency");

        List<GenericValue> toStore = new ArrayList<GenericValue>();

        // AmazonOrder comes with a buyerEmailAddress which in Amazon is the unique identifier of a customer
        // If this customer is already in our system, then he/she should be in AmazonParty, which links to Party
        // If not, then we create a new Party with PartyClassificaiton and ContactMech, and then record it as an email party with AmazonParty

        // Check to see if a party exists for this Amazon customer
        String orderPartyId = null;
        String emailContactMechId = null;
        String customerPhoneContactMechId = null;
        GenericValue amazonParty = amazonOrder.getRelatedOne("AmazonParty");
        if (UtilValidate.isNotEmpty(amazonParty)) {
            orderPartyId = amazonParty.getString("partyId");
            emailContactMechId = amazonParty.getString("emailContactMechId");
        } else {

            // Construct a party/person
            orderPartyId = delegator.getNextSeqId("Party");
            GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", orderPartyId, "partyTypeId", "PERSON"));
            party.set("description", AmazonConstants.createdByAmazonApp);
            delegator.create(party);
            delegator.create(constructPerson(amazonOrder, delegator, orderPartyId));

            // PartyClassification
            GenericValue partyClassification = delegator.makeValue("PartyClassification", UtilMisc.toMap("partyId", orderPartyId, "partyClassificationGroupId", "AMAZON_CUSTOMERS", "fromDate", UtilDateTime.nowTimestamp()));
            delegator.create(partyClassification);

            // Email ContactMech, PartyContactMech and PartyContactMechPurposes
            emailContactMechId = delegator.getNextSeqId("ContactMech");
            delegator.storeAll(constructEmailAddress(delegator, emailContactMechId, amazonOrder, orderPartyId));

            // AmazonParty
            amazonParty = delegator.makeValue("AmazonParty", UtilMisc.toMap("buyerEmailAddress", amazonOrder.getString("buyerEmailAddress"), "partyId", orderPartyId, "emailContactMechId", emailContactMechId));
            delegator.create(amazonParty);
        }

        // Customer phone number ContactMech, PartyContactMech and PartyContactMechPurposes
        if (UtilValidate.isNotEmpty(amazonOrder.getString("buyerPhoneNumber"))) {
            GenericValue customerPhoneNumber = resolveExistingPhoneNumber(amazonOrder.getString("buyerPhoneNumber"), orderPartyId, delegator, locale);
            if (UtilValidate.isEmpty(customerPhoneNumber)) {

                // Construct a phone number
                customerPhoneContactMechId = delegator.getNextSeqId("ContactMech");
                toStore.addAll(constructPhoneNumber(amazonOrder.getString("buyerPhoneNumber"), delegator, customerPhoneContactMechId, locale, orderPartyId));
            } else {
                customerPhoneContactMechId = customerPhoneNumber.getString("contactMechId");
            }
        }

        // Validate the state/province and country
        String addressCountryCode = amazonOrder.getString("addressCountryCode");
        String addressCountryCodeStripped = null;
        GenericValue countryGeo = resolveGeo(addressCountryCode, delegator, null);
        if (UtilValidate.isEmpty(countryGeo)) {

            // Try again, removing any punctuation
            addressCountryCodeStripped = addressCountryCode.replaceAll("[^\\p{L}\\p{Lu}]", "");
            countryGeo = resolveGeo(addressCountryCodeStripped, delegator, null);
        }
        if (UtilValidate.isEmpty(countryGeo)) {
            errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_BadGeoCountry", UtilMisc.toMap("geoString", UtilMisc.toList(addressCountryCode, addressCountryCodeStripped)), locale);
            throw new Exception(errorMessage);
        }
        String addressStateOrRegion = amazonOrder.getString("addressStateOrRegion");
        String addressStateOrRegionStripped = null;
        GenericValue stateProvinceGeo = resolveGeo(addressStateOrRegion, delegator, countryGeo);
        if (UtilValidate.isEmpty(stateProvinceGeo)) {

            // Try again, removing any punctuation
            addressStateOrRegionStripped = addressStateOrRegion.replaceAll("[^\\p{L}\\p{Lu}]", "");
            stateProvinceGeo = resolveGeo(addressStateOrRegionStripped, delegator, countryGeo);
        }
        if (UtilValidate.isEmpty(stateProvinceGeo)) {
            errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_BadGeoStateProvince", UtilMisc.toMap("geoString", UtilMisc.toList(addressStateOrRegion, addressStateOrRegionStripped)), locale);
            throw new Exception(errorMessage);
        }

        // Shipping address
        String shippingAddressContactMechId = null;
        GenericValue shippingPostalAddress = resolveExistingPostalAddress(amazonOrder, orderPartyId, stateProvinceGeo, countryGeo, delegator);
        if (UtilValidate.isEmpty(shippingPostalAddress)) {

            // Construct a postal address
            shippingAddressContactMechId = delegator.getNextSeqId("ContactMech");
            toStore.addAll(constructPostalAddress(delegator, shippingAddressContactMechId, orderPartyId, amazonOrder, stateProvinceGeo, countryGeo));
        } else {
            shippingAddressContactMechId = shippingPostalAddress.getString("contactMechId");
        }

        // Shipping phone number
        String addressPhoneNumber = amazonOrder.getString("addressPhoneNumber");
        String shippingPhoneContactMechId = null;
        if (UtilValidate.isNotEmpty(addressPhoneNumber)) {
            shippingPhoneContactMechId = null;
            GenericValue shippingPhoneNumber = resolveExistingPhoneNumber(addressPhoneNumber, orderPartyId, delegator, locale);
        if (UtilValidate.isEmpty(shippingPhoneNumber)) {

            // Construct a phone number
            shippingPhoneContactMechId = delegator.getNextSeqId("ContactMech");
            toStore.addAll(constructPhoneNumber(amazonOrder.getString("addressPhoneNumber"), delegator, shippingPhoneContactMechId, locale, orderPartyId));
        } else {
            shippingPhoneContactMechId = shippingPhoneNumber.getString("contactMechId");
        }
        }

        // Get the orderId
        Map<String, Object> serviceResult = dispatcher.runSync("getNextOrderId", UtilMisc.toMap("partyId", productStore.getString("payToPartyId"), "productStoreId", AmazonConstants.productStoreId, "userLogin", userLogin));
        if (ServiceUtil.isError(serviceResult)) {
            throw new Exception(ServiceUtil.getErrorMessage(serviceResult));
        }
        String orderId = (String) serviceResult.get("orderId");

        // OrderHeader
        GenericValue orderHeader = constructOrderHeader(delegator, orderId, amazonOrder, userLogin, currencyUomId, orderPartyId, productStore);
        BigDecimal grandTotal = calculateGrandTotal(itemPriceComponents, itemPromos, itemFees);
        orderHeader.set("grandTotal", grandTotal.doubleValue());
        toStore.add(orderHeader);

        // Set this order to bill to third party if the field is set for the Amazon integration product store
        if (UtilValidate.isNotEmpty(productStore.get("billToThirdPartyId"))) {
            String oppId = delegator.getNextSeqId("OrderPaymentPreference");
            toStore.add(delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", oppId, "orderId", orderId, "paymentMethodTypeId", "EXT_BILL_3RDPTY ", "maxAmount", grandTotal, "statusId", "PAYMENT_AUTHORIZED")));
        }

        // OrderStatus
        toStore.add(constructOrderStatus(delegator, orderId, null, "ORDER_CREATED", userLogin));

        // OrderItemShipGroup
        String shipGroupSeqId = "00001";
        toStore.add(constructOrderItemShipGroup(amazonOrder, delegator, orderId, shipGroupSeqId, shippingAddressContactMechId, shippingPhoneContactMechId, timeZone, locale));

        List<GenericValue> orderItems = new ArrayList<GenericValue>();
        Map<GenericValue, GenericValue> orderItemCrossRef = new HashMap<GenericValue, GenericValue>();
        for (int x = 1; x <= amazonOrderItems.size(); x++) {

            // OrderItem
            GenericValue amazonOrderItem = amazonOrderItems.get(x - 1);
            String orderItemSeqId = UtilFormatOut.formatPaddedNumber(x, 5);
            GenericValue orderItem = constructOrderItem(delegator, orderId, orderItemSeqId, amazonOrderItem, locale, itemPriceComponents);
            orderItems.add(orderItem);
            toStore.add(orderItem);
            orderItemCrossRef.put(orderItem, amazonOrderItem);

            // OrderStatus
            toStore.add(constructOrderStatus(delegator, orderId, orderItemSeqId, "ITEM_CREATED", userLogin));

            // OrderItemShipGroupAssoc
            GenericValue orderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId));
            orderItemShipGroupAssoc.set("orderItemSeqId", orderItemSeqId);
            orderItemShipGroupAssoc.set("shipGroupSeqId", shipGroupSeqId);
            orderItemShipGroupAssoc.set("quantity", amazonOrderItem.getDouble("quantity"));
            toStore.add(orderItemShipGroupAssoc);

            // Promotion OrderAdjustments
            for (GenericValue itemPromo : itemPromos.get(amazonOrderItem.getString("amazonOrderItemCode"))) {
                GenericValue orderAdjustment = constructOrderAdjustment(delegator, orderId, orderItemSeqId, shipGroupSeqId, itemPromo.getDouble("promoAmount"), "PROMOTION_ADJUSTMENT");
                orderAdjustment.set("productPromoId", itemPromo.getString("merchantPromotionId"));
                toStore.add(orderAdjustment);

                // ProductPromoUse
                if (UtilValidate.isNotEmpty(itemPromo.getString("promotionClaimCode"))) {
                    GenericValue productPromoUse = delegator.makeValue("ProductPromoUse", UtilMisc.toMap("orderId", orderId, "promoSequenceId", UtilFormatOut.formatPaddedNumber(x - 1, 5)));
                    productPromoUse.set("productPromoId", itemPromo.getString("merchantPromotionId"));
                    productPromoUse.set("productPromoCodeId", itemPromo.getString("promotionClaimCode"));
                    productPromoUse.set("totalDiscountAmount", itemPromo.getDouble("promoAmount"));
                    productPromoUse.set("partyId", orderPartyId);
                    toStore.add(productPromoUse);
                }
            }

            // Shipping OrderAdjustments
            for (GenericValue priceComponent : itemPriceComponents.get(amazonOrderItem.getString("amazonOrderItemCode"))) {
                if (!"Shipping".equals(priceComponent.getString("componentType"))) {
                    continue;
                }
                toStore.add(constructOrderAdjustment(delegator, orderId, orderItemSeqId, shipGroupSeqId, priceComponent.getDouble("componentAmount"), "SHIPPING_CHARGES"));
            }

            // Tax OrderAdjustments
            List<GenericValue> amazonOrderItemTaxJurisdtns = amazonOrderItem.getRelated("AmazonOrderItemTaxJurisdtn");
            for (GenericValue amazonOrderItemTaxJurisdtn : amazonOrderItemTaxJurisdtns) {

                // Get the related TaxCollectedAmounts for the amazonOrderItemTaxJurisdtn
                List<GenericValue> amazonOrderItemTaxAmounts = amazonOrderItemTaxJurisdtn.getRelatedByAnd("AmazonOrderItemTaxAmount", UtilMisc.toMap("taxAmountType", "TaxCollectedAmounts"));
                BigDecimal taxTotal = AmazonConstants.ZERO;

                // Sum the TaxCollectedAmounts collected (for qty 1)
                for (GenericValue amazonOrderItemTaxAmount : amazonOrderItemTaxAmounts) {
                    taxTotal = taxTotal.add(amazonOrderItemTaxAmount.getBigDecimal("taxAmount").setScale(AmazonConstants.decimals, AmazonConstants.rounding));
                }

                // Skip zero-amount adjustments
                if (taxTotal.signum() <= 0) {
                    continue;
                }

                // Construct the OrderAdjustment
                GenericValue orderAdjustment = constructOrderAdjustment(delegator, orderId, orderItemSeqId, shipGroupSeqId, new Double(taxTotal.doubleValue()), "SALES_TAX");

                // Get the correct TaxAuthority for the tax jurisdiction
                GenericValue taxAuthority = resolveTaxAuthority(amazonOrderItemTaxJurisdtn, locale);

                // Error if a TaxAuthority is required and one isn't found
                if (AmazonConstants.requireTaxAuthority && UtilValidate.isEmpty(taxAuthority)) {
                    errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_MissingTaxAuthMap", amazonOrderItemTaxJurisdtn, locale);
                    throw new Exception(errorMessage);
                }

                if (UtilValidate.isNotEmpty(taxAuthority)) {
                    orderAdjustment.set("taxAuthGeoId", taxAuthority.getString("taxAuthGeoId"));
                    orderAdjustment.set("taxAuthPartyId", taxAuthority.getString("taxAuthPartyId"));

                    // Get the overrideGlAccountId
                    GenericValue taxAuthorityGlAccount = delegator.findByPrimaryKey("TaxAuthorityGlAccount", UtilMisc.toMap("taxAuthPartyId", taxAuthority.getString("taxAuthPartyId"), "taxAuthGeoId", taxAuthority.getString("taxAuthGeoId"), "organizationPartyId", productStore.getString("payToPartyId")));
                    if (UtilValidate.isNotEmpty(taxAuthorityGlAccount)) {
                        orderAdjustment.set("overrideGlAccountId", taxAuthorityGlAccount.getString("glAccountId"));
                    }
                }

                toStore.add(orderAdjustment);
            }

            // Fee OrderAdjustments
            for (GenericValue fee : itemFees.get(amazonOrderItem.getString("amazonOrderItemCode"))) {
                toStore.add(constructOrderAdjustment(delegator, orderId, orderItemSeqId, shipGroupSeqId, fee.getDouble("feeAmount"), "FEE"));
            }

            // AmazonOrderItemImport
            GenericValue amazonOrderItemImport = delegator.makeValue("AmazonOrderItemImport", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "amazonOrderId", amazonOrder.getString("amazonOrderId")));
            amazonOrderItemImport.set("amazonOrderItemCode", amazonOrderItem.getString("amazonOrderItemCode"));
            amazonOrderItemImport.set("productId", orderItem.getString("productId"));
            toStore.add(amazonOrderItemImport);
        }

        // OrderContactMechs
        GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechId", shippingAddressContactMechId, "contactMechPurposeTypeId", "SHIPPING_LOCATION"));
        toStore.add(orderContactMech);
        orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechId", emailContactMechId, "contactMechPurposeTypeId", "ORDER_EMAIL"));
        toStore.add(orderContactMech);

        // Party and Order Roles
        for (String roleTypeId : AmazonConstants.orderPartyRoleTypeIds) {
            GenericValue partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", orderPartyId, "roleTypeId", roleTypeId));
            if (UtilValidate.isEmpty(partyRole)) {
                toStore.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", orderPartyId, "roleTypeId", roleTypeId)));
            }
            toStore.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", orderPartyId, "roleTypeId", roleTypeId)));
        }

        // Make sure the organization party has the BILL_FROM_VENDOR role
        GenericValue orgPartyBillToRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", productStore.getString("payToPartyId"), "roleTypeId", "BILL_FROM_VENDOR"));
        if (UtilValidate.isEmpty(orgPartyBillToRole)) {
            toStore.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", productStore.getString("payToPartyId"), "roleTypeId", "BILL_FROM_VENDOR")));
        }
        toStore.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", productStore.getString("payToPartyId"), "roleTypeId", "BILL_FROM_VENDOR")));

        // AmazonOrderImport
        GenericValue amazonOrderImport = delegator.makeValue("AmazonOrderImport", UtilMisc.toMap("orderId", orderId, "amazonOrderId", amazonOrder.getString("amazonOrderId"), "buyerPartyId", orderPartyId));
        amazonOrderImport.set("addressPostalContactMechId", shippingAddressContactMechId);
        amazonOrderImport.set("addressPhoneContactMechId", shippingPhoneContactMechId);
        amazonOrderImport.set("buyerPhoneContactMechId", customerPhoneContactMechId);
        toStore.add(amazonOrderImport);

        delegator.storeAll(toStore);

        // Reserve inventory for each order item
        for (GenericValue orderItem : orderItems) {
            Map<String, Object> reserveContext = UtilMisc.<String, Object>toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "requireInventory", "Y".equals(productStore.getString("requireInventory")) ? "Y" : "N", "userLogin", userLogin);
            reserveContext.put("productId", orderItem.getString("productId"));
            reserveContext.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
            reserveContext.put("quantity", orderItem.getBigDecimal("quantity"));
            reserveContext.put("facilityId", productStore.getString("inventoryFacilityId"));
            serviceResult = dispatcher.runSync("reserveProductInventoryByFacility", reserveContext);
            if (ServiceUtil.isError(serviceResult)) {
                throw new Exception(ServiceUtil.getErrorMessage(serviceResult));
            }
            if (UtilValidate.isNotEmpty(serviceResult.get("quantityNotReserved"))) {
                BigDecimal quantityNotReserved = (BigDecimal) serviceResult.get("quantityNotReserved");
                if ("Y".equals(productStore.getString("requireInventory")) && quantityNotReserved.signum() > 0) {
                    GenericValue amazonOrderItem = orderItemCrossRef.get(orderItem);
                    errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_InsufficientInventory", UtilMisc.toMap("productId", orderItem.getString("productId"), "quantityNotReserved", quantityNotReserved, "amazonOrderId", amazonOrderItem.getString("amazonOrderId"), "amazonOrderItemCode", amazonOrderItem.getString("amazonOrderItemCode")), locale);
                    throw new Exception(errorMessage);
        }
            }
        }

        // Approve the order, if necessary
        if (AmazonConstants.approveOrders) {
            serviceResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "statusId", "ITEM_APPROVED", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                throw new Exception(ServiceUtil.getErrorMessage(serviceResult));
            }
        }

        return orderId;
    }

    private static GenericValue constructOrderAdjustment(Delegator delegator, String orderId, String orderItemSeqId, String shipGroupSeqId, Double amount, String orderAdjustmentTypeId) {
        GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"), "orderId", orderId, "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId));
        orderAdjustment.set("orderAdjustmentTypeId", orderAdjustmentTypeId);
        orderAdjustment.set("amount", amount);
        return orderAdjustment;
    }

    private static GenericValue constructOrderStatus(Delegator delegator, String orderId, String orderItemSeqId, String statusId, GenericValue userLogin) {
        GenericValue orderStatus = delegator.makeValue("OrderStatus", UtilMisc.toMap("orderId", orderId, "orderStatusId", delegator.getNextSeqId("OrderStatus"), "statusId", statusId));
        orderStatus.set("orderItemSeqId", orderItemSeqId);
        orderStatus.set("statusDatetime", UtilDateTime.nowTimestamp());
        orderStatus.set("statusUserLogin", userLogin.getString("userLoginId"));
        return orderStatus;
    }

    private static GenericValue constructOrderItemShipGroup(GenericValue amazonOrder, Delegator delegator, String orderId, String shipGroupSeqId, String shippingAddressContactMechId, String shippingPhoneContactMechId, TimeZone timeZone, Locale locale) {
        String serviceLevel = amazonOrder.getString("fulfillmentServiceLevel");
        GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId));
        orderItemShipGroup.set("shipGroupSeqId", shipGroupSeqId);
        orderItemShipGroup.set("shipmentMethodTypeId", AmazonConstants.shipmentMethodTypeIds.get(serviceLevel));
        orderItemShipGroup.set("carrierPartyId", AmazonConstants.carrierPartyIds.get(serviceLevel));
        orderItemShipGroup.set("carrierRoleTypeId", "CARRIER");
        orderItemShipGroup.set("contactMechId", shippingAddressContactMechId);
        orderItemShipGroup.set("telecomContactMechId", shippingPhoneContactMechId);
        orderItemShipGroup.set("maySplit", "N");
        orderItemShipGroup.set("isGift", "N");
        orderItemShipGroup.set("shipByDate", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.DATE, AmazonConstants.maxDaysToShip.get(serviceLevel), timeZone, locale));
        return orderItemShipGroup;
    }

    private static List<GenericValue> constructPostalAddress(Delegator delegator, String shippingAddressContactMechId, String orderPartyId, GenericValue amazonOrder, GenericValue stateProvinceGeo, GenericValue countryGeo) {
        List<GenericValue> postalAddressValues = new ArrayList<GenericValue>();
        GenericValue shippingPostalAddress = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", shippingAddressContactMechId, "contactMechTypeId", "POSTAL_ADDRESS"));
        postalAddressValues.add(shippingPostalAddress);
        GenericValue postalPartyContactMech = delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", shippingAddressContactMechId));
        postalPartyContactMech.set("partyId", orderPartyId);
        postalPartyContactMech.put("fromDate", UtilDateTime.nowTimestamp());
        postalPartyContactMech.set("allowSolicitation", "N");
        postalAddressValues.add(postalPartyContactMech);
        GenericValue postalAddress = delegator.makeValue("PostalAddress", UtilMisc.toMap("contactMechId", shippingAddressContactMechId));
        postalAddress.setNonPKFields(getAddressFields(amazonOrder));
        postalAddress.set("stateProvinceGeoId", stateProvinceGeo.getString("geoId"));
        postalAddress.set("countryGeoId", countryGeo.getString("geoId"));
        postalAddressValues.add(postalAddress);
        for (String contactMechPurposeType : AmazonConstants.shippingAddressContactMechPurposes) {
            GenericValue partyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose", UtilMisc.toMap("partyId", orderPartyId, "contactMechId", shippingAddressContactMechId, "contactMechPurposeTypeId", contactMechPurposeType, "fromDate", UtilDateTime.nowTimestamp()));
            postalAddressValues.add(partyContactMechPurpose);
        }
        return postalAddressValues;
    }

    private static GenericValue resolveExistingPostalAddress(GenericValue amazonOrder, String orderPartyId, GenericValue stateProvinceGeo, GenericValue countryGeo, Delegator delegator) throws GenericEntityException {
        Map<String, String> addressData = getAddressFields(amazonOrder);
        EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyId"), EntityOperator.EQUALS, EntityFunction.UPPER(orderPartyId)),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("contactMechTypeId"), EntityOperator.EQUALS, "POSTAL_ADDRESS"),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("toName"), EntityOperator.EQUALS, EntityFunction.UPPER(addressData.get("toName"))),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address1"), EntityOperator.EQUALS, EntityFunction.UPPER(addressData.get("address1"))),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address2"), EntityOperator.EQUALS, EntityFunction.UPPER(addressData.get("address2"))),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("city"), EntityOperator.EQUALS, EntityFunction.UPPER(addressData.get("city"))),
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("postalCode"), EntityOperator.EQUALS, EntityFunction.UPPER(addressData.get("postalCode"))),
                                    EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, stateProvinceGeo.getString("geoId")),
                                    EntityCondition.makeCondition("countryGeoId", EntityOperator.EQUALS, countryGeo.getString("geoId")));
        List<GenericValue> partyAndPostalAddresses = delegator.findByCondition("PartyAndPostalAddress", cond, null, null);
        partyAndPostalAddresses = EntityUtil.filterByDate(partyAndPostalAddresses);
        return EntityUtil.getFirst(partyAndPostalAddresses);
    }

    private static Map<String, String> getAddressFields(GenericValue amazonOrder) {
        Map<String, String> addressData = new HashMap<String, String>();
        addressData.put("toName", amazonOrder.getString("addressName"));
        addressData.put("address1", amazonOrder.getString("addressFieldOne"));
        String addressFieldTwo = amazonOrder.getString("addressFieldTwo");
        if (UtilValidate.isNotEmpty(amazonOrder.getString("addressFieldThree"))) {
            addressFieldTwo += " " + amazonOrder.getString("addressFieldThree");
        }
        addressData.put("address2", addressFieldTwo);
        addressData.put("city", amazonOrder.getString("addressCity"));
        addressData.put("postalCode", amazonOrder.getString("addressPostalCode"));
        return addressData;
    }

    private static GenericValue resolveExistingPhoneNumber(String phoneNumber, String orderPartyId, Delegator delegator, Locale locale) throws GenericEntityException {
        if (UtilValidate.isEmpty(phoneNumber)) {
            return null;
        }
        Map<String, String> phoneNumberData = getPhoneNumberFields(phoneNumber, locale);
        EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, orderPartyId),
                                        EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER"),
                                        EntityCondition.makeCondition("countryCode", EntityOperator.EQUALS, phoneNumberData.get("countryCode")),
                                        EntityCondition.makeCondition("areaCode", EntityOperator.EQUALS, phoneNumberData.get("areaCode")),
                                        EntityCondition.makeCondition("contactNumber", EntityOperator.EQUALS, phoneNumberData.get("contactNumber")),
                                        EntityCondition.makeCondition("extension", EntityOperator.EQUALS, phoneNumberData.get("extension")));
        List<GenericValue> partyAndTelecomNumbers = delegator.findByCondition("PartyAndTelecomNumber", cond, null, null);
        partyAndTelecomNumbers = EntityUtil.filterByDate(partyAndTelecomNumbers);
        return EntityUtil.getFirst(partyAndTelecomNumbers);
    }

    private static Map<String, String> getPhoneNumberFields(String phoneNumber, Locale locale) {
        Map<String, String> phoneNumberData = new HashMap<String, String>();
        Matcher matcher = AmazonConstants.phoneNumberPattern.matcher(phoneNumber);
        if (matcher.matches()) {
            if (UtilValidate.isNotEmpty(matcher.group(AmazonConstants.phoneNumberPatternCountryCodeGroup))) {
                phoneNumberData.put("countryCode", matcher.group(AmazonConstants.phoneNumberPatternCountryCodeGroup));
            }
            if (UtilValidate.isNotEmpty(matcher.group(AmazonConstants.phoneNumberPatternAreaCodeGroup))) {
                phoneNumberData.put("areaCode", matcher.group(AmazonConstants.phoneNumberPatternAreaCodeGroup));
            }
            if (UtilValidate.isNotEmpty(matcher.group(AmazonConstants.phoneNumberPatternPhoneNumberGroup))) {
                phoneNumberData.put("contactNumber", matcher.group(AmazonConstants.phoneNumberPatternPhoneNumberGroup).replaceAll("\\D", ""));
            }
            if (UtilValidate.isNotEmpty(matcher.group(AmazonConstants.phoneNumberPatternExtensionGroup))) {
                phoneNumberData.put("extension", matcher.group(AmazonConstants.phoneNumberPatternExtensionGroup));
            }
        } else {
            String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_PhoneNumberParseFailure", UtilMisc.toMap("phoneNumber", phoneNumber), locale);
            Debug.logWarning(message, MODULE);
            phoneNumberData.put("contactNumber", phoneNumber);
        }
        return phoneNumberData;
    }

    private static GenericValue resolveGeo(String geoString, Delegator delegator, GenericValue parentGeo) throws GenericEntityException {

        boolean isCountry = UtilValidate.isEmpty(parentGeo);
        List geoTypeIds = isCountry ? Arrays.asList("COUNTRY") : Arrays.asList("STATE", "PROVINCE");
        String entity = isCountry ? "Geo" : "GeoAssocAndGeoTo";

        // Try to match the geoCode first, then abbreviation, then geoName, filtered by geoTypeId
        GenericValue geo = null;
        List<EntityCondition> conds = UtilMisc.<EntityCondition>toList(
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("geoCode"), EntityOperator.EQUALS, EntityFunction.UPPER(geoString)),
                                    EntityCondition.makeCondition("geoTypeId", EntityOperator.IN, geoTypeIds));
        if (!isCountry) {
            conds.add(EntityCondition.makeCondition("geoIdFrom", EntityOperator.EQUALS, parentGeo.getString("geoId")));
            conds.add(EntityCondition.makeCondition("geoAssocTypeId", EntityOperator.EQUALS, "REGIONS"));
        }
        EntityCondition cond = EntityCondition.makeCondition(conds, EntityOperator.AND);
        geo = EntityUtil.getFirst(delegator.findByCondition(entity, cond, null, null));
        if (UtilValidate.isEmpty(geo)) {
            cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("abbreviation"), EntityOperator.EQUALS, EntityFunction.UPPER(geoString)),
                                    EntityCondition.makeCondition("geoTypeId", EntityOperator.IN, geoTypeIds));
            geo = EntityUtil.getFirst(delegator.findByCondition(entity, cond, null, null));
        }
        if (UtilValidate.isEmpty(geo)) {
            cond = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("geoName"), EntityOperator.EQUALS, EntityFunction.UPPER(geoString)),
                                    EntityCondition.makeCondition("geoTypeId", EntityOperator.IN, geoTypeIds));
            geo = EntityUtil.getFirst(delegator.findByCondition(entity, cond, null, null));
        }
        return geo;
    }

    private static GenericValue constructOrderItem(Delegator delegator, String orderId, String orderItemSeqId, GenericValue amazonOrderItem, Locale locale, LinkedHashMap<String, List<GenericValue>> itemPriceComponents) throws Exception {
        String errorMessage = null;
        GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", orderId));
        orderItem.set("orderItemSeqId", orderItemSeqId);
        orderItem.set("externalId", amazonOrderItem.getString("amazonOrderItemCode"));
        orderItem.set("orderItemTypeId", "PRODUCT_ORDER_ITEM");
        orderItem.set("isPromo", "N");
        orderItem.set("itemDescription", amazonOrderItem.getString("title"));
        orderItem.set("statusId", "ITEM_CREATED");
        orderItem.set("quantity", amazonOrderItem.getDouble("quantity"));
        orderItem.set("isModifiedPrice", "N");
        String productId = null;
        if (AmazonConstants.useProductIdAsSKU) {
            productId = amazonOrderItem.getString("sku");
        } else if (AmazonConstants.useUPCAsSKU) {

            // Try UPC-A first
            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, "UPCA"),
                                        EntityCondition.makeCondition("idValue", EntityOperator.EQUALS, amazonOrderItem.getString("sku")),
                                        EntityCondition.makeCondition("idValue", EntityOperator.NOT_EQUAL, ""));
            GenericValue goodIdent = EntityUtil.getFirst(delegator.findByCondition("GoodIdentification", cond, null, UtilMisc.toList("lastUpdatedStamp DESC")));
            if (UtilValidate.isNotEmpty(goodIdent)) {
                productId = goodIdent.getString("productId");
            } else {

                // Try UPC-A compressed to UPC-E
                String upce = UtilProduct.compressUPCA(amazonOrderItem.getString("sku"));
                if (UtilValidate.isNotEmpty(upce)) {
                    cond = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, "UPCE"),
                                        EntityCondition.makeCondition("idValue", EntityOperator.EQUALS, upce),
                                        EntityCondition.makeCondition("idValue", EntityOperator.NOT_EQUAL, ""));
                    goodIdent = EntityUtil.getFirst(delegator.findByCondition("GoodIdentification", cond, null, UtilMisc.toList("lastUpdatedStamp DESC")));
                    if (UtilValidate.isNotEmpty(goodIdent)) {
                        productId = goodIdent.getString("productId");
                    }
                }
            }
        } else {
            GenericValue goodIdent = EntityUtil.getFirst(delegator.findByAnd("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "SKU", "idValue", amazonOrderItem.getString("sku"))));
            if (UtilValidate.isNotEmpty(goodIdent)) {
                productId = goodIdent.getString("productId");
            }
        }
        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        if (UtilValidate.isEmpty(product)) {
            errorMessage = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ProductIdNotFound", UtilMisc.toMap("sku", amazonOrderItem.getString("sku")), locale);
            throw new Exception(errorMessage);
        }
        orderItem.set("productId", productId);
        BigDecimal itemTotal = AmazonConstants.ZERO;
        for (GenericValue priceComponent : itemPriceComponents.get(amazonOrderItem.getString("amazonOrderItemCode"))) {
            if ("Principal".equals(priceComponent.getString("componentType"))) {
                itemTotal = itemTotal.add(priceComponent.getBigDecimal("componentAmount").setScale(AmazonConstants.decimals, AmazonConstants.rounding));
            }
        }
        BigDecimal itemPrice = itemTotal.divide(amazonOrderItem.getBigDecimal("quantity")).setScale(AmazonConstants.decimals, AmazonConstants.rounding);
        orderItem.set("unitListPrice", itemPrice.doubleValue());
        orderItem.set("unitPrice", itemPrice.doubleValue());
        return orderItem;
    }

    private static List<GenericValue> constructPhoneNumber(String phoneNumber, Delegator delegator, String customerPhoneContactMechId, Locale locale, String orderPartyId) {
        List<GenericValue> phoneValues = new ArrayList<GenericValue>();
        GenericValue customerPhoneContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", customerPhoneContactMechId, "contactMechTypeId", "TELECOM_NUMBER"));
        phoneValues.add(customerPhoneContactMech);
        Map<String, String> phoneNumberData = getPhoneNumberFields(phoneNumber, locale);
        GenericValue customerTelecomNumber = delegator.makeValue("TelecomNumber", UtilMisc.toMap("contactMechId", customerPhoneContactMechId));
        customerTelecomNumber.set("countryCode", phoneNumberData.get("countryCode"));
        customerTelecomNumber.set("areaCode", phoneNumberData.get("areaCode"));
        customerTelecomNumber.set("contactNumber", phoneNumberData.get("contactNumber"));
        phoneValues.add(customerTelecomNumber);
        GenericValue customerPhonePartyContactMech = delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", customerPhoneContactMechId));
        customerPhonePartyContactMech.set("partyId", orderPartyId);
        customerPhonePartyContactMech.put("fromDate", UtilDateTime.nowTimestamp());
        customerPhonePartyContactMech.set("allowSolicitation", "N");
        customerPhonePartyContactMech.set("extension", phoneNumberData.get("extension"));
        phoneValues.add(customerPhonePartyContactMech);
        for (String contactMechPurposeTypeId : AmazonConstants.customerPhoneContactMechPurposes) {
            GenericValue contactMechPurpose = delegator.makeValue("PartyContactMechPurpose", UtilMisc.toMap("contactMechId", customerPhoneContactMechId, "partyId", orderPartyId, "fromDate", UtilDateTime.nowTimestamp(), "contactMechPurposeTypeId", contactMechPurposeTypeId));
            phoneValues.add(contactMechPurpose);
        }
        return phoneValues;
    }

    private static List<GenericValue> constructEmailAddress(Delegator delegator, String emailContactMechId, GenericValue amazonOrder, String orderPartyId) {
        List<GenericValue> emailValues = new ArrayList<GenericValue>();
        GenericValue emailContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", emailContactMechId, "contactMechTypeId", "EMAIL_ADDRESS"));
        emailContactMech.set("infoString", amazonOrder.getString("buyerEmailAddress"));
        emailValues.add(emailContactMech);
        GenericValue emailPartyContactMech = delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", emailContactMechId));
        emailPartyContactMech.set("partyId", orderPartyId);
        emailPartyContactMech.put("fromDate", UtilDateTime.nowTimestamp());
        emailPartyContactMech.set("allowSolicitation", "N");
        emailValues.add(emailPartyContactMech);
        for (String contactMechPurposeTypeId : AmazonConstants.emailContactMechPurposes) {
            GenericValue contactMechPurpose = delegator.makeValue("PartyContactMechPurpose", UtilMisc.toMap("contactMechId", emailContactMechId, "partyId", orderPartyId, "fromDate", UtilDateTime.nowTimestamp(), "contactMechPurposeTypeId", contactMechPurposeTypeId));
            emailValues.add(contactMechPurpose);
        }
        return emailValues;
    }

    private static GenericValue constructPerson(GenericValue amazonOrder, Delegator delegator, String orderPartyId) {
        // Amazon stores the name as one field, so we'll try to split it based on the first space. If there aren't any spaces
        //  then it all goes into the lastName field.
        String buyerName = amazonOrder.getString("buyerName");
        String firstName = buyerName.indexOf(" ") != -1 ? buyerName.split(" ")[0] : null;
        String lastName = buyerName.indexOf(" ") != -1 ? buyerName.substring(buyerName.indexOf(" ") + 1) : buyerName;
        GenericValue person = delegator.makeValue("Person", UtilMisc.toMap("partyId", orderPartyId));
        person.set("firstName", firstName);
        person.set("lastName", lastName);
        return person;
    }

    private static GenericValue constructOrderHeader(Delegator delegator, String orderId, GenericValue amazonOrder, GenericValue userLogin, String currencyUomId, String orderPartyId, GenericValue productStore) {
        GenericValue orderHeader = delegator.makeValue("OrderHeader", UtilMisc.toMap("orderId", orderId));
        orderHeader.set("externalId", amazonOrder.getString("amazonOrderId"));
        orderHeader.set("salesChannelEnumId", productStore.getString("defaultSalesChannelEnumId"));
        orderHeader.set("orderTypeId", "SALES_ORDER");
        orderHeader.set("orderDate", amazonOrder.getTimestamp("orderDate"));
        orderHeader.set("entryDate", UtilDateTime.nowTimestamp());
        orderHeader.set("statusId", "ORDER_CREATED");
        orderHeader.set("createdBy", userLogin.getString("userLoginId"));
        orderHeader.set("productStoreId", AmazonConstants.productStoreId);
        orderHeader.set("currencyUom", currencyUomId);
        orderHeader.set("remainingSubTotal", 0);
        orderHeader.set("billFromPartyId", productStore.getString("payToPartyId"));
        orderHeader.set("billToPartyId", orderPartyId);
        return orderHeader;
    }

    private static BigDecimal calculateGrandTotal(LinkedHashMap<String, List<GenericValue>> itemPriceComponents, LinkedHashMap<String, List<GenericValue>> itemPromos, LinkedHashMap<String, List<GenericValue>> itemFees) {
        BigDecimal grandTotal = AmazonConstants.ZERO;
        for (List<GenericValue> priceComponents : itemPriceComponents.values()) {
            for (GenericValue priceComponent : priceComponents) {
                grandTotal = grandTotal.add(priceComponent.getBigDecimal("componentAmount"));
            }
        }
        for (List<GenericValue> promos : itemPromos.values()) {
            for (GenericValue promo : promos) {
                grandTotal = grandTotal.add(promo.getBigDecimal("promoAmount"));
            }
        }
        for (List<GenericValue> fees : itemFees.values()) {
            for (GenericValue fee : fees) {
                grandTotal = grandTotal.add(fee.getBigDecimal("feeAmount"));
            }
        }
        grandTotal = grandTotal.setScale(AmazonConstants.decimals, AmazonConstants.rounding);
        return grandTotal;
    }

    /**
     * Get TaxAuthority mapped to the amazonOrderItemTaxJurisdtn via AmazonOrderTaxJurisToAuth - try to match district/city/county/state, then city/county/state,
     *  county/state and finally just state.
     * @param amazonOrderItemTaxJurisdtn a <code>GenericValue</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static GenericValue resolveTaxAuthority(GenericValue amazonOrderItemTaxJurisdtn, Locale locale) throws GenericEntityException {
        GenericValue taxAuthority = null;
        Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TaxJurisMatchTrying", amazonOrderItemTaxJurisdtn, locale), MODULE);
        GenericValue amazonOrderTaxJurisToAuth = amazonOrderItemTaxJurisdtn.getRelatedOne("AmazonOrderTaxJurisToAuth");
        if (UtilValidate.isEmpty(amazonOrderTaxJurisToAuth)) {
            Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TaxJurisMatchFail", amazonOrderItemTaxJurisdtn, locale), MODULE);
            for (String fieldName : Arrays.asList("taxJurisDistrict", "taxJurisCity", "taxJurisCounty", "taxJurisState")) {
                amazonOrderItemTaxJurisdtn.set(fieldName, "_NA_");
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TaxJurisMatchTrying", amazonOrderItemTaxJurisdtn, locale), MODULE);
                amazonOrderTaxJurisToAuth = amazonOrderItemTaxJurisdtn.getRelatedOne("AmazonOrderTaxJurisToAuth");
                if (UtilValidate.isNotEmpty(amazonOrderTaxJurisToAuth)) {
                    break;
                }
                Debug.logInfo(UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_TaxJurisMatchFail", amazonOrderItemTaxJurisdtn, locale), MODULE);
            }
            amazonOrderItemTaxJurisdtn.refresh();
        }
        if (UtilValidate.isNotEmpty(amazonOrderTaxJurisToAuth)) {
            taxAuthority = amazonOrderTaxJurisToAuth.getRelatedOne("TaxAuthority");
        }
        return taxAuthority;
    }

    /**
     * Posts acknowledgement to Amazon of successfully downloaded order documents.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> acknowledgeOrderDocumentDownload(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            // Get a list of all order documents which haven't been acknowledged or for which acknowledgement failed last time
            List<GenericValue> amazonOrderDocuments = delegator.findByCondition("AmazonOrderDocument", EntityCondition.makeCondition("ackStatusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusDocNotAcknowledged, AmazonConstants.statusDocAcknowledgedError)), null, null);
            if (UtilValidate.isEmpty(amazonOrderDocuments)) {
                String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckDocumentNoDocuments", locale);
                Debug.logInfo(message, MODULE);
                return ServiceUtil.returnSuccess(message);
            }
            List<String> successfulDocumentIds = EntityUtil.getFieldListFromEntityList(amazonOrderDocuments, "documentId", true);

            // Attempt to acknowledge the documents
            boolean success = true;
            String ackErrorMessage = null;
            Map<String, String> ackResults = null;
            try {
                ackResults = AmazonConstants.soapClient.acknowledgeDocumentDownload(successfulDocumentIds);
            } catch (RemoteException e) {
                success = false;
                ackErrorMessage = e.getMessage();
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckDocumentError", UtilMisc.toMap("documentIds", successfulDocumentIds, "errorMessage", ackErrorMessage), locale);
                Debug.logError(errorLog, MODULE);
            }

            // Update the records in the database with the result
            for (GenericValue amazonOrderDocument : amazonOrderDocuments) {
                boolean docAckSuccess = success;
                if (!AmazonConstants.downloadAckSuccessResult.equals(ackResults.get(amazonOrderDocument.getString("documentId")))) {
                    docAckSuccess = false;
                    ackErrorMessage = ackResults.get(amazonOrderDocument.getString("documentId"));
                }
                amazonOrderDocument.set("ackStatusId", docAckSuccess ? AmazonConstants.statusDocAcknowledged : AmazonConstants.statusDocAcknowledgedError);
                amazonOrderDocument.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
                amazonOrderDocument.set("acknowledgeErrorMessage", docAckSuccess ? null : ackErrorMessage);
                if (!docAckSuccess) {
                    Map<String, String> errorMap = UtilMisc.toMap("documentId", amazonOrderDocument.getString("documentId"), "errorMessage", ackErrorMessage);
                    if (AmazonConstants.sendErrorEmails) {
                        AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_AckOrderDoc", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                    }
                }
            }
            delegator.storeAll(amazonOrderDocuments);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Posts acknowledgement to Amazon of successfully imported orders.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> acknowledgeImportedOrders(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            // Get a list of all orders which haven't been acknowledged or for which acknowledgement failed last time
            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                                        EntityCondition.makeCondition("ackStatusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusOrderNotAcknowledged, AmazonConstants.statusOrderSuccessAcknowledgementError)),
                                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, AmazonConstants.statusOrderImported));
            List<GenericValue> amazonOrders = delegator.findByCondition("AmazonOrder", cond, null, null);
            if (UtilValidate.isEmpty(amazonOrders)) {
                String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckOrderNoOrders", locale);
                Debug.logInfo(message, MODULE);
                return ServiceUtil.returnSuccess(message);
            }
            List<String> amazonOrderIds = EntityUtil.getFieldListFromEntityList(amazonOrders, "amazonOrderId", true);

            // Construct the acknowledgement document
            Document doc = AmazonConstants.soapClient.createDocumentHeader("OrderAcknowledgement");
            Element root = doc.getDocumentElement();
            for (int messageId = 1; messageId <= amazonOrders.size(); messageId++) {
                GenericValue amazonOrder = amazonOrders.get(messageId - 1);
                amazonOrder.set("acknowledgeMessageId", "" + messageId);

                Element message = doc.createElement("Message");
                root.appendChild(message);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, doc);
                Element orderAck = doc.createElement("OrderAcknowledgement");
                message.appendChild(orderAck);
                UtilXml.addChildElementValue(orderAck, "AmazonOrderID", amazonOrder.getString("amazonOrderId"), doc);
                UtilXml.addChildElementValue(orderAck, "MerchantOrderID", amazonOrder.getRelatedOne("AmazonOrderImport").getString("orderId"), doc);
                UtilXml.addChildElementValue(orderAck, "StatusCode", "Success", doc);
                List<GenericValue> orderItems = amazonOrder.getRelated("AmazonOrderItemImport");
                for (GenericValue orderItem : orderItems) {
                    Element item = doc.createElement("Item");
                    orderAck.appendChild(item);
                    UtilXml.addChildElementValue(item, "AmazonOrderItemCode", orderItem.getString("amazonOrderItemCode"), doc);
                    String merchantOrderItemID = orderItem.getString("orderId") + orderItem.getString("orderItemSeqId");
                    UtilXml.addChildElementValue(item, "MerchantOrderItemID", merchantOrderItemID, doc);
                }
            }

            // Attempt to acknowledge the orders
            boolean success = true;
            String ackErrorMessage = null;
            long processingDocumentId = -1;
            try {
                processingDocumentId = AmazonConstants.soapClient.acknowledgeOrderDownload(UtilXml.writeXmlDocument(doc));
            } catch (RemoteException e) {
                success = false;
                ackErrorMessage = e.getMessage();
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckOrderError", UtilMisc.toMap("amazonOrderIds", amazonOrderIds, "errorMessage", ackErrorMessage), locale);
                Debug.logError(errorLog, MODULE);
            }

            // Update the records in the database with the result
            for (GenericValue amazonOrder : amazonOrders) {
                amazonOrder.set("ackStatusId", success ? AmazonConstants.statusOrderAckSent : AmazonConstants.statusOrderSuccessAcknowledgementError);
                amazonOrder.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
                amazonOrder.set("acknowledgeErrorMessage", success ? null : ackErrorMessage);
                amazonOrder.set("processingDocumentId", processingDocumentId);
                if (!success) {
                    Map<String, String> errorMap = UtilMisc.toMap("amazonOrderId", amazonOrder.getString("amazonOrderId"), "errorMessage", ackErrorMessage);
                    if (AmazonConstants.sendErrorEmails) {
                        AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_AckOrder", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                    }
                }
            }
            delegator.storeAll(amazonOrders);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Posts acknowledgement to Amazon of an unsuccessfully imported order.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> cancelUnimportedOrder(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String amazonOrderId = (String) context.get("amazonOrderId");

        try {

            GenericValue amazonOrder = delegator.findByPrimaryKey("AmazonOrder", UtilMisc.toMap("amazonOrderId", amazonOrderId));
            if (UtilValidate.isEmpty(amazonOrder)) {
                String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AmazonOrderNotFound", UtilMisc.toMap("amazonOrderId", amazonOrderId), locale);
                Debug.logError(message, MODULE);
                return ServiceUtil.returnError(message);
            }

            // Construct the acknowledgement document
            Document doc = AmazonConstants.soapClient.createDocumentHeader("OrderAcknowledgement");
            Element root = doc.getDocumentElement();
            amazonOrder.set("acknowledgeMessageId", "1");
            Element message = doc.createElement("Message");
            root.appendChild(message);
            UtilXml.addChildElementValue(message, "MessageID", "1", doc);
            Element orderAck = doc.createElement("OrderAcknowledgement");
            message.appendChild(orderAck);
            UtilXml.addChildElementValue(orderAck, "AmazonOrderID", amazonOrderId, doc);
            UtilXml.addChildElementValue(orderAck, "StatusCode", "Failure", doc);
            List<GenericValue> orderItems = amazonOrder.getRelated("AmazonOrderItem");
            for (GenericValue orderItem : orderItems) {
                Element item = doc.createElement("Item");
                orderAck.appendChild(item);
                UtilXml.addChildElementValue(item, "AmazonOrderItemCode", orderItem.getString("amazonOrderItemCode"), doc);
            }

            // Attempt to acknowledge the order
            boolean success = true;
            String ackErrorMessage = null;
            long processingDocumentId = -1;
            try {
                processingDocumentId = AmazonConstants.soapClient.acknowledgeOrderDownload(UtilXml.writeXmlDocument(doc));
            } catch (RemoteException e) {
                success = false;
                ackErrorMessage = e.getMessage();
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckOrderError", UtilMisc.toMap("amazonOrderIds", Arrays.asList(amazonOrderId), "errorMessage", ackErrorMessage), locale);
                Debug.logError(errorLog, MODULE);
            }

            // Update the records in the database with the result
            if (success) {
                amazonOrder.set("statusId", AmazonConstants.statusOrderCancelled);
            }
            amazonOrder.set("ackStatusId", success ? AmazonConstants.statusOrderAckFailureSent : AmazonConstants.statusOrderFailureAcknowledgementError);
            amazonOrder.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
            amazonOrder.set("acknowledgeErrorMessage", success ? null : ackErrorMessage);
            amazonOrder.set("processingDocumentId", processingDocumentId);
            if (!success) {
                Map<String, String> errorMap = UtilMisc.toMap("amazonOrderId", amazonOrderId, "errorMessage", ackErrorMessage);
                if (AmazonConstants.sendErrorEmails) {
                    AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_AckOrder", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                }
            }
            amazonOrder.store();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Queues any shipment quantities related to a Shipment/ShipmentRouteSegment derived from an Amazon order for fulfillment confirmation posting.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> queueShippedItemsForFulfillmentPost(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        try {

            String carrierPartyId = null;
            String shipmentMethodTypeId = null;
            String srsTrackingIdNumber = null;
            String sprsTrackingIdNumber = null;
            GenericValue shipmentRouteSegment = null;

            if (UtilValidate.isEmpty(shipmentRouteSegmentId)) {

                // Try to get the first ShipmentRouteSegment for the shipment
                shipmentRouteSegment = EntityUtil.getFirst(delegator.findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId), Arrays.asList("shipmentRouteSegmentId")));
                shipmentRouteSegmentId = shipmentRouteSegment.getString("shipmentRouteSegmentId");
            }
            if (UtilValidate.isNotEmpty(shipmentRouteSegment)) {

                carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
                shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
                srsTrackingIdNumber = shipmentRouteSegment.getString("trackingIdNumber");
            }

            List<GenericValue> toStore = new ArrayList<GenericValue>();
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            List<GenericValue> shipmentPackages = shipment.getRelated("ShipmentPackage");
            for (GenericValue shipmentPackage : shipmentPackages) {
                String shipmentPackageSeqId = shipmentPackage.getString("shipmentPackageSeqId");

                // The ShipmentPackageRouteSeg tracking number overrides the ShipmentRouteSegment tracking number
                if (UtilValidate.isNotEmpty(shipmentRouteSegmentId)) {
                    GenericValue shipmentPackageRouteSeg = delegator.findByPrimaryKey("ShipmentPackageRouteSeg", UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackageSeqId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
                    sprsTrackingIdNumber = shipmentPackageRouteSeg.getString("trackingCode");
                }

                List<GenericValue> shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent");
                for (GenericValue shipmentPackageContent : shipmentPackageContents) {
                    GenericValue shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem");
                    if (UtilValidate.isEmpty(shipmentItem)) {
                        continue;
                    }
                    List<GenericValue> itemIssuances = shipmentItem.getRelated("ItemIssuance");
                    for (GenericValue itemIssuance : itemIssuances) {
                        GenericValue orderItem = itemIssuance.getRelatedOne("OrderItem");
                        if (UtilValidate.isEmpty(orderItem)) {
                            continue;
                        }
                        GenericValue amazonOrderItemImport = EntityUtil.getFirst(delegator.findByAnd("AmazonOrderItemImport", UtilMisc.toMap("orderId", itemIssuance.getString("orderId"), "orderItemSeqId", itemIssuance.getString("orderItemSeqId"))));
                        if (UtilValidate.isEmpty(amazonOrderItemImport)) {
                            continue;
                        }

                        String amazonOrderId = amazonOrderItemImport.getString("amazonOrderId");
                        String amazonOrderItemCode = amazonOrderItemImport.getString("amazonOrderItemCode");
                        String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");
                        String itemIssuanceId = itemIssuance.getString("itemIssuanceId");
                        Double quantity = shipmentPackageContent.getDouble("quantity");

                        Map<String, Object> valueMap = UtilMisc.<String, Object>toMap("amazonOrderId", amazonOrderId, "amazonOrderItemCode", amazonOrderItemCode, "shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId, "shipmentPackageSeqId", shipmentPackageSeqId, "itemIssuanceId", itemIssuanceId);
                        GenericValue amazonOrderItemFulfillment = delegator.findByPrimaryKey("AmazonOrderItemFulfillment", valueMap);
                        if (UtilValidate.isNotEmpty(amazonOrderItemFulfillment)) {
                            continue;
                        }

                        if (UtilValidate.isEmpty(carrierPartyId) || UtilValidate.isEmpty(shipmentMethodTypeId)) {

                            // Try to get carrier info from the first ship group of the order item
                            GenericValue orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderHeaderItemAndShipGroup", UtilMisc.toMap("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")), Arrays.asList("shipGroupSeqId")));
                            if (UtilValidate.isEmpty(carrierPartyId)) {
                                carrierPartyId = orderItemShipGroup.getString("carrierPartyId");
                            }
                            if (UtilValidate.isEmpty(shipmentMethodTypeId)) {
                                shipmentMethodTypeId = orderItemShipGroup.getString("shipmentMethodTypeId");
                            }
                        }

                        amazonOrderItemFulfillment = delegator.makeValue("AmazonOrderItemFulfillment", valueMap);
                        amazonOrderItemFulfillment.set("quantity", quantity);
                        amazonOrderItemFulfillment.set("trackingIdNumber", UtilValidate.isNotEmpty(sprsTrackingIdNumber) ? sprsTrackingIdNumber : srsTrackingIdNumber);
                        amazonOrderItemFulfillment.set("shipmentRouteSegmentId", shipmentRouteSegmentId);
                        amazonOrderItemFulfillment.set("carrierPartyId", carrierPartyId);
                        amazonOrderItemFulfillment.set("shipmentMethodTypeId", shipmentMethodTypeId);
                        amazonOrderItemFulfillment.set("fulfillmentDate", UtilDateTime.nowTimestamp());
                        amazonOrderItemFulfillment.set("ackStatusId", AmazonConstants.statusOrderShipNotAcked);
                        amazonOrderItemFulfillment.set("ackFailures", 0);
                        toStore.add(amazonOrderItemFulfillment);
                    }
                }
            }

            delegator.storeAll(toStore);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Posts acknowledgement to Amazon of fulfilled order items.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> acknowledgeFulfilledOrderItems(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            // Get a list of all AmazonOrderItemFulfillments which haven't been acknowledged or for which acknowledgement failed last time
            List<GenericValue> amazonOrderItemFulfillments = delegator.findByCondition("AmazonOrderItemFulfillment", EntityCondition.makeCondition("ackStatusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusOrderShipNotAcked, AmazonConstants.statusOrderShipAcknowledgedError)), null, null);
            if (UtilValidate.isEmpty(amazonOrderItemFulfillments)) {
                String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckOrderItemsNoItems", locale);
                Debug.logInfo(message, MODULE);
                return ServiceUtil.returnSuccess(message);
            }
            List<String> amazonOrderItemCodes = EntityUtil.getFieldListFromEntityList(amazonOrderItemFulfillments, "amazonOrderItemCode", true);

            // Construct the fulfillment document
            Document doc = AmazonConstants.soapClient.createDocumentHeader("OrderFulfillment");
            Element root = doc.getDocumentElement();
            for (int messageId = 1; messageId <= amazonOrderItemFulfillments.size(); messageId++) {
                GenericValue amazonOrderItemFulfillment = amazonOrderItemFulfillments.get(messageId - 1);
                amazonOrderItemFulfillment.set("acknowledgeMessageId", "" + messageId);

                Element message = doc.createElement("Message");
                root.appendChild(message);
                UtilXml.addChildElementValue(message, "MessageID", "" + messageId, doc);

                Element orderFulfillment = doc.createElement("OrderFulfillment");
                message.appendChild(orderFulfillment);
                UtilXml.addChildElementValue(orderFulfillment, "AmazonOrderID", amazonOrderItemFulfillment.getString("amazonOrderId"), doc);
                UtilXml.addChildElementValue(orderFulfillment, "MerchantFulfillmentID", amazonOrderItemFulfillment.getString("shipmentId"), doc);
                UtilXml.addChildElementValue(orderFulfillment, "FulfillmentDate", AmazonUtil.convertTimestampToXSDate(amazonOrderItemFulfillment.getTimestamp("fulfillmentDate")), doc);

                Element fulfillmentData = doc.createElement("FulfillmentData");
                orderFulfillment.appendChild(fulfillmentData);

                // Amazon recognizes only three carriers with codes, all others have to use the CarrierName
                String carrierPartyId = amazonOrderItemFulfillment.getString("carrierPartyId");
                String carrierCode = null;
                String carrierName = null;
                if (AmazonConstants.partyIdFedex.equals(carrierPartyId) || AmazonConstants.partyIdUPS.equals(carrierPartyId) || AmazonConstants.partyIdUSPS.equals(carrierPartyId)) {
                    carrierCode = AmazonConstants.carrierPartyIdToCode.get(carrierPartyId);
                } else {
                    carrierName = PartyHelper.getPartyName(delegator, carrierPartyId, false);
                }
                if (UtilValidate.isNotEmpty(carrierCode)) {
                    UtilXml.addChildElementValue(fulfillmentData, "CarrierCode", carrierCode, doc);
                } else {
                    UtilXml.addChildElementValue(fulfillmentData, "CarrierName", carrierName, doc);
                }

                UtilXml.addChildElementValue(fulfillmentData, "ShippingMethod", amazonOrderItemFulfillment.getRelatedOne("ShipmentMethodType").getString("description"), doc);
                UtilXml.addChildElementValue(fulfillmentData, "ShipperTrackingNumber", amazonOrderItemFulfillment.getString("trackingIdNumber"), doc);

                Element item = doc.createElement("Item");
                orderFulfillment.appendChild(item);
                UtilXml.addChildElementValue(item, "AmazonOrderItemCode", amazonOrderItemFulfillment.getString("amazonOrderItemCode"), doc);
                String merchantFulfillmentItemID = amazonOrderItemFulfillment.getString("shipmentId") + amazonOrderItemFulfillment.getString("shipmentItemSeqId");
                UtilXml.addChildElementValue(item, "MerchantFulfillmentItemID", merchantFulfillmentItemID, doc);
                UtilXml.addChildElementValue(item, "Quantity", "" + amazonOrderItemFulfillment.getDouble("quantity").intValue(), doc);
            }

            // Attempt to acknowledge the orders
            boolean success = true;
            String ackErrorMessage = null;
            long processingDocumentId = -1;
            try {
                processingDocumentId = AmazonConstants.soapClient.acknowledgeOrderItemFulfillment(UtilXml.writeXmlDocument(doc));
            } catch (RemoteException e) {
                success = false;
                ackErrorMessage = e.getMessage();
                String errorLog = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_AckOrderItemsError", UtilMisc.toMap("amazonOrderItemCodes", amazonOrderItemCodes, "errorMessage", ackErrorMessage), locale);
                Debug.logError(errorLog, MODULE);
            }

            // Update the records in the database with the result
            for (GenericValue amazonOrderItemFulfillment : amazonOrderItemFulfillments) {
                amazonOrderItemFulfillment.set("ackStatusId", success ? AmazonConstants.statusOrderShipAckSent : AmazonConstants.statusOrderShipAcknowledgedError);
                amazonOrderItemFulfillment.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
                amazonOrderItemFulfillment.set("acknowledgeErrorMessage", success ? null : ackErrorMessage);
                amazonOrderItemFulfillment.set("processingDocumentId", processingDocumentId);
                if (!success) {
                    Map<String, String> errorMap = UtilMisc.toMap("amazonOrderId", amazonOrderItemFulfillment.getString("amazonOrderId"), "errorMessage", ackErrorMessage);
                    if (AmazonConstants.sendErrorEmails) {
                        AmazonUtil.sendErrorEmail(dispatcher, userLogin, errorMap, UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_ErrorEmailSubject_AckOrderItemFulfill", errorMap, AmazonConstants.errorEmailLocale), AmazonConstants.errorEmailScreenUriOrders);
                    }
                }
            }
            delegator.storeAll(amazonOrderItemFulfillments);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Checks the processing status of any outstanding posted documents attached to Amazon-related objects, and if the processing report is
     *  ready, downloads and parses it in order to update the objects' acknowledgement statuses.
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service context <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> checkAcknowledgementStatuses(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            // Get a list of all Amazon-related objects which need their acknowledgment checked. Add values from other entities to the valuesToCheckList as necessary.
            List<GenericValue> valuesToCheck = new ArrayList<GenericValue>();
            valuesToCheck.addAll(delegator.findByCondition("AmazonOrder", EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusOrderImported, AmazonConstants.statusOrderImportedError, AmazonConstants.statusOrderCancelled)),
                        EntityCondition.makeCondition("ackStatusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusOrderAckSent, AmazonConstants.statusOrderAckFailureSent))
                    ), null, null));
            valuesToCheck.addAll(delegator.findByAnd("AmazonOrderItemFulfillment", UtilMisc.toMap("ackStatusId", AmazonConstants.statusOrderShipAckSent)));
            valuesToCheck.addAll(delegator.findByCondition("AmazonProduct", EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("statusId", EntityOperator.IN, Arrays.asList(AmazonConstants.statusProductPosted, AmazonConstants.statusProductDeleted)),
                        EntityCondition.makeCondition("ackStatusId", EntityOperator.EQUALS, AmazonConstants.statusProductNotAcked)
                    ), null, Arrays.asList("productId")));
            valuesToCheck.addAll(delegator.findByAnd("AmazonProductPrice", UtilMisc.toMap("statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductNotAcked), Arrays.asList("productId")));
            valuesToCheck.addAll(delegator.findByAnd("AmazonProductImageAndAck", UtilMisc.toMap("statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductNotAcked), Arrays.asList("productId")));
            valuesToCheck.addAll(delegator.findByAnd("AmazonProductInventory", UtilMisc.toMap("statusId", AmazonConstants.statusProductPosted, "ackStatusId", AmazonConstants.statusProductNotAcked), Arrays.asList("productId")));

            if (UtilValidate.isEmpty(valuesToCheck)) {
                String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_CheckAcksNoValues", locale);
                Debug.logInfo(message, MODULE);
                return ServiceUtil.returnSuccess(message);
            }

            // Get a list of unique documentIds
            List<Long> documentIds = EntityUtil.getFieldListFromEntityList(valuesToCheck, "processingDocumentId", true);

            for (Long documentId : documentIds) {
                String processingReportXml = AmazonConstants.soapClient.getProcessingReportById(documentId.longValue());

                // If the processing report isn't done yet, skip it
                if (UtilValidate.isEmpty(processingReportXml)) {
                    String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_CheckAcksDocNotReady", UtilMisc.toMap("documentId", documentId), locale);
                    Debug.logInfo(message, MODULE);
                    continue;
                }

                Document procReport = null;
                try {
                    procReport = UtilXml.readXmlDocument(processingReportXml);
                    Debug.logVerbose(processingReportXml, MODULE);
                } catch (Exception e) {
                    String message = UtilProperties.getMessage(AmazonConstants.errorResource, "AmazonError_CheckAcksDocParseError", UtilMisc.toMap("documentId", documentId, "errorMessage", e.getMessage()), locale);
                    Debug.logError(message, MODULE);
                    continue;
                }

                List<? extends Element> results = UtilXml.childElementList(UtilXml.firstChildElement(UtilXml.firstChildElement(procReport.getDocumentElement(), "Message"), "ProcessingReport"), "Result");
                List<GenericValue> documentValues = EntityUtil.filterByAnd(valuesToCheck, UtilMisc.toMap("processingDocumentId", documentId));

                // Aggregate the error message descriptions by messageId
                Map<String, String> errorMessages = new HashMap<String, String>();
                for (Element result : results) {
                    String resultCode = UtilXml.childElementValue(result, "ResultCode");
                    if (!AmazonConstants.procReportResultCodeError.equalsIgnoreCase(resultCode)) {
                        continue;
                    }
                    String messageId = UtilXml.childElementValue(result, "MessageID");
                    String resultDescription = UtilXml.childElementValue(result, "ResultDescription");
                    errorMessages.put(messageId, errorMessages.containsKey(messageId) ? errorMessages.get(messageId) + System.getProperty("line.separator") + resultDescription : resultDescription);
                }

                // We can rely on the fact that a given processing document will be composed of only one sort of documentValue
                GenericValue checkDocumentValue = documentValues.get(0);
                String successStatusId = null;
                String errorStatusId = null;
                String successAckStatusId = null;
                String errorAckStatusId = null;
                String failureCountField = null;
                String errorEmailScreenUri = null;
                String errorEmailSubjectLabel = null;
                String feedType = null;
                if ("AmazonOrder".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    if (AmazonConstants.statusOrderAckSent.equalsIgnoreCase(checkDocumentValue.getString("ackStatusId"))) {
                        successAckStatusId = AmazonConstants.statusOrderSuccessAcknowledged;
                        errorAckStatusId = AmazonConstants.statusOrderSuccessAcknowledgementError;
                    } else if (AmazonConstants.statusOrderAckFailureSent.equalsIgnoreCase(checkDocumentValue.getString("ackStatusId"))) {
                        successAckStatusId = AmazonConstants.statusOrderFailureAcknowledged;
                        errorAckStatusId = AmazonConstants.statusOrderFailureAcknowledgementError;
                    }
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriOrderAckValidate;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckOrderValidate";
                } else if ("AmazonOrderItemFulfillment".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    successAckStatusId = AmazonConstants.statusOrderShipAcknowledged;
                    errorAckStatusId = AmazonConstants.statusOrderShipAcknowledgedError;
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriOrderItemFulfillValidate;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckOrderItemFulfillValidate";
                    failureCountField = "ackFailures";
                } else if ("AmazonProduct".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    if (AmazonConstants.statusProductPosted.equalsIgnoreCase(checkDocumentValue.getString("statusId"))) {
                        errorStatusId = AmazonConstants.statusProductError;
                    } else if (AmazonConstants.statusProductDeleted.equalsIgnoreCase(checkDocumentValue.getString("statusId"))) {
                        errorStatusId = AmazonConstants.statusProductDeleteError;
                    }
                    successAckStatusId = AmazonConstants.statusProductAckRecv;
                    errorAckStatusId = AmazonConstants.statusProductAckError;
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriProducts;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckProduct";
                    feedType = AmazonConstants.messageTypeProduct;
                    failureCountField = "postFailures";
                } else if ("AmazonProductPrice".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    errorStatusId = AmazonConstants.statusProductError;
                    successAckStatusId = AmazonConstants.statusProductAckRecv;
                    errorAckStatusId = AmazonConstants.statusProductAckError;
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriProducts;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckPrice";
                    feedType = AmazonConstants.messageTypePrice;
                    failureCountField = "postFailures";
                } else if ("AmazonProductImageAndAck".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    errorStatusId = AmazonConstants.statusProductError;
                    successAckStatusId = AmazonConstants.statusProductAckRecv;
                    errorAckStatusId = AmazonConstants.statusProductAckError;
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriProducts;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckImage";
                    feedType = AmazonConstants.messageTypeProductImage;
                    failureCountField = "postFailures";
                } else if ("AmazonProductInventory".equalsIgnoreCase(checkDocumentValue.getEntityName())) {
                    errorStatusId = AmazonConstants.statusProductError;
                    successAckStatusId = AmazonConstants.statusProductAckRecv;
                    errorAckStatusId = AmazonConstants.statusProductAckError;
                    errorEmailScreenUri = AmazonConstants.errorEmailScreenUriProducts;
                    errorEmailSubjectLabel = "AmazonError_ErrorEmailSubject_AckInventory";
                    feedType = AmazonConstants.messageTypeInventory;
                    failureCountField = "postFailures";
                } else {
                    // Add other entities here as needed
                }

                LinkedHashMap<GenericValue, String> emailErrorMessages = new LinkedHashMap<GenericValue, String>();

                // Iterate through the values for the document
                for (GenericValue documentValue : documentValues) {

                    String errorMessage = "";

                    // AmazonProductImage is handled differently since it's split between AmazonProductImage and AmazonProductImageAck
                    if ("AmazonProductImageAndAck".equalsIgnoreCase(documentValue.getEntityName())) {

                        // Retrieve the parent AmazonProductImageAck value to establish the error condition/message and update
                        documentValue = documentValue.getRelatedOne("AmazonProductImageAck");
                        String messageId = documentValue.getString("acknowledgeMessageId");
                        if (errorMessages.containsKey(messageId)) {
                            errorMessage += errorMessages.get(messageId);
                        }

                        // Errors for messageId 0 apply to every value
                        if (errorMessages.containsKey("0")) {
                            errorMessage = errorMessages.get("0") + errorMessage;
                        }

                        boolean error = UtilValidate.isNotEmpty(errorMessage);
                        documentValue.set("ackStatusId", error ? errorAckStatusId : successAckStatusId);
                        documentValue.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
                        documentValue.set("acknowledgeErrorMessage", error ? errorMessage : null);
                        documentValue.store();

                        // Update the parent AmazonProductImage value as well
                        documentValue = documentValue.getRelatedOne("AmazonProductImage");
                        if (errorStatusId != null && error) {
                            documentValue.set("statusId", errorStatusId);
                        }
                        if (successStatusId != null && !error) {
                            documentValue.set("statusId", successStatusId);
                        }
                        if (failureCountField != null) {
                            documentValue.set("postFailures", error ? documentValue.getLong(failureCountField) + 1 : 0);
                        }
                        documentValue.store();
                        if (AmazonConstants.sendErrorEmails && error) {
                            emailErrorMessages.put(documentValue, errorMessage);
                        }

                    } else {

                        String messageId = documentValue.getString("acknowledgeMessageId");
                        if (errorMessages.containsKey(messageId)) {
                            errorMessage += errorMessages.get(messageId);
                        }
                        if (errorMessages.containsKey("0")) {
                            errorMessage = errorMessages.get("0") + errorMessage;
                        }
                        boolean error = UtilValidate.isNotEmpty(errorMessage);
                        if (errorStatusId != null && error) {
                            documentValue.set("statusId", errorStatusId);
                        }
                        if (successStatusId != null && !error) {
                            documentValue.set("statusId", successStatusId);
                        }
                        if (failureCountField != null) {
                            // some entities have an ackFailures field, some a postFailures field, so check the model entity
                            ModelEntity modelEntity = delegator.getModelEntity(documentValue.getEntityName());
                            String fieldName = "postFailures";
                            if ((modelEntity != null) && (modelEntity.getField("ackFailures") != null)) {
                                fieldName = "ackFailures";
                            }
                            documentValue.set(fieldName, error ? documentValue.getLong(failureCountField) + 1 : 0);
                        }
                        documentValue.set("ackStatusId", error ? errorAckStatusId : successAckStatusId);
                        documentValue.set("acknowledgeTimestamp", UtilDateTime.nowTimestamp());
                        documentValue.set("acknowledgeErrorMessage", error ? errorMessage : null);
                        documentValue.store();
                        if (AmazonConstants.sendErrorEmails && error) {
                            emailErrorMessages.put(documentValue, errorMessage);
                        }
                    }

                }

                // Track the most recent successful download of a processing document (note that we don't care whether the contents of the document report success, we
                //  only care that the document itself was retrieved successfully)
                if (UtilValidate.isNotEmpty(feedType)) {
                    delegator.removeByAnd("AmazonProductFeedProcessing", UtilMisc.toMap("feedType", feedType));
                    delegator.createOrStore(delegator.makeValue("AmazonProductFeedProcessing", UtilMisc.toMap("processingDocumentId", documentId, "feedType", feedType, "acknowledgeTimestamp", UtilDateTime.nowTimestamp())));
                }

                if (AmazonConstants.sendErrorEmails && UtilValidate.isNotEmpty(emailErrorMessages)) {
                    AmazonUtil.sendBulkErrorEmail(dispatcher, userLogin, emailErrorMessages, UtilProperties.getMessage(AmazonConstants.errorResource, errorEmailSubjectLabel, AmazonConstants.errorEmailLocale), errorEmailScreenUri);
                }

            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (SOAPException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

}
