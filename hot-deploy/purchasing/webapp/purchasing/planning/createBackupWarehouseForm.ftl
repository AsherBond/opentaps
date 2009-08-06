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
<div class="subSectionHeader"><span class="subSectionTitle">${uiLabelMap.PurchAddNewBackupWarehouse}</span></div>
<form action="<@ofbizUrl>createBackupWarehouse</@ofbizUrl>" method="post" name="createBackupWarehouse">
  <@inputHidden name="facilityIdTo" value=facilityTo.facilityId />
  <@inputHidden name="organizationPartyId" value=session.getAttribute("organizationPartyId") />
  <table class="twoColumnForm">
    <@displayLinkRow title=uiLabelMap.PurchPrimaryWarehouse href="viewBackupWarehouses?facilityId=${facilityTo.facilityId}" text="${facilityTo.facilityName} (${facilityTo.facilityId})" />    
    <@inputTextRow name="facilityName" title=uiLabelMap.CommonName titleClass="requiredField" />
    <@inputLookupRow name="ownerPartyId" title=uiLabelMap.ProductFacilityOwner form="createBackupWarehouse" lookup="LookupPerson" titleClass="requiredField" />
    <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency titleClass="requiredField" />
    <@inputSelectRow name="defaultInventoryItemTypeId" title=uiLabelMap.ProductInventoryItemType
        list=inventoryItemTypes key="inventoryItemTypeId" displayField="description" required=true titleClass="requiredField" default=defaultInventoryItemTypeId />
    <@inputLookupRow name="managerPartyId" title=uiLabelMap.PurchInitialManager form="createBackupWarehouse" lookup="LookupPerson" titleClass="requiredField" />
    <@inputTextRow name="sequenceNum" title=uiLabelMap.CommonPriority titleClass="requiredField" size=3 />
    <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFrom form="createFacilityAssoc" default=nowTimestamp titleClass="requiredField" />
    <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThru form="createFacilityAssoc" />
    <@inputSubmitRow title=uiLabelMap.OpentapsAddNew />
  </table>
</form>
</div>
