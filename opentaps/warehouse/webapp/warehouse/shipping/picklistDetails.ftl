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

<#if picklistInfo?has_content>
<table class="listTable">
  <tr>
    <@displayCell text=uiLabelMap.ProductPickList/>
    <@displayCell text=picklistInfo.picklistId/>
  </tr>
  <tr>
    <@displayCell text=uiLabelMap.CommonStatus/>
    <#if picklistIsOpen?exists>
      <form method="post" action="<@ofbizUrl>updatePicklist</@ofbizUrl>" style="display: inline;">
        <@inputHidden name="facilityId" value=picklistInfo.facilityId />
        <@inputHidden name="picklistId" value=picklistInfo.picklistId />
        <td>
          <@inputSelect name="statusId"   list=picklistInfo.statusValidChangeToDetailList key="statusIdTo" required=false defaultOptionText=picklistInfo.statusDescription ; option >
            ${option.description} (${option.transitionName?if_exists})
          </@inputSelect>
          <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
        </td>
      </form>
    <#else>
      <@displayCell text=picklistInfo.statusDescription/>
    </#if>
  </tr>
  <tr>
    <@displayCell text=uiLabelMap.CommonDate/>
    <@displayDateCell date=picklistInfo.picklistDate/>
  </tr>
  <tr>
    <@displayCell text=uiLabelMap.OpentapsShipVia/>
    <@displayCell text=picklistInfo.shipmentMethodTypeDescription/>
  </tr>
  <tr>
    <@displayCell text=uiLabelMap.CommonCreatedBy/>
    <@displayCell text=picklistInfo.createdByUserLogin/>
  </tr>
  <tr>
    <@displayCell text=uiLabelMap.CommonDescription/>
    <@displayCell text=picklistInfo.description/>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <#-- picklist items -->
  <#if picklistData?has_content>
    <tr class="listTableHeader">
      <@displayCell text=uiLabelMap.WarehouseBin/>
      <@displayCell text=uiLabelMap.WarehouseOrderShipGroup/>
      <@displayCell text=uiLabelMap.CommonFor/>
      <@displayCell text=uiLabelMap.OrderPoNumber/>
      <#if picklistIsOpen?exists>
        <@displayCell text=uiLabelMap.OpentapsMoveTo/>
      </#if>
      <#if picklistIsOpen?exists><td/></#if>
      <#if isPicklistPicked?exists><td/></#if>
  </tr>
    <#list picklistData.keySet().toArray()?sort as binNumber>
      <#assign binShipGroups = picklistData.get(binNumber)/>
      <#assign binId = binIds.get(binNumber)/>
      <#list binShipGroups.keySet() as shipGroup>
        <#assign orderHeader = orderData.get(shipGroup.orderId)/>
        <tr>
          <@displayCell text=binNumber/>
          <@displayLinkCell href="javascript:opentaps.expandCollapse('${shipGroup.orderId}${shipGroup.shipGroupSeqId}')" text="${shipGroup.orderId}/${shipGroup.shipGroupSeqId}"/>
          <@displayCell text=orderHeader.shipToCustomerName?if_exists/>
          <@displayCell text=orderHeader.poNumber/>
          <#if isPicklistPicked?exists>
            <td align="right">
              <#if hasNotPackedOisg.get(shipGroup.shipGroupSeqId)>
                <div><@displayLink text=uiLabelMap.WarehousePackOrder href="PackOrder?orderId=${shipGroup.orderId}&amp;shipGroupSeqId=${shipGroup.shipGroupSeqId}" class="buttontext"/></div>
              <#else>
                <@display text=uiLabelMap.WarehousePacked/>
              </#if>
            </td>
          </#if>
          <#if picklistIsOpen?exists>
            <td>
              <form method="post" action="<@ofbizUrl>updatePicklistBin</@ofbizUrl>" style="display: inline;">
                <@inputHidden name="facilityId" value=picklistInfo.facilityId />
                <@inputHidden name="picklistBinId" value=binId />
                ${uiLabelMap.WarehouseBin}
                <@inputText size="2" name="binLocationNumber" default=binNumber />
                ${uiLabelMap.PageTitlePickList}
                <@inputSelect name="picklistId" list=picklistActiveList key="picklistId" ; picklistActive >
                  ${picklistActive.picklistId} (${picklistActive.statusDescription})
                </@inputSelect>
                <input type="submit" value="${uiLabelMap.OpentapsMove}" class="smallSubmit"/>
              </form>
            </td>
          </#if>
        </tr>
        <tr>
          <td colspan="5">
            <@flexArea targetId="${shipGroup.orderId}${shipGroup.shipGroupSeqId}" style="border:none" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
              <table class="listTable">
                <tr class="listTableHeader" style="background-color:white">
                  <@displayCell text=uiLabelMap.ProductProduct/>
                  <@displayCell text=uiLabelMap.OpentapsUPC/>
                  <@displayCell text=uiLabelMap.WarehouseLotOrSerial/>                  
                  <@displayCell text=uiLabelMap.CommonQuantity/>
                  <#if picklistIsOpen?exists>
                    <td/>
                  </#if>
                </tr>
                <#assign picklistItems = binShipGroups.get(shipGroup)/>
                <#list picklistItems as picklistItem>
                  <tr>
                    <@displayCell text="${picklistItem.internalName?if_exists} (${picklistItem.oiProductId})"/>
                    <@displayCell text="${picklistItem.upc?if_exists}"/>
                    <@displayCell text="${picklistItem.iiLotId?if_exists} ${picklistItem.iiSerialNumber?if_exists} "/>
                    <@displayCell text=picklistItem.piQuantity/>
                    <#if picklistIsOpen?exists>
                      <td>
                      <#-- picklist item is repeated for each bin many times, so the form key needs to have bin and item index --> 
                      <@form name="deletePicklistItemForm_${binId}_${picklistItem_index}" url="deletePicklistItem" picklistId="${picklistItem.pPicklistId}" picklistBinId="${binId}" orderId="${shipGroup.orderId}" orderItemSeqId="${picklistItem.orderItemSeqId}" shipGroupSeqId="${picklistItem.shipGroupSeqId}" inventoryItemId="${picklistItem.inventoryItemId}" facilityId="${picklistItem.facilityId?if_exists}"/>
                      <@submitFormLink form="deletePicklistItemForm_${binId}_${picklistItem_index}" text=uiLabelMap.CommonDelete />
                    </#if>
                  </tr>
                </#list>
              </table>
            </@flexArea>
          </td>
        </tr>
      </#list>
    </#list>
  <#else> <#-- no item -->
    <tr><td colspan="2"><div class="head3">${uiLabelMap.WarehousePicklistEmpty}</div></td></tr>
  </#if>
</table>
</#if>
