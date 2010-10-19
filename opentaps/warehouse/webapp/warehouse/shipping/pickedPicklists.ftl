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

  <@form name="closePicklistsForm" url="closePicklists" picklistId="" />


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
          <td class="alignRight">
          <@submitFormLinkConfirm form="closePicklistsForm" text=uiLabelMap.WarehouseClosePicklists picklistId=picklistInfo.picklistId />
          </td>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
