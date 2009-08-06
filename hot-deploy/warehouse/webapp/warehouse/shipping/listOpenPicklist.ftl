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

<@paginate name="listOpenPicklists" list=picklistListBuilder rememberPage=false>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable">
      <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.ProductPickList orderBy="picklistId" />
        <@headerCell title=uiLabelMap.CommonDate orderBy="picklistDate" />
        <@displayCell text=uiLabelMap.WarehouseNumberOfOrder />
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" />
        <@headerCell title=uiLabelMap.ProductShipmentMethodType orderBy="shipmentMethodType" />
        <td/>
      </tr>
      <#list pageRows as picklist>
        <tr class="${tableRowClass(picklist_index)}">
          <@displayLinkCell text=picklist.picklistId href="picklistDetails?picklistId=${picklist.picklistId}" />
          <@displayDateCell date=picklist.picklistDate />
          <@displayCell text=picklist.picklistInfo.picklistOrderCount />
          <@displayCell text=picklist.statusDescription />
          <@displayCell text=picklist.shipmentMethodTypeDescription?if_exists />
          <td><a href="<@ofbizUrl>PicklistReport.pdf?picklistId=${picklist.picklistId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.OpentapsContentType_ApplicationPDF}</a></td>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
