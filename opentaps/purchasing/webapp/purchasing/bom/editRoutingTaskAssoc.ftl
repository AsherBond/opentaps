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

<script type="text/javascript">
/*<![CDATA[*/
function copyAndAddRoutingTask() {
    document.addtaskassocform.copyTask.value = "Y";
    document.addtaskassocform.submit();
}
function addRoutingTask() {
    document.addtaskassocform.copyTask.value = "N";
    document.addtaskassocform.submit();
}
/*]]>*/
</script>

<@sectionHeader title=uiLabelMap.ManufacturingEditRoutingTaskAssoc />
<#if security.hasEntityPermission("MANUFACTURING", "_CREATE", session)>
  <form method="post" action="<@ofbizUrl>AddRoutingTaskAssoc</@ofbizUrl>" name="addtaskassocform">
    <@inputHidden name="workEffortId" value=workEffortId />
    <@inputHidden name="workEffortIdFrom" value=workEffortId />
    <@inputHidden name="workEffortAssocTypeId" value="ROUTING_COMPONENT" />
    <@inputHidden name="copyTask" value="N" />
    <table class="fourColumnForm">
      <tr>
        <@displayTitleCell title=uiLabelMap.ManufacturingRoutingTaskId />
        <@inputLookupCell name="workEffortIdTo" form="addtaskassocform" lookup="LookupRoutingTask" />

        <@displayTitleCell title=uiLabelMap.CommonFromDate />
        <@inputDateCell name="fromDate" size=20 />
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonSequenceNum />
        <@inputTextCell name="sequenceNum" />

        <@displayTitleCell title=uiLabelMap.CommonThruDate />
        <@inputDateCell name="thruDate" size=20 />
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align="left" colspan="3">
          <a href="javascript:addRoutingTask();" class="buttontext">${uiLabelMap.ManufacturingAddExistingRoutingTask}</a>
          &nbsp;-&nbsp;
          <a href="javascript:copyAndAddRoutingTask();" class="buttontext">${uiLabelMap.ManufacturingCopyAndAddRoutingTask}</a>
        </td>
      </tr>
    </table>
  </form>
</#if>
<br/>
