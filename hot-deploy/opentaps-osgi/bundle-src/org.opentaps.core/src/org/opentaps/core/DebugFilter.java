/*
 * Copyright (c) 2006 - 2010 Open Source Strategies, Inc.
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
package org.opentaps.core;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * <p>A filter sample class.<br>
 * Do nothing special but may be useful for debugging.</p>
 * TODO: log more detailed information about request.
 */
public class DebugFilter implements Filter {

    /** {@inheritDoc} */
    public void destroy() {
        Activator.getInstance().logInfo("Destroyed filter", null, null);
    }

    /** {@inheritDoc} */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Activator.getInstance().logInfo("Filter request [" + request + "]", null, null);
        chain.doFilter(request, response);
    }

    /** {@inheritDoc} */
    public void init(FilterConfig config) throws ServletException {
        Activator.getInstance().logInfo("Init with config [" + config + "]", null, null);
    }

}
