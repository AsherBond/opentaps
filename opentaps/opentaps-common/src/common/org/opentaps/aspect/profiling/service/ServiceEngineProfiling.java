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
package org.opentaps.aspect.profiling.service;

import java.util.Locale;
import java.util.Map;

import org.codehaus.aspectwerkz.definition.Pointcut;
import org.codehaus.aspectwerkz.joinpoint.JoinPoint;
import org.ofbiz.service.ModelService;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

public class ServiceEngineProfiling {


    public static final String module = ServiceEngineProfiling.class.getName();
    private static final EtmMonitor etmMonitor = EtmManager.getEtmMonitor();

    /**
     * @Expression execution(* org.ofbiz.service.eca.ServiceEcaRule.eval(..))
     */
    Pointcut serviceEcaRuleEval;

    /**
     * @Around serviceEcaRuleEval
     */
      public Object arroundServiceEcaRuleEval(JoinPoint joinPoint) throws Throwable {
          EtmPoint point = etmMonitor.createPoint( "ServiceEcaRuleEval");
          Object returnObject = joinPoint.proceed();
          point.collect();
          return returnObject;
      }

    /**
     * @Expression execution(* org.ofbiz.service.eca.ServiceEcaUtil.evalRules(..))
     */
    Pointcut serviceEcaUtilEvalRules;

    /**
     * @Around serviceEcaUtilEvalRules
     */
    public Object arroundServiceEcaUtilEvalRules(JoinPoint joinPoint) throws Throwable {
          EtmPoint point = etmMonitor.createPoint( "ServiceEcaUtilEvalRules");
          Object returnObject = joinPoint.proceed();
          point.collect();
          return returnObject;
    }

    /**
     * @Expression execution(* org.ofbiz.service.ModelService.validate(..)) && args(test, mode, locale)
     */
    void modelServiceValidate(Map test, String mode, Locale locale){}

    /**
     * @Around modelServiceValidate(test, mode, locale)
     */
    public Object arroundModelServiceValidate(JoinPoint joinPoint, Map test, String mode, Locale locale) throws Throwable {
        EtmPoint point = null;
        if (ModelService.IN_PARAM.equals(mode)) {
            point = etmMonitor.createPoint( "ModelServiceValidate.Input");
        } else if (ModelService.OUT_PARAM.equals(mode)) {
            point = etmMonitor.createPoint( "ModelServiceValidate.Output");
        } else {
            point = etmMonitor.createPoint( "ModelServiceValidate");
        }
        Object returnObject = joinPoint.proceed();
        point.collect();
        return returnObject;
    }

    /**
     * @Expression execution(* org.ofbiz.service.engine.GenericEngine.sendCallbacks(..))
     */
    Pointcut genericEngineSendCallbacks;

    /**
     * @Around genericEngineSendCallbacks
     */
    public Object arroundGenericEngineSendCallbacks(JoinPoint joinPoint) throws Throwable {
        EtmPoint point = etmMonitor.createPoint( "GenericEngineSendCallbacks");
        Object returnObject = joinPoint.proceed();
        point.collect();
        return returnObject;
    }

    /**
     * @Expression execution(* org.ofbiz.service.engine.GenericEngine.runSyncIgnore(..)) && args(localName, modelService, context)
     */
    void genericEngineRunSyncIgnore(String localName, ModelService modelService, Map context){}

    /**
     * @Around genericEngineRunSyncIgnore(localName, modelService, context)
     */
    public Object arroundGenericEngineRunSyncIgnore(JoinPoint joinPoint, String localName, ModelService modelService, Map context) throws Throwable {
        EtmPoint point = etmMonitor.createPoint( "GenericEngineRunSync[" + modelService.name + "]");
        Object returnObject = joinPoint.proceed();
        point.collect();
        return returnObject;
    }

    /**
     * @Expression execution(* org.ofbiz.service.engine.GenericEngine.runSync(..)) && args(localName, modelService, context)
     */
    void genericEngineRunSync(String localName, ModelService modelService, Map context){}

    /**
     * @Around genericEngineRunSync(localName, modelService, context)
     */
    public Object arroundGenericEngineRunSync(JoinPoint joinPoint, String localName, ModelService modelService, Map context) throws Throwable {
        EtmPoint point = etmMonitor.createPoint( "GenericEngineRunSync[" + modelService.name + "]");
        joinPoint.proceed();
        point.collect();
        return null;
    }


    /**
     * @Expression execution(* org.ofbiz.service.engine.GenericEngine.runAsync(..)) && args(localName, modelService, context, persist)
     */
    void genericEngineRunAsyncIgnore(String localName, ModelService modelService, Map context, boolean persist){}

    /**
     * @Around genericEngineRunAsyncIgnore(localName, modelService, context, persist)
     */
    public Object arroundGenericEngineRunAsyncIgnore(JoinPoint joinPoint, String localName, ModelService modelService, Map context, boolean persist) throws Throwable {
        EtmPoint point = etmMonitor.createPoint( "GenericEngineRunAsync[" + modelService.name + "]");
        Object returnObject = joinPoint.proceed();
        point.collect();
        return returnObject;
    }

    /**
     * @Expression execution(* org.ofbiz.service.engine.GenericEngine.runSync(..)) && args(localName, modelService, context, requester, persist)
     */
    void genericEngineRunAsync(String localName, ModelService modelService, Map context, boolean persist){}

    /**
     * @Around genericEngineRunAsync(localName, modelService, context, persist)
     */
    public Object arroundGenericEngineRunSync(JoinPoint joinPoint, String localName, ModelService modelService, Map context, boolean persist) throws Throwable {
        EtmPoint point = etmMonitor.createPoint( "GenericEngineRunAsync[" + modelService.name + "]");
        Object returnObject = joinPoint.proceed();
        point.collect();
        return returnObject;
    }

}
