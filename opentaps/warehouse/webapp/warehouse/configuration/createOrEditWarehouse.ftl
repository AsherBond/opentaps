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

<#if facility.facilityId?exists>
  <#assign title = uiLabelMap.WarehouseViewWarehouse />
  <#assign target = "updateWarehouse" />
  <#assign submitName = uiLabelMap.CommonUpdate />
<#else>
  <#assign title = uiLabelMap.WarehouseCreateNewWarehouse />
  <#assign target = "createWarehouse" />
  <#assign submitName = uiLabelMap.CommonCreate />
</#if>

<@frameSectionHeader title=title />

<form name="createOrEditWarehouse" method="post" action="<@ofbizUrl>${target}</@ofbizUrl>">
  <#if facility.facilityId?exists>
    <@inputHidden name="facilityId" value=facility.facilityId />
  </#if>

  <table class="twoColumnForm">

    <@inputTextRow name="facilityName" title=uiLabelMap.CommonName titleClass="requiredField" size=40 default=(facility.facilityName)! />
    <@inputSelectRow name="facilityTypeId" title=uiLabelMap.ProductFacilityType titleClass="requiredField" list=facilityTypes key="facilityTypeId" displayField="description" default=(facility.facilityTypeId)! />
    <@inputTextRow name="squareFootage" title=uiLabelMap.ProductSquareFootage size=6 default=(facility.squareFootage)! />
    <@inputAutoCompletePartyGroupRow name="ownerPartyId" title=uiLabelMap.ProductFacilityOwner titleClass="requiredField" size=30 default=(facility.ownerPartyId)! />
    <@inputSelectRow name="defaultInventoryItemTypeId" title=uiLabelMap.ProductInventoryItemType titleClass="requiredField" list=inventoryItemTypes key="inventoryItemTypeId" displayField="description" default=(facility.defaultInventoryItemTypeId)! />
    <@inputSelectRow name="defaultWeightUomId" title=uiLabelMap.ProductFacilityDefaultWeightUnit list=weightUomList key="uomId" displayField="description" default=(facility.defaultWeightUomId)! />
    <@inputTextRow name="defaultDaysToShip" title=uiLabelMap.WarehouseDefaultDaysToShip size=6 default=(facility.defaultDaysToShip)! />
    <@inputSubmitRow title=submitName />

  </table>
</form>
