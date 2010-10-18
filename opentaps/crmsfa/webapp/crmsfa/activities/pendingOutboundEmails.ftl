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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@flexAreaClassic targetId="pendingOutboundEmails" title=uiLabelMap.CrmActivitiesPendingOutboundEmails save=true defaultState="open" style="border:none; margin:0; padding:0">
<@paginate name="pendingOutboundEmails" list=outboundEmails currentPage=currentPage>
  <#noparse>
  <@navigationBar />
  <table class="listTable">
    <tr class="listTableHeader">
      <@headerCell title=uiLabelMap.CommonDate orderBy="datetimeEnded"/>
      <@headerCell title=uiLabelMap.CommonFrom orderBy="partyFromName"/>
      <@headerCell title=uiLabelMap.CommonTo orderBy="partyToName"/>
      <@headerCell title=uiLabelMap.PartySubject orderBy="subject"/>
      <@headerCell title=uiLabelMap.CommonAssocs orderBy=""/>
      <td>&nbsp;</td> <#-- some buttons -->
    </tr>

    <#list pageRows as row>  <#-- passed in from pagination -->
     <tr class="${tableRowClass(row_index)}">
       <@displayDateCell date=row.datetimeStarted/>
       <td>
            <#if row.partyIdFromUrl?exists>
                <a href="${row.partyIdFromUrl?if_exists}">${row.partyFromName?default("")} (${row.partyIdFrom})</a>
            <#else>
                ${row.fromString?default("")}
            </#if>
       </td>
       <td>
            <#if row.partyIdToUrl?exists>
                <a href="${row.partyIdToUrl?if_exists}">${row.partyToName?default("")} (${row.partyIdTo})</a>
            <#else>
                ${row.toAddressString?default("")}
            </#if>
       </td>
       <td><@displayLink href="viewActivity?workEffortId=${row.workEffortId}&amp;fromPage=${parameters.currentPage}" text=row.subject class="linktext"/></td>
       <td><#if row.assocOrder?has_content>${uiLabelMap.OrderOrder}: <a href="<@ofbizUrl>orderview?orderId=${row.assocOrder.orderId}</@ofbizUrl>">${row.assocOrder.orderId}</a><br/></#if>
           <#if row.assocCase?has_content>${uiLabelMap.CrmCase}: <a href="<@ofbizUrl>viewCase?custRequestId=${row.assocCase.custRequestId}</@ofbizUrl>">${row.assocCase.custRequestId}</a></#if>
       </td>
       <td nowrap="nowrap" style="white-space: nowrap; width:1%">
         <a href="<@ofbizUrl>writeEmail?workEffortId=${row.workEffortId}&amp;communicationEventId=${row.communicationEventId}&amp;action=reply</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_reply.png</@ofbizContentUrl>" alt="${uiLabelMap.PartyReply}" title="${uiLabelMap.PartyReply}" border="0"></a>
         <a href="<@ofbizUrl>writeEmail?workEffortId=${row.workEffortId}&amp;communicationEventId=${row.communicationEventId}&amp;action=forward</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_forward.png</@ofbizContentUrl>" alt="${uiLabelMap.OpentapsEmailForward}" title="${uiLabelMap.OpentapsEmailForward}" border="0"></a>
         <#if row.hasDeleteEmailPermission == "true">
           <@inputConfirmImage src="/opentaps_images/buttons/glass_buttons_red_X.png" title=uiLabelMap.CrmDeleteEmail href="deleteActivityEmail?communicationEventId=${row.communicationEventId}&amp;workEffortId=${row.workEffortId}&amp;delContentDataResource=Y&amp;donePage=${parameters.currentPage}" />
         </#if>
       </td>
     </tr>
    </#list>
  </table>
  </#noparse>
</@paginate>

</@flexAreaClassic>
