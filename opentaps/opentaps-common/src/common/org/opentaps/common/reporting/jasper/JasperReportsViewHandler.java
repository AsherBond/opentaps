/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */

/* Copyright (c) Open Source Strategies, Inc.
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

package org.opentaps.common.reporting.jasper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.FontKey;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.engine.export.JRTextExporterParameter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.PdfFont;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.util.JRProperties;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.ContextFilter;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.opentaps.common.reporting.UtilReports;
import org.opentaps.common.reporting.UtilReports.ContentType;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;

import com.lowagie.text.pdf.BaseFont;

/**
 * Class renders Jasper Reports of any supported content type.
 */
public class JasperReportsViewHandler extends AbstractViewHandler {

    protected ServletContext context;
    public static final String MODULE = JasperReportsViewHandler.class.getName();

    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    /* (non-Javadoc)
     * @see org.ofbiz.webapp.view.ViewHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) {
        this.context = context;
    }

    /* (non-Javadoc)
     * @see org.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        Connection conn = null;
        Session session = null;

        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet
        if (request == null) {
            throw new ViewHandlerException("The HttpServletRequest object was null, how did that happen?");
        }
        if (UtilValidate.isEmpty(page)) {
            throw new ViewHandlerException("View page was null or empty, but must be specified");
        }
        if (UtilValidate.isEmpty(info) && Debug.infoOn()) {
            Debug.logInfo("View info string was null or empty, (optionally used to specify an Entity that is mapped to the Entity Engine datasource that the report will use).", MODULE);
        }

        // tell the ContextFilter we are forwarding
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
            throw new ViewHandlerException("The delegator object was null, how did that happen?");
        }

        try {
            // Collects parameters/properties
            Map<String, Object> parameters = (Map<String, Object>) request.getAttribute("jrParameters");
            if (UtilValidate.isEmpty(parameters)) {
                parameters = UtilHttp.getParameterMap(request);
            }

            if (!parameters.containsKey("SUBREPORT_DIR")) {
                parameters.put("SUBREPORT_DIR", context.getRealPath("/"));
            }
            Locale locale = UtilHttp.getLocale(request);
            parameters.put("REPORT_LOCALE", locale);

            String location = null;
            String reportId = (String) parameters.get("reportId");
            if (UtilValidate.isNotEmpty(reportId)) {
                GenericValue reportRegistry = delegator.findByPrimaryKeyCache("ReportRegistry", UtilMisc.toMap("reportId", reportId));
                if (UtilValidate.isNotEmpty(reportRegistry)) {
                    location = reportRegistry.getString("reportLocation");
                }
            }

            if (UtilValidate.isEmpty(location)) {
                location = page;
            }

            // Trying to get report object from the given location.
            JasperReport report = UtilReports.getReportObject(location);
            if (report == null) {
                throw new ViewHandlerException("Fatal error. Report object can not be created for some unknown reason.");
            }

            // Provide access to uiLabelMap if resource bundle isn't defined. 
            if (UtilValidate.isEmpty(report.getResourceBundle())) {
                JRResourceBundle resources = new JRResourceBundle(locale);
                if (resources.size() > 0) {
                    parameters.put("REPORT_RESOURCE_BUNDLE", resources);
                }
            }

            // Identify what output user want to get. It's depend on requested MIME type that
            // can be passed in parameter 'reportType' or attribute content-type of the view-map tag in
            // controller file. If absent both, HTML by default.
            String myContentType = request.getParameter("reportType");
            if (UtilReports.getContentType(myContentType) == null) {
                myContentType = contentType;
                if (UtilValidate.isEmpty(myContentType)) {
                    myContentType = ContentType.HTML.toString();
                }
            }
            response.setContentType(myContentType);

            // If report is exporting to XLS or CSV format then disable pagination.
            // Also supply parameter isPlainFormat that can be used report designers in the case to do special things.
            if (ContentType.XLS.toString().equals(myContentType) || ContentType.CSV.toString().equals(myContentType)) {
                parameters.put("IS_IGNORE_PAGINATION", Boolean.TRUE);
                parameters.put("isPlainFormat", Boolean.TRUE);
            } else {
                parameters.put("isPlainFormat", Boolean.FALSE);
            }

            // Try to find data source for report
            JRDataSource jrDataSource = (JRDataSource) request.getAttribute("jrDataSource");
            JasperPrint jp = null;
            if (jrDataSource == null) {
                String datasourceName = delegator.getEntityHelperName(info);
                String jndiDataSourceName = (String) parameters.get("jndiDS");

                // report may use HQL, put hibernate session
                Infrastructure i = new Infrastructure((LocalDispatcher) request.getAttribute("dispatcher"));
                session = i.getSession();
                parameters.put("HIBERNATE_SESSION", session);

                if (UtilValidate.isNotEmpty(datasourceName) && UtilValidate.isEmpty(jndiDataSourceName)) {
                    // given entity
                    conn = ConnectionFactory.getConnection(datasourceName);
                    jp = JasperFillManager.fillReport(report, parameters, conn);
                } else {
                    // JDBC direct connection
                    if (UtilValidate.isEmpty(jndiDataSourceName)) {
                        conn = ConnectionFactory.getConnection(delegator.getGroupHelperName("org.ofbiz"));
                    } else {
                        conn = ConnectionFactory.getConnection(delegator.getGroupHelperName("org.opentaps." + jndiDataSourceName));
                    }

                    jp = JasperFillManager.fillReport(report, parameters, conn);
                }
            } else {
                // custom data source object
                jp = JasperFillManager.fillReport(report, parameters, jrDataSource);
            }

            if (jp.getPages().size() < 1) {
                Debug.logError("Report is empty.", MODULE);
            } else {
                Debug.logInfo("Got report, there are " + jp.getPages().size() + " pages.", MODULE);
            }

            // Generates and exports report
            ContentType content = UtilReports.getContentType(myContentType);
            generate(request.getParameter("reportName"), jp, content, request, response);

        } catch (java.sql.SQLException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException("SQL exception is occurred <" + e.getMessage() + ">", e);
        } catch (JRException e) {
            Debug.logError("Can't generate Jasper report. Error: " + e.getMessage(), MODULE);
            throw new ViewHandlerException("Unexpected JasperReports exception <" + e.getMessage() + ">", e);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException(e);
        } catch (MalformedURLException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException(e);
        } catch (FileNotFoundException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException(e);
        } catch (IOException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException(e);
        } catch (InfrastructureException e) {
            Debug.logError(e.getMessage(), MODULE);
            throw new ViewHandlerException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            conn = null;
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Method will stream out the requested report in various report types (pdf, word, excel, text, xml).
     * 
     * @param name report friendly name
     * @param jasperPrint compiled report object
     * @param contentType MIME type, a <code>ContentType</code> value
     * @param request HttpServletResponce object
     * @param response HttpServletResponse object
     * @throws IOException
     * @throws JRException
     */
    private void generate(String name, JasperPrint jasperPrint, ContentType contentType, HttpServletRequest request, HttpServletResponse response) throws IOException, JRException {
        ServletOutputStream os = response.getOutputStream();
        Map<JRExporterParameter, Object> exporterParameters = FastMap.newInstance();
        JRExporter exporter = null;
        String reportName = UtilValidate.isEmpty(name) ? jasperPrint.getName() : name;
        if (UtilValidate.isEmpty(reportName)) {
            reportName = "myReport";
        }

        if (contentType.equals(ContentType.PDF)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.pdf\"", reportName));
            exporter = new JRPdfExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
            exporterParameters.put(JRPdfExporterParameter.FONT_MAP, getPdfFontMap());
            // add logged user as author
            HttpSession session = request.getSession();
            GenericValue person = (GenericValue) session.getAttribute("person");
            if (UtilValidate.isNotEmpty(person)) {
                exporterParameters.put(JRPdfExporterParameter.METADATA_AUTHOR, PartyHelper.getPartyName((Delegator) request.getAttribute("delegator"), person.getString("partyId"), false));
            }
            // add product name as creator
            String opentaps = UtilProperties.getPropertyValue("OpentapsUiLabels.properties", "OpentapsProductName");
            if (UtilValidate.isNotEmpty(opentaps)) exporterParameters.put(JRPdfExporterParameter.METADATA_CREATOR, opentaps);

        } else if (contentType.equals(ContentType.HTML)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.html\"", reportName));
            exporter = new JRHtmlExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
            exporterParameters.put(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);
            exporterParameters.put(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.FALSE);
        } else if (contentType.equals(ContentType.XLS)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.xls\"", reportName));
            exporter = new JExcelApiExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
        } else if (contentType.equals(ContentType.XML)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.xls\"", reportName));
            exporter = new JRXmlExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
            exporter.setParameters(exporterParameters);
        } else if (contentType.equals(ContentType.CSV)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.csv\"", reportName));
            exporter = new JRCsvExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
        } else if (contentType.equals(ContentType.RTF)){
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.rtf\"", reportName));
            exporter = new JRRtfExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
        } else if (contentType.equals(ContentType.TXT)){
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.txt\"", reportName));
            exporter = new JRTextExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
            exporterParameters.put(JRTextExporterParameter.CHARACTER_WIDTH, new Integer(80));
            exporterParameters.put(JRTextExporterParameter.CHARACTER_HEIGHT, new Integer(25));
        } else if (contentType.equals(ContentType.ODT)) {
            response.setHeader(HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=\"%1$s.odf\"", reportName));
            exporter = new JROdtExporter();
            exporterParameters.put(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporterParameters.put(JRExporterParameter.OUTPUT_STREAM, os);
        }

        exporter.setParameters(exporterParameters);
        exporter.exportReport();

    }

    /**
     * TODO: oandreyev: This hack must be removed. JasperReports must find and load fonts w/o assistance.
     * @return Map
     */
    Map<FontKey, PdfFont> getPdfFontMap() {

        String fontPath = context.getRealPath(JRProperties.getProperty("net.sf.jasperreports.export.pdf.fontdir.jasper"));

        HashMap<FontKey, PdfFont> fontMap = new HashMap<FontKey, PdfFont>();

        FontKey key = null;
        PdfFont font = null;

        key = new FontKey("SansSerif", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSans.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("SansSerif", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("SansSerif", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-Oblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("SansSerif", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-BoldOblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("Serif", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("Serif", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("Serif", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-Italic.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("Serif", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-BoldItalic.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSans.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-Oblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-BoldOblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Serif", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Serif", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Serif", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-Italic.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Serif", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSerif-BoldItalic.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Condensed", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSansCondensed.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Condensed", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSansCondensed-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Condensed", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSansCondensed-Oblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Condensed", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSansCondensed-BoldOblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Mono", false, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSansMono.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Mono", true, false);
        font = new PdfFont(fontPath + "/" + "DejaVuSansMono-Bold.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Mono", false, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSansMono-Oblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Mono", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSansMono-BoldOblique.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        key = new FontKey("DejaVu Sans Light", true, true);
        font = new PdfFont(fontPath + "/" + "DejaVuSans-ExtraLight.ttf", BaseFont.IDENTITY_H, true);
        fontMap.put(key, font);

        return fontMap;

    }
}
