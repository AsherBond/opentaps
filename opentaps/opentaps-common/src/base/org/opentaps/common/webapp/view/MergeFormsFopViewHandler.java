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

package org.opentaps.common.webapp.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import freemarker.template.TemplateException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.opentaps.common.template.freemarker.FreemarkerUtil;
import org.opentaps.common.util.XslFoConversion;
import org.w3c.dom.Document;

public abstract class MergeFormsFopViewHandler implements ViewHandler {

    public static final String module = MergeFormsFopViewHandler.class.getName();
    public static final String opentapsResource = "opentaps";
    public static final String commonResource = "CommonUiLabels";
    public static final String errorResource = "OpentapsErrorLabels";

    private static final String XML = "text/xml";
    private static final String PDF = "application/pdf";
    private static final String HTML = "text/html";
    private static final String MSWORD = "application/ms-word";
    private static final String TXT = "text/plain";

    public static String defaultFileName = "MergeForm";

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    private String getMergeFormText(Delegator delegator, String mergeFormId) {
        if (UtilValidate.isEmpty(mergeFormId) || UtilValidate.isEmpty(delegator)) {
            return null;
        }

        String text = null;
        try {
            GenericValue mergeForm = delegator.findByPrimaryKey("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
            text = mergeForm.getString("mergeFormText");
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return text;
        }
        return text;
    }

    /**
     * @see org.ofbiz.webapp.view.ViewHandler#render(String,String,String,String,String,javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));

        String mergeFormId = request.getParameter("mergeFormId");
        String reportType = request.getParameter("reportType");
        boolean leaveTags = "true".equalsIgnoreCase(request.getParameter("leaveTags")) || "Y".equalsIgnoreCase(request.getParameter("leaveTags"));
        boolean highlightTags = "true".equalsIgnoreCase(request.getParameter("highlightTags")) || "Y".equalsIgnoreCase(request.getParameter("highlightTags"));

        if (UtilValidate.isEmpty(mergeFormId) || UtilValidate.isEmpty(reportType)) {
            throw new ViewHandlerException(UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationInvalidForm", locale));
        }

        // Get the stylesheet location for HTML to XSL:FO transforms
        String stylesheetLocation = UtilProperties.getPropertyValue(opentapsResource, "opentaps.formMerge.stylesheetLocation");
        if (UtilValidate.isEmpty(stylesheetLocation)) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_PropertyNotConfigured", UtilMisc.toMap("propertyName", "opentaps.formMerge.stylesheetLocation", "resource", opentapsResource), locale);
            Debug.logError(errorMessage, module);
            throw new ViewHandlerException(errorMessage);
        }

        // Get the html from the form
        String html = getMergeFormText(delegator, mergeFormId);
        if (UtilValidate.isEmpty(html)) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FormGeneration", locale);
            html = "<p> " + errorMessage + " </p>";
        } else {

            // Run html through an ftl processor
            html = this.renderForm(html, request, leaveTags, highlightTags);
            if (html == null) html = "";
        }

        // Create the output stream for the FORM
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Determine use of FOP based on content type
        if (reportType.equals(PDF) ||
            reportType.equals(XML) ||
            reportType.equals(TXT)) {

            XslFoConversion xslfoc = new XslFoConversion();
            Document xslfo = xslfoc.convertHtml2XslFo(html, stylesheetLocation);
            if (UtilValidate.isEmpty(xslfo)) {
                throw new ViewHandlerException(UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationBadXslFo", locale));
            }
            try {
                if (reportType.equals(PDF) || reportType.equals(TXT)) {

                    FopFactory fopFac = ApacheFopWorker.getFactoryInstance();
                    Fop fop = fopFac.newFop(reportType, out);
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    Source source = new StreamSource(new StringReader(UtilXml.writeXmlDocument(xslfo)));
                    Result result = new SAXResult(fop.getDefaultHandler());
                    transformer.transform(source, result);

                } else {

                    // No need for xsl/fo conversion for XSL-FO
                    out.write(new String(UtilXml.writeXmlDocument(xslfo)).getBytes());
                }
            } catch (Throwable t) {
                throw new ViewHandlerException(UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationBadXslFo", locale), t);
            }
        } else {

            // No need for xlsfo/fop conversion
            try {
                out.write(new String("<html><body>" + html + "</body></html>").getBytes());
            } catch (Throwable t) {
                throw new ViewHandlerException(UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationBadXslFo", locale), t);
            }

        }

        // Set the content type, content disposition and length
        response.setContentType(reportType);
        response.setContentLength(out.size());
        String fileName = getFilename(request);
        if (reportType.equals(PDF)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".pdf\"");
        } else if (reportType.equals(XML)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xml\"");
        } else if (reportType.equals(TXT)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".txt\"");
        } else if (reportType.equals(MSWORD)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".doc\"");
        } else {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".html\"");
        }

        // Write to the browser
        try {
            out.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ViewHandlerException(UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationStreamError", locale), e);
        }
    }

    private String renderForm(String text, HttpServletRequest request, boolean leaveTags, boolean highlightTags) {

        if (UtilValidate.isEmpty(text)) {
            return null;
        }

        String ftlText = null;
        StringWriter wr = new StringWriter();
        Map mergeContext = null;
        try {
            mergeContext = getFormMergeContext(request);
        } catch (Exception e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FormGenerationNoContext", UtilHttp.getParameterMap(request), UtilMisc.ensureLocale(UtilHttp.getLocale(request)));
            Debug.logError(errorMessage, module);
            return null;
        }

        try {
            FreemarkerUtil.renderTemplateWithTags("MergeForm", text, mergeContext, wr, leaveTags, highlightTags);
            ftlText = wr.toString();

        } catch (Exception e) {
            Debug.logInfo(e, module);
            return null;
        }
        return ftlText;
    }

    public abstract String getFilename(HttpServletRequest request);
    public abstract Map getFormMergeContext(HttpServletRequest request) throws TemplateException, IOException;
}
