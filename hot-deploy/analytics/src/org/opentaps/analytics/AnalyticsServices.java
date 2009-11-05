/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.reporting.etl.UtilEtl;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.domain.analytics.AnalyticsServicesInterface;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;
import org.pentaho.di.core.exception.KettleException;


public class AnalyticsServices extends Service implements AnalyticsServicesInterface {

    public static final String MODULE = AnalyticsServices.class.getName();

    private String organizationPartyId;

    /** {@inheritDoc} */
    public void updateDataWarehouse() throws ServiceException {
        // load file that contains list of transformations 
        String configName = UtilConfig.getPropertyValue("analytics", "loading.templates");
        URL config = UtilURL.fromResource(configName);
        if (config == null) {
            return;
        }

        List<String> scripts = FastList.<String>newInstance();

        try {
            // organize file names in list
            InputStream is = config.openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = rd.readLine()) != null) {
                if (UtilValidate.isNotEmpty(line) && !line.startsWith("#")) {
                    scripts.add(line);
                }
            }

            if (UtilValidate.isEmpty(scripts)) {
                return;
            }

            // looks through list of transformation
            String basePath = UtilConfig.getPropertyValue("analytics", "template.base.path");
            if (UtilValidate.isEmpty(basePath)) {
                basePath = "scripts/etl";
            }

            for (String filename : scripts) {
                // run a transformation
                UtilEtl.runTrans(String.format("component://analytics/%1$s/%2$s", basePath, filename), new String[] {organizationPartyId});
            }

        } catch (IOException e) {
            throw new ServiceException(e);
        } catch (KettleException e) {
            throw new ServiceException(e);
        }
    }

    public void setOrganizationPartyId(String partyId) {
        organizationPartyId = partyId;
    }
}
