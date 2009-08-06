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

<form name="autoCreateInventoryTransfers" action="<@ofbizUrl>autoCreateInventoryTransfers</@ofbizUrl>" method="post">
    <table>
        <@inputSelectRow name="warehouseFacilityId" title=uiLabelMap.WarehouseFromWarehouse list=facilities key="facilityId" displayField="facilityName" default=parameters.facilityId/>
        <@inputHidden name="productionFacilityId" value=productionFacilityId/>
        <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFrom default=fromDate form="autoCreateInventoryTransfers"/>
        <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThru default=thruDate form="autoCreateInventoryTransfers"/>
        <@inputSubmitRow title=uiLabelMap.CommonCreate/>
    </table>
</form>
