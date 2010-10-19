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
package org.opentaps.aspect.profiling.testtools;
import org.codehaus.aspectwerkz.definition.Pointcut;
import org.codehaus.aspectwerkz.joinpoint.JoinPoint;
import org.ofbiz.base.util.Debug;

import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.renderer.SimpleTextRenderer;

public class TestRunContainerProfiling {

    public static final String module = TestRunContainerProfiling.class.getName();
    private static final EtmMonitor etmMonitor = EtmManager.getEtmMonitor();

    /**
     * Pointcut testRunContainerStart is pointcut to TestRunContainer.start.
     * @Expression execution(* org.ofbiz.testtools.TestRunContainer.start(..))
     */
    Pointcut testRunContainerStart;

    /**
     * @Around testRunContainerStart
     */
   public Object arroundTestRunContainerStart(JoinPoint joinPoint) throws Throwable {
          // load jetm to monitor time
          Debug.logInfo("Start JETM monitoring", module);
          BasicEtmConfigurator.configure();
          etmMonitor.start();
          Object ret = joinPoint.proceed();
          //visualize results
          Debug.log("[JETM] ------------------------------------------------------------------ [JETM]", module);
          etmMonitor.render(new SimpleTextRenderer(Debug.getPrintWriter()));
          Debug.log("[JETM] ------------------------------------------------------------------ [JETM]", module);
          etmMonitor.stop();
          return ret;
   }
}
