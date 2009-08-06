<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">
  <a name="CatalogRequests"></a>
  <div class="subSectionHeader">
      <div class="subSectionTitle">${uiLabelMap.CrmCatalogRequests}</div>
  </div>
  
  <table class="listTable" cellspacing="0">
    <tr class="listTableHeader">
      <td>${uiLabelMap.CommonDate}</td>
      <td>${uiLabelMap.CrmAddress}</td>
      <td>${uiLabelMap.CrmTakenBy}</td>
      <td>${uiLabelMap.CrmFulfilled}</td>
    </tr>
    <#list catalogRequests?default([]) as catalogRequest>
      <tr class="${tableRowClass(catalogRequest_index)}">
        <@displayDateCell date=catalogRequest.custRequestDate format="DATE"/>
        <@displayCell text="${catalogRequest.address1?if_exists} - ${catalogRequest.city?if_exists}"/>
        <@displayCell text=catalogRequest.requestTakerPartyName?if_exists/>
        <#if catalogRequest.fulfilledDateTime?exists>
          <#assign fulfilledDate = getLocalizedDate(catalogRequest.fulfilledDateTime)/>
        <#else>
          <#assign fulfilledDate = ""/>
        </#if>
        <@displayCell text=fulfilledDate/>
      </tr>
    </#list>
  </table>
</div>
