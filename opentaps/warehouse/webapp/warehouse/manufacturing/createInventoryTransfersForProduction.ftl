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

<form name="autoCreateInventoryTransfers" action="<@ofbizUrl>autoCreateInventoryTransfers</@ofbizUrl>" method="post">
    <table>
        <@inputSelectRow name="warehouseFacilityId" title=uiLabelMap.WarehouseFromWarehouse list=facilities key="facilityId" displayField="facilityName" default=parameters.facilityId/>
        <@inputHidden name="productionFacilityId" value=productionFacilityId/>
        <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFrom default=fromDate form="autoCreateInventoryTransfers"/>
        <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThru default=thruDate form="autoCreateInventoryTransfers"/>
        <@inputSubmitRow title=uiLabelMap.CommonCreate/>
    </table>
</form>
