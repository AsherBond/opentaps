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

package com.opensourcestrategies.financials.integration;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Services for exporting General Ledger activities.
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a>
 * @version    $Rev$
 * @since      2.2
*/
public final class GLExportServices {

    private GLExportServices() { }

    private static String MODULE = GLExportServices.class.getName();

    public static Map<String, Object> exportGLToFile(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        List<GenericValue> allTransactions = (List<GenericValue>) context.get("valuesToCreate");
        allTransactions.addAll((List<GenericValue>) context.get("valuesToStore"));

        // name of template for generating exported GL and name of file to write it to
        String fileName = UtilProperties.getPropertyValue("GLExport.properties", "file.name");
        String templateName = UtilProperties.getPropertyValue("GLExport.properties", "template.name");
        Debug.logInfo("templateName = " + templateName, MODULE);

        StringBuffer exportedGL = new StringBuffer();

        try {
            // only export posted transactions
            allTransactions = EntityUtil.filterByAnd(allTransactions, UtilMisc.toMap("isPosted", "Y"));

            Map<String, String> glAccountMapping = new HashMap<String, String>();          // map of OFBiz glAccountId to external system's gl account
            Map<String, String> externalAccountParties = new HashMap<String, String>();    // map of external gl account to any required party (vendors, customers)

            for (Iterator<GenericValue> aTi = allTransactions.iterator(); aTi.hasNext();) {
                GenericValue acctgTrans = aTi.next();
                // peculiar QBXML requirement - debits must come before credits
                List<GenericValue> acctgTransEntries = acctgTrans.getRelated("AcctgTransEntry", UtilMisc.toList("debitCreditFlag DESC", "acctgTransEntrySeqId"));
                // update the GL mapping
                for (Iterator<GenericValue> aTEi = acctgTransEntries.iterator(); aTEi.hasNext();) {
                    GenericValue acctgTransEntry = aTEi.next();

                    if (glAccountMapping.get(acctgTransEntry.getString("glAccountId")) == null) {
                        // gl account mappings are in the properties file
                        String mappedGlAccountId = UtilProperties.getPropertyValue("GLExport.properties", "glAccountId." + acctgTransEntry.getString("glAccountId"));

                        // if there is no target gl account, then the service should fail so the user can fix it and try again
                        if ((mappedGlAccountId == null) || (mappedGlAccountId.equals(""))) {
                            return ServiceUtil.returnError("No mapping for GL account " + acctgTransEntry.getString("glAccountId") + " was found.  Cannot export");
                        } else {
                            glAccountMapping.put(acctgTransEntry.getString("glAccountId"), mappedGlAccountId);
                            String externalPartyId = UtilProperties.getPropertyValue("GLExport.properties", "party." + mappedGlAccountId);
                            if ((externalPartyId != null) && !(externalPartyId.equals(""))) {
                                externalAccountParties.put(mappedGlAccountId, externalPartyId);
                            }
                        }
                    }
                }

                // use the template to generate an exported version of this transactions
                Map<String, Object> inContext = UtilMisc.<String, Object>toMap("acctgTrans", acctgTrans, "acctgTransEntries", acctgTransEntries, "glAccountMapping", glAccountMapping,
                                               "externalAccountParties", externalAccountParties);
                Debug.logInfo("acctgTransId = " + acctgTrans.getString("acctgTransId") + " with " + acctgTransEntries.size() + " entries", MODULE);
                StringWriter outWriter = new StringWriter();
                ContentWorker.renderContentAsText(dispatcher, delegator, templateName, outWriter, inContext, locale, "text/plain", null, null, false);
                Debug.logInfo("output: " + outWriter.toString(), MODULE);

                // now add it to all the other transactions so far
                exportedGL.append(outWriter.toString());
            }

            // write the file
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))));
            out.println(exportedGL.toString());
            out.close();

            // all done.  put in some values to satisfy the records
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("toCreateInserted", new Long(allTransactions.size()));
            result.put("toCreateUpdated", new Long(0));
            result.put("toCreateNotUpdated", new Long(0));
            result.put("toStoreInserted", new Long(0));
            result.put("toStoreUpdated", new Long(0));
            result.put("toStoreNotUpdated", new Long(0));
            result.put("toRemoveDeleted", new Long(0));
            result.put("toRemoveAlreadyDeleted", new Long(0));
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (FileNotFoundException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (IOException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

    }
}
