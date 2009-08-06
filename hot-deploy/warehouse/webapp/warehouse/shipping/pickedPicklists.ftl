<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<@paginate name="pickedPicklists" list=picklists>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable">
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <tr class="listTableHeader">
        <@displayCell text=uiLabelMap.ProductPickList/>
        <@displayCell text=uiLabelMap.CommonDate/>
        <@displayCell text=uiLabelMap.OpentapsShipVia/>
        <@displayCell text=uiLabelMap.CommonCreatedBy/>
        <@displayCell text=uiLabelMap.CommonDescription/>
      </tr>
      <#list pageRows as picklistInfo>
        <tr class="${tableRowClass(picklistInfo_index)}">
          <@displayLinkCell href="picklistDetails?picklistId=${picklistInfo.picklistId}" text=picklistInfo.picklistId/>
          <@displayDateCell date=picklistInfo.picklistDate/>
          <@displayCell text=picklistInfo.shipmentMethodTypeDescription/>
          <@displayCell text=picklistInfo.createdByUserLogin/>
          <@displayCell text=picklistInfo.description/>
          <@displayLinkCell href="closePicklists?picklistId=${picklistInfo.picklistId}" text=uiLabelMap.WarehouseClosePicklists class="subMenuButton"/>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
