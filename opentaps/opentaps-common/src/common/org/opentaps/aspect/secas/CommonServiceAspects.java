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
package org.opentaps.aspect.secas;

import java.util.Map;

import org.codehaus.aspectwerkz.joinpoint.JoinPoint;
import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;

public class CommonServiceAspects  {

    public static final String module = CommonServiceAspects.class.getName();

    /**
     * @Expression execution(public static Map org.ofbiz.common.CommonServices.echoService(..)) && args(dctx, context)
     */
    void echoServiceAroundDemo(DispatchContext dctx, Map context){}

    /**
     * @Around echoServiceAroundDemo(dctx, context)
     */
    public Object firstEchoServiceAroundDemo(JoinPoint jp, DispatchContext dctx, Map context) throws Throwable {
        Debug.logInfo("Around Advise Demo: enter into firstEchoServiceAroundDemo", module);
        // Calls next advice since it exists
        Object results = jp.proceed();
        Debug.logInfo("Around Advise Demo: leave firstEchoServiceAroundDemo", module);
        // returns !!echoService!! results. Invoking code believe in this.
        return results;
    }

    /**
     * @Around echoServiceAroundDemo(dctx, context)
     */
    public Object nextEchoServiceAroundDemo(JoinPoint jp, DispatchContext dctx, Map context) throws Throwable {
        Debug.logInfo("Around Advise Demo: enter into nextEchoServiceAroundDemo", module);
        // call CommonServices.echoService() since no more joined advices
        Debug.logInfo("Around Advise Demo: Call echoService", module);
        Object results = jp.proceed();

        Debug.logInfo("Around Advise Demo: leave nextEchoServiceAroundDemo", module);

        return results;
    }
}
