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

package org.opentaps.common.util;

import java.io.File;
import java.io.IOException;

import java.io.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.*;
import javax.servlet.http.*;

public class POIWorkbookServlet extends HttpServlet {

    @Override public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override public void destroy() {
        super.destroy();
    }

    /**
     * Processes requests for both HTTP GET and POST methods.
     * @param request servlet request
     * @param response servlet response
     * @exception ServletException if an error occurs
     * @exception IOException if an error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String filename = null;
        File file = null;

        ServletOutputStream out = null;
        FileInputStream fileToDownload = null;

        try {
            filename = request.getParameter("file");
            out = response.getOutputStream();

            if (filename != null) { // do not allow for path characters in the filename, for security reasons
                if (filename.contains("%2F") || filename.contains("/")) {
                    response.setContentType("text/html");
                    out.print("Bad Request!");
                } else {
                    file = new File(UtilCommon.getAbsoluteFilePath(request, filename));
                    fileToDownload = new FileInputStream(file);
                    response.setContentLength(fileToDownload.available());

                    int c;
                    while ((c = fileToDownload.read()) != -1) {
                        out.write(c);
                    }
                    response.setContentType("application/vnd.ms-excel");
                    response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                    out.flush();
                }
            } else {
                response.setContentType("text/html");
                out.print("Bad Request!");
            }
        } catch (FileNotFoundException e) {
            response.setContentType("text/html");
            out.print("File not found on server!");
        } finally {
            out.close();
            if (fileToDownload != null) {
                fileToDownload.close();
                // Delete the file under /runtime/output/ this is optional
                file.delete();
            }
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet roequest
     * @param response servlet response
     * @exception ServletException if an error occurs
     * @exception IOException if an error occurs
     */
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP POST method.
     * @param request servlet request
     * @param response servlet response
     * @exception ServletException if an error occurs
     * @exception IOException if an error occurs
     */
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a <code>String</code> value
     */
    @Override public String getServletInfo() {
        return "Example to create a workbook in a servlet using HSSF";
    }

}
