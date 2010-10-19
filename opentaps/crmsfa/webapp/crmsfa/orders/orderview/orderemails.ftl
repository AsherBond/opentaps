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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<#if order?has_content>

<#assign newButton><a href="<@ofbizUrl>writeEmail?orderId=${order.orderId}&toEmail=${newEmailAddress?default({}).infoString?default("")}</@ofbizUrl>" class="buttontext">${uiLabelMap.CrmOrderSendNewEmail}</a></#assign>

<@flexAreaClassic targetId="orderEmails" title=uiLabelMap.CrmOrderEmails defaultState="open" headerContent=newButton style="margin:0; padding:0">
  <#if emails?has_content>
    <table class="listTable" style="border:none">
      <tr class="listTableHeader" style="border:none">
        <td class="titleCell" style="text-align:left">${uiLabelMap.CommonDate}</td>
        <td class="titleCell" style="text-align:left">${uiLabelMap.CommonFrom}</td>
        <td class="titleCell" style="text-align:left">${uiLabelMap.CommonTo}</td>
        <td class="titleCell" style="text-align:left">${uiLabelMap.PartySubject}</td>
        <td>&nbsp;</td>
      </tr>
      <#list emails as email>
        <tr class="${tableRowClass(email_index)}">
          <td width="15%"><#if email.datetimeEnded?has_content>${getLocalizedDate(email.datetimeEnded, "DATE")}<#else>${uiLabelMap.CrmPending}</#if></td>
          <td width="15%">
            <#if email.partyIdFromUrl?exists>
              <a href="${email.partyIdFromUrl?if_exists}">${email.partyFromName?default("")} (${email.partyIdFrom})</a>
            <#else>
              ${email.fromString?default("")}
            </#if>
          </td>
          <td width="15%">
            <#if email.partyIdToUrl?exists>
              <a href="${email.partyIdToUrl?if_exists}">${email.partyToName?default("")} (${email.partyIdTo})</a>
            <#else>
              ${email.toAddressString?default("")}
            </#if>
          </td>
          <td width="45%"><@displayLink href="viewActivity?workEffortId=${email.workEffortId}" text=email.subject class="linktext"/></td>
          <td width="10%" style="white-space: nowrap; text-align:right">
            <a href="<@ofbizUrl>writeEmail?workEffortId=${email.workEffortId}&amp;communicationEventId=${email.communicationEventId}&amp;action=reply</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_reply.png</@ofbizContentUrl>" alt="${uiLabelMap.PartyReply}" title="${uiLabelMap.PartyReply}" border="0"></a>
            <a href="<@ofbizUrl>writeEmail?workEffortId=${email.workEffortId}&amp;communicationEventId=${email.communicationEventId}&amp;action=forward</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_forward.png</@ofbizContentUrl>" alt="${uiLabelMap.OpentapsEmailForward}" title="${uiLabelMap.OpentapsEmailForward}" border="0"></a>
          </td>
        </tr>
      </#list>
    </table>
  <#else>            
    <div class="tabletext">&nbsp;${uiLabelMap.CrmOrderNoEmails}.</div>
  </#if>
</@flexAreaClassic>

</#if>
