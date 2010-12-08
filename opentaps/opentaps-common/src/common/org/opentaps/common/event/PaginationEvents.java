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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.party.party.PartyHelper;
import org.opentaps.common.builder.ListBuilder;
import org.opentaps.common.builder.ListBuilderException;
import org.opentaps.common.pagination.PaginationState;
import org.opentaps.common.pagination.Paginator;
import org.opentaps.common.pagination.PaginatorFactory;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * Events that interact with paginators to fetch pages, change the sort order,
 * and change the size of the viewport.
 */
public final class PaginationEvents {

    private PaginationEvents() { }

    private static final String MODULE = PaginationEvents.class.getName();

    /**
     * Catchall request for fetching pages. It accepts a paginatorName and an
     * action to perform.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String paginate(HttpServletRequest request, HttpServletResponse response) {
        List<?> page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        String action = UtilCommon.getParameter(request, "action");
        String pageNumberString = UtilCommon.getParameter(request, "pageNumber");

        if (paginator != null) {
            try {
                if (pageNumberString != null) {
                    try {
                        long pageNumber = Long.parseLong(pageNumberString);
                        page = paginator.getPageNumber(pageNumber);
                    } catch (NumberFormatException e) {
                        Debug.logWarning("Failed to get page numer [" + pageNumberString + "] to to format error: " + e.getMessage(), MODULE);
                        page = paginator.getCurrentPage();
                    }
                } else if (action == null || "getCurrentPage".equals(action)) {
                    page = paginator.getCurrentPage();
                } else if ("getNextPage".equals(action)) {
                    page = paginator.getNextPage();
                } else if ("getPreviousPage".equals(action)) {
                    page = paginator.getPreviousPage();
                } else if ("getFirstPage".equals(action)) {
                    page = paginator.getFirstPage();
                } else if ("getLastPage".equals(action)) {
                    page = paginator.getLastPage();
                } else {
                    Debug.logWarning("Paginate action [" + action + "] not supported.", MODULE);
                    page = paginator.getCurrentPage();
                }
            } catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    /**
     * Changes the paginator order by.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String changePaginationOrder(HttpServletRequest request, HttpServletResponse response) {
        List<?> page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        if (paginator != null) {
            String orderByString = UtilCommon.getParameter(request, "orderBy");
            String orderByReverseString = UtilCommon.getParameter(request, "orderByReverse");
            List<String> orderBy = PaginationState.loadOrderBy(orderByString);
            List<String> orderByReverse = PaginationState.loadOrderBy(orderByReverseString);

            if (orderByReverse == null) {
                paginator.changeOrderBy(orderBy);
            } else {
                paginator.changeOrderBy(orderBy, orderByReverse);
            }
            try {
                page = paginator.getCurrentPage();
            } catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    /**
     * Changes the paginator view size.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String changePaginationViewSize(HttpServletRequest request, HttpServletResponse response) {
        List<?> page = FastList.newInstance();
        Paginator paginator = PaginatorFactory.getPaginator(request);
        if (paginator != null) {
            long delta = 0;
            long size = 0;
            String deltaValue = UtilCommon.getParameter(request, "delta");
            String newSize = UtilCommon.getParameter(request, "newSize");
            String toggleViewAll = UtilCommon.getParameter(request, "toggleViewAll");
            if (deltaValue != null) {
                try {
                    delta = Long.parseLong(deltaValue);
                } catch (NumberFormatException e) {
                    Debug.logError(e, e.getMessage(), MODULE);
                }
                if (delta != 0) {
                    paginator.changeViewSize(delta);
                }
            } else if (newSize != null) {
                try {
                    size = Long.parseLong(newSize);
                } catch (NumberFormatException e) {
                    Debug.logError(e, e.getMessage(), MODULE);
                }
                if (size != 0) {
                    paginator.setViewSize(size);
                }
            } else if (toggleViewAll != null) {
                paginator.toggleViewAll();
            }

            try {
                page = paginator.getCurrentPage();
            } catch (ListBuilderException e) {
                return doListBuilderExceptionResponse(request, response, paginator, e);
            }
        }
        return doPaginationResponse(request, response, paginator, page);
    }

    /**
     * Decides whether to do a JSON response or set the values in the request
     * attributes for formlets.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @param paginator a <code>Paginator</code> value
     * @param page a <code>List</code> value
     * @return a <code>String</code> value
     */
    public static String doPaginationResponse(HttpServletRequest request, HttpServletResponse response, Paginator paginator, List<?> page) {
        if (paginator == null) {
            return "success"; // Formlet event handler will deal with this
        }
        if (paginator.isFormlet()) {
            return doPaginationResponseFormlet(request, paginator, page);
        } else {
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
     * </ul>
     * @param response a <code>HttpServletResponse</code> value
     * @param paginator a <code>Paginator</code> value
     * @param page a <code>List</code> value
     * @return a <code>String</code> value
     */
    public static String doPaginationResponseJSON(HttpServletResponse response, Paginator paginator, List<?> page) {
        JSONObject map = new JSONObject();
        map.put("pageRows", JSONArray.fromObject(page).toString());
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
     * @param request a <code>HttpServletRequest</code> value
     * @param paginator a <code>Paginator</code> value
     * @param page a <code>List</code> value
     * @return a <code>String</code> value
     */
    public static String doPaginationResponseFormlet(HttpServletRequest request, Paginator paginator, List<?> page) {
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
     * @param request the servlet request
     * @param response servlet response
     * @return a String object that represents the process status
     */

    public static String renderPaginatedListAsExcel(HttpServletRequest request, HttpServletResponse response) {
        String strPaginatorName = request.getParameter("paginatorName");
        String strOpentapsApplicationName = request.getParameter("opentapsApplicationName");
        if (StringUtils.isEmpty(strPaginatorName)) {
            Debug.logError("The parameter [paginatorName] is missing in the request.", MODULE);
            return "error";
        }

        if (StringUtils.isEmpty(strOpentapsApplicationName)) {
            Debug.logError("The parameter [opentapsApplicationName] is missing in the request.", MODULE);
            return "error";
        }

        String retval = exportPaginatedListToExcel(strPaginatorName, strOpentapsApplicationName, request, response);

        return retval;
    }

    private static String exportPaginatedListToExcel(final String strPaginatorName, final String strOpentapsApplicationName, HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Paginator paginator = PaginatorFactory.getPaginator(request.getSession(), strPaginatorName, strOpentapsApplicationName);
        if (paginator == null) {
            Debug.logError("Failed to retrieve the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", MODULE);
            return "error";
        }

        // paginator exists in session
        ListBuilder listBuilder = paginator.getListBuilder();
        if (listBuilder == null) {
            Debug.logError("Null list builder is found in the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", MODULE);
            return "error";
        }

        List dataList = null;
        try {
            dataList = listBuilder.getCompleteList();
        } catch (ListBuilderException lbe) {
            Debug.logError("ListBuilderException is caught while exporting the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "] : " + lbe.getMessage(), MODULE);
            return "error";
        }

        if (dataList == null || dataList.isEmpty()) {
            Debug.logWarning("Empty data list is returned in the paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", MODULE);
            return "error";
        }
        
        String entityName = null;
        if (UtilValidate.isNotEmpty(dataList)) {
            Object entity = dataList.get(0);
            if (entity instanceof GenericEntity) {
                entityName = ((GenericEntity) entity).getEntityName();
            }
        }

        // find the type of the List and convert to a List of Map
        List<Map<String, Object>> mapDataList = FastList.newInstance();
        if (UtilValidate.isNotEmpty(dataList)) {
            for (Object o : dataList) {
                if (o instanceof GenericEntity) {
                    // change GenericEntity object to normal Map
                    Map newMap = new FastMap();
                    newMap.putAll((GenericEntity) o);
                    mapDataList.add(newMap);
                } else if (o instanceof Map) {
                    mapDataList.add((Map<String, Object>) o);
                } else if (o instanceof EntityInterface) {
                    mapDataList.add(((EntityInterface) o).toMap());
                }
            }
        }


        // write the data list to Excel

        // get the field key list
        Map<String, Object> element = mapDataList.get(0);
        List<String> keys = new ArrayList<String>();
        if (element != null) {
            Set<String> keySet = element.keySet();
            for (String key : keySet) {
                if ("lastUpdatedStamp".equals(key)
                    || "lastUpdatedTxStamp".equals(key)
                    || "createdStamp".equals(key)
                    || "createdTxStamp".equals(key)) {
                    continue;
                }

                keys.add(key);
            }
        }

        if (UtilValidate.isNotEmpty(entityName) && "AcctgTransAndEntries".equals(entityName)) {
            for (Map<String, Object> mapData : mapDataList) {
                String partyId = (String) mapData.get("partyId");
                String partyName = partyId == null ?  null : PartyHelper.getPartyName(delegator, partyId, true);
                mapData.put("partyName", partyName);
                String debitCreditFlag = (String) mapData.get("debitCreditFlag");
                BigDecimal amount = (BigDecimal) mapData.get("amount");
                BigDecimal debitAmount = "D".equals(debitCreditFlag) ?  amount : null;
                BigDecimal creditAmount = "C".equals(debitCreditFlag) ?  amount : null;
                mapData.put("debitAmount", debitAmount);
                mapData.put("creditAmount", creditAmount);
            }
            // Add a Party Name column after requires1099 column
            keys.add(keys.indexOf("requires1099"), "partyName");
            // remove first name/last name/group name columns in export excel
            keys.remove("firstName");
            keys.remove("lastName");
            keys.remove("groupName");
            // add debitAmount/creditAmount columns, remove debitCreditFlag/amount columns
            keys.add("debitAmount");
            keys.add("creditAmount");
            keys.remove("amount");
            keys.remove("debitCreditFlag");
        }
        
        
        if (keys.isEmpty()) {
            Debug.logError("Empty field name list is returned in the data list of paginator [" + strPaginatorName + "] in the application [" + strOpentapsApplicationName + "]", MODULE);
            return "error";
        }

        // create a map for column header labels
        Map<String, Object> columnHeaderMap = new HashMap<String, Object>();
        for (String key : keys) {
            columnHeaderMap.put(key, key);
        }

        List<Map<String, Object>> excelDataList = new ArrayList<Map<String, Object>>();
        excelDataList.add(columnHeaderMap);
        excelDataList.addAll(mapDataList);

        // using random string as part of file name, avoid multi user operate in same time.
        String fileName = "data_" + String.valueOf((int) (Math.random() * 100000)) + ".xls";
        String excelFilePath = UtilCommon.getAbsoluteFilePath(request, fileName);
        try {
            UtilCommon.saveToExcel(excelFilePath, "data", keys, excelDataList);
        } catch (IOException ioe) {
            Debug.logError("IOException is thrown while trying to write to the Excel file: " + ioe.getMessage(), MODULE);
            return "error";
        }

        return downloadExcel(fileName, request, response);
    }

    /**
     * Download an existing Excel file from the ${opentaps_home}/runtime/output
     * directory. The Excel file is deleted after the download.
     *
     * @param filename the file name String object
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    private static String downloadExcel(String filename, HttpServletRequest request, HttpServletResponse response) {
        File file = null;
        ServletOutputStream out = null;
        FileInputStream fileToDownload = null;

        try {
            out = response.getOutputStream();

            file = new File(UtilCommon.getAbsoluteFilePath(request, filename));
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
            Debug.logError("Failed to open the file: " + filename, MODULE);
            return "error";
        } catch (IOException ioe) {
            Debug.logError("IOException is thrown while trying to download the Excel file: " + ioe.getMessage(), MODULE);
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
                Debug.logError("IOException is thrown while trying to download the Excel file: " + ioe.getMessage(), MODULE);
                return "error";
            }
        }

        return "success";
    }

}
