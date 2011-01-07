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
/* Copyright (c) Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.opensourcestrategies.crmsfa.activities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.mail.MimeMessageWrapper;
import org.opentaps.common.domain.party.PartyRepository;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.Activity;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.domain.activities.ActivityRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Contact;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

import com.opensourcestrategies.crmsfa.cases.UtilCase;
import com.opensourcestrategies.crmsfa.opportunities.UtilOpportunity;
import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Activities services. The service documentation is in services_activities.xml.
 */
public final class ActivitiesServices {

    private ActivitiesServices() { }

    private static final String MODULE = ActivitiesServices.class.getName();
    public static final String resource = "CRMSFAUiLabels";
    public static final String notificationResource = "notification";
    public static final String crmsfaProperties = "crmsfa";

    private static final int COUNT = -1;

    public static Map<String, Object> sendActivityEmail(DispatchContext dctx, Map<String, Object> context) {
        return sendOrSaveEmailHelper(dctx, context, true, "CrmErrorSendEmailFail");
    }

    public static Map<String, Object> saveActivityEmail(DispatchContext dctx, Map<String, Object> context) {
        return sendOrSaveEmailHelper(dctx, context, false, "CrmErrorSaveEmailFail");
    }

    /**
     * Saving and sending are very complex services that are nearly identical in most ways.
     * There are four things that break the identity in minor ways that can be handled with
     * booleans. The four things are: Send new email, send existing email, save new email,
     * and send existing email. Instead of creating four separate methods several hundred
     * lines each, we do everything here.
     */
    @SuppressWarnings("unchecked")
	private static Map<String, Object> sendOrSaveEmailHelper(DispatchContext dctx, Map<String, Object> context, boolean sending, String errorLabel) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // use the toEmail and internalPartyId to find the contactMechIdTo
        String toEmail = (String) context.get("toEmail");

        // if the email exists already, these will be set
        String communicationEventId = (String) context.get("communicationEventId");
        String workEffortId = (String) context.get("workEffortId");
        boolean existing = ((communicationEventId == null) || communicationEventId.equals("") ? false : true);
        String origCommEventId = (String) context.get("origCommEventId");

        try {

            String serviceName = (existing ? "updateCommunicationEvent" : "createCommunicationEvent");
            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(context, "IN");

            // validate the associations
            Map<String, Object> serviceResults = validateWorkEffortAssociations(dctx, context);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
            }

            // Retrieve, validate and parse the To addresses (assumed to be comma-delimited)
            String validToAddresses = null;
            Set<String> toAddresses = UtilCommon.getValidEmailAddressesFromString(toEmail);
            if (UtilValidate.isNotEmpty(toAddresses)) {
                validToAddresses = StringUtil.join(UtilMisc.toList(toAddresses), ",");
                input.put("toString", validToAddresses);
            } else {
                if (UtilValidate.isNotEmpty(toEmail)) {
                    Debug.logError("No valid email addresses could be found from: [" + toEmail + "]", MODULE);
                }
            }

            // Search for contactMechIdTo using the passed in To email addresses - use the first found
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("infoString", EntityOperator.IN, UtilMisc.toList(toAddresses)));
            GenericValue partyContactMechTo = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByCondition("PartyAndContactMech", conditions, null, null)));
            if (UtilValidate.isNotEmpty(partyContactMechTo)) {
                input.put("contactMechIdTo", partyContactMechTo.getString("contactMechId"));
                input.put("partyIdTo", partyContactMechTo.getString("partyId"));
                input.put("roleTypeIdTo", partyContactMechTo.getString("roleTypeId"));
            }

            /*
             * We're done with validation, now we begin the complex task of creating comm events, workefforts, and updating them
             * so that the email is sent or saved properly. The most verbose way of doing this requires four different methods with
             * a lot of redundant code. But here we use two booleans "existing" and "sending" to decide what to do. This is much
             * more compact but can be prone to subtle state errors. On the other hand, changes to the form input require only one
             * change here instead of four changes.
             */

            // create a PENDING comm event or update the existing one; set the comm event data with the form input
            if (existing) {
                input.put("communicationEventId", communicationEventId);
            } else {
                input.put("entryDate", UtilDateTime.nowTimestamp());
            }
            input.put("contactMechTypeId", "EMAIL_ADDRESS");
            input.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
            input.put("statusId", "COM_PENDING");
            input.put("partyIdFrom", userLogin.getString("partyId"));
            input.put("roleTypeIdFrom", PartyHelper.getFirstValidRoleTypeId(userLogin.getString("partyId"), PartyHelper.TEAM_MEMBER_ROLES, delegator));
            // check if an original CommunicationEvent Id was given
            if (UtilValidate.isNotEmpty(origCommEventId)) {
                input.put("origCommEventId", origCommEventId);
            }

            // Retrieve, validate and parse the CC and BCC addresses (assumed to be comma-delimited)
            String validCCAddresses = null;
            Set<String> ccAddresses = UtilCommon.getValidEmailAddressesFromString((String) context.get("ccEmail"));
            if (UtilValidate.isNotEmpty(ccAddresses)) {
                validCCAddresses = StringUtil.join(UtilMisc.toList(ccAddresses), ",");
                input.put("ccString", validCCAddresses);
            }
            String validBCCAddresses = null;
            Set<String> bccAddresses = UtilCommon.getValidEmailAddressesFromString((String) context.get("bccEmail"));
            if (UtilValidate.isNotEmpty(bccAddresses)) {
                validBCCAddresses = StringUtil.join(UtilMisc.toList(bccAddresses), ",");
                input.put("bccString", validBCCAddresses);
            }

            serviceResults = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
            }

            // get the communication event id if the comm event was created
            if (!existing) {
                communicationEventId = (String) serviceResults.get("communicationEventId");
            }

            // Create the content etc. for each email attachment
            // multiPartMap is populated by the ServiceEventHandler with (we hope) the following keys for each uploaded file: uploadedFile_#; _uploadedFile_0_contentType; _uploadedFile_0_fileName
            Map<String, Object> multiPartMap = (Map<String, Object>) context.get("multiPartMap");
            int fileCounter = 1;
            if (UtilValidate.isNotEmpty(multiPartMap)) {
                Iterator<String> mpit = multiPartMap.keySet().iterator();
                while (mpit.hasNext()) {
                    String key = mpit.next();

                    // Since the ServiceEventHandler adds all form inputs to the map, just deal with the ones matching the correct input name (eg. 'uploadedFile_0', 'uploadedFile_1', etc)
                    if (!key.startsWith("uploadedFile_")) {
                        continue;
                    }

                    // Some browsers will submit an empty string for an empty input type="file", so ignore the ones that are empty
                    if (UtilValidate.isEmpty(multiPartMap.get(key))) {
                        continue;
                    }

                    ByteBuffer uploadedFile = (ByteBuffer) multiPartMap.get(key);
                    String uploadedFileName = (String) multiPartMap.get("_" + key + "_fileName");
                    String uploadedFileContentType = (String) multiPartMap.get("_" + key + "_contentType");

                    // Check to see that we have everything
                    if (UtilValidate.isEmpty(uploadedFileName)) {
                        continue; // not really a file if there is no name
                    } else if (UtilValidate.isEmpty(uploadedFile) || UtilValidate.isEmpty(uploadedFileContentType)) {
                        return UtilMessage.createAndLogServiceError("CrmErrorSendEmailMissingFileUploadData", locale, MODULE);
                    }

                    // Populate the context for the DataResource/Content/CommEventContentAssoc creation service
                    Map<String, Object> createContentContext = new HashMap<String, Object>();
                    try {
                        createContentContext.put("userLogin", userLogin);
                        createContentContext.put("contentName", uploadedFileName);
                        createContentContext.put("uploadedFile", uploadedFile);
                        createContentContext.put("_uploadedFile_fileName", uploadedFileName);
                        createContentContext.put("_uploadedFile_contentType", uploadedFileContentType);

                        Map<String, Object> tmpResult = dispatcher.runSync("uploadFile", createContentContext);
                        if (ServiceUtil.isError(tmpResult)) {
                            return UtilMessage.createAndLogServiceError(tmpResult, "CrmErrorCreateContentFail", locale, MODULE);
                        }
                        String contentId = (String) tmpResult.get("contentId");
                        if (UtilValidate.isNotEmpty(contentId)) {
                            tmpResult = dispatcher.runSync("createCommEventContentAssoc", UtilMisc.toMap("contentId", contentId, "communicationEventId", communicationEventId,
                                    "sequenceNum", new Long(fileCounter), "userLogin", userLogin));
                            if (ServiceUtil.isError(tmpResult)) {
                                return UtilMessage.createAndLogServiceError(tmpResult, "CrmErrorCreateContentFail", locale, MODULE);
                            }
                        } else {
                            return ServiceUtil.returnError("Upload file ran successfully for [" + uploadedFileName + "] but no contentId was returned");
                        }

                    } catch (GenericServiceException e) {
                        return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
                    }
                    fileCounter++;
                }
            }

            if (sending) {

                Map<String, Object> sendMailContext = new HashMap<String, Object>();
                sendMailContext.put("subject", context.get("subject"));
                String contactMechIdFrom = (String) context.get("contactMechIdFrom");
                sendMailContext.put("sendFrom", UtilCommon.emailAndPersonalName(contactMechIdFrom, delegator));
                sendMailContext.put("partyId", context.get("partyId"));
                sendMailContext.put("contentType", context.get("contentMimeTypeId"));

                String addCaseIdToSubject = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.case.addCaseNumberToOutgoingEmails", "false");
                if ("true".equals(addCaseIdToSubject) || "Y".equals(addCaseIdToSubject)) {
                    String custRequestId = (String) context.get("custRequestId");
                    String subject = (String) context.get("subject");
                    if (UtilValidate.isNotEmpty(custRequestId)) {

                        // Insert the custRequestId into the email subject line, if it doesn't already reference it
                        List<String> custRequestIds = ActivitiesHelper.getCustRequestIdsFromString(subject, delegator);
                        if (!custRequestIds.contains(custRequestId)) {
                            String emailSubjectCaseString = ActivitiesHelper.getEmailSubjectCaseString(custRequestId);
                            String emailSubject = UtilProperties.getMessage(crmsfaProperties, "crmsfa.case.emailSubject", UtilMisc.toMap("subject", subject, "emailSubjectCaseFormat", emailSubjectCaseString), locale);

                            // Default to a basic subject (EG: "Case:10000 subject") if the configured subject line can't be found
                            if (UtilValidate.isEmpty(emailSubject)) {
                                emailSubject = "[Case:" + custRequestId + "] " + subject;
                            }
                            sendMailContext.put("subject", emailSubject);
                        }
                    }
                }

                String addOrderIdToSubject = UtilProperties.getPropertyValue(crmsfaProperties, "crmsfa.order.addOrderNumberToOutgoingEmails", "false");
                if ("true".equals(addOrderIdToSubject) || "Y".equals(addOrderIdToSubject)) {
                    String orderId = (String) context.get("orderId");
                    String subject = (String) context.get("subject");
                    if (UtilValidate.isNotEmpty(orderId)) {

                        // Insert the orderId into the email subject line, if it doesn't already reference it
                        List<String> orderIds = ActivitiesHelper.getOrderIdsFromString(subject, delegator);
                        if (!orderIds.contains(orderId)) {
                            String emailSubjectOrderString = ActivitiesHelper.getEmailSubjectOrderString(orderId);
                            String emailSubject = UtilProperties.getMessage(crmsfaProperties, "crmsfa.order.emailSubject", UtilMisc.toMap("subject", subject, "emailSubjectOrderFormat", emailSubjectOrderString), locale);

                            // Default to a basic subject (EG: "Order:10000 subject") if the configured subject line can't be found
                            if (UtilValidate.isEmpty(emailSubject)) {
                                emailSubject = "[Order:" + orderId + "] " + subject;
                            }
                            sendMailContext.put("subject", emailSubject);
                        }
                    }
                }

                // Assemble the body and the attachments into a list of body parts
                List<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
                List<GenericValue> commEventContentDataResources = delegator.findByAnd("CommEventContentDataResource", UtilMisc.toMap("communicationEventId", communicationEventId));
                commEventContentDataResources = EntityUtil.filterByDate(commEventContentDataResources);
                Iterator<GenericValue> cecait = commEventContentDataResources.iterator();
                while (cecait.hasNext()) {
                    GenericValue commEventContentDataResource = cecait.next();
                    String dataResourceId = commEventContentDataResource.getString("dataResourceId");
                    String mimeTypeId = commEventContentDataResource.getString("drMimeTypeId");
                    String fileName = commEventContentDataResource.getString("drDataResourceName");
                    Map<String, Object> attachment = UtilMisc.<String, Object>toMap("type", mimeTypeId, "filename", fileName);
                    try {
                        ByteWrapper byteWrapper = UtilCommon.getContentAsByteWrapper(delegator, dataResourceId, null, null, locale, null);
                        attachment.put("content", byteWrapper.getBytes());
                    } catch (IOException e) {
                        return UtilMessage.createAndLogServiceError("CrmErrorSendEmailUnableToGetDataResource", UtilMisc.toMap("dataResourceId", dataResourceId), locale, MODULE);
                    } catch (GeneralException e) {
                        return UtilMessage.createAndLogServiceError("CrmErrorSendEmailUnableToGetDataResource", UtilMisc.toMap("dataResourceId", dataResourceId), locale, MODULE);
                    }
                    attachments.add(attachment);
                }

                if (UtilValidate.isNotEmpty(validToAddresses)) {
                    sendMailContext.put("sendTo", validToAddresses);
                }
                if (UtilValidate.isNotEmpty(validCCAddresses)) {
                    sendMailContext.put("sendCc", validCCAddresses);
                }
                if (UtilValidate.isNotEmpty(validBCCAddresses)) {
                    sendMailContext.put("sendBcc", validBCCAddresses);
                }

                String emailServiceName = "sendMail";
                if (UtilValidate.isEmpty(attachments)) {
                    sendMailContext.put("body", context.get("content"));
                } else {
                    // Construct the list of parts so that the message body is first, just in case some email clients break
                    List<Map<String, Object>> bodyParts = UtilMisc.toList(UtilMisc.toMap("content", context.get("content"), "type", context.get("contentMimeTypeId")));
                    bodyParts.addAll(attachments);
                    sendMailContext.put("bodyParts", bodyParts);
                    emailServiceName = "sendMailMultiPart";
                }

                // Send the email synchronously
                Map<String, Object> sendMailResult = dispatcher.runSync(emailServiceName, sendMailContext);
                if (ServiceUtil.isError(sendMailResult)) {
                    return UtilMessage.createAndLogServiceError(sendMailResult, errorLabel, locale, MODULE);
                }

                // Update communication event to status COM_COMPLETE, and to update the subject if it's changed
                input = UtilMisc.toMap("communicationEventId", communicationEventId, "userLogin", userLogin);
                input.put("statusId", "COM_COMPLETE");
                input.put("datetimeEnded", UtilDateTime.nowTimestamp());
                input.put("subject", sendMailContext.get("subject"));
                serviceResults = dispatcher.runSync("updateCommunicationEvent", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }

                // now update or create a work effort to record this email as a completed task
                input = UtilMisc.toMap("workEffortTypeId", "TASK", "currentStatusId", "TASK_COMPLETED", "userLogin", userLogin);
                if (existing) {
                    input.put("workEffortId", workEffortId);
                }
                input.put("actualStartDate", context.get("datetimeStarted"));
                if (UtilValidate.isEmpty(input.get("actualStartDate"))) {
                    input.put("actualStartDate", UtilDateTime.nowTimestamp());
                }
                input.put("actualCompletionDate", UtilDateTime.nowTimestamp());
                input.put("workEffortName", context.get("subject"));
                input.put("workEffortPurposeTypeId", "WEPT_TASK_EMAIL");
                serviceName = (existing ? "updateWorkEffort" : "createWorkEffort");
                serviceResults = dispatcher.runSync(serviceName, input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }
            } else {
                // Create or update a scheduled (TASK_STARTED) TASK WorkEffort to save this email
                input = UtilMisc.toMap("workEffortTypeId", "TASK", "currentStatusId", "TASK_STARTED", "userLogin", userLogin);
                if (existing) {
                    input.put("workEffortId", workEffortId);
                }
                input.put("actualStartDate", context.get("datetimeStarted"));
                if (UtilValidate.isEmpty(input.get("actualStartDate"))) {
                    input.put("actualStartDate", UtilDateTime.nowTimestamp());
                }
                input.put("workEffortName", context.get("subject"));
                input.put("workEffortPurposeTypeId", "WEPT_TASK_EMAIL");
                serviceResults = dispatcher.runSync(existing ? "updateWorkEffort" : "createWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }
            }

            // get the work effort ID from the serviceResults if a workEffort was created (note that the last service run in this case is always createWorkEffort)
            if (!existing) {
                workEffortId = (String) serviceResults.get("workEffortId");
            }

            // create an association between the task and comm event (safe even if existing)
            input = UtilMisc.toMap("userLogin", userLogin, "communicationEventId", communicationEventId, "workEffortId", workEffortId);
            serviceResults = dispatcher.runSync("createCommunicationEventWorkEff", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
            }

            // Associate the work effort any orders
            String orderId = (String) context.get("orderId");
            if (UtilValidate.isNotEmpty(orderId)) {
                GenericValue orderHeaderWorkEffort = delegator.findByPrimaryKey("OrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId));
                if (UtilValidate.isEmpty(orderHeaderWorkEffort)) {
                    Map<String, Object> createOrderHeaderWEResult = dispatcher.runSync("createOrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId, "userLogin", userLogin));
                    if (ServiceUtil.isError(createOrderHeaderWEResult)) {
                        return createOrderHeaderWEResult;
                    }
                }
            }

            // we need to zap all associations now if there are any previously saved ones
            if (existing) {
                UtilActivity.removeAllAssociationsForWorkEffort(workEffortId, delegator);
            }

            // Create separate lists for the to, CC and BCC addresses
            List<GenericValue> partyAndContactMechsTo = findPartyAndContactMechsForEmailAddress(toAddresses, delegator);
            List<GenericValue> partyAndContactMechsCC = findPartyAndContactMechsForEmailAddress(ccAddresses, delegator);
            List<GenericValue> partyAndContactMechsBCC = findPartyAndContactMechsForEmailAddress(bccAddresses, delegator);
            associateCommunicationEventWorkEffortAndParties(partyAndContactMechsTo, communicationEventId, "EMAIL_RECIPIENT_TO", workEffortId, delegator, dispatcher, userLogin);
            associateCommunicationEventWorkEffortAndParties(partyAndContactMechsCC, communicationEventId, "EMAIL_RECIPIENT_CC", workEffortId, delegator, dispatcher, userLogin);
            associateCommunicationEventWorkEffortAndParties(partyAndContactMechsBCC, communicationEventId, "EMAIL_RECIPIENT_BCC", workEffortId, delegator, dispatcher, userLogin);

            // pass in List of all email addresses
            List<String> allEmailAddresses = new ArrayList<String>();
            allEmailAddresses.addAll(toAddresses);
            allEmailAddresses.addAll(ccAddresses);
            // don't think BCC addresses is needed: this variable is intended for creating owners for incoming emails, and those don't come with BCC

            // create the associations and finish
            serviceResults = createWorkEffortPartyAssociations(dctx, context, workEffortId, errorLabel, !existing);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("workEffortId", workEffortId);
            return results;

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, errorLabel, locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, errorLabel, locale, MODULE);
        }
    }

    /**
     * Creates a Work Effort and associates it with the Communication Event
     */
    private static String associateCommunicationEventAndWorkEffort(GenericValue communicationEvent, String initialWorkEffortStatusId, LocalDispatcher dispatcher, GenericValue userLogin, DispatchContext dctx, Map<String, Object> context)
                   throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceResults = null;
        Map<String, Object> input = null;
        String communicationEventId = communicationEvent.getString("communicationEventId");

        // Create Workeffort from CommunicationEvent with task status as scheduled (TASK_SCHEDULED) instead of complete (TASK_COMPLETED)
        //  so that it shows up in Pending Activities rather than Activities History
        input = UtilMisc.<String, Object>toMap("workEffortTypeId", "TASK", "currentStatusId", initialWorkEffortStatusId, "userLogin", userLogin);
        Timestamp actualStartDate = communicationEvent.getTimestamp("datetimeStarted");
        Timestamp actualCompletionDate = communicationEvent.getTimestamp("datetimeEnded");

        // Replace empty start/end dates on the communication event with the current time, and update it if necessary
        if (actualStartDate == null || actualCompletionDate == null) {
            actualStartDate = UtilDateTime.nowTimestamp();
            if (actualStartDate == null) {
                actualStartDate = UtilDateTime.nowTimestamp();
                communicationEvent.put("datetimeStarted", actualStartDate);
            }
            if (actualCompletionDate == null) {
                actualCompletionDate = UtilDateTime.nowTimestamp();
                communicationEvent.put("datetimeEnded", actualCompletionDate);
            }
            dispatcher.getDelegator().store(communicationEvent);
        }
        input.put("actualStartDate", actualStartDate);
        input.put("actualCompletionDate", actualCompletionDate);
        String subject = communicationEvent.getString("subject");
        if (subject != null) {
            // trim the subject, since it is usually a much longer field than work effort name
            input.put("workEffortName", subject.length() > 100 ? subject.substring(0, 100) : subject);
        }
        input.put("workEffortPurposeTypeId", "WEPT_TASK_EMAIL");
        serviceResults = dispatcher.runSync("createWorkEffort", input);
        if (ServiceUtil.isError(serviceResults)) {
            Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
            throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
        }

        // Get the workeffort ID from the serviceResults
        String workEffortId = (String) serviceResults.get("workEffortId");

        // Create an association between the task and comm event
        input = UtilMisc.toMap("userLogin", userLogin, "communicationEventId", communicationEventId, "workEffortId", workEffortId);
        serviceResults = dispatcher.runSync("createCommunicationEventWorkEff", input);
        if (ServiceUtil.isError(serviceResults)) {
            Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
            throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
        }

        // Create an association between the WorkEffort and the CommunicationEvent using the partyIdFrom field
        context.put("internalPartyId", communicationEvent.getString("partyIdFrom"));
        createWorkEffortPartyAssociations(dctx, context, workEffortId, "CrmErrorProcessIncomingEmailFail", false);

        return workEffortId;
    }

    /**
     * Creates a CommunicationEventRole and WorkeffortPartyAssignment (if the party has a CRM role) for each party in the list
     * */
    private static void associateCommunicationEventWorkEffortAndParties(List<GenericValue> partyAndContactMechs, String communicationEventId, String roleTypeId, String workEffortId, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin)
                   throws GenericEntityException, GenericServiceException {
        if (UtilValidate.isNotEmpty(partyAndContactMechs)) {

            Map<String, Object> serviceResults = null;
            Map<String, Object> input = null;

            List<String> validRoleTypeIds = new ArrayList<String>(PartyHelper.TEAM_MEMBER_ROLES);
            validRoleTypeIds.addAll(PartyHelper.CLIENT_PARTY_ROLES);

            Set<String> partyIds = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(partyAndContactMechs, "partyId", true));
            Set<String> emailAddresses = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(partyAndContactMechs, "infoString", true));      // for looking for the owner of this activity against an email

            for (String partyId : partyIds) {

                // Add a CommunicationEventRole for the party, if one doesn't already exist
                long commEventRoles = delegator.findCountByAnd("CommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId));
                if (commEventRoles == 0) {
                    serviceResults = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                    }

                    // Use the first PartyAndContactMech for that partyId in the partyAndContactMech list
                    EntityCondition filterConditions = EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId), EntityCondition.makeCondition("contactMechId", EntityOperator.NOT_EQUAL, null));
                    GenericValue partyAndContactMech = EntityUtil.getFirst(EntityUtil.filterByCondition(partyAndContactMechs, filterConditions));

                    // Create the communicationEventRole
                    serviceResults = dispatcher.runSync("createCommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId, "contactMechId", partyAndContactMech.getString("contactMechId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                    }
                }

                if (UtilValidate.isNotEmpty(workEffortId)) {

                    // Assign the party to the workeffort if they have a CRM role, and if they aren't already assigned
                    List<GenericValue> workEffortPartyAssignments = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("partyId", partyId, "workEffortId", workEffortId));
                    workEffortPartyAssignments = EntityUtil.filterByDate(workEffortPartyAssignments);

                    if (UtilValidate.isEmpty(workEffortPartyAssignments)) {
                        String crmRoleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, validRoleTypeIds, delegator);
                        if (crmRoleTypeId == null) {
                            Debug.logWarning("No valid roles found for partyId [" + partyId + "], so it will not be assigned to activity " + workEffortId, MODULE);
                        } else {
                            // if this party is an internal party (crmsfa user), the activity does not have an owner yet, and
                            // this current party is associated with any of the email addresses as "Owner of Received Emails", then
                            // the party is the owner
                            // note that this means the activity can only have one owner at a time
                            if (PartyHelper.TEAM_MEMBER_ROLES.contains(crmRoleTypeId) && (UtilValidate.isEmpty(UtilActivity.getActivityOwner(workEffortId, delegator)))) {
                                if (UtilValidate.isNotEmpty(org.opentaps.common.party.PartyHelper.getCurrentContactMechsForParty(partyId, "EMAIL_ADDRESS", "RECEIVE_EMAIL_OWNER",
                                        UtilMisc.toList(EntityCondition.makeCondition("infoString", EntityOperator.IN, emailAddresses)), delegator))) {
                                    crmRoleTypeId = "CAL_OWNER";
                                    Debug.logInfo("Will be assigning [" + partyId + "] as owner of [" + workEffortId + "]", MODULE);
                                }
                            }

                            input = UtilMisc.toMap("partyId", partyId, "workEffortId", workEffortId, "roleTypeId", crmRoleTypeId, "statusId", "PRTYASGN_ASSIGNED", "userLogin", userLogin);
                            serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                            if (ServiceUtil.isError(serviceResults)) {
                                Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Calls the storeIncomingEmail service to process incoming emails
     */
    public static Map<String, Object> processIncomingEmail(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue login = (GenericValue) context.get("userLogin");
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        Map<String, Object> serviceResults = null;
        Map<String, Object> input = null;

        try {
            if (wrapper == null) {
                Debug.logError("Null message wrapper when trying to store email", MODULE);
                return UtilMessage.createAndLogServiceError("Null message wrapper", locale, MODULE);
            }

            // Use the system userLogin for these operations
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));

            if (userLogin == null) {
                Debug.logWarning("Null userLogin in when trying to process incoming email, using system", MODULE);
                userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            }

            // Call storeIncomingEmail service
            input = UtilMisc.<String, Object>toMap("messageWrapper", wrapper, "userLogin", userLogin);
            serviceResults = dispatcher.runSync("storeIncomingEmail", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
            }

            // Get communicationEventId
            String communicationEventId = (String) serviceResults.get("communicationEventId");
            if (communicationEventId == null) {
                // the storeIncomingEmail of ofbiz09.04 will return success without a communicationEventId when the email is a duplicate
                // based on CommunicationEvent.messageId, so if that's the case we must return an error
                MimeMessage message = wrapper.getMessage();
                try {
                    return ServiceUtil.returnError("Email from [" + message.getFrom() + "] with subject [" + message.getSubject() + "] and message ID [" + message.getMessageID() + "] was not stored with a communicationEventId.  Possible cause is that this message has already been stored.");
                } catch (MessagingException ex) {
                    return ServiceUtil.returnError(ex.getMessage());
                }
            }
            // Find CommunicationEvent from its Id
            GenericValue communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));


            // Retrieve addresses from the email
            Set<String> emailAddressesFrom = new HashSet<String>();
            Set<String> emailAddressesTo = new HashSet<String>();
            Set<String> emailAddressesCC = new HashSet<String>();
            Message message = wrapper.getMessage();
            try {
                Address[] addressesFrom = message.getFrom();
                Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
                Address[] addressesCC = message.getRecipients(MimeMessage.RecipientType.CC);
                for (int x = 0; x < addressesFrom.length; x++) {
                    emailAddressesFrom.add(((InternetAddress) addressesFrom[x]).getAddress());
                }
                if (addressesTo != null) {
                    for (int x = 0; x < addressesTo.length; x++) {
                        emailAddressesTo.add(((InternetAddress) addressesTo[x]).getAddress());
                    }
                }
                if (addressesCC != null) {
                    for (int x = 0; x < addressesCC.length; x++) {
                        emailAddressesCC.add(((InternetAddress) addressesCC[x]).getAddress());
                    }
                }
            } catch (MessagingException e) {
                return UtilMessage.createAndLogServiceError(e, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
            }

            if (UtilValidate.isEmpty(emailAddressesFrom) && UtilValidate.isEmpty(emailAddressesTo) && UtilValidate.isEmpty(emailAddressesCC)) {
                return UtilMessage.createAndLogServiceError("CrmErrorProcessIncomingEmailFailNoAddresses", locale, MODULE);
            } else {
                // find parties and contact mechs matching the email addresses
                List<GenericValue> partyAndContactMechsFrom = findPartyAndContactMechsForEmailAddress(emailAddressesFrom, delegator);
                List<GenericValue> partyAndContactMechsTo = findPartyAndContactMechsForEmailAddress(emailAddressesTo, delegator);
                List<GenericValue> partyAndContactMechsCC = findPartyAndContactMechsForEmailAddress(emailAddressesCC, delegator);

                // Examine the From parties to discover if the email originated from an internal party
                Set<String> fromPartyIds = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(partyAndContactMechsFrom, "partyId", true));
                String initialWorkEffortStatusId = "TASK_STARTED";
                Iterator<String> fpiit = fromPartyIds.iterator();
                while (fpiit.hasNext()) {
                    String fromPartyId = fpiit.next();
                    if (UtilValidate.isNotEmpty(PartyHelper.getFirstValidRoleTypeId(fromPartyId, PartyHelper.TEAM_MEMBER_ROLES, delegator))) {

                        // Email tasks from internal parties should have their status set to complete immediately
                        initialWorkEffortStatusId = "TASK_COMPLETED";
                        break;
                    }
                }

                String workEffortId = associateCommunicationEventAndWorkEffort(communicationEvent, initialWorkEffortStatusId, dispatcher, userLogin, dctx, context);

                associateCommunicationEventWorkEffortAndParties(partyAndContactMechsFrom, communicationEventId, "EMAIL_SENDER", workEffortId, delegator, dispatcher, userLogin);
                associateCommunicationEventWorkEffortAndParties(partyAndContactMechsTo, communicationEventId, "EMAIL_RECIPIENT_TO", workEffortId, delegator, dispatcher, userLogin);
                associateCommunicationEventWorkEffortAndParties(partyAndContactMechsCC, communicationEventId, "EMAIL_RECIPIENT_CC", workEffortId, delegator, dispatcher, userLogin);

                // Associate incoming email with accounts
                List<GenericValue> partyAndContactMechsCcAndTo = FastList.newInstance();
                partyAndContactMechsCcAndTo.addAll(partyAndContactMechsTo);
                partyAndContactMechsCcAndTo.addAll(partyAndContactMechsCC);

                // creates accounts collection to assign current workEffort
                DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(login));
                PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
                PartyRepository repo = (PartyRepository) partyDomain.getPartyRepository();

                Set<Account> accounts = FastSet.newInstance();
                for (GenericValue partyWithContactMech : partyAndContactMechsCcAndTo) {
                    try {
                        Contact contact = repo.getContactById(partyWithContactMech.getString("partyId"));
                        Set<Account> contactAccounts = contact.getAccounts();
                        if (UtilValidate.isNotEmpty(contactAccounts)) {
                            accounts.addAll(contactAccounts);
                        }
                    } catch (RepositoryException re) {
                        continue;
                    } catch (EntityNotFoundException e) {
                        continue;
                    }
                }

                // looks through list of accounts and assign the workEffort to all of them
                for (Account account : accounts) {
                    Map<String, Object> callCtxt = UtilMisc.toMap("workEffortId", workEffortId, "partyId", account.getPartyId(), "statusId", "PRTYASGN_ASSIGNED", "roleTypeId", "ACCOUNT", "userLogin", userLogin);
                    dispatcher.runSync("assignPartyToWorkEffort", callCtxt);
                }

                // Find a list of associated custRequestIds and orderIds by parsing the email
                List<String> custRequestIds = ActivitiesHelper.getCustRequestIdsFromCommEvent(communicationEvent, delegator);
                List<String> orderIds = ActivitiesHelper.getOrderIdsFromString(communicationEvent.getString("subject"), delegator);

                for (String custRequestId : custRequestIds) {
                    GenericValue custRequest = delegator.findByPrimaryKey("CustRequest", UtilMisc.toMap("custRequestId", custRequestId));
                    if (UtilValidate.isEmpty(custRequest)) {
                        Debug.logWarning("Ignoring invalid custRequestId " + custRequestId + " in crmsfa.processIncomingEmail", MODULE);
                        continue;
                    }

                    // If the case has been closed (CRQ_COMPLETED), reopen it (CRQ_REOPENED)
                    if ("CRQ_COMPLETED".equals(custRequest.getString("statusId"))) {
                        Map<String, Object> reopenCaseResult = dispatcher.runSync("updateCustRequest", UtilMisc.toMap("custRequestId", custRequestId, "statusId", "CRQ_REOPENED", "userLogin", userLogin));
                        if (ServiceUtil.isError(reopenCaseResult)) {
                            return reopenCaseResult;
                        }
                    }

                    // Associate the case with the activity via CustRequestWorkEffort, if it isn't already
                    GenericValue custRequestWorkEffort = delegator.findByPrimaryKey("CustRequestWorkEffort", UtilMisc.toMap("custRequestId", custRequestId, "workEffortId", workEffortId));
                    if (UtilValidate.isNotEmpty(custRequestWorkEffort)) {
                        continue;
                    }
                    Map<String, Object> createWorkEffortRequestResult = dispatcher.runSync("createWorkEffortRequest", UtilMisc.toMap("custRequestId", custRequestId, "workEffortId", workEffortId, "userLogin", userLogin));
                    if (ServiceUtil.isError(createWorkEffortRequestResult)) {
                        return createWorkEffortRequestResult;
                    }

                }

                for (String orderId : orderIds) {
                    GenericValue orderHeaderWorkEffort = delegator.findByPrimaryKey("OrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId));
                    if (UtilValidate.isNotEmpty(orderHeaderWorkEffort)) {
                        continue;
                    }
                    Map<String, Object> createOrderHeaderWEResult = dispatcher.runSync("createOrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId, "userLogin", userLogin));
                    if (ServiceUtil.isError(createOrderHeaderWEResult)) {
                        return createOrderHeaderWEResult;
                    }
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /*************************************************************************/
    /*                     Create/Update Activities                          */
    /*************************************************************************/

    public static Map<String, Object> createActivity(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        try {
            Timestamp estimatedStartDate = (Timestamp) context.get("estimatedStartDate");
            Timestamp estimatedCompletionDate = (Timestamp) context.get("estimatedCompletionDate");
            String duration = (String) context.get("duration");
            if (UtilValidate.isEmpty(estimatedCompletionDate)) {

                if (UtilValidate.isEmpty(duration)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorActivityRequiresEstCompletionOrDuration", locale, MODULE);
                } else {
                    // If an estimated completion date hasn't been supplied, try to calculate it from the duration
                    estimatedCompletionDate = UtilCommon.getEndTimestamp(estimatedStartDate, duration, locale, timeZone);
                }
            }

            // check for conflicts
            String forceIfConflicts = (String) context.get("forceIfConflicts");
            if (forceIfConflicts == null || forceIfConflicts.equals("N")) {
                List<GenericValue> events = UtilActivity.getActivityConflicts(userLogin, estimatedStartDate, estimatedCompletionDate);
                if (events.size() > 0) {
                    StringBuffer msg = new StringBuffer("You have one or more conflicting events during the specified time. ");
                    msg.append("<a class=\"messageLink\" href=\"/crmsfa/control/myHome?calendarView=day&start=");
                    msg.append(UtilDateTime.getDayStart(estimatedStartDate, timeZone, locale).getTime());
                    msg.append("\">Click here to view them.</a>");
                    return UtilMessage.createAndLogServiceError(msg.toString(), "CrmErrorCreateActivityFail", locale, MODULE);
                }
            }

            // by default set security scope to public
            String scopeEnumId = (String) context.get("scopeEnumId");
            if (UtilValidate.isEmpty(scopeEnumId)) {
                scopeEnumId = "WES_PUBLIC";
            }

            // validate the associations
            Map<String, Object> serviceResults = validateWorkEffortAssociations(dctx, context);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateActivityFail", locale, MODULE);
            }

            // create the workeffort from the context data, which results in a workEffortId
            ModelService service = dctx.getModelService("createWorkEffort");
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("estimatedCompletionDate", estimatedCompletionDate);
            input.put("scopeEnumId", scopeEnumId);
            serviceResults = dispatcher.runSync("createWorkEffort", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateActivityFail", locale, MODULE);
            }
            String workEffortId = (String) serviceResults.get("workEffortId");

            // create the associations and finish
            serviceResults = createWorkEffortPartyAssociations(dctx, context, workEffortId, "CrmErrorCreateActivityFail", true);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateActivityFail", locale, MODULE);
            }

            // Send notification email to initial parties
            Map<String, Object> sendEmailResult = dispatcher.runSync("crmsfa.sendActivityNotificationEmails", UtilMisc.toMap("partyId", userLogin.getString("partyId"), "workEffortId", workEffortId, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailResult)) {
                return sendEmailResult;
            }
            String internalPartyId = (String) context.get("internalPartyId");
            if (UtilValidate.isNotEmpty(internalPartyId)) {
                sendEmailResult = dispatcher.runSync("crmsfa.sendActivityNotificationEmails", UtilMisc.toMap("partyId", internalPartyId, "workEffortId", workEffortId, "userLogin", userLogin));
                if (ServiceUtil.isError(sendEmailResult)) {
                    return sendEmailResult;
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("workEffortId", workEffortId);
            return results;
        } catch (IllegalArgumentException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateActivityFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateActivityFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateActivityFail", locale, MODULE);
        }
    }

    public static Map<String, Object> updateActivity(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        String workEffortId = (String) context.get("workEffortId");
        try {
            // check if userlogin can update this work effort
            if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            Timestamp estimatedStartDate = (Timestamp) context.get("estimatedStartDate");
            Timestamp estimatedCompletionDate = (Timestamp) context.get("estimatedCompletionDate");
            String duration = (String) context.get("duration");
            if (UtilValidate.isEmpty(estimatedCompletionDate)) {

                if (UtilValidate.isEmpty(duration)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorActivityRequiresEstCompletionOrDuration", locale, MODULE);
                } else {
                // If an estimated completion date hasn't been supplied, try to calculate it from the duration
                    estimatedCompletionDate = UtilCommon.getEndTimestamp(estimatedStartDate, (String) context.get("duration"), locale, timeZone);
                }
            }

            // check for conflicts
            String forceIfConflicts = (String) context.get("forceIfConflicts");
            if (forceIfConflicts == null || forceIfConflicts.equals("N")) {
                List<GenericValue> events = UtilActivity.getActivityConflicts(userLogin, estimatedStartDate, estimatedCompletionDate, workEffortId);
                if (events.size() > 0) {
                    StringBuffer msg = new StringBuffer("You have one or more conflicting events during the specified time. ");
                    msg.append("<a class=\"messageLink\" href=\"/crmsfa/control/myHome?calendarView=day&start=");
                    msg.append(UtilDateTime.getDayStart(estimatedStartDate, timeZone, locale).getTime());
                    msg.append("\">Click here to view them.</a>");
                    return UtilMessage.createAndLogServiceError(msg.toString(), "CrmErrorCreateActivityFail", locale, MODULE);
                }
            }

            // if permission checking is required allow update only to the owner or the super user (CRMSFA_ACT_ADMIN)
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            String oldScopeEnumId = workEffort.getString("scopeEnumId");
            String newScopeEnumId = (String)context.get("scopeEnumId");
            // check if security scope has changed
            if (((newScopeEnumId != null) && (!newScopeEnumId.equals(oldScopeEnumId)))
                || ((newScopeEnumId == null) && (oldScopeEnumId != null))) {
                if (!CrmsfaSecurity.hasSecurityScopePermission(security, userLogin, workEffortId, true)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorPermissionActivitySecurityScopeChangeDenied",
                        UtilMisc.toMap("workEffortId", (String)context.get("workEffortId"),
                        "scopeEnumId", newScopeEnumId), locale, MODULE);
                }
            }

            // validate the associations
            Map<String, Object> serviceResults = validateWorkEffortAssociations(dctx, context);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityFail", locale, MODULE);
            }

            // update the workeffort from the context data
            ModelService service = dctx.getModelService("updateWorkEffort");
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("estimatedCompletionDate", estimatedCompletionDate);
            if (UtilValidate.isNotEmpty(input.get("currentStatusId"))) {
                if (((String) input.get("currentStatusId")).endsWith("_STARTED") && UtilValidate.isEmpty(input.get("actualStartDate"))) {
                    input.put("actualStartDate", UtilDateTime.nowTimestamp());
                } else if (((String) input.get("currentStatusId")).endsWith("_COMPLETED") && UtilValidate.isEmpty(input.get("actualCompletionDate"))) {
                    input.put("actualCompletionDate", UtilDateTime.nowTimestamp());
                }
            }
            serviceResults = dispatcher.runSync("updateWorkEffort", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityFail", locale, MODULE);
            }

            // delete existing associations
            UtilActivity.removeAllAssociationsForWorkEffort(workEffortId, delegator);

            // create the associations and finish
            return createWorkEffortPartyAssociations(dctx, context, workEffortId, "CrmErrorUpdateActivityFail", false);

        } catch (IllegalArgumentException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateActivityFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        }
    }

    public static Map<String, Object> updateActivityWithoutAssoc(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        try {
            // check if userlogin can update this work effort
            if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // update the workeffort from the context data
            ModelService service = dctx.getModelService("updateWorkEffort");
            Map<String, Object> input = service.makeValid(context, "IN");
            if ("TASK_STARTED".equals(input.get("currentStatusId")) && UtilValidate.isEmpty(input.get("actualStartDate"))) {
                input.put("actualStartDate", UtilDateTime.nowTimestamp());
            }
            Map<String, Object> serviceResults = dispatcher.runSync("updateWorkEffort", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityFail", locale, MODULE);
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        }
    }

    /*
     * Updates the status of the CommunicationEvents associated with a WorkEffort
     * If WorkEffort status is started then change the status of related CommunicationEvents from entered to pending.
     * If WorkEffort status is completed then change the status of related CommunicationEvents from pending to completed.
     */
    public static Map<String, Object> updateActivityCommEvent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        Map<String, Object> input = null;
        Map<String, Object> serviceResults = null;
        try {
            //Find WorkEffort from its Id
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));

            String workEffortStatus = workEffort.getString("currentStatusId");

            List<GenericValue> commEvents = null;
            /*
             * If WorkEffort status is started then change the status of related CommunicationEvents from entered to pending.
             * If WorkEffort status is completed then change the status of related CommunicationEvents from pending to completed.
             */
            if ("TASK_STARTED".equals(workEffortStatus)) {
                commEvents = delegator.findByAnd("WorkEffortCommunicationEventView", UtilMisc.toMap("workEffortId", workEffortId, "statusId", "COM_ENTERED"));

                if (commEvents.size() == 0) {
                    return ServiceUtil.returnSuccess();
                }
                for (Iterator<GenericValue> iter = commEvents.iterator(); iter.hasNext(); ) {
                    GenericValue commEvent = iter.next();

                    input = UtilMisc.toMap("communicationEventId", commEvent.getString("communicationEventId"), "statusId", "COM_PENDING", "userLogin", userLogin);
                    serviceResults = dispatcher.runSync("updateCommunicationEvent", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityCommEventFail", locale, MODULE);
                    }
                }
            } else if ("TASK_COMPLETED".equals(workEffortStatus)) {
                commEvents = delegator.findByAnd("WorkEffortCommunicationEventView", UtilMisc.toMap("workEffortId", workEffortId, "statusId", "COM_PENDING"));

                if (commEvents.size() == 0) {
                    return ServiceUtil.returnSuccess();
                }
                for (Iterator<GenericValue> iter = commEvents.iterator(); iter.hasNext(); ) {
                    GenericValue commEvent = iter.next();

                    input = UtilMisc.toMap("communicationEventId", commEvent.getString("communicationEventId"), "statusId", "COM_COMPLETE", "userLogin", userLogin);
                    // in some cases the datetimeEnded might still be empty, set it from the workeffort actualCompletionDate or by default now
                    if (UtilValidate.isEmpty(commEvent.get("datetimeEnded"))) {
                        Timestamp datetimeEnded = workEffort.getTimestamp("actualCompletionDate");
                        if (datetimeEnded == null) {
                            datetimeEnded = UtilDateTime.nowTimestamp();
                        }

                        input.put("datetimeEnded", datetimeEnded);
                    }

                    serviceResults = dispatcher.runSync("updateCommunicationEvent", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityCommEventFail", locale, MODULE);
                    }
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityCommEventFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityCommEventFail", locale, MODULE);
        }
    }

    /**
     * Service changes owner of the activity. Since only owner is allowed we
     * beforehand obsolete all assigned parties with role CAL_OWNER.
     */
    public static Map<String, Object> changeActivityOwner(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();

        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("newOwnerPartyId");

        try {
            // check if user's permissions to change activity owner
            if (!CrmsfaSecurity.hasChangeActivityOwnerPermission(delegator, security, userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // make sure the party exists
            if (delegator.findCountByAnd("Party", UtilMisc.toMap("partyId", partyId)) == 0) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", partyId), locale, MODULE);
            }

            // as only owner can be assigned to activity we obsolete all existent
            List<GenericValue> existingOwners = UtilActivity.getActivityParties(delegator, workEffortId, Arrays.asList("CAL_OWNER"));
            for (GenericValue owner : existingOwners) {
                owner.set("thruDate", UtilDateTime.nowTimestamp());
                owner.store();
            }

            Map<String, Object> callContext = new HashMap<String, Object>();
            callContext.put("userLogin", userLogin);
            callContext.put("locale", locale);
            callContext.put("workEffortId", workEffortId);
            callContext.put("partyId", partyId);
            callContext.put("fromDate", UtilDateTime.nowTimestamp());
            callContext.put("roleTypeId", "CAL_OWNER");
            callContext.put("statusId", "PRTYASGN_ASSIGNED");

            dispatcher.runSync("assignPartyToWorkEffort", callContext);

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        } catch (GenericServiceException gse) {
            return UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateActivityAssociation(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String custRequestId = (String) context.get("custRequestId");
        String newOwnerPartyId = (String) context.get("newOwnerPartyId");
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            // Check to see that the workEffort really exists
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorActivityNotFound", UtilMisc.toMap("workEffortId", workEffortId), locale, MODULE);
            }

            try {

                /*
                 * Change owner of the activity. We must make call to crmsfa.changeActivityOwner
                 * before hasActivityPermission call since this service checks permissions in different way.
                 */
                if (UtilValidate.isNotEmpty(newOwnerPartyId)) {
                    GenericValue oldOwnerParty = UtilActivity.getActivityOwner(workEffortId, delegator);
                    String oldOwnerPartyId = null;
                    if (oldOwnerParty != null) {
                        oldOwnerPartyId = oldOwnerParty.getString("partyId");
                    }
                    if (!newOwnerPartyId.equals(oldOwnerPartyId)) {

                        Map<String, Object> callContext = new HashMap<String, Object>();
                        callContext.put("userLogin", userLogin);
                        callContext.put("locale", locale);
                        callContext.put("workEffortId", workEffortId);
                        callContext.put("newOwnerPartyId", newOwnerPartyId);

                        dispatcher.runSync("crmsfa.changeActivityOwner", callContext);
                    }
                }

            } catch (GenericServiceException gse) {
                ServiceUtil.returnFailure(gse.getMessage());
            }

            try {

                // If there's a custRequestId, make sure the custRequest really exists
                if (UtilValidate.isNotEmpty(custRequestId)) {
                    GenericValue custRequest = delegator.findByPrimaryKey("CustRequest", UtilMisc.toMap("custRequestId", custRequestId));
                    if (UtilValidate.isEmpty(custRequest)) {
                        return UtilMessage.createAndLogServiceFailure("CrmErrorCaseNotFound", UtilMisc.toMap("custRequestId", custRequestId), locale, MODULE);
                    }
                }

                if (UtilValidate.isNotEmpty(custRequestId) && delegator.findCountByAnd("CustRequestWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "custRequestId", custRequestId)) == 0) {

                    // We're not really updating the workEffort, but make a security check based on it anyway
                    if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                        return UtilMessage.createAndLogServiceFailure("CrmErrorPermissionDenied", null, locale, MODULE);
                    }

                    // Remove any existing associations for this workEffort
                    delegator.removeByAnd("CustRequestWorkEffort", UtilMisc.toMap("workEffortId", workEffortId));

                    // Make the new association
                    // No create service for this entity, so use the delegator
                    delegator.create("CustRequestWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "custRequestId", custRequestId));

                }

            } catch (GenericEntityException gee) {
                ServiceUtil.returnFailure(gee.getMessage());
            }

            try {

                String orderId = (String) context.get("orderId");
                if (UtilValidate.isNotEmpty(orderId)) {
                    GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                    if (UtilValidate.isEmpty(orderHeader)) {
                        return UtilMessage.createAndLogServiceFailure("OpentapsError_OrderNotFound", UtilMisc.toMap("orderId", orderId), locale, MODULE);
                    }

                    if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                        return UtilMessage.createAndLogServiceFailure("CrmErrorPermissionDenied", null, locale, MODULE);
                    }

                    // Remove any existing associations to orders and create the new association
                    List<String> orderIds = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("OrderHeaderWorkEffort", UtilMisc.toMap("workEffortId", workEffortId)), "orderId", true);
                    for (String orderIdToRemove : orderIds) {
                        Map<String, Object> deleteOHWEResult = dispatcher.runSync("deleteOrderHeaderWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "orderId", orderIdToRemove, "userLogin", userLogin));
                        if (ServiceUtil.isError(deleteOHWEResult)) {
                            return deleteOHWEResult;
                        }
                    }
                    Map<String, Object> createOHWEResult = dispatcher.runSync("createOrderHeaderWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "orderId", orderId, "userLogin", userLogin));
                    if (ServiceUtil.isError(createOHWEResult)) {
                        return createOHWEResult;
                    }
                }
            } catch (GenericServiceException e) {
                ServiceUtil.returnFailure(e.getMessage());
            } catch (GenericEntityException e) {
                ServiceUtil.returnFailure(e.getMessage());
            }

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, "CrmErrorProcessIncomingEmailFail", locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> logTask(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        String workEffortPurposeTypeId = (String) context.get("workEffortPurposeTypeId");

        try {
            // get the actual completion date from the duration (default to now and 1 hour)
            Timestamp actualStartDate = (Timestamp) context.get("actualStartDate");
            if (actualStartDate == null) {
                actualStartDate = UtilDateTime.nowTimestamp();
            }
            Timestamp actualCompletionDate = (Timestamp) context.get("actualCompletionDate");
            // if actualCompletionDate is not given, use the duration instead
            if (actualCompletionDate == null) {
                actualCompletionDate = UtilCommon.getEndTimestamp(actualStartDate, (String) context.get("duration"), locale, timeZone);
            }

            // validate the associations
            Map<String, Object> serviceResults = validateWorkEffortAssociations(dctx, context);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorLogTaskFail", locale, MODULE);
            }

            //input.put("partyIdFrom", context.get("fromPartyId"));
            //input.put("roleTypeIdFrom", PartyHelper.getFirstValidRoleTypeId((String) context.get("partyId"), PartyHelper.TEAM_MEMBER_ROLES, delegator));

            // create the workeffort from the context data, which results in a workEffortId
            ModelService service = dctx.getModelService("createWorkEffort");
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("actualCompletionDate", actualCompletionDate);
            input.put("workEffortTypeId", "TASK");
            input.put("currentStatusId", "TASK_COMPLETED");
            serviceResults = dispatcher.runSync("createWorkEffort", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorLogTaskFail", locale, MODULE);
            }
            String workEffortId = (String) serviceResults.get("workEffortId");

            // create the associations
            serviceResults = createWorkEffortPartyAssociations(dctx, context, workEffortId, "CrmErrorLogTaskFail", true);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorLogTaskFail", locale, MODULE);
            }

            // assumne inbound
            boolean outbound = false;
            String partyIdTo = userLogin.getString("partyId");
            String partyIdFrom = (String) context.get("internalPartyId");

            // then change if it's actually outbound
            if (context.get("outbound").equals("Y")) {
                outbound = true;
                partyIdTo = (String) context.get("internalPartyId");
                partyIdFrom = userLogin.getString("partyId");
            }

            // create a completed comm event with as much information as we have
            service = dctx.getModelService("createCommunicationEvent");
            input = service.makeValid(context, "IN");
            input.put("subject", context.get("workEffortName"));
            input.put("entryDate", UtilDateTime.nowTimestamp());
            input.put("datetimeStarted", actualStartDate);
            input.put("datetimeEnded", actualCompletionDate);
            if ("WEPT_TASK_EMAIL".equals(workEffortPurposeTypeId)) {
                input.put("contactMechTypeId", "EMAIL_ADDRESS");
                input.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
            } else if ("WEPT_TASK_PHONE_CALL".equals(workEffortPurposeTypeId)) {
                input.put("contactMechTypeId", "TELECOM_NUMBER");
                input.put("communicationEventTypeId", "PHONE_COMMUNICATION");
            } else {
                Debug.logWarning("Work effort purpose type [" + workEffortPurposeTypeId + "] not known, not able to set communication event and contact mech types", MODULE);
            }
            input.put("statusId", "COM_COMPLETE");
            input.put("partyIdTo", partyIdTo);
            input.put("partyIdFrom", partyIdFrom);
            if (outbound) {
                if (partyIdTo != null) {
                    input.put("roleTypeIdTo", PartyHelper.getFirstValidInternalPartyRoleTypeId(partyIdTo, delegator));
                }
                input.put("roleTypeIdFrom", PartyHelper.getFirstValidTeamMemberRoleTypeId(partyIdFrom, delegator));
            } else {
                if (partyIdFrom != null) {
                    input.put("roleTypeIdFrom", PartyHelper.getFirstValidInternalPartyRoleTypeId(partyIdFrom, delegator));
                }
                input.put("roleTypeIdTo", PartyHelper.getFirstValidTeamMemberRoleTypeId(partyIdTo, delegator));
            }
            serviceResults = dispatcher.runSync("createCommunicationEvent", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorLogTaskFail", locale, MODULE);
            }
            String communicationEventId = (String) serviceResults.get("communicationEventId");

            // create an association between the task and comm event (safe even if existing)
            input = UtilMisc.toMap("userLogin", userLogin, "communicationEventId", communicationEventId, "workEffortId", workEffortId);
            serviceResults = dispatcher.runSync("createCommunicationEventWorkEff", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorLogTaskFail", locale, MODULE);
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("workEffortId", workEffortId);
            return results;
        } catch (IllegalArgumentException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorLogTaskFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorLogTaskFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorLogTaskFail", locale, MODULE);
        }
    }

    /*************************************************************************/
    /*                  WorkEffortPartyAssignment Services                   */
    /*************************************************************************/

    public static Map<String, Object> addWorkEffortPartyAssignment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        try {
            // check if userlogin can update this work effort
            if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the user has the required security scope to add a party to this activity
            // (must be assignee or super user (CRMSFA_ACT_ADMIN))
            if (!CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, userLogin, workEffortId, false)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionNotAllowedToAddPartyToActivity",
                    UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId), locale, MODULE);
            }

            // Make sure the party exists
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isEmpty(party)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", context, locale, MODULE);
            }

            // if an unexpired existing relationship exists, then skip (this is to avoid duplicates)
            List<GenericValue> oldAssocs = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId)));
            if (oldAssocs.size() == 0) {

                // assign party as a CAL_ATTENDEE with status assigned to the work effort
                Map<String, Object> input = UtilMisc.<String, Object>toMap("workEffortId", workEffortId, "partyId", partyId, "roleTypeId", "CAL_ATTENDEE", "statusId", "PRTYASGN_ASSIGNED",
                        "availabilityStatusId", "WEPA_AV_AVAILABLE");
                input.put("userLogin", userLogin);
                Map<String, Object> serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateActivityFail", locale, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeWorkEffortPartyAssignment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        try {
            // check if userlogin can update this work effort
            if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the user has the required security scope to remove a party from this activity
            // (must be assignee or super user (CRMSFA_ACT_ADMIN))
            if (!CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, userLogin, workEffortId, false)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionNotAllowedToRemovePartyFromActivity",
                    UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId), locale, MODULE);
            }

            // remove by hand because service in work effort uses fromDate as a required field
            List<GenericValue> associations = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId)));
            if (associations.size() == 0) {
                return UtilMessage.createAndLogServiceError("Cannot remove party with ID [" + partyId + "]", "CrmErrorUpdateActivityFail", locale, MODULE);
            }
            for (Iterator<GenericValue> iter = associations.iterator(); iter.hasNext();) {
                GenericValue assoc = iter.next();
                assoc.set("thruDate", UtilDateTime.nowTimestamp());
                assoc.store();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateWorkEffortPartyAssignment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        try {
            // check if userlogin can update this work effort
            if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the user has the required security scope to update a party assigned to this activity
            // (must be assignee or super user (CRMSFA_ACT_ADMIN))
            if (!CrmsfaSecurity.hasActivityUpdatePartiesPermission(security, userLogin, workEffortId, false)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionNotAllowedToUpdatePartyAssignedToActivity",
                    UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId), locale, MODULE);
            }

            // TODO: done by hand because work effort service requires a permission that's not been added to crmsfa
            List<GenericValue> associations = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId)));
            if (associations.size() > 0) {
                GenericValue assoc = associations.get(0);
                // input context, don't overwrite if context has null or empty, no name prefixes, set both pks and non-pks
                assoc.setAllFields(context, false, null, null);
                assoc.store();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateActivityFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /*************************************************************************/
    /*                           Find Activities                             */
    /*************************************************************************/

    @SuppressWarnings("unchecked")
	public static Map<String, Object> findActivities(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String partyId = (String) context.get("partyId");
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String custRequestId = (String) context.get("custRequestId");
        List<EntityCondition> pendingActsAddConditions = (List<EntityCondition>) context.get("pendingActsAddConditions");
        List<EntityCondition> completedActsAddConditions = (List<EntityCondition>) context.get("completedActsAddConditions");
        List<String> pendingOrderByFields = (List<String>) context.get("pendingOrderByFields");
        List<String> completedOrderByFields = (List<String>) context.get("completedOrderByFields");

        Map<String, Object> results = ServiceUtil.returnSuccess();

        // determine which entity to search and the key relationship for searching it
        String entityName = null;
        EntityCondition keyCondition = null;

        if ((partyId != null) && !(partyId.equals(""))) {
            entityName = "WorkEffortAndPartyAssign";
            keyCondition = EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId);
        } else if ((salesOpportunityId != null) && !(salesOpportunityId.equals(""))) {
            entityName = "WorkEffortAndSalesOpportunity";
            keyCondition = EntityCondition.makeCondition("salesOpportunityId", EntityOperator.EQUALS, salesOpportunityId);
        } else if ((custRequestId != null) && !(custRequestId.equals(""))) {
            entityName = "WorkEffortCustRequestView";
            keyCondition = EntityCondition.makeCondition("custRequestId", EntityOperator.EQUALS, custRequestId);
        }

        if ((entityName == null) || (keyCondition == null)) {
            return UtilMessage.createAndLogServiceError("No parameters specified for crmsfa.findActivities", "CrmErrorFindActivitiesFail", locale, MODULE);
        }

        try {
            List<String> fieldsToSelect = UtilMisc.toList("workEffortId", "workEffortTypeId", "workEffortName", "currentStatusId", "estimatedStartDate", "estimatedCompletionDate");
            fieldsToSelect.add("workEffortPurposeTypeId");
            fieldsToSelect.add("actualStartDate");
            fieldsToSelect.add("actualCompletionDate");

            List<EntityCondition> pendingActivitiesCondList = UtilMisc.toList(keyCondition);
            for (Iterator<String> iter = UtilActivity.ACT_STATUSES_COMPLETED.iterator(); iter.hasNext();) {
                pendingActivitiesCondList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, iter.next()));
            }
            if (entityName.equals("WorkEffortAndPartyAssign")) {
                pendingActivitiesCondList.add(EntityUtil.getFilterByDateExpr());
            }
            if (UtilValidate.isNotEmpty(pendingActsAddConditions)) {
                pendingActivitiesCondList.addAll(pendingActsAddConditions);
            }
            EntityCondition pendingActivitiesCond = EntityCondition.makeCondition(pendingActivitiesCondList, EntityOperator.AND);
            List<GenericValue> pendingActivities = delegator.findByCondition(entityName, pendingActivitiesCond, fieldsToSelect, pendingOrderByFields);

            List<EntityCondition> completedActivitiesCondList = UtilMisc.toList(keyCondition);
            for (Iterator<String> iter = UtilActivity.ACT_STATUSES_PENDING.iterator(); iter.hasNext();) {
                completedActivitiesCondList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, iter.next()));
            }
            if (entityName.equals("WorkEffortAndPartyAssign")) {
                completedActivitiesCondList.add(EntityUtil.getFilterByDateExpr());
            }
            if (UtilValidate.isNotEmpty(completedActsAddConditions)) {
                completedActivitiesCondList.addAll(completedActsAddConditions);
            }
            EntityCondition completedActivitiesCond = EntityCondition.makeCondition(completedActivitiesCondList, EntityOperator.AND);
            List<GenericValue> completedActivities = delegator.findByCondition(entityName, completedActivitiesCond, fieldsToSelect, completedOrderByFields);

            // if user has activity admin permission he/she can view all activities
            // if he/she doesn't have super user privileges we must filter with additional security scope conditions
            if (!security.hasEntityPermission("CRMSFA", "_ACT_ADMIN", userLogin)) {
                // additional security scope conditions for users WITHOUT CRMSFA_ACT_ADMIN permission
                EntityCondition securityScopeMainCond = UtilActivity.getSecurityScopeCondition(userLogin);

                // from the previously selected pendingActivities, select the ones the user has security scope permission to view
                // Attention : must check if there are pending activities otherwise the query will return all activities
                if (UtilValidate.isNotEmpty(pendingActivities)) {
                    List<String> pendingActivitiesIds = EntityUtil.getFieldListFromEntityList(pendingActivities, "workEffortId", true);
                    EntityCondition pendingActivitiesFilteredBySecurityScopeCond = EntityCondition.makeCondition(EntityOperator.AND,
                                                                               securityScopeMainCond,
                                                                               EntityCondition.makeCondition("workEffortId", EntityOperator.IN, pendingActivitiesIds),
                                                                               EntityUtil.getFilterByDateExpr());
                    pendingActivities = delegator.findByCondition("WorkEffortAndPartyAssign", pendingActivitiesFilteredBySecurityScopeCond, null, fieldsToSelect, pendingOrderByFields, UtilCommon.DISTINCT_READ_OPTIONS);
                }

                // from the previously selected completedActivities, select the ones the user has security scope permission to view
                // Attention : must check if there are completed activities otherwise the query will return all activities
                if (UtilValidate.isNotEmpty(completedActivities)) {
                    List<String> completedActivitiesIds = EntityUtil.getFieldListFromEntityList(completedActivities, "workEffortId", true);
                    EntityCondition completedActivitiesFilteredBySecurityScopeCond = EntityCondition.makeCondition(EntityOperator.AND,
                                                                               securityScopeMainCond,
                                                                               EntityCondition.makeCondition("workEffortId", EntityOperator.IN , completedActivitiesIds),
                                                                               EntityUtil.getFilterByDateExpr());
                    completedActivities = delegator.findByCondition("WorkEffortAndPartyAssign", completedActivitiesFilteredBySecurityScopeCond, null, fieldsToSelect, completedOrderByFields, UtilCommon.DISTINCT_READ_OPTIONS);
                }
            }

            results.put("pendingActivities", pendingActivities);
            results.put("completedActivities", completedActivities);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorFindActivitiesFail", locale, MODULE);
        }
        return results;
    }

    /*************************************************************************/
    /*                            Helper Methods                             */
    /*************************************************************************/

    /**
     * Helper method to create WorkEffort associations for an internal party (account, contact or lead), a case and an opportunity.
     * If you need to remove existing ones, use the method removeAllAssociationsForWorkEffort() first.
     *
     * @param   reassign    Whether the CAL_OWNER should be overwritten by the userLogin or not
     * @return  If an error occurs, returns service error which can be tested with ServiceUtil.isError(), otherwise a service success
     */
    private static Map<String, Object> createWorkEffortPartyAssociations(DispatchContext dctx, Map<String, Object> context, String workEffortId, String errorLabel, boolean reassign)
        throws GenericEntityException, GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Map<String, Object> input = null;
        Map<String, Object> serviceResults = null;

        // association IDs
        String internalPartyId = (String) context.get("internalPartyId");
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String custRequestId = (String) context.get("custRequestId");

        /*
         * The first step is to collect all the ACCOUNT, CONTACT or PROSPECT parties that we should associate with the workEffort.
         * This includes those ACCOUNTS and CONTACTS that are associated with the case or opportunity that was specified in the input.
         * Then we find the first valid role type for each, which is required for the work effort association. If any of these parties
         * has no valid role types, then a bad ID was passed in. This serves to validate the association input.
         */
        List<String> partyAssociationIds = new ArrayList<String>();
        if (internalPartyId != null) {
            partyAssociationIds.add(internalPartyId);
        }
        if (salesOpportunityId != null) {
            partyAssociationIds.addAll(UtilOpportunity.getOpportunityAccountPartyIds(delegator, salesOpportunityId));
            partyAssociationIds.addAll(UtilOpportunity.getOpportunityContactPartyIds(delegator, salesOpportunityId));
        }
        if (custRequestId != null) {
            List<GenericValue> parties = UtilCase.getCaseAccountsAndContacts(delegator, custRequestId);
            for (Iterator<GenericValue> iter = parties.iterator(); iter.hasNext();) {
                partyAssociationIds.add(iter.next().getString("partyId"));
            }
        }
        // now get the roles
        List<String> partyAssocRoleTypeIds = new ArrayList<String>();
        for (Iterator<String> iter = partyAssociationIds.iterator(); iter.hasNext();) {
            String partyId = iter.next();
            String roleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, PartyHelper.CLIENT_PARTY_ROLES, delegator);
            if (roleTypeId == null) {
                roleTypeId = "_NA_"; // this permits non-crmsfa parties to be associated
            }
            partyAssocRoleTypeIds.add(roleTypeId);
        }

        /*
         * The remaining task is to create the associations to work effort.
         */

        if (reassign) {
            // expire all associations of type CAL_OWNER for this work effort
            List<GenericValue> oldOwners = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", workEffortId, "roleTypeId", "CAL_OWNER")));
            for (Iterator<GenericValue> iter = oldOwners.iterator(); iter.hasNext();) {
                GenericValue old = iter.next();
                old.set("thruDate", UtilDateTime.nowTimestamp());
                old.store();
            }

            // first make sure the userlogin has a role CAL_OWNER
            input = UtilMisc.<String, Object>toMap("partyId", userLogin.getString("partyId"), "roleTypeId", "CAL_OWNER");
            List<GenericValue> partyRoles = delegator.findByAnd("PartyRole", input);
            if (partyRoles.size() == 0)  {
                input.put("userLogin", userLogin);
                serviceResults = dispatcher.runSync("createPartyRole", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }
            }

            // then create the assignment
            input.put("workEffortId", workEffortId);
            input.put("userLogin", userLogin);
            input.put("roleTypeId", "CAL_OWNER");
            input.put("statusId", "PRTYASGN_ASSIGNED");
            input.put("availabilityStatusId", context.get("availabilityStatusId")); // add our availability status
            serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
            }
        }

        // associate the opportunity with the work effort if it is not associated yet
        if (salesOpportunityId != null) {
            input = UtilMisc.<String, Object>toMap("salesOpportunityId", salesOpportunityId, "workEffortId", workEffortId);
            GenericValue map = delegator.findByPrimaryKey("SalesOpportunityWorkEffort", input);
            if (map == null) {
                map = delegator.makeValue("SalesOpportunityWorkEffort", input);
                // TODO: created by hand because we don't have a service for this yet
                map.create();
            }
        }

        // associate the case with the work effort if it is not associated yet
        if (custRequestId != null) {
            if (UtilValidate.isEmpty(delegator.findByPrimaryKeyCache("CustRequestWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "custRequestId", custRequestId)))) {
                serviceResults = dispatcher.runSync("createWorkEffortRequest",
                    UtilMisc.toMap("workEffortId", workEffortId, "custRequestId", custRequestId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }
            }
        }

        // now for each party association, assign the party and its role to the work effort
        if (partyAssociationIds != null) {
            Iterator<String> roleIter = partyAssocRoleTypeIds.iterator();
            Iterator<String> partyIter = partyAssociationIds.iterator();
            while (partyIter.hasNext()) {
                String partyId = partyIter.next();
                String roleTypeId = roleIter.next();

                // if an unexpired existing relationship exists, then skip (this is to avoid duplicates)
                List<GenericValue> oldAssocs = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                            UtilMisc.toMap("workEffortId", workEffortId, "roleTypeId", roleTypeId, "partyId", partyId)));
                if (oldAssocs.size() > 0) {
                    continue;
                }

                // now create the new one
                input = UtilMisc.<String, Object>toMap("workEffortId", workEffortId, "partyId", partyId, "roleTypeId", roleTypeId, "statusId", "PRTYASGN_ASSIGNED");
                input.put("userLogin", userLogin);
                serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, errorLabel, locale, MODULE);
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Helper method to validate the work effort associations that will be created.
     */
    private static Map<String, Object> validateWorkEffortAssociations(DispatchContext dctx, Map<String, Object> context)
        throws GenericEntityException, GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        // association IDs
        String internalPartyId = (String) context.get("internalPartyId");
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String custRequestId = (String) context.get("custRequestId");

        // if there's an internal party, check if we have update on that party
        if (internalPartyId != null) {
            String module = CrmsfaSecurity.getSecurityModuleOfInternalParty(internalPartyId, delegator);
            if (module == null) {
                // if the party does not have a CRMSFA role, then we will allow activity actions for now
                return ServiceUtil.returnSuccess();
            }
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, module, "_UPDATE", userLogin, internalPartyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
        }

        // if there's an opportunity, check if we can update it
        if (salesOpportunityId != null) {
            if (!CrmsfaSecurity.hasOpportunityPermission(security, "_UPDATE", userLogin, salesOpportunityId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
        }

        // if there's an opportunity, check if we can update it
        if (custRequestId != null) {
            if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
        }

        // if we get here, all checks passed
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> autoCreateTimesheetEntryForActivity(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        String workEffortId = (String) context.get("workEffortId");

        try {
            // useful for creating background records and transactions
            GenericValue systemUser = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));

            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorActivityNotFound", UtilMisc.toMap("workEffortId", workEffortId), locale, MODULE);
            }
            if (!("TASK_COMPLETED".equals(workEffort.getString("currentStatusId"))) && !("EVENT_COMPLETED".equals(workEffort.getString("currentStatusId")))) {
                Debug.logInfo("Activity [" + workEffortId + "] is not completed yet, not creating time entry for it", MODULE);
                return ServiceUtil.returnSuccess();
                }

            // find all the internal parties involved in this work effort
            List<String> internalAssignedParties = ActivitiesHelper.findInternalWorkeffortPartyIds(workEffortId, delegator);
            if (UtilValidate.isEmpty(internalAssignedParties)) {
                Debug.logInfo("No CRM/SFA parties assigned to work effort [" + workEffortId + "], not creating time sheet entries", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // find client parties assigned to this activity
            List<GenericValue> clientParties = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toList(
                        EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId),
                        EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, PartyHelper.CLIENT_PARTY_ROLES),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED")));

            // a List of client partyIds to be used in EntityOperator.IN to lookup timesheets later
            List<String> clientPartyIds = new ArrayList<String>();
            if (UtilValidate.isEmpty(clientParties)) {
                Debug.logInfo("No client parties found for work effort [" + workEffortId + "]", MODULE);
            } else {
                for (Iterator<GenericValue> clientIt = clientParties.iterator(); clientIt.hasNext();) {
                    GenericValue clientParty = clientIt.next();
                    clientPartyIds.add(clientParty.getString("partyId"));
                }
            }

           for (Iterator<String> internalIt = internalAssignedParties.iterator(); internalIt.hasNext();) {
                String internalAssignedPartyId = internalIt.next();
                Debug.logInfo("Processing internal party [" + internalAssignedPartyId + "]", MODULE);

                // find all the timesheets of all the internal parties involved and client parties
                List<EntityCondition> timesheetParams = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, internalAssignedPartyId));
                if (UtilValidate.isNotEmpty(workEffort.get("actualStartDate"))) {
                    timesheetParams.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, workEffort.getTimestamp("actualStartDate")));
                }
                if (UtilValidate.isNotEmpty(workEffort.get("actualCompletionDate"))) {
                    timesheetParams.add(EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, workEffort.getTimestamp("actualCompletionDate")));
                }
                if (!UtilValidate.isEmpty(clientPartyIds)) {
                    timesheetParams.add(EntityCondition.makeCondition("clientPartyId", EntityOperator.IN, clientPartyIds));
                }

                List<GenericValue> timesheets = delegator.findByAnd("Timesheet", timesheetParams);
                if (UtilValidate.isEmpty(timesheets)) {
                    Debug.logInfo("No timesheets found for party [" + internalAssignedPartyId + "] which began before [" + workEffort.getTimestamp("actualStartDate") + "] and ended after [" + workEffort.getTimestamp("actualCompletionDate") + "], not creating time shee entries for work effort [" + workEffortId + "] and party [" + internalAssignedPartyId + "]", MODULE);
                    continue;  // go on to the next internal party
                }

                // there will be a separate timesheet for each internal party and for each client party combination
                for (Iterator<GenericValue> timesheetsIt = timesheets.iterator(); timesheetsIt.hasNext();) {
                    GenericValue timesheet = timesheetsIt.next();
                    Debug.logInfo("Processing timesheet " + timesheet, MODULE);

                    // create timesheet entries for each internal party + client party combination
                    Map<String, Object> timeEntryParams = UtilMisc.toMap("partyId", internalAssignedPartyId,
                            "fromDate", workEffort.getTimestamp("actualStartDate"),
                            "thruDate", workEffort.getTimestamp("actualCompletionDate"),
                            "workEffortId", workEffortId,
                            "timesheetId", timesheet.getString("timesheetId"),
                            "comments", workEffort.getString("workEffortName"));
                    timeEntryParams.put("userLogin", systemUser);
                    Map<String, Object> tmpResult = dispatcher.runSync("createTimeEntry", timeEntryParams);
                    if (ServiceUtil.isError(tmpResult)) {
                        return tmpResult;
                    }
                }
            }

        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of internal parties related to the activity to email.
     */
    public static Map<String, Object> sendActivityNotificationEmails(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String partyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, partyId, false);

            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorActivityNotFound", UtilMisc.toMap("workEffortId", workEffortId), locale, MODULE);
            }
            String workEffortName = UtilValidate.isEmpty(workEffort.getString("workEffortName")) ? workEffortId : workEffort.getString("workEffortName");

            // Retrieve a list of all internal partyIds associated with the activity
            Set<String> internalPartyIds = new HashSet<String>(ActivitiesHelper.findInternalWorkeffortPartyIds(workEffortId, delegator));
            List<GenericValue> allAssignedParties = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toList(
                                                EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId),
                                                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED"),
                                                EntityUtil.getFilterByDateExpr()));
            Set<String> assignedPartyIds = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(allAssignedParties, "partyId", true));

            // Notify all internal parties
            Set<String> partiesToNotify = new HashSet<String>(internalPartyIds);

            String eventTypeId = workEffort.getString("workEffortTypeId").toLowerCase();
            String eventType = null;
            if (assignedPartyIds.contains(partyId)) {
                eventType = ".add";

                // Added internal or external parties should get an email about it
                partiesToNotify.add(partyId);

            } else {
                eventType = ".remove";

                // Removed internal parties should get an email about it
                if (internalPartyIds.contains(partyId)) {
                    partiesToNotify.add(partyId);
                }
            }

            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("partyId", partyId, "partyName", partyName, "workEffortId", workEffortId, "workEffortName", workEffortName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url.activity", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject." + eventTypeId + eventType, messageMap, locale);

            Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("eventType", eventTypeId + eventType);
            bodyParameters.putAll(messageMap);

            Map<String, Object>  sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", UtilMisc.toList(partiesToNotify), "eventType", "activity" + eventType, "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult;
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Removes associated communicationEvents (with attachments) when a pending email task is cancelled
     */
    public static Map<String, Object> deleteCancelledActivityEmail(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String workEffortTypeId = (String) context.get("workEffortTypeId");
        String workEffortPurposeTypeId = (String) context.get("workEffortPurposeTypeId");
        String currentStatusId = (String) context.get("currentStatusId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        // Make sure we don't do anything if this isn't an email task activity that has been cancelled
        if (!"TASK".equals(workEffortTypeId)) {
            return ServiceUtil.returnSuccess();
        }
        if (!"WEPT_TASK_EMAIL".equals(workEffortPurposeTypeId)) {
            return ServiceUtil.returnSuccess();
        }
        if (!"TASK_CANCELLED".equals(currentStatusId)) {
            return ServiceUtil.returnSuccess();
        }

        // Check if userLogin can update this work effort
        if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {

            // Retrieve the related pending email communicationEvents via the CommunicationEventWorkEff record
            GenericValue communicationEventWorkEff = EntityUtil.getFirst(delegator.findByAnd("CommunicationEventWorkEff", UtilMisc.toMap("workEffortId", workEffortId)));
            List<GenericValue> communicationEvents = communicationEventWorkEff.getRelatedByAnd("CommunicationEvent", UtilMisc.toMap("communicationEventTypeId", "EMAIL_COMMUNICATION", "statusId", "COM_PENDING"));
            if (UtilValidate.isEmpty(communicationEvents)) {
                return ServiceUtil.returnSuccess();
            }

            // For each communication event, call the crmsfa.deleteActivityEmail service
            Iterator<GenericValue> rceit = communicationEvents.iterator();
            while (rceit.hasNext()) {
                GenericValue communicationEvent = rceit.next();

                Map<String, Object> deleteActivityEmailResult = dispatcher.runSync("crmsfa.deleteActivityEmail", UtilMisc.toMap("communicationEventId", communicationEvent.getString("communicationEventId"), "workEffortId", workEffortId, "delContentDataResource", "true", "userLogin", userLogin));
                if (ServiceUtil.isError(deleteActivityEmailResult)) {
                    return deleteActivityEmailResult;
                }
            }

        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> deleteActivityEmail(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String communicationEventId = (String) context.get("communicationEventId");
        String workEffortId = (String) context.get("workEffortId");
        String delContentDataResourceStr = (String) context.get("delContentDataResource");
        String donePage = (String) context.get("donePage");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        // by default delete the attachments
        String delContentDataResource = ("false".equalsIgnoreCase(delContentDataResourceStr) || "N".equalsIgnoreCase(delContentDataResourceStr)) ? "false" : "true";

        // Check if userLogin can update this work effort
        if (!CrmsfaSecurity.hasActivityPermission(security, "_UPDATE", userLogin, workEffortId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        try {

            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            ActivityRepositoryInterface activityRepository = domainLoader.getDomainsDirectory().getActivitiesDomain().getActivityRepository();
            ActivityFactRepositoryInterface activityFactRepository = domainLoader.getDomainsDirectory().getActivitiesDomain().getActivityFactRepository();

            // Get Activity
            Activity activity = activityRepository.getActivityById(workEffortId);
            List<Party> parties = activity.getParticipants();

            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError("No activity found with work effort ID [" + workEffortId + "]");
            }

            if (communicationEventId != null) {
                // Remove any existing associations to CommunicationEventOrder
                List<String> eventOrderIds = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("CommunicationEventOrder", UtilMisc.toMap("communicationEventId", communicationEventId)), "orderId", true);
                for (String orderIdToRemove : eventOrderIds) {
                    Map<String, Object> deleteOHWEResult = dispatcher.runSync("removeCommunicationEventOrder", UtilMisc.toMap("communicationEventId", communicationEventId, "orderId", orderIdToRemove, "userLogin", userLogin));
                    if (ServiceUtil.isError(deleteOHWEResult)) {
                        return deleteOHWEResult;
                    }
                }

                // delete just this particular communicationEventId
                results = deleteActivityCommEventAndDataResource(workEffortId, communicationEventId, delContentDataResource, userLogin, dispatcher);
            } else {
                List<GenericValue> communicationEvents = workEffort.getRelated("CommunicationEventWorkEff");
                for (GenericValue communicationEvent: communicationEvents) {
                    results = deleteActivityCommEventAndDataResource(workEffortId, communicationEvent.getString("communicationEventId"), delContentDataResource, userLogin, dispatcher);
                }
            }

            // Remove any existing associations to OrderHeaderWorkEffort
            List<String> orderIds = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("OrderHeaderWorkEffort", UtilMisc.toMap("workEffortId", workEffortId)), "orderId", true);
            for (String orderIdToRemove : orderIds) {
                Map<String, Object> deleteOHWEResult = dispatcher.runSync("deleteOrderHeaderWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "orderId", orderIdToRemove, "userLogin", userLogin));
                if (ServiceUtil.isError(deleteOHWEResult)) {
                    return deleteOHWEResult;
                }
            }

            // Call the deleteWorkEffort service
            Map<String, Object> deleteWorkEffortResult = dispatcher.runSync("deleteWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "userLogin", userLogin));
            if (ServiceUtil.isError(deleteWorkEffortResult)) {
                return deleteWorkEffortResult;
            }

            // Transform to ActivityFact with negative counter equals -1
            activityFactRepository.transformToActivityFacts(activity, parties, COUNT);

        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (RepositoryException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (EntityNotFoundException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }

        results.put("donePage", donePage);
        return results;
    }

    private static Map deleteActivityCommEventAndDataResource(String workEffortId, String communicationEventId, String delContentDataResource, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        Map<String, Object> deleteCommunicationEventWorkEffResult = dispatcher.runSync("deleteCommunicationEventWorkEff", UtilMisc.toMap("workEffortId", workEffortId, "communicationEventId", communicationEventId, "userLogin", userLogin));
        if (ServiceUtil.isError(deleteCommunicationEventWorkEffResult)) {
            return deleteCommunicationEventWorkEffResult;
        }

        // Call the deleteCommunicationEvent service
        Map<String, Object> deleteCommunicationEventResult = dispatcher.runSync("deleteCommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId, "delContentDataResource", delContentDataResource, "userLogin", userLogin));
        if (ServiceUtil.isError(deleteCommunicationEventResult)) {
            return deleteCommunicationEventResult;
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Search all CommunicationEvent matching the given email address and associate them to the
     *  given partyId and contachMechId.
     * @param dctx
     * @param context
     */
    public static Map<String, Object> updateActivityEmailsAssocs(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String emailAddress = (String) context.get("emailAddress");
        String partyId = (String) context.get("partyId");
        String contactMechId = (String) context.get("contactMechId");

        try {
             // Use the system userLogin for these operations
            GenericValue userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));

            // find all communication events concerning the email address
            List<GenericValue> communicationEventsFrom = findWorkEffortCommunicationEventViews("fromString", emailAddress, delegator);
            List<GenericValue> communicationEventsTo   = findWorkEffortCommunicationEventViews("toString", emailAddress, delegator);
            List<GenericValue> communicationEventsBcc  = findWorkEffortCommunicationEventViews("bccString", emailAddress, delegator);
            List<GenericValue> communicationEventsCc   = findWorkEffortCommunicationEventViews("ccString", emailAddress, delegator);

            // find the PartyAndContactMech of the given party, ass needed by associateCommunicationEventWorkEffortAndParties
            List<GenericValue> partyAndContactMechs = delegator.findByCondition("PartyAndContactMech",
                                                                                EntityCondition.makeCondition(EntityOperator.AND,
                                                               EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                                                               EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, contactMechId)),
                                                                                null, UtilMisc.toList("fromDate"));

            // associate all emails FROM
            for (GenericValue commEvent : communicationEventsFrom) {
                try {
                    commEvent.set("contactMechIdFrom", contactMechId);
                    commEvent.set("partyIdFrom", partyId);
                    commEvent.set("statusId", "COM_ENTERED");
                    commEvent.store();
                    associateCommunicationEventWorkEffortAndParties(partyAndContactMechs, (String) commEvent.get("communicationEventId"), "EMAIL_SENDER", (String) commEvent.get("workEffortId"), delegator, dispatcher, userLogin);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }

            // associate all emails TO
            for (GenericValue commEvent : communicationEventsTo) {
                try {
                    commEvent.set("contactMechIdTo", contactMechId);
                    commEvent.set("partyIdTo", partyId);
                    commEvent.set("statusId", "COM_ENTERED");
                    commEvent.store();
                    associateCommunicationEventWorkEffortAndParties(partyAndContactMechs, (String) commEvent.get("communicationEventId"), "EMAIL_RECIPIENT_TO", (String) commEvent.get("workEffortId"), delegator, dispatcher, userLogin);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }

            // associate all emails BCC
            for (GenericValue commEvent : communicationEventsBcc) {
                try {
                    associateCommunicationEventWorkEffortAndParties(partyAndContactMechs, (String) commEvent.get("communicationEventId"), "EMAIL_RECIPIENT_BCC", (String) commEvent.get("workEffortId"), delegator, dispatcher, userLogin);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }

            // associate all emails CC
            for (GenericValue commEvent : communicationEventsCc) {
                try {
                    associateCommunicationEventWorkEffortAndParties(partyAndContactMechs, (String) commEvent.get("communicationEventId"), "EMAIL_RECIPIENT_CC", (String) commEvent.get("workEffortId"), delegator, dispatcher, userLogin);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        return results;
    }

    /**
     * Finds the list of <code>PartyAndContactMech</code> matching the given list of email addresses.
     * This method respect the email address case insensitivity setting "general.properties", "mail.address.caseInsensitive".
     * @param addresses the list of addresses
     * @param delegator the delegator
     * @return the list of <code>PartyAndContactMech</code>
     * @throws GenericEntityException if an error occurs
     */
    private static List<GenericValue> findPartyAndContactMechsForEmailAddress(Collection<String> addresses, Delegator delegator) throws GenericEntityException {
        // option for matching email addresses and parties
        String caseInsensitiveEmail = UtilProperties.getPropertyValue("general.properties", "mail.address.caseInsensitive", "N");
        boolean ci = "Y".equals(caseInsensitiveEmail);

        List<GenericValue> partyAndContactMechs;

        // case insensitive condition does not work with the IN operator in the delegator
        if (ci) {
            partyAndContactMechs = new ArrayList<GenericValue>();
            for (String address : addresses) {
                List<GenericValue> partyAndContactMechPartial = delegator.findByCondition("PartyAndContactMech", EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.EQUALS, EntityFunction.UPPER(address)), null, UtilMisc.toList("fromDate"));
                partyAndContactMechs.addAll(partyAndContactMechPartial);
            }
        } else {
            partyAndContactMechs = delegator.findByCondition("PartyAndContactMech", EntityCondition.makeCondition("infoString", EntityOperator.IN, addresses), null, UtilMisc.toList("fromDate"));
        }

        partyAndContactMechs = EntityUtil.filterByDate(partyAndContactMechs, true);
        return partyAndContactMechs;
    }

    private static List<GenericValue> findWorkEffortCommunicationEventViews(String field, String address, Delegator delegator) throws GenericEntityException {
        // option for matching email addresses and parties
        String caseInsensitiveEmail = UtilProperties.getPropertyValue("general.properties", "mail.address.caseInsensitive", "N");
        boolean ci = "Y".equals(caseInsensitiveEmail);
        if (ci) {
            return delegator.findByCondition("WorkEffortCommunicationEventView", EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(field), EntityOperator.LIKE, EntityFunction.UPPER(address)), null, null);
        } else {
            return delegator.findByCondition("WorkEffortCommunicationEventView", EntityCondition.makeCondition(field, EntityOperator.LIKE, address), null, null);
        }
    }

}

