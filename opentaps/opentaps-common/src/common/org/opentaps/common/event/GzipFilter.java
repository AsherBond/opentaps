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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.ofbiz.base.util.Debug;

/**
 * Gzip filter, add gzip Content-Encoding, and using .js.gzip to replace the .js request.
 */
public class GzipFilter implements Filter {

    /** {@inheritDoc} */
    public void destroy() {
    }

    /** {@inheritDoc} */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponseWrapper response = new HttpServletResponseWrapper((HttpServletResponse) resp);
        String encoding = request.getHeader("Accept-Encoding");    
        boolean supportsGzip = false;
        // Now, check to see if the browser supports the GZIP compression
        if (encoding != null) {
          if (encoding.toLowerCase().indexOf("gzip") > -1) 
            supportsGzip = true;
        }
        Debug.log("supportsGzip : " + supportsGzip + ", encoding : " + encoding + ", requestURL : " + request.getRequestURL());
        if (supportsGzip) {
            // add content encoding
            response.setHeader("Content-Encoding", "gzip");
            GzipResponse compressionResponse= new GzipResponse(response);   
            chain.doFilter(request, compressionResponse);   
            compressionResponse.close();   
        } else {
            chain.doFilter(req, resp);
        }
    }

    /** {@inheritDoc} */
    public void init(FilterConfig config) throws ServletException {
    }
}
