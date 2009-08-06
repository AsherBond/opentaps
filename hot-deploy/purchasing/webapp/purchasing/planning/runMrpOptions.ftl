<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form name="runMrpOptions" method="POST" action="<@ofbizUrl>runMrpGo</@ofbizUrl>">
   <table>
     <@inputSelectRow title=uiLabelMap.ProductFacility name="facilityId" list=facilities required=true titleClass="requiredField" displayField="facilityName"/>
     <#-- This is commented out for now until we get to a real multi-facility MRP model, hopefully soon
     <@inputSelectRow title=uiLabelMap.ProductFacilityGroup name="facilityGroupId" list=facilityGroups required=false displayField="facilityGroupName"/>
      -->
     <@inputLookupRow title=uiLabelMap.ProductSupplier name="supplierPartyId" lookup="LookupSupplier" form="runMrpOptions"/>      
     <tr>
          <@displayTitleCell title=uiLabelMap.ProductProductStore/>
          <@inputSelectCell name="productStoreId" list=productStores required=false displayField="storeName"/>
          <@displayTitleCell title=uiLabelMap.PurchMrpStoreGroup/>
          <@inputSelectCell name="productStoreGroupId" list=productStoreGroups required=false displayField="productStoreGroupName"/>
     </tr>
     <@inputAutoCompleteProductRow name="productId" title="${uiLabelMap.ProductProductId}" form="runMrpOptions"/>      
     <tr>
       <@displayTitleCell title=uiLabelMap.PurchMrpSalesOrderDefaultYearsInFuture/>
       <@inputTextCell name="defaultYearsOffset" default="1" size="10"/>                
       <@displayTitleCell title=uiLabelMap.PurchMrpReceiptEventBuffer/>
       <td><@inputText name="receiptEventBuffer" default="1" size="5"/>
           <@inputSelect name="receiptBufferTimeUomId" key="uomId" list=bufferTimeUoms displayField="abbreviation"/></td>
      </tr>
      <tr>
          <@displayTitleCell title=uiLabelMap.PurchMrpPercentageOfSalesForecast/>
          <@inputTextCell name="percentageOfSalesForecast" default="0" size="10"/>
          <@displayTitleCell title=uiLabelMap.PurchMrpCreateTransferRequirements/>
          <@inputIndicatorCell name="createTransferRequirements" default="N"/>
     </tr>
      <tr>
          <td/>
          <td/>
          <@displayTitleCell title=uiLabelMap.PurchMrpCreatePendingManufacturingRequirements/>
          <@inputIndicatorCell name="createPendingManufacturingRequirements" default="N"/>
     </tr>

      <@inputSubmitRow title=uiLabelMap.PurchRunMRP/>
   </table>
</form>
