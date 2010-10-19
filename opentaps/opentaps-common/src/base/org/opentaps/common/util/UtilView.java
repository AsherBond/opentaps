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

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.ServiceUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * UtilView - A place for entity view history opentaps helper methods
 */
public class UtilView {

    public static final String module = UtilView.class.getName();

    /**
     * Checks if an entity has been updated since the last view.  The EntityViewHistory table stores the time
     * of the last view.  When an update happens, the entries in EntityViewHistory table are cleared.
     * Thus, if no last view exists, an update has occured.  
     */
    public static boolean isUpdatedSinceLastView(Delegator delegator, String entityName, String primaryKeyId, String userLoginId) throws GenericEntityException {
        GenericValue history = delegator.findByPrimaryKey("EntityViewHistory", UtilMisc.toMap("entityName", entityName,
                "primaryKeyId", primaryKeyId, "userLoginId", userLoginId));
        return (history == null ? true : false);
    }

    /** As above, but argument is an entity value or any entity that has the entity's primary key as a field. */
    public static boolean isUpdatedSinceLastView(GenericValue genericValue, String userLoginId) throws GenericEntityException {
        Delegator delegator = genericValue.getDelegator();
        ModelEntity entity = (ModelEntity)delegator.getModelEntity(genericValue.getEntityName());
        return isUpdatedSinceLastView(delegator, genericValue.getEntityName(), 
                genericValue.getString(entity.getFirstPkFieldName()), userLoginId);
    }
    
    /**
     * Mark the entity as updated for all users.  This is mainly used by the service of the same name.
     */
    public static void markAsUpdated(Delegator delegator, String entityName, String primaryKeyId) throws GenericEntityException {
        delegator.removeByAnd("EntityViewHistory", UtilMisc.toMap("entityName", entityName, "primaryKeyId", primaryKeyId));
    }

    /**
     * Mark entity as viewed by the given user login.
     */
    public static void markAsViewed(GenericValue genericValue, String userLoginId) throws GenericEntityException {
        Delegator delegator = genericValue.getDelegator();
        ModelEntity entity = (ModelEntity)delegator.getModelEntity(genericValue.getEntityName());        
        Map input = UtilMisc.toMap("entityName", genericValue.getEntityName(), "primaryKeyId", genericValue.get(entity.getFirstPkFieldName()), "userLoginId", userLoginId);
        GenericValue history = delegator.findByPrimaryKey("EntityViewHistory", input);
        if (history != null) {
            history.set("viewedTimestamp", UtilDateTime.nowTimestamp());
            history.store();
        } else {
            history = delegator.makeValue("EntityViewHistory", input);
            history.put("viewedTimestamp", UtilDateTime.nowTimestamp());
            history.create();
        }
    }

    /**
     * Checks if an CustRequest has been updated since the last view.  The EntityViewHistory table stores the time
     * of the last view.  When an update happens, the entries in EntityViewHistory table are cleared.
     * Thus, if no last view exists, an update has occured.
     * @param delegator a <code>Delegator</code> instance
     * @param custRequestId the id of CustRequest
     * @param userLoginId the id of user login
     * @return if update
     */
    public static boolean isUpdatedSinceLastView(Delegator delegator, String custRequestId, String userLoginId) throws GenericEntityException {
        GenericValue history = delegator.findByPrimaryKey("EntityViewHistory", UtilMisc.toMap("entityName", "CustRequest",
                "primaryKeyId", custRequestId, "userLoginId", userLoginId));
        return (history == null ? true : false);
    }

}
