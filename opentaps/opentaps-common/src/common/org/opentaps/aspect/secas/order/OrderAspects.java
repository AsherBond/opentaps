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
package org.opentaps.aspect.secas.order;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.aspectwerkz.joinpoint.JoinPoint;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.order.OrderServices;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

public class OrderAspects {

    public static final String module = OrderAspects.class.getName();

    /**
     * @Expression execution(public static Map org.ofbiz.order.order.OrderServices.createOrder(..)) && args(dctx, context)
     */
    void aroundCreateOrder(DispatchContext dctx, Map context){}

    /**
     * @Around aroundCreateOrder(dctx, context)
     */
    public Object aroundCreateOrder(JoinPoint jp, DispatchContext dctx, Map context) {
        Object results = null;

        try {

            results = (Map<String, Object>) jp.proceed();

            if (!ServiceUtil.isError((Map) results)) {
                Map<String, Object> callResults = OrderServices.setOrderHeaderPartiesFromRoles(dctx, UtilCommon.makeValidSECAContext(dctx, (Map) results, true));
                if (ServiceUtil.isError(callResults)) {
                    Map<String, Object> errorResult = ServiceUtil.returnError((String) callResults.get("errorMessage"), (List) callResults.get("errorMessageList"));
                    return errorResult;
                }
            }
        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, UtilCommon.getLocale(context), module);
        } catch (Throwable e) {
            return UtilMessage.createAndLogServiceError(e.getLocalizedMessage(), UtilCommon.getLocale(context), module);
        }

        return results;
    }

}
