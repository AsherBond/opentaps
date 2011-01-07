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

package org.opentaps.common.pagination;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.builder.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.List;

/**
 * Helps with creating and fetching of paginator objects.  There are a variety of conventions used by the pagination
 * system.  Those having to do with the nitty, gritty details of creation, saving in the session, and fetching are
 * encapsulated in this class.
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public final class PaginatorFactory {

    private PaginatorFactory() { }

    /**
     * Fetches a paginator from the session provided that paginatorName and applicationName are request parameters.
     * This is useful only for the pagination requests and event handlers.
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>Paginator</code> value
     */
    public static Paginator getPaginator(HttpServletRequest request) {
        String paginatorName = getSessionPaginatorName(request);
        HttpSession session = request.getSession();
        return (Paginator) session.getAttribute(paginatorName);
    }

    /**
     * Get the named paginator from the session.
     * @param session a <code>HttpSession</code> value
     * @param paginatorName a <code>String</code> value
     * @param applicationName a <code>String</code> value
     * @return a <code>Paginator</code> value
     */
    public static Paginator getPaginator(HttpSession session, String paginatorName, String applicationName) {
        String sessionName = getSessionPaginatorName(paginatorName, applicationName);
        return (Paginator) session.getAttribute(sessionName);
    }

    // builds the session paginator name given all name details
    private static String getSessionPaginatorName(String paginatorName, String applicationName) {
        return "paginator." + applicationName + "." + paginatorName;
    }

    // generates the paginator name for a session
    private static String getSessionPaginatorName(HttpServletRequest request) {
        String paginatorName = request.getParameter("paginatorName");
        if (paginatorName == null) {
            throw new IllegalArgumentException("Missing paginatorName parameter for pagination request.  Paginator in question is [" + paginatorName + "].");
        }

        String applicationName = request.getParameter("applicationName");
        if (applicationName == null) {
            throw new IllegalArgumentException("Missing applicationName parameter for pagination request.  Paginator in question is [" + paginatorName + "].");
        }

        return getSessionPaginatorName(paginatorName, applicationName);
    }

    // Saves a paginator in the session with a special name.
    private static void savePaginator(HttpSession session, Paginator paginator) {
        String sessionName = getSessionPaginatorName(paginator.getPaginatorName(), paginator.getApplicationName());
        session.setAttribute(sessionName, paginator);
    }

    /**
     * Main factory method used by PaginateTransform.
     * @param paginatorName a <code>String</code> value
     * @param list an <code>Object</code> value
     * @param context a <code>Map</code> value
     * @param params a <code>Map</code> value
     * @param rememberPage a <code>boolean</code> value
     * @param rememberOrderBy a <code>boolean</code> value
     * @param renderExcelButton a <code>boolean</code> value
     * @return a <code>Paginator</code> value
     * @exception ListBuilderException if an error occurs
     */
    public static Paginator createPaginatorForTransform(String paginatorName, Object list, Map context, Map params, boolean rememberPage, boolean rememberOrderBy, boolean renderExcelButton) throws ListBuilderException {
        HttpSession session = (HttpSession) context.get("session");
        String applicationName = (String) context.get("opentapsApplicationName");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // make the paginator depending on the list type
        Paginator paginator = null;
        ListBuilder builder = null;

        if (list instanceof EntityListBuilder) {
            Delegator delegator = (Delegator) context.get("delegator");
            ((EntityListBuilder) list).setDelegator(delegator);
            builder = (ListBuilder) list;
        } else if (list instanceof bsh.This) {
            builder = new BshListBuilder((bsh.This) list, (Delegator) context.get("delegator"));
        } else if (list instanceof List) {
            builder = new MapListBuilder((List) list);
        } else {
            throw new ListBuilderException("Failed to create paginator named ["+paginatorName+"] from macro because the suplied list object ["+list.getClass().getName()+"] is not supported");
        }

        // for now, all paginators created this way are formlet paginators
        paginator = new Paginator(paginatorName, builder, params, userLogin, applicationName, rememberPage, rememberOrderBy, renderExcelButton);
        paginator.setIsFormlet(true);
        savePaginator(session, paginator);

        return paginator;
    }

    /**
     * Updates the paginator if the user reloads the page and something significant has changed.
     * @param paginator a <code>Paginator</code> value
     * @param list an <code>Object</code> value
     * @param context a <code>Map</code> value
     * @param params a <code>Map</code> value
     * @param rememberPage a <code>boolean</code> value
     * @param rememberOrderBy a <code>boolean</code> value
     * @param renderExcelButton a <code>boolean</code> value
     * @exception ListBuilderException if an error occurs
     */
    public static void updatePaginatorForTransform(Paginator paginator, Object list, Map context, Map params, boolean rememberPage, boolean rememberOrderBy, boolean renderExcelButton) throws ListBuilderException {

        // update paginator variables if they changed
        paginator.setParams(params);
        paginator.setRememberPage(rememberPage);
        paginator.setRememberOrderBy(rememberOrderBy);
        paginator.setRenderExcelButton(renderExcelButton);

        // return user to first page if we're not remembering the page
        if (!rememberPage) {
            paginator.setCursorIndex(0);
        }

        // for Lists, rebuild completely
        if (list instanceof List) {
            ListBuilder builder = new MapListBuilder((List) list);
            paginator.setListBuilder(builder);
            return;
        }

        // for bsh, we need to reload the closure, which rebuilds everything
        if (list instanceof bsh.This) {
            ListBuilder builder = new BshListBuilder((bsh.This) list, (Delegator) context.get("delegator"));
            if (rememberOrderBy) {
                builder.changeOrderBy(paginator.getOrderBy()); // preserve the order by
            }
            paginator.setListBuilder(builder);
            return;
        }

        // replace the list builder entirely, because developer might have changed it
        if (list instanceof ListBuilder) {
            ListBuilder builder = (ListBuilder) list;
            if (rememberOrderBy) {
                builder.changeOrderBy(paginator.getOrderBy()); // preserve the order by
            }
            paginator.setListBuilder(builder);
        }

        // set the delegator again because the above will have erased it
        if (list instanceof EntityListBuilder) {
            EntityListBuilder builder = (EntityListBuilder) paginator.getListBuilder();
            builder.setDelegator((Delegator) context.get("delegator"));
        }
    }
}
