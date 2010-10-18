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

<#if workEffort?has_content> 

<#-- some defaults -->
<#assign custRequestId = ""/>
<#assign salesOpportunityId = ""/>
<#assign orderId = ""/>
<#if custRequest?has_content><#assign custRequestId = custRequest.custRequestId/></#if>
<#if salesOpportunity?has_content><#assign salesOpportunityId = salesOpportunity.salesOpportunityId/></#if>
<#if orderHeader?has_content><#assign orderId = orderHeader.orderId/></#if>

<@frameSection title=uiLabelMap.CrmActivityLinks>
    <#-- if event/task is completed then no more updating -->
     <#if !(isActive)>
        <#if salesOpportunity?exists>
            <span class="tabletext">${uiLabelMap.CrmOpportunity}</span><br/>
            <@displayLink href="viewOpportunity?salesOpportunityId=${salesOpportunityId}" text="${salesOpportunity.opportunityName?if_exists} (${salesOpportunityId})"/>
        </#if>
            <span class="tabletext">${uiLabelMap.CrmCase}</span><br/>
        <#if custRequest?exists>
            <@displayLink href="viewCase?custRequestId=${custRequestId}" text="${custRequest.custRequestName?if_exists} (${custRequestId})"/>
        <#else><span class="tabletext">${uiLabelMap.CommonNone}
             <a class="buttontext" href="<@ofbizUrl>createCaseForActivityForm?<#if firstContactParty?has_content>contactPartyId=${firstContactParty.partyId?default("")}</#if>&amp;workEffortId=${workEffort.workEffortId}&amp;custRequestName=${workEffort.workEffortName?default("")}</@ofbizUrl>">${uiLabelMap.CommonNew}</a></span>
        </#if>
        <#if orderHeader?exists>
            <span class="tabletext">${uiLabelMap.OrderOrder}</span><br/>
            <@displayLink href="orderview?orderId=${orderHeader.orderId}" text="${orderHeader.orderName?if_exists} (${orderHeader.orderId})"/>
        </#if>
       <#else>
        <#-- otherwise allow updating, but right now only owner and cases can be updated -->
        <form name="updateActivityAssocForm" method="POST" action="<@ofbizUrl>updateActivityAssociation</@ofbizUrl>">
          <@inputHidden name="workEffortId" value="${workEffort.workEffortId}"/>
          <#if hasChangeOwnerPermission && teamMembers?has_content>
            <@display text="${uiLabelMap.OpentapsOwner}"/><br/>
            <select id="newOwnerPartyId" name="newOwnerPartyId" class="inputBox" style="width: 96%">
            <option value="" <#if !(activityOwnerParty?has_content)> selected="selected"</#if>></option>
            <#list teamMembers?default([]) as option>
              <#if option.get("partyId") == (activityOwnerParty.get("partyId"))?if_exists>
              <#assign selected = "selected"><#else><#assign selected = ""></#if>
              <option ${selected} value="${option.get("partyId")}">
              ${option.get("firstName")}&nbsp;${option.get("lastName")}
              </option>
            </#list>
            </select>
          </#if>
          <@display text="${uiLabelMap.CrmCase}"/>
          <#if custRequest?exists>
            <@displayLink  href="viewCase?custRequestId=${custRequestId}" text="${custRequest.custRequestName?if_exists} (${custRequestId})"/>
          <#else>
             <a class="buttontext" href="<@ofbizUrl>createCaseForActivityForm?<#if firstContactParty?has_content>contactPartyId=${firstContactParty.partyId?default("")}</#if>&amp;workEffortId=${workEffort.workEffortId}&amp;custRequestName=${workEffort.workEffortName?default("")}</@ofbizUrl>">${uiLabelMap.CommonNew}</a>
          </#if>
          <br/>
          <@inputLookup name="custRequestId" lookup="LookupCases" form="updateActivityAssocForm" default="${custRequestId?if_exists}" size="15" />
          <br/>
          <@display text="${uiLabelMap.OrderOrder}"/>
          <#if orderHeader?exists>
            <@displayLink href="orderview?orderId=${orderHeader.orderId}" text="${orderHeader.orderName?if_exists} (${orderHeader.orderId})"/>
          </#if>
          <br/>
          <@inputLookup name="orderId" lookup="LookupSalesOrders" form="updateActivityAssocForm" default="${orderId?if_exists}" size="15"/>
          <@inputButton title="${uiLabelMap.CommonUpdate}"/>
        </form>
      </#if>
     </table>
</@frameSection>
</#if>
