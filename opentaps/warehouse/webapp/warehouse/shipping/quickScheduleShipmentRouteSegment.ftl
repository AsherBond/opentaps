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

<div class="spacer">&nbsp;</div>
<div>
    <form name="quickScheduleShipmentRouteSegment" action="<@ofbizUrl>quickScheduleShipmentRouteSegment</@ofbizUrl>" method="post">
        <input type="hidden" name="clearAll" value="Y"/>
        <div class="tabletext">
            <span class="requiredFieldNormal">${uiLabelMap.ProductShipmentId}:</span>
            <input type="text" class="inputBox" size="8" name="shipmentId" id="shipmentId" value="${parameters.shipmentId?default("")}" onChange="document.getElementById('submit').focus()"/>
            <span class="requiredFieldNormal">${uiLabelMap.FormFieldTitle_shipmentRouteSegmentId}:</span>
            <input type="text" class="inputBox" size="8" name="shipmentRouteSegmentId" value="00001"/>
<#--
            <span>${uiLabelMap.FormFieldTitle_carrierPartyId}:</span>
            <input type="text" class="inputBox" size="8" name="carrierPartyId" value=""/>
            <span class="tabletext">
                <a href="javascript:call_fieldlookup2(document.quickScheduleAndPrintShipmentRouteSegment.carrierPartyId,'LookupPartyName');">
                    <img src="/images/fieldlookup.gif" width="15" height="14" border="0" alt="Click here For Field Lookup">
                </a>
            </span>
-->
            <span class="requiredFieldNormal">${uiLabelMap.WarehousePrinter}:</span>
            <select name="printerName" class="selectBox">
               <#list printers?default([]) as printer>
                   <option value="${printer}" ${(defaultPrinter == printer)?string("selected=\"selected\"","")}>${printer}</option>
               </#list>
            </select>
            <input type="submit" value="${uiLabelMap.WarehouseScheduleThisShipment}" class="smallSubmit" id="submit"/>
        </div>
    </form>
</div>
<script type="text/javascript">
    document.getElementById('shipmentId').focus();
</script>
<div class="spacer">&nbsp;</div>
