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
<#assign dateFormat = StringUtil.wrapString(Static["org.ofbiz.base.util.UtilDateTime"].getJsDateTimeFormat(Static["org.ofbiz.base.util.UtilDateTime"].getDateFormat(locale)))/>

<style type="text/css">
    table#orderPriorities {
        width: 100%;
        margin-left: 0px;
    }
    
    table#orderPriorities thead th {
        background: ${configProperties["crmsfa.theme.color.background.orders"]?default("#888")};
        color: #FFF;
    }
    
    div#orderPrioritiesContainer {
        overflow: auto;
        height: 410px;
    }
    .spinner {
        margin-top: 30px;
    }
</style>


<script type="text/javascript">
/*<![CDATA[*/
    var orderPriorities = [];
    
    <#-- Retrieve the data -->    
    opentaps.addOnLoad(function() {opentaps.sendRequest('<@ofbizUrl>getOrderPriorityList</@ofbizUrl>', {onlyApprovedOrders: '${onlyApprovedOrders?string("true", "false")}', containsProductId: '${containsProductId?if_exists}'}, loadOrderPriorities, {target: 'orderPriorities', size: 28}) });

    function loadOrderPriorities(response) {

        <#-- the getOrderPriorityList service returns a List called orderPriorityList -->        
        var orderPriorityList = response.orderPriorityList;

        <#-- Assemble the working array for the order priority objects -->
        for (var x = 0; x < orderPriorityList.size() ; x++) {
            orderPriorityList[x].sequenceId = x + 1;
            orderPriorities[x] = orderPriorityList[x];
        }
        renderOrderPriorities();
    }

    <#if ! viewIsFiltered>
    function increaseRowPriority(evt) {
        changeRowPriority(true, evt);
    }
    
    function decreaseRowPriority(evt) {
        changeRowPriority(false, evt);
    }
    
    function changeRowPriority(/*Boolean*/ moveUp, /* Event */ evt) {
        if (window.event && ! evt) evt = window.event;
        if (! evt) return;
        
        var button = evt.target ? evt.target : evt.srcelement;
        if (button == null) return;
    
        var tableRow = button.parentNode.parentNode;
        var rowIndex = tableRow.rowIndex;
        
        if (moveUp && rowIndex == 1) {
            return;
        }
    
        var tbody = document.getElementById("orderPrioritiesBody");
        var rows = tbody.getElementsByTagName("tr");
    
        if ((! moveUp) && rowIndex == rows.length) {
            return;
        }
    
        var movingRow = rows[rowIndex - 1];
        if (moveUp) {
            var rowBefore = rows[rowIndex - 2];
            opentaps.insertBefore(movingRow, rowBefore);
        } else {
            var rowAfter = rows[rowIndex];
            opentaps.insertAfter(movingRow, rowAfter);
        }
    }
    </#if>
    
    function renderOrderPriorities() {
        <#if ! viewIsFiltered>
        <#-- Make the table body a drag target and each table row a draggable element -->       
        var target = opentaps.makeDropTarget('orderPrioritiesBody');
        for (var x = 0; x < orderPriorities.length; x++) {
          var tableRow = createTableRow(orderPriorities[x]);
          opentaps.makeDragSource(target, tableRow);
          setupCalendar(orderPriorities[x]);
        }
        <#else>
        var target = document.getElementById('orderPrioritiesBody');
        for (var x = 0; x < orderPriorities.length; x++) {
          var tableRow = createTableRow(orderPriorities[x]);
          target.appendChild(tableRow);
          setupCalendar(orderPriorities[x]);
        }
        </#if>
    }

    function setupCalendar(/* Array */ data) {
         var shipByDateInputId = 'shipByDate_' + data.orderId + '_' + data.shipGroupSeqId;
         Calendar.setup({
          inputField: shipByDateInputId,
          ifFormat: "${StringUtil.wrapString(Static["org.ofbiz.base.util.UtilDateTime"].getJsDateTimeFormat(Static["org.ofbiz.base.util.UtilDateTime"].getDateFormat(locale)))}",
          button: shipByDateInputId+"-button",
          align: "Bl",
          showOthers: true,
          cache: true
        });
    }

    function createTableRow(/* Array */ data) { 

        var orderId = data.orderId;
        var shipGroupSeqId = data.shipGroupSeqId;

        var row = opentaps.createTableRow();
        row.id = 'tr_' + orderId + '_' + shipGroupSeqId;
        row.orderId = orderId;
        row.shipGroupSeqId = shipGroupSeqId;

        var cell;

        <#-- SequenceId -->
        <#if ! viewIsFiltered>
          cell = row.appendChild(opentaps.createTableCell(null, null, null, 'center'));
          var idString = opentaps.createSpan(null, data.sequenceId, 'tabletext');;
          cell.appendChild(idString);
          row.appendChild(cell);  
        </#if>

        <#-- OrderId/ShipGroupSeqId Link -->
        cell = opentaps.createTableCell(null, null, null, 'center');
        var link = opentaps.createAnchor(null, '<@ofbizUrl>orderview</@ofbizUrl>?orderId=' + orderId, orderId + ' / ' + shipGroupSeqId, 'linktext');
        cell.appendChild(link);
        row.appendChild(cell);

        <#-- CustomerId -->
        cell = opentaps.createTableCell(null, null, null, 'center');
        var customerString = opentaps.createSpan(null, data.customerName + ' (' + data.customerId + ')', 'tabletext');
        cell.appendChild(customerString);
        row.appendChild(cell);

        <#-- Shipping Address -->
        cell = opentaps.createTableCell(null, null, null, 'center');
        if (data.address != null) {
            var shippingAddressString = formatAddress(data.address);
            if (shippingAddressString != '') {
                var shippingAddress = opentaps.createSpan(null, shippingAddressString, 'tabletext');
                cell.appendChild(shippingAddress);
            }
        }
        row.appendChild(cell);
        
        <#-- Carrier and Shipment Method -->
        cell = opentaps.createTableCell(null, null, null, 'center');
        var carrierAndShipmentMethodText = '';
        if (data.carrierName != null) {
            carrierAndShipmentMethodText += data.carrierName;
        }
        if (data.shipmentMethodType != null) {
            carrierAndShipmentMethodText += ' ';
            if (data.shipmentMethodType.description != null) {
                carrierAndShipmentMethodText += data.shipmentMethodType.description;
            } else {
                carrierAndShipmentMethodText += data.shipmentMethodType.shipmentMethodTypeId;
            }
        }
        if (carrierAndShipmentMethodText != '') {
            var carrierAndShipmentMethod = opentaps.createSpan(null, carrierAndShipmentMethodText, 'tabletext');
            cell.appendChild(carrierAndShipmentMethod);
        }
        row.appendChild(cell);

        <#-- Ship-by date -->            
        cell = opentaps.createTableCell(null, null, null, 'center');
        if (data.shipByDate == null) {
            shipByDate = "";
        } else {
            shipByDate = new Date();
            shipByDate.setTime(data.shipByDate.time);
            shipByDate = opentaps.formatDate(shipByDate, '${dateFormat}');
        }
        var shipByDateInputId = 'shipByDate_' + orderId + '_' + shipGroupSeqId;
        var shipByDateInput = opentaps.createInput(shipByDateInputId, null, 'text', 'inputBox', null, shipByDate, 10);
        cell.appendChild(shipByDateInput);
        var calendarImage = opentaps.createImg(shipByDateInputId + '-button', '/images/cal.gif', null, {'onclick' : function(){ opentaps.toggleClass(document.getElementById(shipByDateInputId + '-calendar-placeholder'), 'hidden'); }}, '${uiLabelMap.CommonViewCalendar?js_string}');
        cell.appendChild(calendarImage);
        row.appendChild(cell);

        <#-- Estimated ship date -->            
        cell = opentaps.createTableCell(null, null, null, 'center');
        if (data.estimatedShipDate != null) {
            estimatedShipDate = new Date();
            estimatedShipDate.setTime(data.estimatedShipDate.time);
            estimatedShipDate = opentaps.formatDate(estimatedShipDate, '${dateFormat}');
            var estimatedShipDateText = opentaps.createSpan(null, estimatedShipDate, 'tabletext');
            cell.appendChild(estimatedShipDateText);
        }
        row.appendChild(cell);

        <#-- Status -->
        var cell = opentaps.createTableCell(null, null, null, 'center');
        var cellText = opentaps.createSpan(null, data.status.description, 'tabletext');
        cell.appendChild(cellText);
        row.appendChild(cell);  

        <#-- Back orders -->
        var cell = opentaps.createTableCell(null, null, null, 'center');
        if (data.backOrderedQuantity != null && data.backOrderedQuantity > 0) {
            var backOrderedText = opentaps.createImg(null, '/opentaps_images/buttons/tasto_8_architetto_franc_01.png', null, null, null);
            cell.appendChild(backOrderedText);
        }
        row.appendChild(cell);

        <#-- + and - anchors with handlers to move rows up and down -->
        <#if ! viewIsFiltered>            
        cell = opentaps.createTableCell(null, null, null, 'center');
        var upLink = opentaps.createImg(null, '/opentaps_images/buttons/arrow-up-green_benji_par_01.png', null, {'onclick' : increaseRowPriority}, '${uiLabelMap.CrmOrderQueuePriorityIncrease?js_string}');
        var downLink = opentaps.createImg(null, '/opentaps_images/buttons/arrow-down-green_benji_p_01.png', null, {'onclick' : decreaseRowPriority}, '${uiLabelMap.CrmOrderQueuePriorityIncrease?js_string}');
        cell.appendChild(upLink);
        cell.appendChild(downLink);
        row.appendChild(cell);
        </#if>

        return row;
    }

    <#if ! viewIsFiltered>
    function submitResequence() {
        var resequenceForm;
        try {
            resequenceForm = document.getElementById("resequenceForm");
        } catch (e) {
            // IE6 throws a harmless exception here, for some reason
        }
        var tbody = document.getElementById("orderPrioritiesBody");

        <#-- Read the current sequence of the table rows and construct form inputs for each orderId and shipGroupSeqId 
             x here is the sequence in the table of this row, and the backend service will process them in sequence -->
        var rows = tbody.getElementsByTagName("tr");
        for (var x = 0; x < rows.length ; x++) {
            var orderId = rows[x].orderId;
            var shipGroupSeqId = rows[x].shipGroupSeqId;
            var newInput = opentaps.createInput(null, 'orderIds_' + x, 'hidden', null, null, orderId);
            resequenceForm.appendChild(newInput);
            var newInput = opentaps.createInput(null, 'shipGroupSeqIds_' + x, 'hidden', null, null, shipGroupSeqId);
            resequenceForm.appendChild(newInput);
            var shipByDateInput = document.getElementById('shipByDate_' + orderId + '_' + shipGroupSeqId);
            var newInput = opentaps.createInput(null, 'shipByDates_' + x, 'hidden', null, null, shipByDateInput.value);
            resequenceForm.appendChild(newInput);
        }
        resequenceForm.submit();
    }
    <#else>
    function submitReschedule() {
        var rescheduleForm;
        try {
            rescheduleForm = document.getElementById("rescheduleForm");
        } catch (e) {
            // IE6 throws a harmless exception here, for some reason
        }
        var tbody = document.getElementById("orderPrioritiesBody");

        <#-- Read the current sequence of the table rows and construct form inputs for each orderId and shipGroupSeqId with indexed inputs -->
        var rows = tbody.getElementsByTagName("tr");
        for (var x = 0; x < rows.length ; x++) {
            var orderId = rows[x].orderId;
            var shipGroupSeqId = rows[x].shipGroupSeqId;
            var newInput = opentaps.createInput(null, 'orderId_o_' + x, 'hidden', null, null, orderId);
            rescheduleForm.appendChild(newInput);
            var newInput = opentaps.createInput(null, 'shipGroupSeqId_o_' + x, 'hidden', null, null, shipGroupSeqId);
            rescheduleForm.appendChild(newInput);
            var shipByDateInput = document.getElementById('shipByDate_' + orderId + '_' + shipGroupSeqId);
            var newInput = opentaps.createInput(null, 'shipByDate_o_' + x, 'hidden', null, null, shipByDateInput.value);
            rescheduleForm.appendChild(newInput);
        }
        var newInput = opentaps.createInput(null, '_rowCount', 'hidden', null, null, rows.length);
        rescheduleForm.appendChild(newInput);
        rescheduleForm.submit();
    }
    </#if>

    function formatAddress(/*Array*/ address) {
        var addressString = '';
        addressString += address.address1 + ' - ' + address.city;
        return addressString;
    }

    <#-- Decreated. Should be used opentaps.formatDate -->
    function formatDateString(/*Date*/ date) {
        var dateString = '';
        dateString += date.getFullYear() + '-';
        var monthNumber = date.getMonth() + 1;;
        dateString += (monthNumber.toString().length == 2) ? monthNumber : '0' + monthNumber;
        dateString += '-';
        var dateNumber = date.getDate().toString();
        dateString += (dateNumber.length == 2) ? dateNumber : '0' + dateNumber;
        return dateString;
    }
/*]]>*/  
</script>

<#-- This form is used to set the filters -->
<form name="filterForm" method="post" action="<@ofbizUrl>manageOrderQueue</@ofbizUrl>">
  <table class="twoColumnForm">
    <@inputIndicatorRow title=uiLabelMap.CommonShow name="onlyApprovedOrders" yesLabel=uiLabelMap.OpentapsApprovedOrdersOnly noLabel=uiLabelMap.OpentapsAllOpenOrders default="N"/>
    <@inputLookupRow title=uiLabelMap.CrmContainsProductId name="containsProductId" lookup="LookupProduct" form="filterForm" />
    <@inputSubmitRow title=uiLabelMap.CommonFind />
  </table>
</form>

<#-- This form will be populated by the submitResequence function -->
<#if ! viewIsFiltered>
<form id="resequenceForm" name="resequenceForm" method="post" action="<@ofbizUrl>resequenceOrderShipGroupPriorities</@ofbizUrl>"></form>
<#else>
<form id="rescheduleForm" name="rescheduleForm" method="post" action="<@ofbizUrl>rescheduleOrderShipDates</@ofbizUrl>"></form>
</#if>

<div id="orderPrioritiesContainer">
    <table id="orderPriorities" class="filteringTable" cellpadding="0" cellspacing="0" border="0">
        <colgroup span="${viewIsFiltered?string(5,6)}"></colgroup>
        <colgroup style="width: 5em"><col/></colgroup>
        <thead>
            <tr>
                <#if ! viewIsFiltered><th align="center">${uiLabelMap.CrmOrderQueuePriority}</th></#if>
                <th align="center">${uiLabelMap.CrmOrderOrderShipGroup}</th>
                <th align="center">${uiLabelMap.CrmCustomer}</th>
                <th align="center">${uiLabelMap.OpentapsShippingAddress}</th>
                <th align="center">${uiLabelMap.CrmOrderCarrierAndShipmentMethod}</th>
                <th align="center">${uiLabelMap.CrmOrderShipByDate}</th>
                <th align="center">${uiLabelMap.CrmOrderEstimatedShipDateAbbr}</th>
                <th align="center">${uiLabelMap.CommonStatus}</th>
                <th align="center">${uiLabelMap.CrmOrderBackOrdered}</th>
                <#if ! viewIsFiltered><th align="center"></th></#if>
            </tr>
        </thead>

        <#-- The body will be populated by the renderOrderPriorities function -->        
        <tbody id="orderPrioritiesBody"></tbody>
    </table>
    
    <div style="width:100%; text-align:right; margin-top: 15px">
    <#if ! viewIsFiltered>
      <a href="javascript:submitResequence();" class="buttontext" >${uiLabelMap.CrmOrderQueueSaveRefresh}</a>
    <#else>
      <a href="javascript:submitReschedule();" class="buttontext" >${uiLabelMap.CrmOrderQueueSaveRefresh}</a>
    </#if>
    </div>
</div>

