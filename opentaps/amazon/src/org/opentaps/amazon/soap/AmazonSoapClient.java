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

package org.opentaps.amazon.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import javax.activation.DataHandler;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPException;

import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.attachments.OctetStream;
import org.apache.axis.holders.OctetStreamHolder;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.opentaps.amazon.AmazonConstants;
import org.opentaps.amazon.soap.axis.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Soap client for Amazon.com web services, based on Apache Axis-generated code.
 */
public class AmazonSoapClient {

    private static final String MODULE = AmazonSoapClient.class.getName();

    private static Merchant merchant = new Merchant(AmazonConstants.merchantIdentifier, AmazonConstants.merchantName);
    private static MerchantInterface_BindingStub merchantInterface = null;

    /**
     * Creates a new <code>AmazonSoapClient</code> instance.
     *
     * @param userName a <code>String</code> value
     * @param password a <code>String</code> value
     */
    public AmazonSoapClient(String userName, String password) {
        try {
            merchantInterface = (MerchantInterface_BindingStub) new MerchantInterfaceMimeLocator().getMerchantInterface(new URL(AmazonConstants.url));
            merchantInterface.setUsername(userName);
            merchantInterface.setPassword(password);
        } catch (ServiceException e) {
            Debug.logError(e, MODULE);
        } catch (MalformedURLException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * Gets info for all pending order documents from Amazon.
     *
     * @return List of MerchantDocumentInfo
     * @throws RemoteException if an error occurs
     */
    public List<MerchantDocumentInfo> getPendingOrderDocuments() throws RemoteException {
        List<MerchantDocumentInfo> documents = new ArrayList<MerchantDocumentInfo>();
        MerchantDocumentInfo[] documentInfo = merchantInterface.getAllPendingDocumentInfo(merchant, "_GET_ORDERS_DATA_");
        for (MerchantDocumentInfo docInfo : documentInfo) {
            correctDocInfoGeneratedDateTime(docInfo);
            documents.add(docInfo);
        }
        return documents;
    }

    /**
     * Gets the XML payload of a document from Amazon.
     *
     * @param documentId the document ID
     * @return Document XML
     * @throws SOAPException if an error occurs
     * @throws IOException if an error occurs
     */
    public String getDocumentById(String documentId) throws SOAPException, IOException {
        merchantInterface.getDocument(merchant, documentId, new OctetStreamHolder());
        Object[] attachments = merchantInterface.getAttachments();
        if (attachments.length == 0) {
            throw new IOException("No attachments found");
        }
        AttachmentPart attachment = (AttachmentPart) attachments[0];
        DataHandler handler = attachment.getDataHandler();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        handler.writeTo(baos);
        return baos.toString();
    }

    /**
     * Acknowledges download of a list of order document IDs.
     *
     * @param documentIds the document IDs
     * @return a <code>Map</code> of document ID -> status
     * @throws RemoteException if an error occurs
     */
    public Map<String, String> acknowledgeDocumentDownload(List<String> documentIds) throws RemoteException {
        DocumentDownloadAckStatus[] ackStatuses = merchantInterface.postDocumentDownloadAck(merchant, documentIds.toArray(new String[documentIds.size()]));
        Map<String, String> ackResults = new HashMap<String, String>();
        for (DocumentDownloadAckStatus ackStatus : ackStatuses) {
            ackResults.put(ackStatus.getDocumentID(), ackStatus.getDocumentDownloadAckProcessingStatus());
        }
        return ackResults;
    }

    /**
     * Posts an order acknowledgment document to Amazon.
     *
     * @param orderAckDoc an order acknowledgement document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long acknowledgeOrderDownload(String orderAckDoc) throws RemoteException {
        OctetStream os = new OctetStream(orderAckDoc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, "_POST_ORDER_ACKNOWLEDGEMENT_DATA_", os);
        return response.getDocumentTransactionID();
}

    /**
     * Posts a fulfillment document to Amazon.
     *
     * @param fulfillmentDoc a fulfillment document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long acknowledgeOrderItemFulfillment(String fulfillmentDoc) throws RemoteException {
        OctetStream os = new OctetStream(fulfillmentDoc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, "_POST_ORDER_FULFILLMENT_DATA_", os);
        return response.getDocumentTransactionID();
    }

    /**
     * Returns a processing report, if the processing report is finished, null if not.
     *
     * @param documentId the processing report document ID
     * @return the processing report document ID if the processing report is finished, null if not.
     * @throws SOAPException if an error occurs
     * @throws IOException if an error occurs
     */
    public String getProcessingReportById(long documentId) throws SOAPException, IOException {
        DocumentProcessingInfo procInfo = merchantInterface.getDocumentProcessingStatus(merchant, documentId);
        if (!AmazonConstants.docProcessingDoneResult.equals(procInfo.getDocumentProcessingStatus())) {
            return null;
        }
        return getDocumentById(procInfo.getProcessingReport().getDocumentID());
    }

    /**
     * The document's generated time is incorrectly set by the axis-generated code, this corrects it to server time.
     *
     * @param docInfo a <code>MerchantDocumentInfo</code> value
     */
    public void correctDocInfoGeneratedDateTime(MerchantDocumentInfo docInfo) {
        Calendar gmtCalendar = docInfo.getGeneratedDateTime();
        Calendar corrected = Calendar.getInstance();
        corrected.setTimeInMillis(gmtCalendar.getTimeInMillis() + AmazonConstants.amazonTimeZone.getRawOffset()  + AmazonConstants.amazonTimeZone.getDSTSavings());
        docInfo.setGeneratedDateTime(corrected);
    }

    /**
     * Creates an XML document for posting to Amazon.
     *
     * @param messageType Value for the MessageType element
     * @return the <code>Document</code> instance
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
     * Posts a document.
     *
     * @param doc a document
     * @param documentType the document type
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long postDocument(String doc, String documentType) throws RemoteException {
        OctetStream os = new OctetStream(doc.getBytes());
        DocumentSubmissionResponse response = merchantInterface.postDocument(merchant, documentType, os);
        return response.getDocumentTransactionID();
    }

    /**
     * Posts a product document.
     *
     * @param productDoc a product document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long postProducts(String productDoc) throws RemoteException {
        return postDocument(productDoc, "_POST_PRODUCT_DATA_");
    }

    /**
     * Posts product prices.
     *
     * @param productPriceDoc a product price document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long postProductPrices(String productPriceDoc) throws RemoteException {
        return postDocument(productPriceDoc, "_POST_PRODUCT_PRICING_DATA_");
    }

    /**
     * Posts product images.
     *
     * @param productImageDoc a product image document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long postProductImages(String productImageDoc) throws RemoteException {
        return postDocument(productImageDoc, "_POST_PRODUCT_IMAGE_DATA_");
    }

    /**
     * Posts product inventory.
     *
     * @param productInventoryDoc a product inventory document
     * @return the document transaction ID
     * @throws RemoteException if an error occurs
     */
    public long postProductInventory(String productInventoryDoc) throws RemoteException {
        return postDocument(productInventoryDoc, "_POST_INVENTORY_AVAILABILITY_DATA_");
    }
}
