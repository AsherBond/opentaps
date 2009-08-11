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
</table>