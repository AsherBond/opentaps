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
<div>
    <@inputSubmit title=uiLabelMap.ProductScheduleTheseRouteSegments onClick="javascript:document.SchedulingList.submit();"/>
    <@inputSubmit title=uiLabelMap.WarehouseMarkAsShipped onClick="markAsShipped()"/>
</div>

<script type="text/javascript">
    function markAsShipped() {
        var i = 0;
        while(document.SchedulingList['carrierServiceStatusId_o_' + i]) {
           document.SchedulingList['carrierServiceStatusId_o_' + i].value = "SHRSCS_SHIPPED";
           i++;
        }

        document.SchedulingList.action = "<@ofbizUrl>BatchUpdateShipmentRouteSegments?facilityId=</@ofbizUrl>";
        document.SchedulingList.submit();
    }
</script>
