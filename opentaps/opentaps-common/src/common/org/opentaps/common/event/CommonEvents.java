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
package org.opentaps.common.event;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.print.PrintService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.login.LoginServices;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.event.EventHandlerException;
import org.opentaps.common.reporting.UtilReports;
import org.opentaps.common.reporting.jasper.JRResourceBundle;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

/**
 * CommonEvents stores common HttpServletRequest event methods for opentaps applications.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public final class CommonEvents {

    private CommonEvents() { }

    private static final String MODULE = CommonEvents.class.getName();

    /**
     * This method will read the donePage parameter and return it to the controller as the result.
     * It is called from controller.xml using  <event type="java" path="org.opentaps.common.event.CommonEvents" invoke="donePageRequestHelper"/>
     * Then it can be used with <response name="${returnValue}" .../> to determine what to do next.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return The donePage or DONE_PAGE parameter if it exists, otherwise "error"
     */
    public static String donePageRequestHelper(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> parameters = UtilHttp.getParameterMap(request);
        String donePage = (String) parameters.get("donePage");
        if (donePage == null) {
            donePage = (String) parameters.get("DONE_PAGE");
        }
        if (donePage == null) {
            // special case after service-multi
            Set<String> keys = parameters.keySet();
            for (String current : keys) {
                if (current.startsWith("donePage")) {
                    donePage = (String) parameters.get(current);
                    break;
                }
            }
        }
        if (donePage == null) {
            donePage = "error";
        }

        String errorPage = (String) parameters.get("errorPage");
        if (errorPage != null && UtilCommon.hasError(request)) {
            Debug.logInfo("donePageRequestHelper: goto errorPage [" + errorPage + "]", MODULE);
            return errorPage;
        }

        Debug.logInfo("donePageRequestHelper: goto donePage [" + donePage + "]", MODULE);
        return donePage;
    }


    /**
     * Sets the organizationPartyId in the session.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string, "success" if an organization is set, else "selectOrganization"
     */
    public static String setOrganization(HttpServletRequest request, HttpServletResponse response) {
        String organizationPartyId = request.getParameter("organizationPartyId");
        if (organizationPartyId == null || organizationPartyId.trim().length() == 0) {
            try {
                organizationPartyId = UtilCommon.getUserLoginViewPreference(request, UtilConfig.SYSTEM_WIDE, UtilConfig.SET_ORGANIZATION_FORM, UtilConfig.OPTION_DEF_ORGANIZATION);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while retrieve default organization", MODULE);
                return "selectOrganization";
            }
 
            if (organizationPartyId == null || organizationPartyId.trim().length() == 0) {
                return "selectOrganization";
            }
        }

        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        GenericValue organization = null;
        try {
            organization = delegator.findByPrimaryKeyCache("PartyGroup", UtilMisc.toMap("partyId", organizationPartyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Could not get the organization.", MODULE);
        }

        if (organization == null) {
            return "selectOrganization";
        }

        session.setAttribute("organizationParty", organization);
        session.setAttribute("organizationPartyId", organizationPartyId);
        session.setAttribute("applicationContextSet", Boolean.TRUE);

        try {
            UtilCommon.setUserLoginViewPreference(request, UtilConfig.SYSTEM_WIDE, UtilConfig.SET_ORGANIZATION_FORM, UtilConfig.OPTION_DEF_ORGANIZATION, organizationPartyId);
        } catch (GenericEntityException e) {
            // log message and go ahead, application may work w/o default value
            Debug.logWarning(e.getMessage(), MODULE);
        }

        return "success";
    }

    /**
     * Reloads the Person entity which is stored in the session for the current userLogin.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string
     */
    public static String reloadUserLoginPerson(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        try {
            GenericValue person = userLogin.getRelatedOne("Person");
            if (person != null) {
                session.setAttribute("person", person);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting person info for session, ignoring...", MODULE);
        }
        return "success";
    }

    /**
     * Persists the state of an expandable/contractable UI region based on applicationName/screenName/domId/userLoginId.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string
     */
    public static String persistViewExpansionState(HttpServletRequest request, HttpServletResponse response) {
        String domId = request.getParameter("domId");
        String application = request.getParameter("application");
        String screenName = request.getParameter("screenName");
        String viewState = request.getParameter("viewState");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        if (UtilValidate.isEmpty(userLogin) || UtilValidate.isEmpty(userLogin.getString("userLoginId"))) {
            return "success";
        }

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, Object> prefMap = UtilMisc.<String, Object>toMap("application", application, "screenName", screenName, "domId", domId, "userLoginId", userLogin.getString("userLoginId"));

        try {

            // Find any existing stored preferences and use the first
            List<GenericValue> viewPrefs = delegator.findByAnd("ViewPrefAndLocation", prefMap, UtilMisc.toList("viewPrefTypeId DESC"));
            GenericValue viewPrefAndLocation = EntityUtil.getFirst(viewPrefs);

            if (UtilValidate.isEmpty(viewPrefAndLocation)) {

                // Create
                GenericValue viewPrefType = delegator.makeValue("ViewPrefType", UtilMisc.toMap("application", application, "screenName", screenName, "domId", domId));
                String viewPrefTypeId = delegator.getNextSeqId("ViewPrefType");
                viewPrefType.put("viewPrefTypeId", viewPrefTypeId);
                viewPrefType.create();
                GenericValue viewPref = delegator.makeValue("ViewPreference", UtilMisc.toMap("viewPrefTypeId", viewPrefTypeId, "userLoginId", userLogin.getString("userLoginId"), "viewPrefString", viewState));
                viewPref.create();
            } else {

                // Update
                GenericValue viewPref = viewPrefAndLocation.getRelatedOne("ViewPreference");
                viewPref.put("viewPrefString", viewState);
                viewPref.store();
            }
        } catch (GenericEntityException e) {
            return "error";
        }
        return "success";
    }

    /**
     * Implement server-side printing for JasperReports.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string
     * @exception EventHandlerException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static String printJRReport(HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {

        Connection conn = null;

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
            throw new EventHandlerException("The delegator object was null, how did that happen?");
        }

        Locale locale = UtilHttp.getLocale(request.getSession());
        String printerName = UtilCommon.getParameter(request, "printerName");
        String location = UtilCommon.getParameter(request, "reportPath");

        // collect report parameters ...
        Map<String, Object> parameters = null;

        // ... either from jrParameters attribute ... 
        Map<String, Object> jrParameters = (Map<String, Object>) request.getAttribute("jrParameters");
        if (UtilValidate.isNotEmpty(jrParameters)) {
            parameters = FastMap.<String, Object>newInstance();
            parameters.putAll(jrParameters);
        }

        // ... or from request parameter map
        if (UtilValidate.isEmpty(parameters)) {
            parameters = UtilHttp.getParameterMap(request);
        }

        try {

            if (UtilValidate.isEmpty(location)) {
                String reportId = (String) parameters.get("reportId");
                GenericValue report = delegator.findByPrimaryKeyCache("ReportRegistry", UtilMisc.toMap("reportId", reportId));
                if (UtilValidate.isNotEmpty(report)) {
                    location = report.getString("reportLocation");
                }
            }
            if (UtilValidate.isEmpty(location)) {
                UtilMessage.createAndLogEventError(request, "Report location is unknown. Printing has failed.", locale, MODULE);
                return "error";
            }

            JasperReport report = UtilReports.getReportObject(location);
            if (report == null) {
                throw new EventHandlerException("Fatal error. Report object can not be created for some unknown reason.");
            }

            /*
             * Fill parameters
             */

            // Provide JasperReports with access to uiLabelMap for string resources
            JRResourceBundle resources = new JRResourceBundle(locale);
            if (resources.size() > 0) {
                parameters.put("REPORT_RESOURCE_BUNDLE", resources);
            }
            // User's locale
            parameters.put("REPORT_LOCALE", locale);

            JRDataSource jrDataSource = (JRDataSource) request.getAttribute("jrDataSource");
            JasperPrint jasperPrint = null;
            if (jrDataSource == null) {
                conn = ConnectionFactory.getConnection(delegator.getGroupHelperName("org.ofbiz"));
                jasperPrint = JasperFillManager.fillReport(report, parameters, conn);
            } else {
                jasperPrint = JasperFillManager.fillReport(report, parameters, jrDataSource);
            }

            if (jasperPrint.getPages().size() < 1) {
                Debug.logError("Report is empty.", MODULE);
            } else {
                Debug.logInfo("Got report, there are " + jasperPrint.getPages().size() + " pages.", MODULE);
            }

            PrintService ps = UtilReports.getPrintServiceByName(printerName);
            if (ps == null) {
                return UtilMessage.createAndLogEventError(request, "OpentapsError_UnablePrintService", UtilMisc.toMap("printerName", printerName), locale, MODULE);
            }

            JRPrintServiceExporter exporter = new JRPrintServiceExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG, false);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG_ONLY_ONCE, false);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG, false);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG_ONLY_ONCE, false);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE, ps);
            exporter.exportReport();

        } catch (FileNotFoundException fnfe) {
            UtilMessage.createAndLogEventError(request, fnfe, locale, MODULE);
        } catch (JRException jre) {
            UtilMessage.createAndLogEventError(request, jre, locale, MODULE);
        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogEventError(request, gee, locale, MODULE);
        } catch (SQLException sqle) {
            UtilMessage.createAndLogEventError(request, sqle, locale, MODULE);
        } catch (MalformedURLException mue) {
            UtilMessage.createAndLogEventError(request, mue, locale, MODULE);
        }

        UtilMessage.addError(request, "Report was sent to printer [" + printerName + "] successfuly.");
        return "success";
    }

    /**
     * Prepare data to run reports.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response string
     * @exception EventHandlerException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static String runReport(HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        boolean printout = "Y".equals(UtilCommon.getParameter(request, "printout")) ? true : false;
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        String json = UtilCommon.getParameter(request, "parametersTypeJSON");

        try {

            Map<String, Object> jrParameters = (Map<String, Object>) request.getAttribute("jrParameters");
            if (jrParameters == null) {
                jrParameters = FastMap.newInstance();
            }

            Map<String, String> parametersType = UtilValidate.isNotEmpty(json) ? (Map) JSONObject.toBean(JSONObject.fromObject(json), Map.class) : FastMap.newInstance();

            Map<String, Object> parameters = UtilHttp.getParameterMap(request);
            Set<String> keys = parameters.keySet();
            for (String key : keys) {

                // skip this parameters
                if (key.equals("parametersTypeJSON")) {
                    continue;
                }

                // parameters have accounting tags, add
                // string formated tags for reporting purpose
                if (key.startsWith("tag1")) {
                    String accountingTags = UtilAccountingTags.formatTagsAsString(request, UtilCommon.getParameter(request, "acctgTagUsage"), delegator);
                    if (UtilValidate.isNotEmpty(accountingTags)) {
                        jrParameters.put("accountingTags", accountingTags);
                    }
                }

                String typeInfo = parametersType.get(key);
                if (UtilValidate.isNotEmpty(typeInfo)) {
                    if ("Boolean".equals(typeInfo)) {
                        String indicatorValue = UtilCommon.getParameter(request, key);
                        jrParameters.put(key, (UtilValidate.isNotEmpty(indicatorValue) && "Y".equals(indicatorValue)) ? Boolean.TRUE : Boolean.FALSE);
                        continue;
                    }
                    //TODO: oandreyev: add conversions for other supported types. Use ObjectType
                }

                // This parameter may have DateTime composite type
                if (key.indexOf(UtilHttp.COMPOSITE_DELIMITER) > 0) {
                    String[] tokens = key.split(UtilHttp.COMPOSITE_DELIMITER);
                    if ("compositeType".equals(tokens[1])) {
                        Object obj = UtilHttp.makeParamValueFromComposite(request, tokens[0], locale);
                        if (obj != null) {
                            Timestamp ts = (Timestamp) ObjectType.simpleTypeConvert(obj, "Timestamp", null, timeZone, locale, true);
                            if (ts != null) {
                                jrParameters.put(tokens[0], ts);
                            }
                        }
                        continue;
                    }

                    if ("date".equals(tokens[1]) || "hour".equals(tokens[1]) || "minutes".equals(tokens[1]) || "ampm".equals(tokens[1])) {
                        continue;
                    }
                }

                Object value = UtilCommon.getParameter(request, key);
                if (value != null) {
                    jrParameters.put(key, value);
                }
            }

            request.setAttribute("jrParameters", jrParameters);

        } catch (JSONException je) {
            UtilMessage.createAndLogEventError(request, je, locale, MODULE);
        } catch (GeneralException ge) {
            UtilMessage.createAndLogEventError(request, ge, locale, MODULE);
        }

        return printout ? "print" : "export";
    }

    /**
     * The forgot password event handler which is common in all applications.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String forgotPassword(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String sendFrom = UtilProperties.getPropertyValue("notification.properties", "from");
        String subject = UtilProperties.getPropertyValue("notification.properties", "forgotpassword.subject");

        String resource = "SecurityextUiLabels";
        String errMsg = null;
        boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));

        // validate the userloginId input
        String userLoginId = request.getParameter("USERNAME");
        if ((userLoginId != null) && ("true".equals(UtilProperties.getPropertyValue("security.properties", "username.lowercase")))) {
            userLoginId = userLoginId.toLowerCase();
        }
        if (!UtilValidate.isNotEmpty(userLoginId)) {
            // the password was incomplete
            errMsg = UtilProperties.getMessage(resource, "loginevents.username_was_empty_reenter", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        // get the password of the userlogin or generate a new one if there is encryption
        GenericValue supposedUserLogin = null;
        String passwordToSend = null;
        try {
            supposedUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
            if (supposedUserLogin == null) {
                // the Username was not found
                errMsg = UtilProperties.getMessage(resource, "loginevents.username_not_found_reenter", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            if (useEncryption) {
                // password encrypted, can't send, generate new password and email to user
                double randNum = Math.random();

                // multiply by 100,000 to usually make a 5 digit number
                passwordToSend = "auto" + ((long) (randNum * 100000));
                supposedUserLogin.set("currentPassword", HashCrypt.getDigestHash(passwordToSend, LoginServices.getHashType()));
                supposedUserLogin.set("passwordHint", "Auto-Generated Password");
            } else {
                passwordToSend = supposedUserLogin.getString("currentPassword");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("errorMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource, "loginevents.error_accessing_password", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        if (supposedUserLogin == null) {
            // the Username was not found
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("userLoginId", userLoginId);
            errMsg = UtilProperties.getMessage(resource, "loginevents.user_with_the_username_not_found", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        // now find the party and the party's primary email
        GenericValue party = null;
        StringBuffer emails = new StringBuffer();
        try {
            party = supposedUserLogin.getRelatedOne("Party");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            party = null;
        }
        if (party != null) {
            Iterator<GenericValue> emailIter = UtilMisc.toIterator(ContactHelper.getContactMechByPurpose(party, "PRIMARY_EMAIL", false));
            while (emailIter != null && emailIter.hasNext()) {
                GenericValue email = emailIter.next();
                emails.append(emails.length() > 0 ? "," : "").append(email.getString("infoString"));
            }
        }
        if (!UtilValidate.isNotEmpty(emails.toString())) {
            // the Username was not found
            errMsg = UtilProperties.getMessage(resource, "loginevents.no_primary_email_address_set_contact_customer_service", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        // construct the body
        StringBuffer body = new StringBuffer(subject + userLoginId + "\n\n");
        if (useEncryption) {
            body.append("A new password was generated for you: ").append(passwordToSend).append("\n\n");
        } else {
            body.append("Your password is: ").append(passwordToSend).append("\n\n");
        }
        body.append("When you log in, please change your password because this email is not secure.\n");

        // send the email
        Map<String, Object> input = UtilMisc.<String, Object>toMap("subject", subject + userLoginId, "sendFrom", sendFrom, "contentType", "text/plain");
        input.put("sendTo", emails.toString());
        input.put("body", body.toString());

        try {
            Map<String, Object> result = dispatcher.runSync("sendMail", input);

            if (ServiceUtil.isError(result)) {
                Map<String, Object> messageMap = UtilMisc.toMap("errorMessage", result.get(ModelService.ERROR_MESSAGE));
                errMsg = UtilProperties.getMessage(resource, "loginevents.error_unable_email_password_contact_customer_service_errorwas", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (GenericServiceException e) {
            Debug.logWarning(e, "", MODULE);
            errMsg = UtilProperties.getMessage(resource, "loginevents.error_unable_email_password_contact_customer_service", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        // don't save password until after it has been sent
        if (useEncryption) {
            try {
                supposedUserLogin.store();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "", MODULE);
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("errorMessage", e.toString());
                errMsg = UtilProperties.getMessage(resource, "loginevents.error_saving_new_password_email_not_correct_password", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }

        if (useEncryption) {
            errMsg = UtilProperties.getMessage(resource, "loginevents.new_password_createdandsent_check_email", UtilHttp.getLocale(request));
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
        } else {
            errMsg = UtilProperties.getMessage(resource, "loginevents.new_password_sent_check_email", UtilHttp.getLocale(request));
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
        }
        return "success";
    }

}
