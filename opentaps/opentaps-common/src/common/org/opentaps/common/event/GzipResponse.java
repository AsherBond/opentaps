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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.ofbiz.base.util.Debug;

/**
 * Gzip response for gzip the transfer data between client and server.
 */
public class GzipResponse extends HttpServletResponseWrapper {
    private static final String MODULE = GzipResponse.class.getName();
    protected HttpServletResponse response;   
    private ServletOutputStream out;   
    private GzipStream compressedOut;  
    private PrintWriter writer;   
    protected int contentLength;   

    /** {@inheritDoc} */
    public GzipResponse(HttpServletResponse response) throws IOException {   
       super(response);
       this.response = response;   
       compressedOut = new GzipStream(response.getOutputStream()); 
    }

    /** {@inheritDoc} */
    public void setContentLength(int len) { 
       contentLength = len;
    }

    /** {@inheritDoc} */
    public ServletOutputStream getOutputStream() throws IOException {   
       if (null == out) {   
           if (null != writer) {  
              throw new IllegalStateException("getWriter() has already been called on this response.");   
           }
           out = compressedOut;   
       }
       return out; 
    }

    /** {@inheritDoc} */
    public PrintWriter getWriter() throws IOException {   
       if (null == writer) {   
           if (null != out) {   
              throw new IllegalStateException("getOutputStream() has already been called on this response.");
           }
           writer = new PrintWriter(compressedOut);  
       }
       return writer;   
    }

    /** {@inheritDoc} */
    public void flushBuffer() {   
       try {   
           if (writer != null) {
              writer.flush();
           } else if (out != null) {  
              out.flush();   
           }
       } catch (IOException e) {  
           Debug.logError(e, MODULE);   
       }
    }

    /** {@inheritDoc} */
    public void reset() {
       super.reset();   
       try {   
           compressedOut.reset();   
       } catch (IOException e) {  
           throw new RuntimeException(e);   
       }
    }

    public void resetBuffer() {   
       super.resetBuffer();   
       try {   
           compressedOut.reset();   
       } catch (IOException e) {  
           throw new RuntimeException(e);
       }
    }

    public void close() throws IOException {   
       compressedOut.close();   
    }
}
