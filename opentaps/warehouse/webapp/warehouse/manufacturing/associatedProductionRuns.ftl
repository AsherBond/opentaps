<#--
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
-->
<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->


<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- Mandatory work efforts -->
<#if mandatoryWorkEfforts?has_content>
  <p class="tabletext">
    <b>${uiLabelMap.ManufacturingMandatoryProductionRuns}:</b>
    <ul class="bulletList">
    <#list mandatoryWorkEfforts as mandatoryWorkEffortAssoc>
      <#assign mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne("FromWorkEffort")/>
      <#if "PRUN_COMPLETED" == mandatoryWorkEffort.getString("currentStatusId") || "PRUN_CLOSED" == mandatoryWorkEffort.getString("currentStatusId")>
        <#assign done=true/>
      <#else>
        <#assign done=false/>
      </#if>
      <li class="tabletext" style="line-height:2em;<#if done>background-color:#9f9;<#else>background-color:#f99;</#if>">
        <a href="<@ofbizUrl>ShowProductionRun?productionRunId=${mandatoryWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>
        <#if !done>[*]</#if>&nbsp;(${mandatoryWorkEffort.getRelatedOne("CurrentStatusItem").description})
      </li>
    </#list>
    </ul>
  </p>
</#if>
<#-- Dependent work efforts -->
<#if dependentWorkEfforts?has_content>
  <p class="tabletext">
    <b>${uiLabelMap.ManufacturingDependentProductionRuns}:</b>
    <ul class="bulletList">
    <#list dependentWorkEfforts as dependentWorkEffortAssoc>
      <#assign dependentWorkEffort = dependentWorkEffortAssoc.getRelatedOne("ToWorkEffort")/>
      <li class="tabletext" style="line-height:2em;">
        <a href="<@ofbizUrl>ShowProductionRun?productionRunId=${dependentWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${dependentWorkEffort.workEffortName}</a>
        <#if "PRUN_COMPLETED" != dependentWorkEffort.currentStatusId && "PRUN_CLOSED" != dependentWorkEffort.currentStatusId>[*]</#if>&nbsp;(${dependentWorkEffort.getRelatedOne("CurrentStatusItem").description})
      </li>
    </#list>
    </ul>
  </p>
</#if>
