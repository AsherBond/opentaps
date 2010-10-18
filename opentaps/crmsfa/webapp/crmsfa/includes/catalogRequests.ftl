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

<#if !("Y"==disableRequestCatalog?default("N"))>
<a name="CatalogRequests"></a>
<@frameSectionHeader title=uiLabelMap.CrmCatalogRequests/>
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
</#if>