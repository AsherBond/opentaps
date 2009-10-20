<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
Instead of using a screen to do the layout, we use an ftl.  
This is because doing multicolumn layout in CSS as OFBiz does is really
difficult (CSS is weak in this respect).

This may also be the platform for refactoring order header. 

Notice how we replaced the screens.render with #include directives.  These subscreens do not have
any special data that is not set up in orderview.bsh, so the screen render call is redundant.
If special data is needed, either add a new bsh to run after orderview.bsh or include in orderview.bsh.
The whole idea is to keep things simple and not use the screen widget where unnecessary.
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if facilities?has_content>
<script type="text/javascript">
var facilityList = {
    <#list facilities as facility>
    ${facility.facilityId}: "${facility.facilityName}"<#if facility_has_next>,</#if>
    </#list>
};
var orderId = "${order.orderId}";
var orderItemSeqId = null;
</script>
</#if>

<#if order?exists && order.isSalesOrder()>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderInfo.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderTerms.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderPaymentInfo.ftl"/>
    </td>
    <td width="10" nowrap="nowrap">&nbsp;</td>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderContactInfo.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderShippingInfo.ftl"/>
    </td>
  </tr>
</table>

<div style="margin-bottom:15px">
  <#include "orderview/orderitems.ftl"/>
</div>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="3" style="padding-bottom: 15px">
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
    </td>
  </tr>
</table>

<div style="margin-bottom:15px">
<@include location="component://crmsfa/webapp/crmsfa/content/contentList.ftl"/>
</div>
<#include "orderview/ordernotes.ftl"/>
<#include "orderview/transitions.ftl"/>

<#else/>
  <p class="tableheadtext">${uiLabelMap.CrmOrderNotFound}</p>
</#if>
