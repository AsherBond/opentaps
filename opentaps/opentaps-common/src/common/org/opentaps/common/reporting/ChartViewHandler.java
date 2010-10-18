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
package org.opentaps.common.reporting;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.servlet.ServletUtilities;
import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.opentaps.common.util.UtilCommon;

/**
 * This view handler helps to include chart image to page
 * using tag <img/>
 */
public class ChartViewHandler extends AbstractViewHandler {

    public static String module = ChartViewHandler.class.getName();

    /* (non-Javadoc)
     * @see org.ofbiz.webapp.view.ViewHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) {
    }

    /**
     * Encode chart as image/png and send it to browser
     *
     * @param chartContext
     * @param response
     * @throws IOException
     */
    protected void sendChart(Map<String, Object> chartContext, HttpServletResponse response) throws IOException {

        JFreeChart chartObject      = (JFreeChart) chartContext.get("chartObject");
        Integer width               = (Integer) chartContext.get("width");
        Integer height              = (Integer) chartContext.get("height");
        Boolean encodeAlpha         = (Boolean) chartContext.get("alphaCompression");
        Integer compressRatio       = (Integer) chartContext.get("compressRatio");

        if (chartObject != null && width.compareTo(0) > 0 && height.compareTo(0) > 0) {
            response.setContentType("image/png");
            if (encodeAlpha != null || (compressRatio != null && compressRatio.intValue() >= 0 && compressRatio.intValue() <= 9)) {
                ChartUtilities.writeChartAsPNG(response.getOutputStream(), chartObject, width, height, encodeAlpha.booleanValue(), compressRatio.intValue());
            } else {
                ChartUtilities.writeChartAsPNG(response.getOutputStream(), chartObject, width, height);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        /*
         * Looks for parameter "chart" first. Send this temporary image files
         * to client if it exists and return.
         */
        String chartFileName = UtilCommon.getParameter(request, "chart");
        if (UtilValidate.isNotEmpty(chartFileName)) {
            try {
                ServletUtilities.sendTempFile(chartFileName, response);
                if (chartFileName.indexOf(ServletUtilities.getTempOneTimeFilePrefix()) != -1) {
                    // delete temporary file
                    File file = new File(System.getProperty("java.io.tmpdir"), chartFileName);
                    file.delete();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe.getLocalizedMessage(), module);
            }
            return;
        }

        /*
         * Next option eliminate need to store chart in file system. Some event handler
         * included in request chain prior to this view handler can prepare ready to use
         * instance of JFreeChart and place it to request attribute chartContext.
         * Currently chartContext should be Map<String, Object> and we expect to see in it:
         *   "charObject"       : JFreeChart
         *   "width"            : positive Integer
         *   "height"           : positive Integer
         *   "encodeAlpha"      : Boolean (optional)
         *   "compressRatio"    : Integer in range 0-9 (optional)
         */
        Map<String, Object> chartContext = (Map<String, Object>)request.getAttribute("chartContext");
        if (UtilValidate.isNotEmpty(chartContext)) {
            try {
                sendChart(chartContext, response);
            } catch (IOException ioe) {
                Debug.logError(ioe.getLocalizedMessage(), module);
            }
            return;
        }

        /*
         * Prepare context for next options
         */
        Map<String, Object> callContext = FastMap.newInstance();
        callContext.put("parameters", UtilHttp.getParameterMap(request));
        callContext.put("delegator", request.getAttribute("delegator"));
        callContext.put("dispatcher", request.getAttribute("dispatcher"));
        callContext.put("userLogin", request.getSession().getAttribute("userLogin"));
        callContext.put("locale", UtilHttp.getLocale(request));

        /*
         * view-map attribute "page" may contain BeanShell script in component
         * URL format that should return chartContext map.
         */
        if (UtilValidate.isNotEmpty(page) && UtilValidate.isUrl(page) && page.endsWith(".bsh")) {
            try {
                chartContext = (Map<String, Object>)BshUtil.runBshAtLocation(page, callContext);
                if (UtilValidate.isNotEmpty(chartContext)) {
                    sendChart(chartContext, response);
                }
            } catch (GeneralException ge) {
                Debug.logError(ge.getLocalizedMessage(), module);
            } catch (IOException ioe) {
                Debug.logError(ioe.getLocalizedMessage(), module);
            }
            return;
        }

        /*
         * As last resort we can decide that "page" attribute contains class name and "info"
         * contains method Map<String, Object> getSomeChart(Map<String, Object> context).
         * There are parameters, delegator, dispatcher, userLogin and locale in the context.
         * Should return chartContext.
         */
        if (UtilValidate.isNotEmpty(page) && UtilValidate.isNotEmpty(info)) {
            Class handler =  null;
            synchronized(this) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try {
                    handler = loader.loadClass(page);
                    if (handler != null) {
                        Method runMethod = handler.getMethod(info, new Class[] {Map.class});
                        chartContext = (Map<String, Object>) runMethod.invoke(null, callContext);
                        if (UtilValidate.isNotEmpty(chartContext)) {
                            sendChart(chartContext, response);
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    Debug.logError(cnfe.getLocalizedMessage(), module);
                } catch (SecurityException se) {
                    Debug.logError(se.getLocalizedMessage(), module);
                } catch (NoSuchMethodException nsme) {
                    Debug.logError(nsme.getLocalizedMessage(), module);
                } catch (IllegalArgumentException iae) {
                    Debug.logError(iae.getLocalizedMessage(), module);
                } catch (IllegalAccessException iace) {
                    Debug.logError(iace.getLocalizedMessage(), module);
                } catch (InvocationTargetException ite) {
                    Debug.logError(ite.getLocalizedMessage(), module);
                } catch (IOException ioe) {
                    Debug.logError(ioe.getLocalizedMessage(), module);
                }
            }
        }

        // Why you disturb me?
        throw new ViewHandlerException("In order to generate chart you have to provide chart object or file name. There are no such data in request. Please read comments to ChartViewHandler class.");
    }

}
