/*
 * Copyright (c) 2009 - 2010 Open Source Strategies, Inc.
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

package org.opentaps.dataimport;

import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.opentaps.common.util.UtilMessage;

/**
 * Common services and helper methods related to files uploading and management.
 */
public final class CommonImportServices {

    private CommonImportServices() { }

    private static final String MODULE = CommonImportServices.class.getName();

    /**
     * Uploads a file by calling the service correspondingto the file format.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> uploadFileAndRunImportService(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String fileFormat = (String) context.get("fileFormat");

        // for now we only support EXCEL format
        String serviceName;
        if ("EXCEL".equalsIgnoreCase(fileFormat)) {
            serviceName = "uploadExcelFileAndRunImportService";
        } else {
            return UtilMessage.createAndLogServiceError("[" + fileFormat + "] is not a supported file format.", MODULE);
        }

        try {
            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(context, "IN");
            return dispatcher.runSync(serviceName, input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }
}
