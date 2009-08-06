/*
Copyright (c) 2006 - 2009 Open Source Strategies, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the Honest Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
Honest Public License for more details.

You should have received a copy of the Honest Public License
along with this program; if not, write to Funambol,
643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.common.event;

import javolution.util.*;

import org.apache.commons.lang.StringUtils;
import org.opentaps.common.pagination.PaginatorFactory;
import org.opentaps.common.pagination.Paginator;
import org.opentaps.common.pagination.PaginationState;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.builder.ListBuilder;
import org.opentaps.common.builder.ListBuilderException;
import org.ofbiz.base.util.Debug;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Events that interact with paginators to fetch pages, change the sort order,
 * and change the size of the viewport.
 * 
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public class PaginationEvents {

    public static final String module = PaginationEvents.class.getName();

    /**
     * Catchall request for fetching pages. It accepts a paginatorName and an
     * action to perform.
     */
    public static String paginate(HttpServletRequest request, HttpServletResponse response) {
        List page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        String action = UtilCommon.getParameter(request, "action");
        String pageNumberString = UtilCommon.getParameter(request, "pageNumber");

        if (paginator != null) {
            try {
                if (pageNumberString != null) {
                    try {
                        long pageNumber = Long.parseLong(pageNumberString);
                        page = paginator.getPageNumber(pageNumber);
                    }
                    catch (NumberFormatException e) {
                        Debug.logWarning("Failed to get page numer [" + pageNumberString + "] to to format error: " + e.getMessage(), module);
                        page = paginator.getCurrentPage();
                    }
                }
                else if (action == null || "getCurrentPage".equals(action)) {
                    page = paginator.getCurrentPage();
                }
                else if ("getNextPage".equals(action)) {
                    page = paginator.getNextPage();
                }
                else if ("getPreviousPage".equals(action)) {
                    page = paginator.getPreviousPage();
                }
                else if ("getFirstPage".equals(action)) {
                    page = paginator.getFirstPage();
                }
                else if ("getLastPage".equals(action)) {
                    page = paginator.getLastPage();
                }
                else {
                    Debug.logWarning("Paginate action [" + action + "] not supported.", module);
                    page = paginator.getCurrentPage();
                }
            }
            catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    public static String changePaginationOrder(HttpServletRequest request, HttpServletResponse response) {
        List page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        if (paginator != null) {
            String orderByString = UtilCommon.getParameter(request, "orderBy");
            String orderByReverseString = UtilCommon.getParameter(request, "orderByReverse");
            List orderBy = PaginationState.loadOrderBy(orderByString);
            List orderByReverse = PaginationState.loadOrderBy(orderByReverseString);

            if (orderByReverse == null) {
                paginator.changeOrderBy(orderBy);
            }
            else {
                paginator.changeOrderBy(orderBy, orderByReverse);
            }
            try {
                page = paginator.getCurrentPage();
            }
            catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    public static String changePaginationViewSize(HttpServletRequest request, HttpServletResponse response) {
        List page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        if (paginator != null) {
            long delta = 0;
            String deltaValue = UtilCommon.getParameter(request, "delta");
            if (deltaValue != null) {
                try {
                    delta = Long.parseLong(deltaValue);
                }
                catch (NumberFormatException e) {
                    Debug.logError(e, e.getMessage(), module);
                }
            }
            if (delta != 0)
                paginator.changeViewSize(delta);
            try {
                page = paginator.getCurrentPage();
            }
            catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    /**
     * Decides whether to do a JSON response or set the values in the request
     * attributes for formlets.
     */
    public static String doPaginationResponse(HttpServletRequest request, HttpServletResponse response, Paginator paginator, List page) {
        if (paginator == null)
            return "success"; // Formlet event handler will deal with this
        if (paginator.isFormlet()) {
            return doPaginationResponseFormlet(request, paginator, page);
        }
        else {
            return doPaginationResponseJSON(response, paginator, page);
        }
    }

    /**
     * Builds a JSONObject response string with the following parameters:
     * 
     * <ul>
     * <li><b>page:</b> JSONArray containing the page to render.</li>
     * <li><b>pageNumber:</b> The page number.</li>
     * <li><b>totalPages:</b> Total pages in the list.</li>
     * <li><b>viewSize:</b> The size of the viewport.</li>
     * </li>
     */
    public static String doPaginationResponseJSON(HttpServletResponse response, Paginator paginator, List page) {
        JSONObject map = new JSONObject();
        map.put("pageRows", JSONArray.fromCollection(page).toString());
        if (paginator != null) {
            map.put("pageNumber", new Long(paginator.getPageNumber()));
            map.put("totalPages", new Long(paginator.getTotalPages()));
            map.put("viewSize", new Long(paginator.getViewSize()));
        }
        return AjaxEvents.doJSONResponse(response, map);
    }

    /**
     * Stores result data in the request attributes for the formlet.
     * 
     * Other data about the paginator and context are put in the template
     * context by FormletEventHandler, therefore we only care to return
     * information about the actual list of data.
     */
    public static String doPaginationResponseFormlet(HttpServletRequest request, Paginator paginator, List page) {
        request.setAttribute("pageRows", page);
        return "success";
    }

    // when the pagination crashes due to some list building error, such as bad
    // code in a bsh closure, we note this and let FormletEventHandler decide
    // what to do
    private static String doListBuilderExceptionResponse(HttpServletRequest request, HttpServletResponse response, Paginator paginator, ListBuilderException e) {
        request.setAttribute("listBuilderException", e); // save the exception
        return "listBuilderException"; // FormletEventHandler should check for this
    }

    /**
     * Export a paginated data list to Excel.
     * 
     * @param request
     *            the servlet request
     * @param response
     *            servlet response
     * @return a String object that represents the process status
     */

    public static String renderPaginatedListAsExcel(HttpServletRequest request, HttpServletResponse response) {
        String strPaginatorName = request.getParameter("paginatorName");
        String strOpentapsApplicationName = request.getParameter("opentapsApplicationName");
        if (StringUtils.isEmpty(strPaginatorName)) {
            Debug.logError("The parameter [paginatorName] is missing in the request.", module);
            return "error";
        }

        if (StringUtils.isEmpty(strOpentapsApplicationName)) {
            Debug.logError("The parameter [opentapsApplicationName] is missing in the request.", module);
            return "error";
        }

        String retval = exportPaginatedListToExcel(strPaginatorName, strOpentapsApplicationName, request, response);

        return retval;
    }

    private static String exportPaginatedListToExcel(final String strPaginatorName, final String strOpentapsApplicationName, HttpServletRequest request, HttpServletResponse response) {
        Paginator paginator = PaginatorFactory.getPaginator(request.getSession(), strPaginatorName, strOpentapsApplicationName);
        if (paginator == null) {
            Debug.logError("Failed to retrieve the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", module);
            return "error";
        }

        // paginator exists in session
        ListBuilder listBuilder = paginator.getListBuilder();
        if (listBuilder == null) {
            Debug.logError("Null list builder is found in the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", module);
            return "error";
        }

        List dataList = null;
        try {
            dataList = listBuilder.getCompleteList();
        }
        catch (ListBuilderException lbe) {
            Debug.logError("ListBuilderException is caught while exporting the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "] : " + lbe.getMessage(), module);
            return "error";
        }

        if (dataList == null || dataList.isEmpty()) {
            Debug.logWarning("Empty data list is returned in the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", module);
            return "error";
        }

        // write the data list to Excel

        // get the field key list
        Map element = (Map) dataList.get(0);
        List<String> keys = new ArrayList<String>();
        if (element != null) {
            Set<String> keySet = element.keySet();
            for (String key : keySet) {
                if ("lastUpdatedStamp".equals(key) ||
                    "lastUpdatedTxStamp".equals(key) ||
                    "createdStamp".equals(key) ||
                    "createdTxStamp".equals(key)) {
                    continue;
                }
                
                keys.add(key);
            }
        }

        if (keys.isEmpty()) {
            Debug.logError("Empty field name list is returned in the data list of paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", module);
            return "error";
        }

        // create a map for column header labels
        Map columnHeaderMap = new HashMap();
        for (String key : keys) {
            columnHeaderMap.put(key, key);
        }

        List excelDataList = new ArrayList();
        excelDataList.add(columnHeaderMap);
        excelDataList.addAll(dataList);

        // using random string as part of file name, avoid multi user operate in same time.
        String fileName = "data_" + String.valueOf((int) (Math.random() * 100000)) + ".xls";
        String excelFilePath = getAbsoluteFilePath(request, fileName);
        try {
            UtilCommon.saveToExcel(excelFilePath, "data", keys, excelDataList);
        }
        catch (IOException ioe) {
            Debug.logError("IOException is thrown while trying to write to the Excel file: " + ioe.getMessage(), module);
            return "error";
        }

        return downloadExcel(fileName, request, response);
    }

    /**
     * Download an existing Excel file from the ${opentaps_home}/runtime/output
     * directory. The Excel file is deleted after the download.
     * 
     * @param filename
     *            the file name String object
     * @param response
     *            servlet response
     */
    private static String downloadExcel(String filename, HttpServletRequest request, HttpServletResponse response) {
        File file = null;
        ServletOutputStream out = null;
        FileInputStream fileToDownload = null;

        try {
            out = response.getOutputStream();

            file = new File(getAbsoluteFilePath(request, filename));
            fileToDownload = new FileInputStream(file);

            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(fileToDownload.available());

            int c;
            while ((c = fileToDownload.read()) != -1) {
                out.write(c);
            }

            out.flush();
        } catch (FileNotFoundException e) {
            Debug.logError("Failed to open the file: " + filename, module);
            return "error";
        } catch (IOException ioe) {
            Debug.logError("IOException is thrown while trying to download the Excel file: " + ioe.getMessage(), module);
            return "error";
        } finally {
            try {
                out.close();
                if (fileToDownload != null) {
                    fileToDownload.close();
                    // Delete the file under /runtime/output/ this is optional
                    file.delete();
                }
            } catch (IOException ioe) {
                Debug.logError("IOException is thrown while trying to download the Excel file: " + ioe.getMessage(), module);
                return "error";
            }
        }

        return "success";
    }

    private static String getAbsoluteFilePath(HttpServletRequest request, final String filename) {
        ServletContext servletContext = request.getSession().getServletContext();
        String rootPath = servletContext.getRealPath("../../../../");
        String filePath = "/runtime/output/";
        return rootPath + filePath + filename;
    }

}
