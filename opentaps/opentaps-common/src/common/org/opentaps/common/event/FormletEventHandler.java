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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.TemplateException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.webapp.event.JavaEventHandler;
import org.opentaps.common.builder.FormletFactory;
import org.opentaps.common.builder.ListBuilderException;
import org.opentaps.common.pagination.Paginator;
import org.opentaps.common.pagination.PaginatorFactory;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;


/**
 * Formlet event handler.  Formlets are small freemarker fragments that represent
 * dynamically generated forms or lists and are retrieved using AJAX requests.
 * Formlets are tightly coupled to the opentaps pagination, list builder, page
 * builder and form macro systems, so that the developer can easily generate
 * sophisticated and consistent form behavior.  They can be thought of as a feature
 * complete alternative to the OFBiz form widget system.
 *
 * This event handler is used by specific requests that render the formlets,
 * such as the pagination requests.  It stitches together the formlet data,
 * form macros, and standard request attributes so that the formlet
 * has access to all important features.  This handler is also responsible
 * for locating, loading and rendering the formlet templates.  This
 * job cannot be handled by the controller because of the complex interactions
 * between the components.
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public class FormletEventHandler extends JavaEventHandler {

    private static final String MODULE = FormletEventHandler.class.getName();
    public static final String MISSING_PAGINATOR_LABEL = "OpentapsError_MissingPaginator";

    /**
     * Invoke a Java event as usual, then decide what to do with the results.
     */
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        String result = super.invoke(event, requestMap, request, response);

        Locale locale = UtilHttp.getLocale(request);

        if ("error".equals(result)) {
            return result;
        }
        if ("listBuilderException".equals(result)) {
            renderListBuilderException(request, response, locale);
            return "success";
        }

        Paginator paginator = PaginatorFactory.getPaginator(request);
        if (paginator == null) {
            renderMissingPaginatorError(request, response, locale);
            return "success";
        }

        // return if no formlet defined, this will pass through JSON objects and other raw data to javascript event handlers
        if (!paginator.isFormlet()) {
            return result;
        }

        try {
            FormletFactory.renderFormlet(request, response.getWriter(), paginator, locale);
            response.flushBuffer();
        } catch (TemplateException e) {
            throw new EventHandlerException("Failed to render Formlet", e.getCause());
        } catch (IOException e) {
            throw new EventHandlerException("Failed to render Formlet", e.getCause());
        }
        return "success";
    }

    // print out a readable version of what caused the list builder exception
    private void renderListBuilderException(HttpServletRequest request, HttpServletResponse response, Locale locale) throws EventHandlerException {
        ListBuilderException e = (ListBuilderException) request.getAttribute("listBuilderException");
        String paginatorName = UtilCommon.getParameter(request, "paginatorName");
        String errorMsg = "Error while building list for pagination named [" + paginatorName + "]: ";

        // use the nested reason, such as a bsh.Eval error, otherwise the reason of the ListBuilderException itself
        Throwable cause = (e.getCause() == null ? e : e.getCause());
        Debug.logError(cause, errorMsg, MODULE);
        try {
            Writer out = response.getWriter();
            out.write(errorMsg);
            out.write("<pre>");
            cause.printStackTrace(new PrintWriter(out));
            out.write("</pre>");
            response.flushBuffer();
        } catch (IOException ioe) {
            throw new EventHandlerException("Unable to write list builder exception", ioe.getCause());
        }
    }

    // Print out a useful, localized error for user about missing paginator, which instructs them to reload page.
    private void renderMissingPaginatorError(HttpServletRequest request, HttpServletResponse response, Locale locale) throws EventHandlerException {
        String paginatorName = UtilCommon.getParameter(request, "paginatorName");
        if (Debug.infoOn()) {
            Debug.logInfo("Paginator [" + paginatorName + "] missing when attempting to fetch data for it.  This is probably due to user session expiring.", MODULE);
        }
        try {
            Writer out = response.getWriter();
            out.write(UtilMessage.expandLabel(MISSING_PAGINATOR_LABEL, locale));
            response.flushBuffer();
        } catch (IOException e) {
            throw new EventHandlerException("Unable to write missing paginator error", e.getCause());
        }
    }
}
