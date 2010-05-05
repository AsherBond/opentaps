/**
 * Copyright (C) 2006-2007 Funambol
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

package com.funambol.transport.http.server;

import java.io.IOException;

import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.funambol.framework.logging.LogContext;


/**
 * This is a servlet filter that handles LogContext lifespan and session.
 * When a new request is processed, the LogContext is cleaned and then it's
 * valorized with the values in the session.
 * <br/>
 * After processing, the LogContext values are stored in the session
 * so the next time a request with the same session is processed, those values are
 * restored.
 *
 * @version $Id: LogContextFilter.java,v 1.6 2007/02/14 13:48:11 nichele Exp $
 */
public class LogContextFilter implements Filter, Constants {

    // --------------------------------------------------------------- Constants

    // ---------------------------------------------------------- Public Methods
    /**
     * Initializes the filter
     * @param arg the configuration
     * @throws javax.servlet.ServletException if an error occurs
     */
    public void init(FilterConfig arg) throws ServletException {
    }

    /**
     * Filters the request
     * @param request the request
     * @param response the response
     * @param chain the filter chain
     * @throws java.io.IOException if an IO error occurs
     * @throws javax.servlet.ServletException if a Servlet error occurs
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        LogContext.clear();

        HttpServletRequest httprequest     = (HttpServletRequest)request;

        HttpSession        session         = httprequest.getSession();
        String             sessionId       = session.getId();

        Hashtable          previousContext =
            (Hashtable)session.getAttribute(SESSION_ATTRIBUTE_LOG_CONTEXT);

        if (previousContext != null) {
            //
            // Sets in the current log context the previous values
            //
            LogContext.setValues(previousContext);
        } else {
            LogContext.setSessionId(sessionId);
        }
        //
        // The thread id is always set
        //
        LogContext.setThreadId(String.valueOf(Thread.currentThread().getId()));

        try {
            // Continue processing the rest of the filter chain.
            chain.doFilter(request, response);
        } finally {
            //
            // Using Tomcat 5.5, processing the last client message, at this point
            // the session is invalidate (the Sync4jServlet invalidates the http session
            // if the syncml is completed).
            // If a session is invalidated, the setAttribute method throws an
            // exception.
            // From the servlet spec, request.getSession(false) returns null
            // if the session is expired/invalidated, so, if the current session
            // is null, the log context is not stored.
            //
            HttpSession currentSession =
                ((HttpServletRequest)request).getSession(false);

            if (currentSession != null) {
                //
                // Setting the current LogContext in the session in order to have the
                // values in the next request. We have to store a clone of that because
                // after that, the context is cleaned
                //
                session.setAttribute(SESSION_ATTRIBUTE_LOG_CONTEXT,
                                     LogContext.getValues().clone());
                LogContext.clear();
            }
        }
    }

    /**
     * Destroys the filter
     */
    public void destroy() {
    }
}
