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

<#-- reasons is the list of possible variance reasons, facilityId, productId and internalName are the parameters passed to filter the list -->
<@paginate name="physicalInventory" list=physicalInventory reasons=varianceReasons facilityId=facilityId productId=productId internalName=internalName >
<#noparse >
<@navigationHeader title=uiLabelMap.ProductInventoryItems />
<form name="createPhysicalVariances" method="POST" action="<@ofbizUrl>createPhysicalVariances?facilityId=${parameters.facilityId}&amp;productId=${parameters.productId}&amp;internalName=${parameters.internalName}</@ofbizUrl>">
  <#-- turn this into a multi form -->
  <@inputHiddenUseRowSubmit/>
  <@inputHiddenRowCount list=pageRows/>
  <table class="listTable">
    <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.FormFieldTitle_inventoryItemId   orderBy="inventoryItemId" />
        <@headerCell title=uiLabelMap.FormFieldTitle_productId         orderBy="productId" />
        <@headerCell title=uiLabelMap.ProductLotId                     orderBy="lotId" />       
        <@headerCell title=uiLabelMap.FormFieldTitle_internalName      orderBy="internalName" />
        <@headerCell title=uiLabelMap.ProductItemATP                   orderBy="availableToPromiseTotal" />
        <@headerCell title=uiLabelMap.ProductItemQOH                   orderBy="quantityOnHandTotal" />
        <@headerCell title=uiLabelMap.ProductProductATP                orderBy="productATP" />
        <@headerCell title=uiLabelMap.ProductProductQOH                orderBy="productQOH" />
        <td>${uiLabelMap.WarehouseVarianceReason}</td>
        <td width="10%">${uiLabelMap.WarehouseVariance}</td>
    </tr>
    <#list pageRows as item>
        <@inputHidden name="productId"        value=item.productId        index=item_index/>
        <@inputHidden name="inventoryItemId"  value=item.inventoryItemId  index=item_index/>
        <@inputHiddenRowSubmit                index=item_index/>
        <tr class="${tableRowClass(item_index)}">
            <@displayLinkCell      text=item.inventoryItemId   href="EditInventoryItem?inventoryItemId=${item.inventoryItemId}" />
            <@displayCell          text=item.productId/>
            <@displayCell          text=item.lotId/>
            <@displayCell          text=item.internalName?if_exists />
            <@displayCell          text=item.availableToPromiseTotal />
            <@displayCell          text=item.quantityOnHandTotal />
            <@displayCell          text=item.productATP />
            <@displayCell          text=item.productQOH />
            <@inputSelectCell      name="varianceReasonId" list=parameters.reasons key="varianceReasonId" displayField="description" index=item_index />
            <@inputTextCell        name="varianceQty" size=6 index=item_index />
        </tr>
    </#list >
    <#if pageRows?size != 0>
      <tr>
        <td colspan="9">&nbsp;</td>
        <td colspan="1" align="left"><div id="buttonsBar"><@inputSubmit title=uiLabelMap.CommonApply /></div></td>
      </tr>
    </#if>
  </table>
</form>
</#noparse >
</@paginate>