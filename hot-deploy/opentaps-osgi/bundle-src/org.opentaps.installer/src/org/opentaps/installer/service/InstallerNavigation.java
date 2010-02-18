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
package org.opentaps.installer.service;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.opentaps.installer.Activator;

/*
 *
 */
@SuppressWarnings("serial")
public class InstallerNavigation extends HttpServlet {

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String direction = request.getParameter("direction");
        String currentStepId = request.getParameter("stepId");
        if (currentStepId == null || currentStepId.length() == 0) {
            String msg = "There is no stepId parameter that should be current step identifier.";
            Activator.getInstance().logError(msg, null, null);
            throw new ServletException(msg);
        }

        OSSInstaller installer = Activator.getInstance().getInstaller();
        if (installer != null) {

            String url = null;
            if ("prev".equals(direction)) {
                url = installer.prevUri(currentStepId);
            } else {
                url = installer.nextUri(currentStepId);
            }

            Map<String, String> result = new HashMap<String, String>();
            result.put("nextAction", url);
            String j = JSONObject.fromObject(result).toString();

            response.setContentType("application/x-json");
            response.setContentLength(j.getBytes("UTF-8").length);

            Writer out = response.getWriter();
            out.write(j);
            out.flush();
        }
    }

}
