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

<div class="subSectionBlock">
<div class="subSectionHeader"><span class="subSectionTitle">${uiLabelMap.PurchAddNewBackupWarehouse}</span></div>
<form action="<@ofbizUrl>createBackupWarehouse</@ofbizUrl>" method="post" name="createBackupWarehouse">
  <@inputHidden name="facilityIdTo" value=facilityTo.facilityId />
  <@inputHidden name="organizationPartyId" value=session.getAttribute("organizationPartyId") />
  <table class="twoColumnForm">
    <@displayLinkRow title=uiLabelMap.PurchPrimaryWarehouse href="viewBackupWarehouses?facilityId=${facilityTo.facilityId}" text="${facilityTo.facilityName} (${facilityTo.facilityId})" />    
    <@inputTextRow name="facilityName" title=uiLabelMap.CommonName titleClass="requiredField" />
    <@inputAutoCompletePartyGroupRow name="ownerPartyId" title=uiLabelMap.ProductFacilityOwner titleClass="requiredField" size=30/>
    <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency titleClass="requiredField" />
    <@inputSelectRow name="defaultInventoryItemTypeId" title=uiLabelMap.ProductInventoryItemType
        list=inventoryItemTypes key="inventoryItemTypeId" displayField="description" required=true titleClass="requiredField" default=defaultInventoryItemTypeId />
    <@inputAutoCompleteUserLoginPartyRow name="managerPartyId" title=uiLabelMap.PurchInitialManager titleClass="requiredField" size=30/>
    <@inputTextRow name="sequenceNum" title=uiLabelMap.CommonPriority titleClass="requiredField" size=3 />
    <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFrom form="createFacilityAssoc" default=nowTimestamp titleClass="requiredField" />
    <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThru form="createFacilityAssoc" />
    <@inputSubmitRow title=uiLabelMap.OpentapsAddNew />
  </table>
</form>
</div>
