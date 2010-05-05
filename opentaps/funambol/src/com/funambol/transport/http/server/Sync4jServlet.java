/**
 * Copyright (C) 2003-2007 Funambol
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

import java.io.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;

import com.funambol.framework.core.*;
import com.funambol.framework.engine.SyncStrategy;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.protocol.ProtocolException;
import com.funambol.framework.security.Officer;
import com.funambol.framework.server.*;
import com.funambol.framework.server.store.PersistentStore;
import com.funambol.framework.tools.IOTools;

import com.funambol.server.config.Configuration;

/**
 * Receives the HTTP request and does session management.
 *
 * <b>Session Management</b>
 * <p>
 * If the url contains a parameter {PARAM_SESSION_ID}={SESSION_ID}, the session
 * id is used to lookup in the <i>handlerCache</i> if there is already a
 * <i>SyncHolder</i> associated with the given session id it is used to process
 * the incoming request. Otherwise (either if the session parameter is not
 * specified or not cached handler is found), a new session id is created and a
 * new <i>SyncHolder</i> object is instantiated and stored in the cache.<br/>
 * The sessionId is created by the <code>SessionTools</code> using, if there are,
 * the request attribute
 * <ui>
 * <li><code>funambol.server_port</code></li>
 * <li><code>funambol.server_ip</code></li>
 * </ui>
 * <br/>
 *
 * Note that no session expiration is handled by this class. That is delegated
 * to the <i>SyncHolderCache</i> object.
 *
 * @version $Id: Sync4jServlet.java,v 1.36 2007/05/09 09:19:44 nichele Exp $
 */
public final class Sync4jServlet extends HttpServlet implements Constants {

    // ------------------------------------------------------------ Private data

    private FunambolLogger log = FunambolLoggerFactory.getLogger(LOG_NAME);

    private static String syncHolderClass = null;

    //
    // To check if the engine was initialized
    //
    private boolean initialized = false;

    private static final int SIZE_INPUT_BUFFER = 4096;

    private static boolean logMessages    = false;
    private static String  dirlogMessages = null;

    //
    // The session timeout in seconds
    // (default value is 15 minutes...15*60 seconds)
    //
    private static int sessionTimeout = 15 * 60; // 10 minutes

    private static String preferredEncoding = COMPRESSION_TYPE_DEFLATE;
    private static String supportedEncoding = null;

    //
    // The default compression level of the Deflater class
    //
    private static int compressionLevel = -1;

    // ---------------------------------------------------------- Public methods

    public void init() throws ServletException {
        initialized = false;
        String value;

        if (log.isInfoEnabled()) {
            System.getProperties().list(System.out);
            System.out.println("================================================================================");
        }

        ServletConfig config = getServletConfig();

        value = config.getInitParameter(PARAM_SYNCHOLDER_CLASS);

        if (StringUtils.isEmpty(value)) {
            String msg = "The servlet configuration parameter "
            + PARAM_SYNCHOLDER_CLASS
            + " cannot be empty ("
            + value
            + ")"
            ;
            log(msg);
            throw new ServletException(msg);
        }
        syncHolderClass = value;

        value = config.getInitParameter(PARAM_SESSION_TIMEOUT);
        if (!StringUtils.isEmpty(value)) {
            try {
                sessionTimeout = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                String msg = "The servlet configuration parameter "
                + PARAM_SESSION_TIMEOUT
                + " is not an integer number ("
                + value
                + ")"
                ;
                log.fatal(msg);
                throw new ServletException(msg);
            }
        }

        value = config.getInitParameter(PARAM_LOG_MESSAGES);
        if (value.equalsIgnoreCase("true")) {
            logMessages = true;
            dirlogMessages = config.getInitParameter(PARAM_DIRLOG_MESSAGES);
        }

        value = config.getInitParameter(PARAM_PREFERRED_ENCODING);
        if (!StringUtils.isEmpty(value)) {
            preferredEncoding = value;
        }

        value = config.getInitParameter(PARAM_SUPPORTED_ENCODING);
        if (!StringUtils.isEmpty(value)) {
            supportedEncoding = value;
        }

        value = config.getInitParameter(PARAM_COMPRESSION_LEVEL);
        if (!StringUtils.isEmpty(value)) {
            try {
                compressionLevel = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                String msg = "The servlet configuration parameter "
                + PARAM_COMPRESSION_LEVEL
                + " is not an integer number ("
                + value
                + ")"
                ;
                log.fatal(msg);
                throw new ServletException(msg);
            }
        }



        //
        // Now we are ready to bootstrap the Funambol components
        //
        bootstrap();

        //
        // Engine initialized!
        //
        initialized = true;
    }


    public void doPost(final HttpServletRequest  httpRequest ,
                       final HttpServletResponse httpResponse)
    throws ServletException, IOException {

        if (log.isInfoEnabled()) {
            log.info("Handling incoming request");
        }

        String requestURL          = getRequestURL(httpRequest);
        String sessionId           = getSessionId(httpRequest);
        String requestedSessionId  = httpRequest.getRequestedSessionId();
        if (log.isInfoEnabled()) {
            log.info("Request URL: "         + requestURL);
            log.info("Requested sessionId: " + requestedSessionId);
        }

        if (log.isTraceEnabled()) {
            showHeaders(httpRequest);
        }

        if (httpRequest.getSession().isNew()) {
            if (requestedSessionId != null && !requestedSessionId.equals("")) {
                if (!sessionId.equalsIgnoreCase(requestedSessionId)) {
                    //
                    // The client requires a session the is already expired...
                    // returing a 408
                    //
                    if (log.isInfoEnabled()) {
                        log.info("Session '" + requestedSessionId + "' already expired");
                    }
                    httpResponse.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                    return;
                }
            }
            httpRequest.getSession().setMaxInactiveInterval(sessionTimeout);
        }


        long requestTime  = System.currentTimeMillis();

        //
        // Setting the header 'Set-Cookie' in order to avoid the session tracking
        // using cookies.
        //
        // The web container adds a cookie JSESSIONID in order to
        // track the session, and to do that, it adds (transparently) in the response
        // header:
        // Set-Cookie: JSESSIONID=xxxxxxxxxx
        // In order not to use the cookie, the header "Set-Cookie" is set to an empty value
        // In this way, the session tracking is based on the jsessionid parameter
        // specified in the url (url rewriting).
        // The cookie is dangerous because a client could use:
        // http://xxxxxx:yyy/funambol/ds
        // but with a jsessionid specified as cookie. In this way, the server
        // search a previous session with the same id. And if a previous session
        // was interrupted and not expired, the server reuses this one and this
        // can cause an exception because the client sends the msg 1 and maybe
        // the previous session was in the mapping state.
        //
        httpResponse.setHeader("Set-Cookie", "");

        final String contentType = httpRequest.getContentType().split(";")[0];
        final String acceptEncoding  =
            (String)httpRequest.getHeader(HEADER_ACCEPT_ENCODING);
        final String contentEncoding =
            (String)httpRequest.getHeader(HEADER_CONTENT_ENCODING);
        final String sizeThreshold =
            (String)httpRequest.getHeader(HEADER_SIZE_THRESHOLD);

        Map    params     = getRequestParameters(httpRequest);
        Map    headers    = getRequestHeaders   (httpRequest);

        byte[] requestData = null;
        try {
            requestData = getRequestContent(httpRequest    ,
                                            contentEncoding,
                                            requestTime    ,
                                            sessionId      );
        } catch (Exception e) {
            handleError(httpRequest, httpResponse, "Error reading the request", e);
            return;
        }

        //
        // If the session id is not specified in the URL, a new remote object
        // will be created. Otherwise the session id specifies which remote
        // object shall handles the request.
        //
        SyncHolder holder = null;

        try {
            holder = createHolder(httpRequest.getSession());

        } catch (Exception e) {
            handleError(httpRequest, httpResponse, "Error creating SyncBean", e);
            return;
        }

        SyncResponse resp = null;
        try {
            if (com.funambol.framework.core.Constants.MIMETYPE_SYNCMLDS_WBXML.equals(contentType)) {
                resp = holder.processWBXMLMessage(requestURL, requestData, params, headers);
            } else if (com.funambol.framework.core.Constants.MIMETYPE_SYNCMLDS_XML.equals(contentType)) {
                resp = holder.processXMLMessage(requestURL, requestData, params, headers);
            } else {
                throw new ProtocolException( "Mime type "
                                           + contentType
                                           + " not supported or unknown" );
            }
        } catch (Exception e) {
            log.error("Error processing the request", e);

            Throwable cause = e.getCause();

            if ( (cause != null) &&
                 ((cause instanceof ProtocolException) || (cause instanceof Sync4jException)) ) {

                handleError(httpRequest, httpResponse, "Protocol error", cause);
                return;
            } else {
                throw new ServletException(e);
            }
        }

        httpResponse.setContentType(contentType);
        setResponseContent(httpResponse, acceptEncoding, sizeThreshold, resp, requestTime, sessionId);

        if (log.isInfoEnabled()) {
            log.info("Request processed.");
        }

        //
        // If the message completed the SyncML communication, the session
        // must be closed and discarded.
        //
        if (resp.isCompleted()) {
            closeSession(httpRequest.getSession());
        }

    }

    public void doGet(final HttpServletRequest  httpRequest ,
                      final HttpServletResponse httpResponse)
    throws ServletException, IOException {

        String pathInfo   = httpRequest.getPathInfo();

        if (PATH_INFO_STATUS.equals(pathInfo)) {
            if (initialized) {
                //
                //The request sent by the client was successful
                //
                httpResponse.setStatus(httpResponse.SC_OK);
                if (log.isTraceEnabled()) {
                    log.trace("Request succeeded normally");
                }
            } else {
                //
                //The request was unsuccessful to the server being down or overloaded.
                //
                httpResponse.setStatus(httpResponse.SC_SERVICE_UNAVAILABLE);
                if (log.isTraceEnabled()) {
                    log.trace("The HTTP server is unable to handle the request");
                }
            }
        }
    }

    // ------------------------------------------------------- Protected methods

    // --------------------------------------------------------- Private methods
    /**
     * Factory method for <i>SyncHolder</i> objects. If the underlying http session
     * contains a SyncHolder it is returned otherwise a new one is created and
     * stored in the session
     *
     * @param session the http session
     *
     * @return a <i>SyncHolder</i>
     *
     */
    private SyncHolder createHolder(HttpSession session)
    throws Exception {

        String sessionId = session.getId();
        SyncHolder holder = (SyncHolder)session.getAttribute(SESSION_ATTRIBUTE_SYNC_HOLDER);

        if (holder == null) {

            if (log.isTraceEnabled()) {
                log.trace("Holder not found. A new holder will be created");
            }

            holder = (SyncHolder)getClass().forName(syncHolderClass).newInstance();

            holder.setSessionId(sessionId);
            session.setAttribute(SESSION_ATTRIBUTE_SYNC_HOLDER, holder);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Holder found");
            }
        }

        return holder;
    }

    /**
     * Invalidates the http session
     *
     * @param session the session to close
     */
    private void closeSession(HttpSession session) {
        try {
            session.invalidate();
        } catch (Exception e) {
            log.error("Error closing the session", e);
        }
    }

    /**
     * Handles errors conditions returning an appropriate content to the client.
     *
     * @param request the request object
     * @param response the response object
     * @msg   a desctiptive message
     * @t     a throwable object
     *
     */
    private void handleError(final HttpServletRequest  request ,
                             final HttpServletResponse response,
                             String                    msg     ,
                             final Throwable           t       ) {

        if (msg == null) {
            msg = "";
        }

        if (t == null) {
            log.error(msg);
        } else {
            log.error(msg, t);
        }
        try {
            response.sendError(response.SC_BAD_REQUEST, msg);
        } catch (IOException e) {
            log.error("Error sending a BAD REQUEST code to the client", e);
        }
    }


    /**
     * Extracts the request parameters and returns them in a <i>Map</i>
     *
     * @param request the request
     */
    private Map getRequestParameters(HttpServletRequest request) {
        Map params = new HashMap();

        String paramName = null;
        for (Enumeration e=request.getParameterNames(); e.hasMoreElements(); ) {
            paramName = (String)e.nextElement();

            params.put(paramName, request.getParameter(paramName));
        }

        return params;
    }

    /**
     * Extracts the request headers and returns them in a <i>Map</i>
     *
     * @param request the request
     */
    private Map getRequestHeaders(HttpServletRequest request) {
        Map headers = new HashMap();

        String headerName = null;
        for (Enumeration e=request.getHeaderNames(); e.hasMoreElements(); ) {
            headerName = (String)e.nextElement();

            headers.put(headerName.toLowerCase(), request.getHeader(headerName));
        }

        return headers;
    }

    /**
     * Bootstraps the Funambol server components and logs the server startup
     * event on both the console and the logging system.
     */
    private void bootstrap() throws ServletException {
        try {
            Configuration c = null;

            c = Configuration.getConfiguration();
            c.initializeEngineComponents();

            String msg1 = "================================================================================";
            String sp   = "\n\n";
            String msg2 = "Funambol Data Synchronization Server v. "
                    + c.getServerConfig().getServerInfo().getSwV()
                    + " engine started.\nConfiguration object found."
                    ;

            //
            // NOTE: this code uses the stdout by choice, so that it will always
            // be displayed, regardless how logging is configured.
            //
            System.out.println(msg1);
            System.out.println(sp);
            System.out.println(msg2);
            System.out.println(sp);
            System.getProperties().list(System.out);
            System.out.println(sp);
            System.out.println(msg1);

            if (log.isInfoEnabled()) {
                log.info(msg1+sp);
                log.info(msg2+sp);
                log.info(System.getProperties().toString()+sp);
                log.info(msg1);
            }

            String store       = c.getServerConfig().getEngineConfiguration().getStore();
            PersistentStore ps = c.getStore();
            if (ps == null) {
                throw new ServletException("Fatal error creating the PersistentStore object");
            }

            String officer = c.getServerConfig().getEngineConfiguration().getOfficer();
            Officer off    = c.getOfficer();
            if (off == null) {
                throw new ServletException("Fatal error creating the Officer object");
            }

            String strategy  = c.getServerConfig().getEngineConfiguration().getStrategy();
            SyncStrategy str = c.getStrategy();
            if (str == null) {
                throw new ServletException("Fatal error creating the SyncStrategy object");
            }

            if (log.isInfoEnabled()) {
                log.info("Engine configuration:");

                log.info(" - store: "
                         + store
                         + " (" + ps + ")"
                        );

                log.info(" - officer: "
                         + officer
                         + " (" + off + ")"
                        );

                log.info(" - strategy: "
                         + strategy
                         + " (" + str + ")"
                        );

                log.info("Default encoding: "
                         + (new OutputStreamWriter(new ByteArrayOutputStream()))
                         .getEncoding()
                        );

                String serverURI =
                        c.getServerConfig().getEngineConfiguration().getServerURI();

                if (StringUtils.isEmpty(serverURI)) {
                    log.info("Server URI not set.");
                } else {
                    log.info("Server URI set: " + serverURI);
                }
            }
        } catch(Exception e) {
            log.error("Error bootstrapping Sync4jServlet", e);
            throw new ServletException("Error bootstrapping Sync4jServlet", e);
        }
    }

    /**
     * Log the headers of the request
     * @param request HttpServletRequest
     */
    private void showHeaders(HttpServletRequest request) {
        Enumeration enumHeaders = request.getHeaderNames();
        StringBuffer sb = new StringBuffer("Http header: \n");
        String headerName  = null;
        String headerValue = null;
        while (enumHeaders.hasMoreElements()) {
            headerName = (String)enumHeaders.nextElement();
            headerValue = request.getHeader(headerName);
            sb.append("> ").append(headerName);
            sb.append(": ").append(headerValue).append("\n");
        }
        log.trace(sb.toString());
    }

    /**
     * Returns the sessionId of the request. <br/>
     * The sessionId is extracted from the pathInfo or, if there isn't there,
     * we use the id of the <code>HttpSession</code>
     * @param request HttpServletRequest
     * @return String
     */
    private String getSessionId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String sessionId = null;

        if (StringUtils.isNotEmpty(pathInfo)) {
            int index = pathInfo.indexOf(";jsessionid=");
            if (index != -1) {
                sessionId = pathInfo.substring(index + 12);
            }
        }
        if (sessionId == null) {
            sessionId = request.getSession().getId();
        }

        return sessionId;
    }

    /**
     * Returns the request uri getting the request url and the query string
     * @param request HttpServletRequest
     * @return String
     */
    private String getRequestURL(HttpServletRequest request) {
        StringBuffer sb = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            sb.append('?').append(queryString);
        }
        return sb.toString();
    }

    /**
     * Returns the content of HTTP request.
     * Uncompresses the request if the Content-Encoding is gzip or deflate.
     * Updates the Content-Length with the length of the uncompressed request.
     *
     * @param httpRequest the HttpServletRequest
     * @param contentEncoding the content encoding
     * @param requestTime the time in which the request is arrived to servlet
     * @param sessionId the session identifier
     *
     * @return requestData the uncompressed request content
     */
    private byte[] getRequestContent(HttpServletRequest httpRequest    ,
                                     String             contentEncoding,
                                     long               requestTime   ,
                                     String             sessionId     )
    throws IOException {

        byte[]      requestData = null;
        InputStream in          = null;

        try {

            in = httpRequest.getInputStream();

            int contentLength = httpRequest.getContentLength();
            if (contentLength <= 0) {
                contentLength = SIZE_INPUT_BUFFER;
            }

            if (logMessages) {
                //
                // Read the compressed request data to log it
                //
                requestData = IOTools.readContent(in, contentLength);
                logRequest(httpRequest, requestData, requestTime, sessionId);

                //
                // Create a new InputStream to pass at the decompressor
                //
                in = new ByteArrayInputStream(requestData);
            }

            if (contentEncoding != null) {
                if (contentEncoding.equals(COMPRESSION_TYPE_GZIP)) {

                    if (log.isTraceEnabled()) {
                        log.trace("Reading the request using: " + COMPRESSION_TYPE_GZIP);
                    }
                    in = new GZIPInputStream(in);

                } else if (contentEncoding.equals(COMPRESSION_TYPE_DEFLATE)) {

                    if (log.isTraceEnabled()) {
                        log.trace("Reading the request using: " + COMPRESSION_TYPE_DEFLATE);
                    }
                    in = new InflaterInputStream(in);
                }

                String uncompressedContentLength =
                    httpRequest.getHeader("Uncompressed-Content-Length");

                if (uncompressedContentLength != null) {
                    contentLength = Integer.parseInt(uncompressedContentLength);
                } else {
                    contentLength = SIZE_INPUT_BUFFER;
                }

            }

            if (!logMessages || contentEncoding != null) {
                requestData = IOTools.readContent(in, contentLength);
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }

        return requestData;
    }

    /**
     * Sets the content of HTTP response.
     *
     * Compresses the response if the Accept-Encoding is gzip or deflate.
     * Sets the Content-Encoding according to the encoding used.
     * Sets the Content-Length with the length of the compressed response.
     * Sets the Uncompressed-Content-Length with the length of the uncompressed
     * response.
     * The response will be compressed only if the length of the uncompressed
     * response is greater than the give sizeThreashold.
     *
     * @param httpResponse the HttpServletResponse
     * @param requestAcceptEncoding the <code>Accept-Encoding</code> specified
     *                              in the request
     * @param sizeThreashold if the response is smaller of this value, it
     *                       should not be compressed
     * @param resp the SyncResponse object contains the response message
     * @param requestTime the time in which the request is arrived to servlet
     * @param sessionId the session identifier
     *
     */
    private void setResponseContent(HttpServletResponse httpResponse         ,
                                    String              requestAcceptEncoding,
                                    String              sizeThreshold        ,
                                    SyncResponse        resp                 ,
                                    long                requestTime          ,
                                    String              sessionId            )
    throws IOException {

        byte[] responseContent = null;

        OutputStream out = null;
        try {
            out = httpResponse.getOutputStream();

            responseContent = resp.getMessage();
            int uncompressedContentLength = responseContent.length;

            if (supportedEncoding != null && !"".equals(supportedEncoding)) {

                if (log.isTraceEnabled()) {
                    log.trace("Setting Accept-Encoding to " + supportedEncoding);
                }

                httpResponse.setHeader(HEADER_ACCEPT_ENCODING, supportedEncoding);
            }

            String encodingToUse = null;

            if (requestAcceptEncoding != null) {

                if (requestAcceptEncoding.indexOf(COMPRESSION_TYPE_GZIP)    != -1 &&
                    requestAcceptEncoding.indexOf(COMPRESSION_TYPE_DEFLATE) != -1) {

                    encodingToUse = preferredEncoding;

                } else if (requestAcceptEncoding.indexOf(COMPRESSION_TYPE_DEFLATE) != -1) {

                    encodingToUse = COMPRESSION_TYPE_DEFLATE;

                } else if (requestAcceptEncoding.indexOf(COMPRESSION_TYPE_GZIP) != -1) {

                    encodingToUse = COMPRESSION_TYPE_GZIP;

                }
            }

            int threshold = 0;
            try {
                if (sizeThreshold != null && sizeThreshold.length() != 0) {
                    threshold = Integer.parseInt(sizeThreshold);
                }
            } catch (NumberFormatException ex) {
                //
                // Ignoring the specified value
                //
                if (log.isTraceEnabled()) {
                    log.trace("The size threshold specified by the client (" +
                              sizeThreshold + ") is not valid. It is ignored");
                }
            }

            //
            // If the encodingToUse is null or the
            // uncompressed response length is less than
            // sizeThreshold, the response will not be compressed.
            //
            if (encodingToUse == null ||
                uncompressedContentLength < threshold) {

                if (log.isTraceEnabled()) {
                    if (requestAcceptEncoding == null) {
                        log.trace("The client doesn't support any encoding. " +
                                   "The response is not compressed");
                    } else if (encodingToUse == null) {
                        log.trace("The specified Accept-Encoding (" + requestAcceptEncoding +
                                   ") is not recognized. The response is not compressed");
                    }
                    else if (uncompressedContentLength < threshold) {
                        log.trace("The response is not compressed because smaller than " +
                                   threshold);
                    }
                }

                if (log.isTraceEnabled()) {
                    log.trace("Setting Content-Length to: " + uncompressedContentLength);
                }
                httpResponse.setContentLength(uncompressedContentLength);
                out.write(responseContent);
                out.flush();

                return;
            }

            if (encodingToUse != null) {

                if (log.isTraceEnabled()) {
                    log.trace("Compressing the response using: " + encodingToUse);
                    log.trace("Setting Uncompressed-Content-Length to: " + uncompressedContentLength);
                }

                httpResponse.setHeader(HEADER_UNCOMPRESSED_CONTENT_LENGTH,
                                       String.valueOf(uncompressedContentLength));


                if (encodingToUse.equals(COMPRESSION_TYPE_GZIP)) {

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    GZIPOutputStream outTmp = new GZIPOutputStream(bos);
                    outTmp.write(responseContent, 0, uncompressedContentLength);
                    outTmp.flush();
                    outTmp.close();

                    //
                    // Get the compressed data
                    //
                    responseContent = bos.toByteArray();
                    int compressedLength = responseContent.length;

                    if (log.isTraceEnabled()) {
                        log.trace("Setting Content-Length to: " + compressedLength);
                        log.trace("Setting Content-Encoding to: " + COMPRESSION_TYPE_GZIP);
                    }

                    httpResponse.setContentLength(compressedLength);
                    httpResponse.setHeader(HEADER_CONTENT_ENCODING, COMPRESSION_TYPE_GZIP);

                    out.write(responseContent);
                    out.flush();

                } else if (encodingToUse.equals(COMPRESSION_TYPE_DEFLATE)) {

                    //
                    // Create the compressor with specificated level of compression
                    //
                    Deflater compressor = new Deflater();
                    compressor.setLevel(compressionLevel);
                    compressor.setInput(responseContent);
                    compressor.finish();

                    //
                    // Create an expandable byte array to hold the compressed data.
                    // You cannot use an array that's the same size as the orginal because
                    // there is no guarantee that the compressed data will be smaller than
                    // the uncompressed data.
                    //
                    ByteArrayOutputStream bos =
                        new ByteArrayOutputStream(uncompressedContentLength);

                    //
                    // Compress the response
                    //
                    byte[] buf = new byte[SIZE_INPUT_BUFFER];
                    while (!compressor.finished()) {
                        int count = compressor.deflate(buf);
                        bos.write(buf, 0, count);
                    }

                    //
                    // Get the compressed data
                    //
                    responseContent = bos.toByteArray();
                    int compressedLength = responseContent.length;

                    if (log.isTraceEnabled()) {
                        log.trace("Setting Content-Length to: " + compressedLength);
                        log.trace("Setting Content-Encoding to: "  + COMPRESSION_TYPE_DEFLATE);
                    }

                    httpResponse.setContentLength(compressedLength);
                    httpResponse.setHeader(HEADER_CONTENT_ENCODING, COMPRESSION_TYPE_DEFLATE);

                    out.write(responseContent);
                    out.flush();
                }
            }

        } finally {
            if (out != null) {
                out.close();
            }

            if (logMessages) {
                logResponse(responseContent, requestTime, sessionId);
            }
        }
    }

    /**
     * Logs the parameters, the headers and the data (compressed or not) of the
     * request.
     *
     * @param httpResponse the HttpServletResponse
     * @param requestData the byte array of the content of the request
     *                    (compressed or not)
     * @param requestTime the time in which the request is arrived to servlet
     * @param sessionId the session identifier
     *
     */
    private void logRequest(HttpServletRequest httpRequest,
                            byte[]             requestData,
                            long               requestTime,
                            String             sessionId  )
    throws IOException {

        Map params  = getRequestParameters(httpRequest);
        Map headers = getRequestHeaders   (httpRequest);

        File fh = new File(dirlogMessages + File.separator +
                           "RequestParameters_"  +
                           requestTime           +
                           "_"                   +
                           sessionId             +
                           ".txt"
        );

        BufferedWriter out = new BufferedWriter(new FileWriter(fh));
        out.write("**** Request Parameters: \r\n");
        java.util.Iterator itp = params.keySet().iterator();
        while(itp.hasNext()) {
            String n = (String)itp.next();
            out.write(n + ":" + (String)params.get(n) + "\r\n");
        }

        out.write("**** Request Headers   : \r\n");
        java.util.Iterator ith = headers.keySet().iterator();
        while(ith.hasNext()) {
            String n = (String)ith.next();
            out.write(n + ":" + (String)headers.get(n) + "\r\n");
        }
        out.close();

        File f = new File(dirlogMessages + File.separator +
                          "RequestMsg_"  +
                          requestTime    +
                          "_"            +
                          sessionId      +
                          ".bin"
        );
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(requestData);
        fos.flush();
        fos.close();
    }

    /**
     * Logs the response content (compressed or not).
     *
     * @param responseContent the byte array with the response content
     *                        (compressed or not)
     * @param requestTime the time in which the request is arrived to servlet
     * @param sessionId the session identifier
     *
     */
    private void logResponse(byte[] responseContent,
                             long   requestTime    ,
                             String sessionId      )
    throws IOException {

        File f = new File(dirlogMessages + File.separator +
                          "ResponseMsg_" +
                          requestTime    +
                          "_"            +
                          sessionId      +
                          ".bin"
                 );
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(responseContent);
        fos.flush();
        fos.close();

    }
}
