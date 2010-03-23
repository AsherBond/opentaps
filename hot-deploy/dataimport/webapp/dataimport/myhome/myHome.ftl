<#--
 * Copyright (c) 2006 - 2010 Open Source Strategies, Inc.
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

<#macro importForm importService label sectionLabel submitLabel processed notProcessed>
    <form name="${importService}Form" method="post" action="setServiceParameters">
      <@inputHidden name="SERVICE_NAME" value="${importService}"/>
      <@inputHidden name="POOL_NAME" value="pool"/>
      <@inputHidden name="sectionHeaderUiLabel" value="${sectionLabel}"/>
      <@displayCell text="${label}:"/>
      <@displayCell text="${processed}"/>
      <@displayCell text="${notProcessed}"/>
      <#if hasDIAdminPermissions?default(false)>
        <@inputSubmitCell title="${submitLabel}"/>
      </#if>
    </form>
</#macro>

<table  class="headedTable">
  <tr class="header">
    <@displayCell text=uiLabelMap.DataImportImporting/>
    <@displayCell text=uiLabelMap.DataImportNumberProcessed/>
    <@displayCell text=uiLabelMap.DataImportNumberNotProcessed/>
    <#if hasFullPermissions?default(false)><td>&nbsp;</td></#if>
  </tr>
  <tr>
    <@importForm importService="importCustomers"
                 sectionLabel="DataImportImportCustomers"
                 label=uiLabelMap.FinancialsCustomers
                 submitLabel=uiLabelMap.DataImportImportCustomers
                 processed=customersProcessed notProcessed=customersNotProcessed/>
  </tr>
  <tr>
    <@importForm importService="importSuppliers"
                 sectionLabel="DataImportImportSuppliers"
                 label=uiLabelMap.PurchSuppliers
                 submitLabel=uiLabelMap.DataImportImportSuppliers
                 processed=suppliersProcessed notProcessed=suppliersNotProcessed/>
  </tr>
  <tr>
    <@importForm importService="importProducts"
                 sectionLabel="DataImportImportProducts"
                 label=uiLabelMap.ProductProducts
                 submitLabel=uiLabelMap.DataImportImportProducts
                 processed=productsProcessed notProcessed=productsNotProcessed/>
  </tr>
  <tr>
    <@importForm importService="importProductInventory"
                 sectionLabel="DataImportImportInventory"
                 label=uiLabelMap.ProductInventoryItems
                 submitLabel=uiLabelMap.DataImportImportInventory
                 processed=inventoryProcessed notProcessed=inventoryNotProcessed/>
  </tr>
  <tr>
    <@importForm importService="importOrders"
                 sectionLabel="DataImportImportOrders"
                 label=uiLabelMap.DataImportOrderLines
                 submitLabel=uiLabelMap.DataImportImportOrders
                 processed=orderHeadersProcessed notProcessed=orderHeadersNotProcessed/>
  </tr>
  <tr>
    <@displayCell text="${uiLabelMap.DataImportOrderItemLines}:"/>
    <@displayCell text="${orderItemsProcessed}"/>
    <@displayCell text="${orderItemsNotProcessed}"/>
  </tr>
</table>

<br/>

<#if hasDIAdminPermissions?default(false)>
  <@frameSection title=uiLabelMap.DataImportUploadFile>
    <form name="UploadFileAndImport" method="post" enctype="multipart/form-data" action="uploadFileAndImport">
      <@inputHidden name="POOL_NAME" value="pool"/>
      <@inputHidden name="sectionHeaderUiLabel" value="DataImportImportFromFile"/>
      <table class="twoColumnForm">
        <@inputFileRow title=uiLabelMap.DataImportFileToImport name="uploadedFile" />
        <@inputSubmitRow title="${uiLabelMap.DataImportRunImport}"/>
      </table>
    </form>
  </@frameSection>
</#if>
