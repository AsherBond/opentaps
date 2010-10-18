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

<table class="listTable">
  <tbody>
    <tr class="listTableHeader">
      <td>${uiLabelMap.ProductInventoryItemId}</td>
      <td>${uiLabelMap.ProductLocation}</td>
      <td>${uiLabelMap.ProductProductId}</td>
      <td>${uiLabelMap.ProductInternalName}</td>
      <td>${uiLabelMap.ProductSerialNumber}</td>
      <td>${uiLabelMap.ProductLotId}</td>
      <td>${uiLabelMap.ProductDateReceived}</td>
      <td>${uiLabelMap.CommonStatus}</td>
      <td>${uiLabelMap.WarehouseQuantityATPQOH}</td>
    </tr>
    <#if inventoryItems?has_content>
      <#list inventoryItems as item>
        <tr class="${tableRowClass(item_index)}">
          <@displayLinkCell href="EditInventoryItem?inventoryItemId=${item.inventoryItemId}" text=item.inventoryItemId />
          <@displayLinkCell href="findInventoryItem?locationSeqId=${item.locationSeqId!}&amp;performFind=Y" text=item.locationSeqId! />
          <@displayLinkCell href="findInventoryItem?productId=${item.productId}&amp;performFind=Y" text=item.productId />
          <@displayCell text=item.internalName! />
          <@displayCell text=item.serialNumber! />
          <@displayCell text=item.lotId! />
          <@displayDateCell date=item.datetimeReceived! />
          <@displayCell text=item.statusId! />
          <@displayCell text="${item.availableToPromiseTotal!}/${item.quantityOnHandTotal!}" />
        </tr>
        <#-- display accounting tags associated with this item -->
        <#if tagTypesPerOrg?has_content>
          <#assign tagTypes = tagTypesPerOrg.get(item.ownerPartyId) />
          <#if tagTypes?has_content>
          <#assign hasSetAccountingTags = Static["org.opentaps.common.util.UtilAccountingTags"].hasSetAccountingTags(item,tagTypes)>
           <#if hasSetAccountingTags>
            <tr class="${tableRowClass(item_index)}">
              <td>&nbsp;</td>
              <td colspan="8">
                <i><@accountingTagsDisplay tags=tagTypes entity=item /></i>
              </td>
            </tr>
           </#if>
          </#if>
        </#if>
      </#list>
    </#if>
  </tbody>
</table>
