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

<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<script type="text/javascript">
/*<![CDATA[*/
function changeInventoryItemTypeId(inventoryItemTypeId) {
   if (inventoryItemTypeId == "SERIALIZED_INV_ITEM") {
      opentaps.displayDiv("rowProductSerialNumber");
      opentaps.hideDiv("rowProductLotId");
      opentaps.hideDiv("rowProductQuantityAccepted");
      document.selectAllForm.quantityAccepted.value = "1";
   } else {
      opentaps.hideDiv("rowProductSerialNumber");
      opentaps.displayDiv("rowProductLotId");
      opentaps.displayDiv("rowProductQuantityAccepted");
   }
}

//Functions to send request to server for load relate objects
function onProductChange() {
    var productId = document.getElementById('ComboBox_productId').value;
    var facilityId = "${facilityId}";
    if (productId != "" && facilityId != "") {
      var requestData = {'productId' : productId, 'facilityId' : facilityId};
      opentaps.sendRequest('getReceiveInventoryProductRelatedsJSON', requestData, function(data) {loadProductChangeResponse(data)});
    }
}

//Functions load the response from the server
function loadProductChangeResponse(data) {
  loadProductIdAndName(data.productId, data.internalName);
  loadBackOrderedItems(data.backOrderedItems);
  loadGoodIdentifications(data.goodIdentifications);
  loadProductFacilityLocations(data.productFacilityLocations);
  // fill unit price field
  if (data.unitCost == null) {
    document.selectAllForm.unitCost.value = "";
  } else {
    document.selectAllForm.unitCost.value = data.unitCost;
  }
  toggleDisplayProductPerUnitPrice();
}

function toggleDisplayProductPerUnitPrice() {
  // toggle unit price line display or hide 
  if (${hasSetCostPermission?string("true", "false")}) {
    opentaps.displayDiv("rowProductPerUnitPrice");
  } else {
    opentaps.hideDiv("rowProductPerUnitPrice");
  }
}


function loadProductIdAndName(productId, internalName) {
  if (internalName == null || internalName == "") {
    internalName = "No Internal Name";
  }
  opentaps.displayDiv("rowProductIdAndName");
  document.getElementById('spanProductIdAndName').innerHTML = "<b>" + internalName + "</b> (" + productId + ")";

}

function loadGoodIdentifications(records) {
  var table = document.getElementById("tableAllForm");
  var rowAddGoodIdentification = document.getElementById("rowAddGoodIdentification");
  var oldRows = table.getElementsByTagName('tr');
  for (var i = oldRows.length - 1; i >= 0; i--) {
    // remove old nodes
    if(oldRows[i].id.indexOf('rowGoodIdentification_') == 0) {
       opentaps.removeNode(oldRows[i]);
    }
  }
  for (var i = 0; i < records.length; i++) {
   var rec = records[i];
   var row = opentaps.createTableRow('rowGoodIdentification_' + i);
   var text1 = "<span class='tableheadtext'>" + rec.goodIdentificationTypeId + "</span>";
   var text2 = rec.idValue;
   
   var cell1 = opentaps.createTableCell(null, "titleCell", text1, "center");
   row.appendChild(cell1);
   var cell2 = opentaps.createTableCell(null, "tabletext", text2);
   row.appendChild(cell2);
   table.insertBefore(row, rowAddGoodIdentification);
  }
}

function loadBackOrderedItems(records) {
  var table = document.getElementById("tableBackOrderedItems");
  var rowAddBackOrderedItem = document.getElementById("rowAddBackOrderedItem");
  var oldRows = table.getElementsByTagName('tr');
  for (var i = oldRows.length - 1; i >= 0; i--) {
    // remove old nodes
    if(oldRows[i].id.indexOf('rowBackOrderedItem_') == 0) {
       opentaps.removeNode(oldRows[i]);
    }
  }
  // toggle div rowBackOrderedItems
  if (records.length > 0) {
    opentaps.displayDiv("rowBackOrderedItems");
  } else {
    opentaps.hideDiv("rowBackOrderedItems");
  }
  for (var i = 0; i < records.length; i++) {
   var rec = records[i];
   var row = opentaps.createTableRow('rowBackOrderedItem_' + i);
   var text1 = rec.reservedDatetime;
   if (rec.sequenceId != null) {
     text1 += ": " + rec.sequenceId; 
   }
   var text2 = rec.orderId + " : " + rec.orderItemSeqId;
   var text3 = rec.quantity;
   var text4 = rec.quantityNotAvailable;
   var cell1 = opentaps.createTableCell(null, "tabletext", text1, "center");
   row.appendChild(cell1);
   var cell2 = opentaps.createTableCell(null, "tabletext", text2, "center");
   row.appendChild(cell2);
   var cell3 = opentaps.createTableCell(null, "tabletext", text3, "center");
   row.appendChild(cell3);
   var cell4 = opentaps.createTableCell(null, "requiredField", text4, "center");
   row.appendChild(cell4);
   table.insertBefore(row, rowAddBackOrderedItem);
  }
}

function loadProductFacilityLocations(records) {
  var table = document.getElementById("tableAllForm");
  var rowAddProductFacilityLocation = document.getElementById("rowAddProductFacilityLocation");
  var oldRows = table.getElementsByTagName('tr');
  for (var i = oldRows.length - 1; i >= 0; i--) {
    // remove old nodes
    if(oldRows[i].id.indexOf('rowProductFacilityLocation') == 0) {
       opentaps.removeNode(oldRows[i]);
    }
  }
  var row = opentaps.createTableRow('rowProductFacilityLocation');
  var text1 = "<span class='tableheadtext'>" + "${uiLabelMap.ProductFacilityLocation?js_string}" + "</span>";
  var cell1 = opentaps.createTableCell(null, "titleCell", text1, "center");
  row.appendChild(cell1);
  if (records == null || records.length == 0) {
    var text2 = "<input size=\"20\" maxlength=\"20\" name=\"locationSeqId\" class=\"inputBox\" type=\"text\"> <a href=\"javascript:call_fieldlookup2(document.selectAllForm.locationSeqId,'LookupFacilityLocation?facilityId=${facilityId}');\"><img src=\"/images/fieldlookup.gif\" alt=\"Lookup\" border=\"0\" width=\"15\" height=\"14\"></a>";
    var cell2 = opentaps.createTableCell(null, "tabletext", text2);
    row.appendChild(cell2);
  } else {
    var options = new Array();
    for (var n = 0; n < records.length; n++) {
       options.push(records[n].locationSeqId);
       var text = "";
       if (records[n].facilityLocationAreaId != null) {
          text += records[n].facilityLocationAreaId;
       }
       if (records[n].facilityLocationAisleId != null) {
          text += ":" + records[n].facilityLocationAisleId;
       }
       if (records[n].facilityLocationSectionId != null) {
          text += ":" + records[n].facilityLocationSectionId;
       }
       if (records[n].facilityLocationLevelId != null) {
          text += ":" + records[n].facilityLocationLevelId;
       }
       if (records[n].facilityLocationPositionId != null) {
          text += ":" + records[n].facilityLocationPositionId;
       }
       if (records[n].facilityLocationTypeEnumDescription != null) {
          text += ":" + records[n].facilityLocationTypeEnumDescription;
       }
       text += "[" + records[n].locationSeqId + "]";
       options.push(text);
    }
    options.push("");
    options.push("${uiLabelMap.ProductNoLocation?js_string}");
    
    var cell2 = opentaps.createTableCell(null, null, null, null, 2);
    var input = opentaps.createSelect(null, 'locationSeqId', 'inputBox', options);
    cell2.appendChild(input);
    row.appendChild(cell2);
  }
  table.insertBefore(row, rowAddProductFacilityLocation);

}

/*]]>*/
</script>

  <form method="post" action="<@ofbizUrl>receiveSingleInventoryProduct</@ofbizUrl>" name="selectAllForm" style="margin: 0;">
    <@inputHidden name="validateAccountingTags" value="True"/>
    <@inputHidden name="rowCount" value="1"/>
    <table border="0" cellpadding="2" cellspacing="0">
     <tbody id="tableAllForm">
      <tr id="rowBackOrderedItems" style="display:none">
        <td width="14%">&nbsp;</td>
        <td colspan="3" align="left">
          <div style="background-color: #EEE">      
            <span class="requiredField"/>${uiLabelMap.WarehouseBackOrderedProductIsBackOrdered}</span>
            <table width="100%">
             <tbody id="tableBackOrderedItems">
              <tr>
                <th class="tableheadtext" width="35%">${uiLabelMap.OpentapsReservationSequence}</th>
                <th class="tableheadtext" width="35%">${uiLabelMap.OrderOrder}</th>
                <th class="tableheadtext" width="15%">${uiLabelMap.OrderOrderQty}</th>
                <th class="tableheadtext" width="15%">${uiLabelMap.OpentapsQtyBackOrdered}</th>
              </tr>
              <tr id="rowAddBackOrderedItem"><td colspan="4"/></tr>
             <#if backOrderedItems?has_content>
              <#list backOrderedItems as backOrderedItem>
                <tr id="rowBackOrderedItem_${backOrderedItem_index}">
                  <td class="tabletext" align="center">${getLocalizedDate(backOrderedItem.reservedDatetime)} <#if backOrderedItem.sequenceId?has_content>: ${backOrderedItem.sequenceId}</#if></td>
                  <td class="tabletext" align="center">${backOrderedItem.orderId} : ${backOrderedItem.orderItemSeqId}</td>
                  <td class="tabletext" align="center">${backOrderedItem.quantity}</td>
                  <td class="requiredField" align="center">${backOrderedItem.quantityNotAvailable}</td>
                </tr>
              </#list>
             </#if>
             </tbody>
            </table>
          </div>      
        </td>
      </tr>
      <tr><td colspan="4">&nbsp;</td></tr>
      <#-- general request fields -->
      <@inputHidden name="facilityId" value=facilityId! />
      <#-- special service fields -->

      <@inputAutoCompleteProductRow name="productId" title="${uiLabelMap.ProductProductId}" onChange="javascript:onProductChange();" />
      <#assign defaultInvItemTypeId = facility.defaultInventoryItemTypeId?default("NON_SERIAL_INV_ITEM")/>
      <@inputSelectRow title="${uiLabelMap.ProductInventoryItemType}" name="inventoryItemTypeId" list=inventoryItemTypes displayField="description" default="${defaultInvItemTypeId}" onChange="javascript:changeInventoryItemTypeId(this.value)"/>
      <tr id="rowProductIdAndName" <#if !product?has_content>style="display:none"</#if>>
        <td class="titleCell"><span class="tableheadtext">${uiLabelMap.ProductProduct}</span></td>
        <td><span class="tabletext" id="spanProductIdAndName"><#if product?has_content><b>${product.internalName?default('No Internal Name')}</b> (${product.productId?if_exists})</#if></span></td>
      </tr>
      <#if goodIdentifications?has_content>
        <#list goodIdentifications as goodIdentification>
          <@displayRow title=goodIdentification.goodIdentificationTypeId text=goodIdentification.idValue! />
           <tr id="rowGoodIdentification_${goodIdentification_index}"><td class="titleCell" align="center"><span class="tableheadtext">${goodIdentification.goodIdentificationTypeId}</span></td><td class="tabletext">${goodIdentification.idValue!}</td></tr>
        </#list>
      </#if>     
      <tr id="rowAddGoodIdentification"><td span="2" /></tr>
     
      <@inputTextRow title=uiLabelMap.ProductItemDescription name="itemDescription" />
      <#-- Comment this back in when we figure out how to do 3-rd party owner, probably configured
      <@inputLookupRow title=uiLabelMap.ProductFacilityOwner name="ownerPartyId" lookup="LookupPartyName" form="selectAllForm" />
      -->
      <@inputDateTimeRow title=uiLabelMap.ProductDateReceived name="datetimeReceived" default=getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()) />
      <#-- facility location(s) -->
      <tr id="rowAddProductFacilityLocation"><td span="2" /></tr>

      <tr id="rowProductLotId">
         <@displayTitleCell title=uiLabelMap.ProductLotId />
         <td>
            <@inputLookup name="lotId" lookup="LookupLot" form="selectAllForm" />
            <a href="javascript:call_fieldlookup2(document.selectAllForm.lotId,'createLotPopupForm');" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
         </td>
      </tr>
      <@inputTextRow rowId="rowProductSerialNumber" title=uiLabelMap.ProductSerialNumber name="serialNumber" />
      <@inputTextRow rowId="rowProductQuantityAccepted" title=uiLabelMap.ProductQuantityAccepted name="quantityAccepted" default=defaultQuantity?default(1)?string.number size=10 />

      <#-- it does not seem this works, so I'm commenting it out.  Comment it back if it ever works in OFBIZ.  SC
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductQuantityRejected />
          <td>
            <@inputTextCell name="quantityRejected" size=5 />
            <select name="rejectionId" size="1" class="selectBox">
              <option></option>
              <#list rejectReasons as nextRejection>
                <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
              </#list>
            </select>
          </td>
        </tr>
      -->
      <@inputHidden name="quantityRejected" value="0"/>

      <#-- how best to handle unit costs and override permission?  if there is no unit cost, should any user be allowed to set it?
      For now, we'll do it that way for easier testing, but perhaps it's better changed for deployments.  Note the
       getProductCosts service will return a 0 if there is no unit cost.  -->
      <@inputTextRow rowId="rowProductPerUnitPrice" title=uiLabelMap.ProductPerUnitPrice name="unitCost" default=unitCost! size=10 />

      <#-- accounting tags -->
      <#if acctgTags?has_content>
        <@accountingTagsSelectRows tags=acctgTags prefix="acctgTagEnumId" />
      </#if>

      <@inputSubmitRow title=uiLabelMap.CommonReceive />
     </tbody>
    </table>
    <script type="text/javascript">
      changeInventoryItemTypeId("${inventoryItemTypeId?default(defaultInvItemTypeId)}");
      // toggle unit price line display or hide 
	  toggleDisplayProductPerUnitPrice();
    </script>

    
  </form>
