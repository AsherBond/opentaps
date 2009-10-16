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

package org.opentaps.amazon.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.attachments.OctetStream;
import org.apache.axis.holders.OctetStreamHolder;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.opentaps.amazon.AmazonConstants;
import org.opentaps.amazon.soap.axis.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.activation.DataHandler;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPException;

/**
 * Soap client for Amazon.com web services, based on Apache Axis-generated code
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a> 
 * @version    $Rev: 10645 $
 */
public class AmazonSoapClient {

    public static String module = AmazonSoapClient.class.getName();

    private static Merchant merchant = new Merchant(AmazonConstants.merchantIdentifier, AmazonConstants.merchantName);
    private static MerchantInterface_BindingStub merchantInterface = null; 

    public AmazonSoapClient(String userName, String password) {
        try {
            merchantInterface = (MerchantInterface_BindingStub) new MerchantInterfaceMimeLocator().getMerchantInterface(new URL(AmazonConstants.url));
            merchantInterface.setUsername(userName);
            merchantInterface.setPassword(password);
        } catch (ServiceException e) {
            Debug.logError(e, module);
        } catch (MalformedURLException e) {
            Debug.logError(e, module);
        }
    }

    /**
     * Gets info for all pending order documents from Amazon
     *  
     * @return List of MerchantDocumentInfo
     * @throws RemoteException
     */
    public List<MerchantDocumentInfo> getPendingOrderDocuments() throws RemoteException {
        List<MerchantDocumentInfo> documents = new ArrayList<MerchantDocumentInfo>();
        MerchantDocumentInfo documentInfo[] = merchantInterface.getAllPendingDocumentInfo(merchant, "_GET_ORDERS_DATA_");
        for (MerchantDocumentInfo docInfo : documentInfo) {
            correctDocInfoGeneratedDateTime(docInfo);
            documents.add(docInfo);
        }
        return documents;
    }

    /**
     * Gets the XML payload of a document from Amazon
     * 
     * @param documentId
     * @return Document XML
     * @throws SOAPException
     * @throws IOException
     */
    public String getDocumentById(String documentId) throws SOAPException, IOException {
        merchantInterface.getDocument(merchant, documentId, new OctetStreamHolder());
        Object[] attachments = merchantInterface.getAttachments();
        if (attachments.length == 0) throw new IOException("No attachments found");
        AttachmentPart attachment = (AttachmentPart)attachments[0];
        DataHandler handler = attachment.getDataHandler();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        handler.writeTo(baos);
        return baos.toString();
    }

    /**
     * Acknowledges download of a list of order document IDs
     * 
     * @param documentIds
     * @throws RemoteException
     */
    public Map<String, String> acknowledgeDocumentDownload(List<String> documentIds) throws RemoteException {
        DocumentDownloadAckStatus[] ackStatuses = merchantInterface.postDocumentDownloadAck(merchant, documentIds.toArray(new String[documentIds.size()]));
        Map<String, String> ackResults = new HashMap();
        for (DocumentDownloadAckStatus ackStatus : ackStatuses) {
            ackResults.put(ackStatus.getDocumentID(), ackStatus.getDocumentDownloadAckProcessingStatus());
        }
        return ackResults;
    }

    /**
     * Posts an order acknowledgement document to Amazon
     * 
     * @param orderAckDoc
     * @throws RemoteException
     */
    public long acknowledgeOrderDownload(String orderAckDoc) throws RemoteException {
        OctetStream os = new OctetStream(orderAckDoc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, "_POST_ORDER_ACKNOWLEDGEMENT_DATA_", os);
        return response.getDocumentTransactionID();
}

    /**
     * Posts an fulfillment document to Amazon
     * 
     * @param fulfillmentDoc
     * @throws RemoteException
     */
    public long acknowledgeOrderItemFulfillment(String fulfillmentDoc) throws RemoteException {
        OctetStream os = new OctetStream(fulfillmentDoc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, "_POST_ORDER_FULFILLMENT_DATA_", os);
        return response.getDocumentTransactionID();
    }

    /**
     * Returns a processing report, if the processing report is finished, null if not
     * 
     * @param documentId
     * @return
     * @throws SOAPException
     * @throws IOException
     */
    public String getProcessingReportById(long documentId) throws SOAPException, IOException {
        DocumentProcessingInfo procInfo = merchantInterface.getDocumentProcessingStatus(merchant, documentId);
        if (! AmazonConstants.docProcessingDoneResult.equals(procInfo.getDocumentProcessingStatus())) return null;
        return getDocumentById(procInfo.getProcessingReport().getDocumentID());
    }

    /**
     * The document's generated time is incorrectly set by the axis-generated code, this corrects it to server time
     * 
     * @param docInfo
     */
    public void correctDocInfoGeneratedDateTime(MerchantDocumentInfo docInfo) {
        Calendar gmtCalendar = docInfo.getGeneratedDateTime();
        Calendar corrected = Calendar.getInstance();
        corrected.setTimeInMillis(gmtCalendar.getTimeInMillis() + AmazonConstants.amazonTimeZone.getRawOffset()  + AmazonConstants.amazonTimeZone.getDSTSavings());
        docInfo.setGeneratedDateTime(corrected);
    }

    /**
     * Creates an XML document for posting to Amazon
     * 
     * @param messageType Value for the MessageType element
     * @return
     */
    public Document createDocumentHeader(String messageType) {
        Document doc = UtilXml.makeEmptyXmlDocument("AmazonEnvelope");
        Element root = doc.getDocumentElement();
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xsi:noNamespaceSchemaLocation", "amzn-envelope.xsd");
        Element header = doc.createElement("Header");
        root.appendChild(header);
        UtilXml.addChildElementValue(header, "DocumentVersion", "1.01", doc);
        UtilXml.addChildElementValue(header, "MerchantIdentifier", AmazonConstants.merchantIdentifier, doc);
        UtilXml.addChildElementValue(root, "MessageType", messageType, doc);
        return doc;
    }
    
    /**
     * Posts a document
     * 
     * @param productDoc
     * @throws RemoteException
     */
    public long postDocument(String productDoc, String documentType) throws RemoteException {
        OctetStream os = new OctetStream(productDoc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, documentType, os);
        return response.getDocumentTransactionID();
    }

    /**
     * Posts an product document
     * 
     * @param productDoc
     * @throws RemoteException
     */
    public long postProducts(String productDoc) throws RemoteException {
        return postDocument(productDoc, "_POST_PRODUCT_DATA_");
    }

    /**
     * Posts product prices
     * 
     * @param productPriceDoc
     * @throws RemoteException
     */
    public long postProductPrices(String productPriceDoc) throws RemoteException {
        return postDocument(productPriceDoc, "_POST_PRODUCT_PRICING_DATA_");
    }

    /**
     * Posts product images
     * 
     * @param productImageDoc
     * @throws RemoteException
     */
    public long postProductImages(String productImageDoc) throws RemoteException {
        return postDocument(productImageDoc, "_POST_PRODUCT_IMAGE_DATA_");
    }

    /**
     * Posts product inventory
     * 
     * @param productInventoryDoc
     * @throws RemoteException
     */
    public long postProductInventory(String productInventoryDoc) throws RemoteException {
        return postDocument(productInventoryDoc, "_POST_INVENTORY_AVAILABILITY_DATA_");
    }
}
