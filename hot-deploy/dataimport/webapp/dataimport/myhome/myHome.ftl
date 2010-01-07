<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

<table  class="headedTable">
	<tr class="header">
		<@displayCell text="Importing"/>
		<@displayCell text="${uiLabelMap.DataImportNumberProcessed}"/>
		<@displayCell text="${uiLabelMap.DataImportNumberNotProcessed}"/>
		<#if hasFullPermissions?default(false)><td>&nbsp;</td></#if>
	</tr>
	<tr>
	    <form name="RunImportCustomerForm" method="post" action="setServiceParameters">
	        <@inputHidden name="SERVICE_NAME" value="importCustomers"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="_RUN_SYNC_" value="Y"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Customers"/>
    	    <@displayCell text="${uiLabelMap.FinancialsCustomers}:"/>
	        <@displayCell text="${customersProcessed}"/>
	        <@displayCell text="${customersNotProcessed}"/>
	        <#if hasDIAdminPermissions?default(false)>
	            <@inputSubmitCell title="${uiLabelMap.DataImportCustomers}"/>
	        </#if>
	    </form>
	</tr>
	<tr>
	    <form name="RunImportProductsForm" method="post" action="setServiceParameters">
	        <@inputHidden name="SERVICE_NAME" value="importProducts"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Products"/>
	    	<@displayCell text="${uiLabelMap.ProductProducts}:"/>
		    <@displayCell text="${productsProcessed}"/>
		    <@displayCell text="${productsNotProcessed}"/>
		    <#if hasDIAdminPermissions?default(false)>
		        <@inputSubmitCell title="${uiLabelMap.DataImportProducts}"/>
		    </#if>
		</form>
	</tr>
	<tr>
	    <form name="RunImportInventoryForm" method="post" action="setServiceParameters">
	        <@inputHidden name="SERVICE_NAME" value="importProductInventory"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="_RUN_SYNC_" value="Y"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Inventory"/>
	    	<@displayCell text="${uiLabelMap.ProductInventoryItems}:"/>
		    <@displayCell text="${inventoryProcessed}"/>
		    <@displayCell text="${inventoryNotProcessed}"/>
		    <#if hasDIAdminPermissions?default(false)>
		        <@inputSubmitCell title="${uiLabelMap.DataImportInventory}"/>
		    </#if>
		</form>
	</tr>
	<tr>
	    <form name="RunImportOrderHeadersForm" method="post" action="setServiceParameters">
	        <@inputHidden name="sel_service_name" value="importOrders"/>
	        <@inputHidden name="SERVICE_NAME" value="importOrders"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Orders"/>
	    	<@displayCell text="${uiLabelMap.DataImportOrderLines}:"/>
		    <@displayCell text="${orderHeadersProcessed}"/>
		    <@displayCell text="${orderHeadersNotProcessed}"/>
		    <#if hasDIAdminPermissions?default(false)>
		        <@inputSubmitCell title="${uiLabelMap.DataImportOrders}"/>
		    </#if>
		</form>
	</tr>
	<tr>
  	    <@displayCell text="${uiLabelMap.DataImportOrderItemLines}:"/>
	    <@displayCell text="${orderItemsProcessed}"/>
	    <@displayCell text="${orderItemsNotProcessed}"/>
	</tr>
	<tr>
	    <form name="RunImportProductsFormExcel" method="post" action="setServiceParameters">
	        <@inputHidden name="SERVICE_NAME" value="importProductFromExcel"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Products from Excel"/>
	    	<@displayCell text="${uiLabelMap.ProductProductsExcel}:"/>
		    <@displayCell text="$"/>
		    <@displayCell text="$"/>
		    <#if hasDIAdminPermissions?default(false)>
		        <@inputSubmitCell title="${uiLabelMap.DataImportProducts}"/>
		    </#if>
		</form>
	</tr>
	<tr>
	    <form name="RunImportSuppliersForm" method="post" action="setServiceParameters">
	        <@inputHidden name="SERVICE_NAME" value="importSuppliersFromExcel"/>
	        <@inputHidden name="POOL_NAME" value="pool"/>
	        <@inputHidden name="sectionHeaderUiLabel" value="Import Suppliers from Excel"/>
	    	<@displayCell text="${uiLabelMap.SuppliersExcel}:"/>
		    <@displayCell text="$"/>
		    <@displayCell text="$"/>
		    <#if hasDIAdminPermissions?default(false)>
		        <@inputSubmitCell title="${uiLabelMap.DataImportSuppliers}"/>
		    </#if>
		</form>
	</tr>		
</table>