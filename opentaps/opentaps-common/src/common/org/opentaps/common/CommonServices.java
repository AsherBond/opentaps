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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */
package org.opentaps.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.FopRenderer;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.widget.screen.ScreenHelper;


/**
 * Common services for Opentaps-Common.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class CommonServices {

    private CommonServices() { }

    private static final String MODULE = CommonServices.class.getName();

    /**
     * Save a rendered FO screen to a PDF file.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> saveFOScreenToPDFFile(DispatchContext dctx, Map<String, ?> context) {
        String screenLocation = (String) context.get("screenLocation");
        String savePath = (String) context.get("savePath");
        String fileName = (String) context.get("fileName");
        Map<String, Object> screenContext = (Map<String, Object>) context.get("screenContext");
        Map<String, Object> screenParameters = (Map<String, Object>) context.get("screenParameters");

        // Render the template as FO text, then render the FO
        ByteArrayOutputStream baos = null;
        try {
            String foText = ScreenHelper.renderScreenLocationAsText(screenLocation, dctx, screenContext, screenParameters);
            Writer writer = new StringWriter();
            writer.write(foText);
            baos = FopRenderer.render(writer);
        } catch (Exception e) {
            return UtilMessage.createAndLogServiceError(e.getMessage(), MODULE);
        }

        // Make sure the directories exist to save the file
        File savePathDir = new File(savePath);
        if (!savePathDir.exists()) {
            savePathDir.mkdirs();
        }
        if (!savePath.endsWith(System.getProperty("file.separator"))) {
            savePath += System.getProperty("file.separator");
        }

        // Save the file
        try {
            FileOutputStream fileOut = new FileOutputStream(savePath + fileName);
            baos.writeTo(fileOut);
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e.getMessage(), MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service removes lost and useless records from navigation history where
     * "expireAt" less than current time.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> purgeNavHistory(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        try {
            Timestamp now = UtilDateTime.nowTimestamp();
            delegator.removeByCondition("ViewHistory", EntityCondition.makeCondition("expireAt", now));
        } catch (GenericEntityException gee) {
            UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Service prints back the given timestamp.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testDateTimeInput(DispatchContext dctx, Map<String, ?> context) {
        Timestamp sample = (Timestamp) context.get("sampleTimestamp");
        String result = sample.toString();
        return UtilValidate.isNotEmpty(result) ? ServiceUtil.returnSuccess("Your sent timestamp: " + result.toString()) : ServiceUtil.returnSuccess();
    }

    /**
     * Service to test the service engine.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> runServiceEngineTests(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Integer iterations = (Integer) context.get("iterations");
        Integer records = (Integer) context.get("records");

        Timestamp[] startAt = new Timestamp[10];
        Timestamp[] finishAt = new Timestamp[10];
        double[] duration = new double[10];

        try {

            // run empty service w/o transactions, w/o SECA
            dispatcher.disableEcas();
            startAt[0] = UtilDateTime.nowTimestamp();
            for (int i = 0; i < iterations.intValue(); i++) {
                dispatcher.runSync("opentaps.testEmptyService", new FastMap<String, Object>());
            }
            finishAt[0] = UtilDateTime.nowTimestamp();
            duration[0] = UtilDateTime.getInterval(startAt[0], finishAt[0]);

            // run empty service w/ transactions
            startAt[1] = UtilDateTime.nowTimestamp();
            for (int i = 0; i < iterations.intValue(); i++) {
                TransactionUtil.begin();
                dispatcher.runSync("opentaps.testEmptyService", new FastMap<String, Object>());
                TransactionUtil.commit();
            }
            finishAt[1] = UtilDateTime.nowTimestamp();
            duration[1] = UtilDateTime.getInterval(startAt[1], finishAt[1]);

            // run empty service w/o transactions, w/ SECA
            dispatcher.enableEcas();
            startAt[2] = UtilDateTime.nowTimestamp();
            for (int i = 0; i < iterations.intValue(); i++) {
                dispatcher.runSync("opentaps.testEmptyService", new FastMap<String, Object>());
            }
            finishAt[2] = UtilDateTime.nowTimestamp();
            duration[2] = UtilDateTime.getInterval(startAt[2], finishAt[2]);

            // run empty service w/ transactions, w/ SECA
            startAt[3] = UtilDateTime.nowTimestamp();
            for (int i = 0; i < iterations.intValue(); i++) {
                TransactionUtil.begin();
                dispatcher.runSync("opentaps.testEmptyService", new FastMap<String, Object>());
                TransactionUtil.commit();
            }
            finishAt[3] = UtilDateTime.nowTimestamp();
            duration[3] = UtilDateTime.getInterval(startAt[3], finishAt[3]);


            if (records != null && records.intValue() > 0) {

                Map<String, Object> returns = null;

                // creates records count of TestEntity w/ random values
                dispatcher.disableEcas();
                returns = dispatcher.runSync("opentaps.testCreateTestEntity", UtilMisc.toMap("records", records));
                startAt[4] = (Timestamp) returns.get("startAt");
                finishAt[4] = (Timestamp) returns.get("finishAt");
                duration[4] = UtilDateTime.getInterval(startAt[4], finishAt[4]);
                dispatcher.enableEcas();

                // iterate over TestEntity + getRelated("Enumeration")
                returns = dispatcher.runSync("opentaps.testIterateTestEntity", new FastMap<String, Object>());
                startAt[5] = (Timestamp) returns.get("startAt");
                finishAt[5] = (Timestamp) returns.get("finishAt");
                duration[5] = UtilDateTime.getInterval(startAt[5], finishAt[5]);

                // iterate over TestEntity + getRelatedCache("Enumeration")
                returns = dispatcher.runSync("opentaps.testIterateTestEntityCache", new FastMap<String, Object>());
                startAt[6] = (Timestamp) returns.get("startAt");
                finishAt[6] = (Timestamp) returns.get("finishAt");
                duration[6] = UtilDateTime.getInterval(startAt[6], finishAt[6]);

                // query TestEntity w/ DynamicViewEntity
                returns = dispatcher.runSync("opentaps.testQueryTestEntity", new FastMap<String, Object>());
                startAt[7] = (Timestamp) returns.get("startAt");
                finishAt[7] = (Timestamp) returns.get("finishAt");
                duration[7] = UtilDateTime.getInterval(startAt[7], finishAt[7]);

                // updates TestEntity w/ random values, w/o EECA
                dispatcher.disableEcas();
                returns = dispatcher.runSync("opentaps.testUpdateTestEntity", new FastMap<String, Object>());
                startAt[8] = (Timestamp) returns.get("startAt");
                finishAt[8] = (Timestamp) returns.get("finishAt");
                duration[8] = UtilDateTime.getInterval(startAt[8], finishAt[8]);

                dispatcher.enableEcas();
                // updates TestEntity w/ random values, w/ EECA
                returns = dispatcher.runSync("opentaps.testUpdateTestEntity", new FastMap<String, Object>());
                startAt[9] = (Timestamp) returns.get("startAt");
                finishAt[9] = (Timestamp) returns.get("finishAt");
                duration[9] = UtilDateTime.getInterval(startAt[9], finishAt[9]);
            }

            Debug.logInfo(">>> Test results:", MODULE);
            Debug.logInfo(String.format(">>> Run empty service w/o transaction & SECA support %1$d times, test time %2$.2f ms (%3$e ms per call)", iterations.intValue(), duration[0], duration[0] / iterations.intValue()), MODULE);
            Debug.logInfo(String.format(">>> Run empty service w/ transaction support, w/o SECA %1$d times, test time %2$.2f ms (%3$e ms per call)", iterations.intValue(), duration[1], duration[1] / iterations.intValue()), MODULE);
            Debug.logInfo(String.format(">>> Run empty service w/o transaction support, w/ SECA %1$d times, test time %2$.2f ms (%3$e ms per call)", iterations.intValue(), duration[2], duration[2] / iterations.intValue()), MODULE);
            Debug.logInfo(String.format(">>> Run empty service w/ transaction & SECA support %1$d times, test time %2$.2f ms (%3$e ms per call)", iterations.intValue(), duration[3], duration[3] / iterations.intValue()), MODULE);
            Debug.logInfo(String.format(">>> Create %1$d records in TestEntity, test time %2$.2f ms", records.intValue(), duration[4]), MODULE);
            Debug.logInfo(String.format(">>> Iterate over all records using getRelated(), test time %1$.2f ms", duration[5]), MODULE);
            Debug.logInfo(String.format(">>> Iterate over all records using getRelatedCache(), test time %1$.2f ms", duration[6]), MODULE);
            Debug.logInfo(String.format(">>> Query all records using DynamicViewEntity, test time %1$.2f ms", duration[7]), MODULE);
            Debug.logInfo(String.format(">>> Update all records w/ random values w/o EECA, test time %1$.2f ms", duration[8]), MODULE);
            Debug.logInfo(String.format(">>> Update all records w/ random values w/ EECA, test time %1$.2f ms", duration[9]), MODULE);

        } catch (GenericServiceException gse) {
            UtilMessage.createAndLogServiceError(gse, MODULE);
        } catch (GenericTransactionException gte) {
            UtilMessage.createAndLogServiceError(gte, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service in the simplest form possible, does nothing.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testEmptyService(DispatchContext dctx, Map<String, ?> context) {
        return ServiceUtil.returnSuccess();
    }

    /**
     * ECA service in the simplest form possible, does nothing.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testEmptySeca(DispatchContext dctx, Map<String, ?> context) {
        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to test iterating over a result list.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testIterateTestEntity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        Timestamp startAt = UtilDateTime.nowTimestamp();
        Timestamp finishAt = null;

        List<GenericValue> recordset;
        try {
            recordset = delegator.findAll("TestEntity");
            for (GenericValue testEntity : recordset) {
                testEntity.getRelatedCache("Enumeration");
            }
            finishAt = UtilDateTime.nowTimestamp();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("startAt", startAt);
        results.put("finishAt", finishAt);

        return results;
    }

    /**
     * Service to test the entity cache.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testIterateTestEntityCache(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        Timestamp startAt = UtilDateTime.nowTimestamp();
        Timestamp finishAt = null;

        List<GenericValue> recordset;
        try {
            recordset = delegator.findAll("TestEntity");
            for (GenericValue testEntity : recordset) {
                testEntity.getRelatedCache("Enumeration");
            }
            finishAt = UtilDateTime.nowTimestamp();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("startAt", startAt);
        results.put("finishAt", finishAt);

        return results;
    }

    /**
     * Service to test the query.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testQueryTestEntity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        Timestamp startAt = UtilDateTime.nowTimestamp();
        Timestamp finishAt = null;

        List<GenericValue> recordset;
        DynamicViewEntity dv = new DynamicViewEntity();
        dv.addMemberEntity("TA", "TestEntity");
        dv.addMemberEntity("ENUM", "Enumeration");
        dv.addAliasAll("TA", "tards");
        dv.addAliasAll("ENUM", "enum");
        dv.addViewLink("TA", "ENUM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("enumId"));

        try {
            EntityListIterator iter = delegator.findListIteratorByCondition(dv, null, null, null, null, UtilCommon.DISTINCT_READ_OPTIONS);
            recordset = iter.getCompleteList();
            for (@SuppressWarnings("unused") GenericValue testEntity : recordset) {
            }
            iter.close();
            finishAt = UtilDateTime.nowTimestamp();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("startAt", startAt);
        results.put("finishAt", finishAt);

        return results;
    }

    /**
     * Service to test updating an entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testUpdateTestEntity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        Timestamp startAt = null;
        Timestamp finishAt = null;

        try {
            List<GenericValue> enums = delegator.findAll("Enumeration");
            List<String> ids = EntityUtil.getFieldListFromEntityList(enums, "enumId", false);
            int count = ids.size();

            List<GenericValue> recordset = delegator.findAll("TestEntity");

            startAt = UtilDateTime.nowTimestamp();
            for (GenericValue testEntity : recordset) {
                testEntity.set("testStringField", UtilDateTime.nowAsString());
                testEntity.set("enumId", ids.get((int) (count * Math.random())));
                testEntity.store();
            }
            finishAt = UtilDateTime.nowTimestamp();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("startAt", startAt);
        results.put("finishAt", finishAt);

        return results;
    }

    /**
     * Service to test creating an entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> testCreateTestEntity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        Integer records = (Integer) context.get("records");
        int iterationCount = 0;
        if (UtilValidate.isNotEmpty(records)) {
            iterationCount = records.intValue();
        }

        Timestamp startAt = null;
        Timestamp finishAt = null;

        try {
            List<GenericValue> enums = delegator.findAll("Enumeration");
            List<String> ids = EntityUtil.getFieldListFromEntityList(enums, "enumId", false);
            int count = ids.size();

            delegator.removeByCondition("TestEntity", EntityCondition.makeCondition("testId", EntityOperator.LIKE, "%"));

            startAt = UtilDateTime.nowTimestamp();
            for (int i = 0; i < iterationCount; i++) {
                GenericValue testEntity = delegator.makeValue("TestEntity");
                testEntity.set("testId", delegator.getNextSeqId("TestEntity"));
                testEntity.set("testStringField", UtilDateTime.nowAsString());
                testEntity.set("enumId", ids.get((int) (count * Math.random())));
                testEntity.create();
            }
            finishAt = UtilDateTime.nowTimestamp();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("startAt", startAt);
        results.put("finishAt", finishAt);

        return results;
    }
}
