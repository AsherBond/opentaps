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

<#-- page to select shipment address, carrier, and method -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>        

<script type="text/javascript">
/*<![CDATA[*/
  var shipGroupSize = ${cart.getShipGroupSize()};

  dateCheckIntervalMillis = 1000;
  shipGroupShipBeforeDates = {};

  opentaps.addOnLoad(initializeDateCheck);
  
  var dateChangeInterval = null;
  function initializeDateCheck() {
    getShipBeforeDateValues();
    dateChangeInterval = setInterval("checkForDateChanges()", dateCheckIntervalMillis);
    checkForDateChanges();
  }
  
  <#-- Populate the initial values of the date fields -->
  function getShipBeforeDateValues() {
    for (var x = 0; x < shipGroupSize; x++) {
      var shipBeforeDateInput = document.getElementById('shipBeforeDate_' + x);
      if (shipBeforeDateInput != null) shipGroupShipBeforeDates[shipBeforeDateInput.id] = shipBeforeDateInput.value;
    }
  }

  <#-- Poll the date fields for changed values -->
  <#-- TODO: Make this a useful function for a global JS -->
  function checkForDateChanges() {
    for (var elementId in  shipGroupShipBeforeDates) {
      var shipBeforeDateInput = document.getElementById(elementId);
      var newValue = shipBeforeDateInput.value;
      var oldValue = shipGroupShipBeforeDates[elementId];
      if (newValue != oldValue) {
        var shipGroupSeqId = elementId.replace('shipBeforeDate_', '');    
        shipGroupShipBeforeDates[elementId] = newValue;
        updateShipGroup(shipGroupSeqId, false);
      }
    }
  }
  
  function updateShipBeforeDate(/*Number*/ shipGroupSeqId) {
    var shipBeforeDateInput = document.getElementById('shipBeforeDate_' + shipGroupSeqId);
    shipGroupShipBeforeDates['shipBeforeDate_' + shipGroupSeqId] = shipBeforeDateInput.value;
    updateShipGroup(shipGroupSeqId, false);
  }

  function isShippable(/*Number*/ shipGroupSeqId) {
    var contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);
    if (! contactMechIdInput) return false;
    return contactMechIdInput.options[contactMechIdInput.selectedIndex].value != '';  
  }

  function isSplittable(/*Number*/ shipGroupSeqId) {
    var maySplitInput = document.getElementById('maySplit_' + shipGroupSeqId);
    if (! maySplitInput) return false;
    return maySplitInput.options[maySplitInput.selectedIndex].value == "Y";
  }

  function isGift(/*Number*/ shipGroupSeqId) {
    var isGiftInput = document.getElementById('isGift_' + shipGroupSeqId);
    if (! isGiftInput) return false;
    return isGiftInput != null && isGiftInput.checked;
  }

  function updateShipGroup(/*Number*/ shipGroupSeqId, /*Boolean*/ updateShipGroupEstimates) {
    if (isShippable(shipGroupSeqId)) {
      contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);  <#-- document.getElementById gets the element by id from the form -->
      contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
      shippingInfoInput = document.getElementById('shippingInfo_' + shipGroupSeqId);
      shippingInfo = (shippingInfoInput && (shippingInfoInput.options.length > 0)) ? shippingInfoInput.options[shippingInfoInput.selectedIndex].value.split('^') : [];
      carrierPartyId = shippingInfo[0];
      shipmentMethodTypeId = shippingInfo[1];
    } else {
      contactMechId = null;
      carrierPartyId = null;
      shipmentMethodTypeId = 'NO_SHIPPING';
    }
    
    var shipBeforeDateInput = document.getElementById('shipBeforeDate_' + shipGroupSeqId);
    var shipBeforeDate = ((! shipBeforeDateInput) || shipBeforeDateInput.value == '') ? null : shipBeforeDateInput.value;
    
    var maySplit = (isSplittable(shipGroupSeqId)) ? "Y" : "N";
    var isGiftStr = (isGift(shipGroupSeqId)) ? "Y" : "N";

    var shippingInstructionsInput = document.getElementById('shippingInstructions_' + shipGroupSeqId);
    var shippingInstructions = ((! shippingInstructionsInput) || shippingInstructionsInput.value == '') ? null : shippingInstructionsInput.value;
    
    var giftMessageInput = document.getElementById('giftMessage_' + shipGroupSeqId);
    var giftMessage = (isGift(shipGroupSeqId) && giftMessageInput.value != '') ? giftMessageInput.value : null;

    var codInput = document.getElementById('cod_' + shipGroupSeqId);
    var cod = ((! codInput) || codInput.value == '') ? null : codInput.value;

    <#-- Build an associative Array which will be the form parameters -->
    var content = {"shipGroupSeqId" : shipGroupSeqId,
            "maySplit" : maySplit,
            "isGift" : isGiftStr,
            "isCOD" : cod};
            
    if (shipBeforeDate) {
      content["shipBeforeDate"] = shipBeforeDate;
    }
    if (contactMechId) {
      content["contactMechId"] = contactMechId;
    }
    if (carrierPartyId) {
      content["carrierPartyId"] = carrierPartyId;
    }
    if (shipmentMethodTypeId) {
      content["shipmentMethodTypeId"] = shipmentMethodTypeId;
    }
    if (shippingInstructions) {
      content["shippingInstructions"] = shippingInstructions;
    }
    if (giftMessage) {
      content["giftMessage"] = giftMessage;
    }

    var tpAccountNumberInput = document.getElementById('thirdPartyAccountNumber_' + shipGroupSeqId);
    if (tpAccountNumberInput != null) {
        content["thirdPartyAccountNumber"] = tpAccountNumberInput.value;
    }
    var tpPostalCodeInput = document.getElementById('thirdPartyPostalCode_' + shipGroupSeqId);
    if (tpPostalCodeInput != null) {
        content["thirdPartyPostalCode"] = tpPostalCodeInput.value;
    }
    var tpCountryCodeInput = document.getElementById('thirdPartyCountryCode_' + shipGroupSeqId);
    if (tpCountryCodeInput != null) {
        content["thirdPartyCountryCode"] = tpCountryCodeInput.value;
    }
    
    showOrHideShipGroupInputs(shipGroupSeqId); <#-- Modifies screen to show or hide shipping selection for this ship group -->
    showOrHideEditAddressButton(shipGroupSeqId);

    <#-- Defines a function to handle response from updateShipGroup request -->
    var handleFunction = updateShipGroupEstimates && isShippable(shipGroupSeqId) ? function(){getCartShipEstimates(shipGroupSeqId)} : function(){};
    
    var spinnerData = updateShipGroupEstimates && isShippable(shipGroupSeqId) ? {target: 'shippingInfo_' + shipGroupSeqId} : null;
    
    <#-- Call request on server and then call the handle function to deal with the response -->
    opentaps.sendRequest('<@ofbizUrl>updateShipGroup</@ofbizUrl>', content, handleFunction, spinnerData, true, 600000);
     
  }

  function getCartShipEstimates(shipGroupSeqId) {
    <#-- If this function is called, then after ship group is updated, call JSON request to get the estimates -->
    opentaps.sendRequest('<@ofbizUrl>getCartShipEstimatesJSON</@ofbizUrl>', {"shipGroupSeqId" : shipGroupSeqId}, function(data){renderCartShipEstimates(shipGroupSeqId, data)}, {target: 'shippingInfo_' + shipGroupSeqId}, true, 60000);
  }

  function renderCartShipEstimates(/*String*/ shipGroupSeqId, /*Array*/ shipEstimates) {
    var shippingInfoInput = document.getElementById('shippingInfo_' + shipGroupSeqId);
    var selectedCarrierAndMethod = (shippingInfoInput && (shippingInfoInput.options.length > 0)) ? shippingInfoInput.options[shippingInfoInput.selectedIndex].value : '';
    var storeDefaultCarrierPartyId = '${storeDefaultCarrierPartyId?default("")}';
    var storeDefaultShipmentMethodTypeId = '${storeDefaultShipmentMethodTypeId?default("")}';
    var storeDefaultExists = (storeDefaultCarrierPartyId != null && storeDefaultCarrierPartyId != null);

    if (! shipEstimates) shipEstimates = [];
    var selectedStillExists = false;
    for (x = 0; x < shipEstimates.length; x++) {
      if (shipEstimates[x].carrierPartyId + "^" + shipEstimates[x].shipmentMethodTypeId == selectedCarrierAndMethod) {
        selectedStillExists = true;
      }
    }

    var newSelect = document.createElement('select');
    newSelect.id = 'shippingInfo_' + shipGroupSeqId;
    newSelect.name = 'shippingInfo_' + shipGroupSeqId;
    newSelect.className = "inputBox shippingAddressAndMethod";
    newSelect.onchange = function() { updateShipGroup(shipGroupSeqId, false); checkCarrierPartyAccount(shipGroupSeqId);  };
    for (x = 0; x < shipEstimates.length; x++) {
      var shipEstimateData = shipEstimates[x];
      if (typeof(shipEstimateData.shipEstimate) == 'undefined') {
        shipEstimateText = '${uiLabelMap.OpentapsRateNotAvailable?js_string}';
      } else if (shipEstimateData.shipEstimateDouble == 0) {
        shipEstimateText = '${uiLabelMap.OpentapsFreeShipping?js_string}';
      } else {
        shipEstimateText = shipEstimateData.shipEstimate;
      }
      var optionText = '';
      if (shipEstimateData.userDescription) {
        optionText = shipEstimateData.userDescription;
      } else {
        optionText = shipEstimateData.carrierName;
        if (typeof(shipEstimateData.description) != 'undefined') {
          optionText += ' ' + shipEstimateData.description;
        }
      }
      optionText += ' (' + shipEstimateText + ')';
      var optionCarrierAndMethod = shipEstimateData.carrierPartyId + "^" + shipEstimateData.shipmentMethodTypeId;
      var isStoreDefault = (shipEstimateData.carrierPartyId == storeDefaultCarrierPartyId && shipEstimateData.shipmentMethodTypeId == storeDefaultShipmentMethodTypeId);
      var newOptionSelected = optionCarrierAndMethod == selectedCarrierAndMethod;
      // the default selected is either the last option selected, if it still exists, the store default, if it's in the list, otherwise the first element
      var defaultSelected = selectedStillExists ? newOptionSelected : isStoreDefault ? true : x == 0;
      var newOption = new Option(optionText, optionCarrierAndMethod, defaultSelected, newOptionSelected);
      newSelect.options[x] = newOption;
    }

    opentaps.replaceNode(shippingInfoInput, newSelect);

    if (! selectedStillExists) {
      updateShipGroup(shipGroupSeqId, false);
    }
  }

  function editCurrentAddress(/*String*/ shipGroupSeqId) {
    var url = "<@ofbizUrl>ordersEditContactMech?partyId=${orderPartyId}&preContactMechTypeId=POSTAL_ADDRESS&DONE_PAGE=crmsfaQuickCheckout&contactMechPurposeTypeId=SHIPPING_LOCATION&forCart=true</@ofbizUrl>";
    // get selected contact mech
    var contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);
    var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
    if ("" == contactMechId || "_NA_" == contactMechId) return;
    url += "&contactMechId=" + contactMechId;
    location.href = url;
  }

  function showOrHideEditAddressButton(/*String*/ shipGroupSeqId) {
    // get the button
    var editAddressButton = document.getElementById('editContactMechButton_' + shipGroupSeqId);
    if (! editAddressButton) return;
    // get selected contact mech
    var contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);
    var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
    var hideButton = false;
    // do not show edit button for no address or unknown address
    if ("" == contactMechId || "_NA_" == contactMechId) hideButton = true;
    // hide or show the button
    if (hideButton) {
      editAddressButton.style.visibility = 'hidden';
    } else {
      editAddressButton.style.visibility = 'visible';
    }
  }
  
  function showOrHideShipGroupInputs(/*String*/ shipGroupSeqId) {
    var isDropShipGroup = false;
    var supplierPartyIdInput = document.getElementById('supplierPartyId_' + shipGroupSeqId);
    if (supplierPartyIdInput != null && supplierPartyIdInput.value != '') {
      isDropShipGroup = true;
    }
    var noShipOnDropShipGroups = ${noShipOnDropShipGroups?string};
    var hideShippingInfoInput = (! isShippable(shipGroupSeqId)) || (isDropShipGroup && noShipOnDropShipGroups);

    var shippingInfoInput = document.getElementById('shippingInfo_' + shipGroupSeqId);
    if (! shippingInfoInput) return;
    var carrierPartyAccountBox = document.getElementById('carrierPartyAccountBox_' + shipGroupSeqId);

    if (hideShippingInfoInput) {
      shippingInfoInput.style.visibility = 'hidden';
      if (carrierPartyAccountBox) carrierPartyAccountBox.style.visibility = 'hidden';
    } else {
      shippingInfoInput.style.visibility = 'visible';
      showOrHideThirdPartyAccounts(shipGroupSeqId);
    }

    var giftMessageContainer = document.getElementById('giftMessageContainer_' + shipGroupSeqId);
    if (giftMessageContainer != null) {
      if (isGift(shipGroupSeqId)) {
        giftMessageContainer.style.visibility = 'visible';
      } else {
        giftMessageContainer.style.visibility = 'hidden';
      }
    }
    
    if (shippingInfoInput != null && shippingInfoInput.options != null && shippingInfoInput.options.length > 0 && shippingInfoInput.selectedIndex != -1) {
      var selectedCarrierAndMethod = shippingInfoInput.options[shippingInfoInput.selectedIndex].value;
      var carrierPartyId = selectedCarrierAndMethod.split('^')[0];
      var upsBillingContainer = document.getElementById('upsBillingDetails_' + shipGroupSeqId);
      if (upsBillingContainer != null) {
        if (isShippable(shipGroupSeqId) && carrierPartyId == 'UPS') {
          opentaps.removeClass(upsBillingContainer, 'hidden');
        } else {
          opentaps.addClass(upsBillingContainer, 'hidden');
          var thirdPartyPostalCodeInput = document.getElementById('thirdPartyPostalCode_' + shipGroupSeqId);
          if (thirdPartyPostalCodeInput != null) thirdPartyPostalCodeInput.value = '';
          var thirdPartyCountryCodeInput = document.getElementById('thirdPartyCountryCode_' + shipGroupSeqId);
          if (thirdPartyCountryCodeInput != null) thirdPartyCountryCodeInput.value = '';
        }
      }
    }
    
  }

  function updateCheckoutPayment(/*String*/ paymentMethodTypeIdAndPaymentMethodIdStr) {
    paymentMethodTypeIdAndPaymentMethodId = paymentMethodTypeIdAndPaymentMethodIdStr.split('^');
    paymentMethodTypeId = paymentMethodTypeIdAndPaymentMethodId[0];
    paymentMethodId = paymentMethodTypeIdAndPaymentMethodId[1];
    
    var content = {"checkOutPaymentId" : paymentMethodTypeId, "finalizeMode": "payment"};
    if (paymentMethodTypeId == "EXT_BILLACT") {
      content["billingAccountId"] = paymentMethodId;
    } else if (paymentMethodId != '') {
      content["checkOutPaymentId"] = paymentMethodId;
    }

    opentaps.sendRequest('<@ofbizUrl>updateCheckoutPayment</@ofbizUrl>', content);

    cvv = document.getElementById('cvv_' + paymentMethodId);
    if (cvv) setCVV(cvv.value, paymentMethodId);
    
      for (var x = 0; x < shipGroupSize; x++) {
        <#if shippingApplies>
            var codInput = document.getElementById('cod_' + x);
            codInput.value = paymentMethodTypeId == 'EXT_COD' ? "Y" : "N";
            updateShipGroup(x, true);
        <#else>
            updateShipGroup(x, false);
        </#if>
      }

    document.getElementById('continueLink').style.display = 'inline-block';
  }

  function expandCVVInput(/*String*/ paymentMethodId) {
  }
  
  function setThirdPartyDetails(/* String */ shipGroupSeqId, /* Boolean */ clear) {
    var thirdPartyInput = document.getElementById('carrierPartyAccount_' + shipGroupSeqId);
    var thirdPartyAccountNoInput = document.getElementById('thirdPartyAccountNumber_' + shipGroupSeqId);
    var thirdPartyPostalCodeInput = document.getElementById('thirdPartyPostalCode_' + shipGroupSeqId);
    var thirdPartyCountryCodeInput = document.getElementById('thirdPartyCountryCode_' + shipGroupSeqId);
    if (thirdPartyInput == null) return;
    
    var carrierInfo = thirdPartyInput.options[thirdPartyInput.selectedIndex].value.split('^');
    if (thirdPartyAccountNoInput != null) thirdPartyAccountNoInput.value = clear ? '' : carrierInfo[1];
    if (thirdPartyPostalCodeInput != null) thirdPartyPostalCodeInput.value = clear ? '' : carrierInfo[2];
    if (thirdPartyCountryCodeInput != null) thirdPartyCountryCodeInput.value = clear ? '' : carrierInfo[3];

    updateShipGroup(shipGroupSeqId, false);
  }
  
  function checkCarrierPartyAccount(/*String*/ shipGroupSeqId) {
    var shippingInfoInput = document.getElementById('shippingInfo_' + shipGroupSeqId);
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount_' + shipGroupSeqId);
    if (shippingInfoInput == null || carrierPartyAccountSelect == null) return;
    
    var selectedCarrierPartyId = shippingInfoInput.options[shippingInfoInput.selectedIndex].value.split('^')[0];
    var carrierAccountPartyId = carrierPartyAccountSelect.options[carrierPartyAccountSelect.selectedIndex].value.split('^')[0];
    if (selectedCarrierPartyId == carrierAccountPartyId) return;

    setThirdPartyDetails(shipGroupSeqId, true);
    opentaps.sendRequest('<@ofbizUrl>getPartyCarrierAccountsJSON</@ofbizUrl>', {"partyId" : '${cart.getPartyId()?js_string}'}, function(data){renderCarrierPartyAccounts(shipGroupSeqId, selectedCarrierPartyId, data)}, {target: carrierPartyAccountSelect.parentNode}, true, 60000);
  }

  function showOrHideThirdPartyAccounts( /*String*/ shipGroupSeqId ) {
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount_' + shipGroupSeqId);
    var carrierPartyAccountSelectParent = document.getElementById('carrierPartyAccountBox_' + shipGroupSeqId);

    if (!carrierPartyAccountSelect) return;

    if (carrierPartyAccountSelect && carrierPartyAccountSelectParent) {
      if (carrierPartyAccountSelect.options.length > 1) {
        carrierPartyAccountSelectParent.style.visibility = 'visible';
      } else if (carrierPartyAccountSelect.options.length <= 1) {
        carrierPartyAccountSelectParent.style.visibility = 'hidden';
      }
    }
  }

  function renderCarrierPartyAccounts(/*String*/ shipGroupSeqId, /* String */ selectedCarrierPartyId, /*Array*/ data) {
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount_' + shipGroupSeqId);
    var selectedValue = carrierPartyAccountSelect.options[carrierPartyAccountSelect.selectedIndex].value;

    var carrierAccounts = opentaps.evalJson(data);
    
    var newSelect = document.createElement('select');
    newSelect.id = 'carrierPartyAccount_' + shipGroupSeqId;
    newSelect.name = 'carrierPartyAccount_' + shipGroupSeqId;
    newSelect.className = 'inputBox';
    newSelect.onchange = function() {setThirdPartyDetails(shipGroupSeqId)};
    newSelect.options[0] = new Option("", "^^^^", false, false);
    var z = 1;
    for (var carrierPartyId in carrierAccounts) {
      if (carrierPartyId == selectedCarrierPartyId) {
        var accounts = carrierAccounts[carrierPartyId];
        for (y = 0; y < accounts.length; y++) {
          var optionValue = carrierPartyId + '^' + accounts[y].accountNumber + '^' + (accounts[y].postalCode != null ? accounts[y].postalCode : '') + '^' + (accounts[y].countryGeoCode != null ? accounts[y].countryGeoCode : '') + '^' + (accounts[y].isDefault != null ? accounts[y].isDefault : 'N');
          var optionText = accounts[y].carrierName + ' ' + accounts[y].accountNumber;
          if (accounts[y].postalCode != null && typeof(accounts[y].postalCode) != 'undefined') {
            optionText += ' (';
            if (accounts[y].countryGeoCode != null && typeof(accounts[y].countryGeoCode) != 'undefined') {
              optionText += accounts[y].countryGeoCode;
            }
            optionText += ' ' + accounts[y].postalCode + ')';          
          }
          if (optionValue == selectedValue) {
            var newOptionSelected = true;
          } else if (accounts[y].isDefault == 'Y') {
            var newOptionSelected = true;
          } else {
            var newOptionSelected = false;
          };
          var newOption = new Option(optionText, optionValue, false, newOptionSelected);
          newSelect.options[z] = newOption;
          z++;
        }
      }
    }

    opentaps.replaceNode(carrierPartyAccountSelect, newSelect);
    showOrHideThirdPartyAccounts(shipGroupSeqId);
    setThirdPartyDetails(shipGroupSeqId, false);
  }

  function setCVV(/*String*/ cvv, /*String*/ paymentMethodId) {
    opentaps.sendRequest('<@ofbizUrl>setCVV</@ofbizUrl>', {'cvv' : cvv, 'paymentMethodId' : paymentMethodId});
  }
/*]]>*/
</script>


<#assign selectedPaymentMethodId = ""/>
<#if cart.getPaymentMethodIds()?has_content>
  <#assign selectedPaymentMethodId = cart.getPaymentMethodIds().getFirst()/>
</#if>
<#assign selectedPaymentMethodTypeId = ""/>
<#if cart.getPaymentMethodTypeIds()?has_content>
  <#assign selectedPaymentMethodTypeId = cart.getPaymentMethodTypeIds().getFirst()/>
</#if>

<#assign initialContinueDisplay="none"/>
<#if selectedPaymentMethodTypeId?has_content>
  <#assign initialContinueDisplay="inline-block"/>
</#if>

<#if shippingApplies>
  <#assign extraOption><a class="subMenuButton" href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${uiLabelMap.OpentapsOrderReturnToOrder}</a><a id="continueLink" class="subMenuButton" href="<@ofbizUrl>finalizeOrder?finalizeMode=init&amp;finalizeReqShipInfo=false</@ofbizUrl>" style="display:${initialContinueDisplay}">${uiLabelMap.CrmOrderReviewOrder}</a></#assign>
  <@frameSection title=uiLabelMap.CrmOrderShipToSettings extra=extraOption>
    <form name="shipSetting" action="setShipmentOption" method="post">
      <#assign shipGroups = cart.getShipGroups()/>
      <#list shipGroups as shipGroup>
        <#assign shipGroupSeqId = shipGroups.indexOf(shipGroup)/>
        <#assign supplierPartyId = cart.getSupplierPartyId(shipGroupSeqId)?default("")/>
        <input type="hidden" id="cod_${shipGroupSeqId}" name="cod_${shipGroupSeqId}" value="N"/>
        <input type="hidden" id="supplierPartyId_${shipGroupSeqId}" name="supplierPartyId_${shipGroupSeqId}" value="${cart.getSupplierPartyId(shipGroupSeqId)?default("")}" disabled="true"/>
        <#if shipGroups?size &gt; 1 || supplierPartyId?has_content>
          <#if supplierPartyId?has_content>
            <#assign supplierPartyName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, supplierPartyId, false)/>
          <#else>
            <#assign supplierPartyName = ""/>
          </#if>
          <div class="shipGroupHeader" id="shipGroupHeader_${shipGroupSeqId}">
          <#assign shipGroupNumber = shipGroupSeqId + 1/>
          <@expandLabel label="CrmOrderShipGroupNumber" params={"shipGroupNumber", shipGroupNumber} />
          <span id="shipGroupSupplierTitle_${shipGroupSeqId}"><#if supplierPartyName?has_content>(${uiLabelMap.CrmDropShippedFrom}&nbsp;${supplierPartyName})</#if></span>
          </div>
        </#if>
        <div style="width: 100%">

          <#-- Order Items (left column) -->
          <div class="shipGroup_items" style="white-space:normal">
            <table class="shipGroupItems">
              <colgroup id="itemId"><col/></colgroup>
              <colgroup id="itemName"><col/></colgroup>
              <colgroup id="itemQuantity"><col/></colgroup>
              <th colspan="2"><div class="shipGroupSectionHeader">${uiLabelMap.CrmOrderItem}</div></th>
              <th style="text-align:center"><div class="shipGroupSectionHeader">${uiLabelMap.CrmOrderItemQty}</div></th>
              <#assign cartItems = cart.getShipGroupItems(shipGroupSeqId).keySet().toArray() />
              <#list cartItems as item>
                <#assign warnings = cart.getProductWarnings(delegator, item.getProductId())>
                <tr>
                  <td>${item.getProductId()}</td>
                  <td>${item.getName()}</td>
                  <td class="itemQuantity">${item.getQuantity()}</td>
                </tr>
                <#if warnings?has_content>
                    <#list warnings as warning>
                     <tr><td class="productWarning" colspan="3">${uiLabelMap.CrmProductWarning} : ${warning?if_exists}</td></tr>
                    </#list>                  
                </#if>                   
              </#list>
            </table>
          </div>

          <#-- Shipping Address and Method (middle) -->
          <div class="shipGroup_shippingAddressAndMethod">
            <div class="shipGroupSectionHeader">${uiLabelMap.CrmOrderShippingAddressAndMethod}
              <#if customerLink?has_content> (<a class="linktext" href="${customerLink}">${uiLabelMap.CrmCustomerDetails}</a>)</#if>
            </div>
            <div style="clear:both">
              <#-- Shipping Address -->
              <#assign shipGroupContactMechId = cart.getShippingContactMechId(shipGroupSeqId)?default("") />
              <select id="contactMechId_${shipGroupSeqId}" name="contactMechId_${shipGroupSeqId}" class="inputBox shippingAddressAndMethod" onChange="updateShipGroup(${shipGroupSeqId}, true)">
                <option value="">${uiLabelMap.CrmSelectShippingAddress}</option>
                <#if currentOrderContactMechs.get(shipGroupSeqId)?has_content >
                  <#assign contactMech = currentOrderContactMechs.get(shipGroupSeqId) />
                  <#if "_NA_" != contactMech.contactMechId>
                    <#assign address = contactMech.getRelatedOne("PostalAddress") />
                    <option value="${contactMech.contactMechId}" ${(contactMech.contactMechId == shipGroupContactMechId)?string("selected=\"selected\"", "")}>
                      ${address.address1} - ${address.city?if_exists} <#if address.countryGeoId?has_content > - <@displayGeoName geoId=address.countryGeoId /></#if>
                    </option>
                  </#if>
                </#if>
                <option value="_NA_" ${("_NA_" == shipGroupContactMechId)?string("selected=\"selected\"", "")}>${uiLabelMap.CrmAddressUnknown}</option>
                <#list shippingContactMechList as contactMech>
                  <#assign address = contactMech.getRelatedOne("PostalAddress") />
                  <option value="${contactMech.contactMechId}" ${(contactMech.contactMechId == shipGroupContactMechId)?string("selected=\"selected\"", "")}>
                  <#if address.toName?has_content>${address.toName}, </#if>${address.address1}, ${address.city?if_exists}<#if address.stateProvinceGeoId?has_content>, ${address.stateProvinceGeoId}</#if>
                  </option>
                </#list>
              </select>
            </div>
            <div style="clear:both">
              <#-- Shipping Method -->
              <select id="shippingInfo_${shipGroupSeqId}" name="shippingInfo_${shipGroupSeqId}" class="inputBox shippingAddressAndMethod" onChange="updateShipGroup(${shipGroupSeqId}, false);checkCarrierPartyAccount(${shipGroupSeqId});" style="visibility:${("NO_SHIPPING" == shipGroup.shipmentMethodTypeId?default(""))?string("hidden", "visible")}">
                <#assign carrierShipmentMethodList = carrierShipmentMethods.get(shipGroupSeqId)?default([])/>
                <#list carrierShipmentMethodList as carrierMethod>
                  <#assign carrierPartyId = carrierMethod.carrierPartyId?default("_NA_")/>
                  <#assign carrierPartyName = carrierMethod.carrierName?default("")/>
                  <#assign compoundKey = carrierPartyId + "^" + carrierMethod.shipmentMethodTypeId />
                  <#if (cart.getCarrierPartyId(shipGroupSeqId)?default("") + "^" + cart.getShipmentMethodTypeId(shipGroupSeqId)?default("")) == compoundKey><#assign selected = "selected"/><#else><#assign selected = ""/></#if>
                  <#assign shippingEstimate = carrierMethod.shipEstimate?if_exists/>
                  <#assign shipEstimateDouble = carrierMethod.shipEstimateDouble?if_exists/>
                  <option value="${compoundKey}" ${selected}>
                   <#if carrierMethod.userDescription?has_content>${carrierMethod.userDescription}<#else>${carrierPartyName} ${carrierMethod.description?if_exists}</#if>
                   <#if shippingEstimate?has_content>
                      (<#if shipEstimateDouble?default(0.0) == 0.0>${uiLabelMap.OpentapsFreeShipping}<#else>${shippingEstimate}</#if>)
                    <#else>
                      (${uiLabelMap.OpentapsNotAvailable})
                    </#if>
                  </option>
                </#list>
              </select>
            </div>
            <div style="text-align: center; clear:both; margin-bottom: 3px">
              <a class="buttontext" href="<@ofbizUrl>ordersEditContactMech?partyId=${orderPartyId}&preContactMechTypeId=POSTAL_ADDRESS&DONE_PAGE=crmsfaQuickCheckout&contactMechPurposeTypeId=SHIPPING_LOCATION&forCart=true</@ofbizUrl>">${uiLabelMap.PartyAddNewAddress}</a>
              <a id="editContactMechButton_${shipGroupSeqId}" class="buttontext" href="#" onClick="editCurrentAddress(${shipGroupSeqId});">${uiLabelMap.CrmEditSelectedAddress}</a>
            </div>
            <#assign carrierPartyId = cart.getCarrierPartyId(shipGroupSeqId)?if_exists/>
            <#if ! supplierPartyId?has_content && carrierPartyId?has_content>
              <#-- Third Party Accounts -->
              <#assign carrierAccounts = thirdPartyInfo?default({}).get(carrierPartyId)?default([])/>
              <div id="carrierPartyAccountBox_${shipGroupSeqId}" style="visibility:${carrierAccounts?has_content?string("visible", "hidden")};text-align:right">
                <span class="shipGroupLabel" >${uiLabelMap.CrmAccounts}:&nbsp;</span>
                <select id="carrierPartyAccount_${shipGroupSeqId}" class="inputBox" onChange="setThirdPartyDetails(${shipGroupSeqId})">
                  <option value="^^^^">&nbsp;</option>
                  <#list carrierAccounts as carrierInfo>
                    <option value="${carrierPartyId}^${carrierInfo.accountNumber?if_exists}^${carrierInfo.postalCode?if_exists}^${carrierInfo.countryGeoCode?if_exists}^${carrierInfo.isDefault?if_exists}">
                      ${carrierInfo.carrierName}:&nbsp;${carrierInfo.accountNumber?if_exists}
                      <#if carrierInfo.postalCode?has_content>(${carrierInfo.countryGeoCode?if_exists} ${carrierInfo.postalCode})</#if>
                    </option>
                  </#list>
                </select>
              </div>
              <div>
                <div style="text-align:right">
                  <span class="shipGroupLabel" >${uiLabelMap.CrmOrderThirdPartyAccountNo}:&nbsp;</span>
                  <input name="thirdPartyAccountNumber_${shipGroupSeqId}" id="thirdPartyAccountNumber_${shipGroupSeqId}" type="text" class="inputBox" size="10" maxlength="10" value="${cart.getThirdPartyAccountNumber(shipGroupSeqId)?if_exists}" onChange="updateShipGroup(${shipGroupSeqId}, false)"/>
                </div>
                <div id="upsBillingDetails_${shipGroupSeqId}" style="text-align:right;" <#if cart.getCarrierPartyId(shipGroupSeqId) != "UPS">class="hidden"</#if>>
                  <div>
                    <span class="shipGroupLabel" >${uiLabelMap.CrmOrderThirdPartyAccountZip}:&nbsp;</span>
                    <input name="thirdPartyPostalCode_${shipGroupSeqId}" id="thirdPartyPostalCode_${shipGroupSeqId}" type="text" class="inputBox" size="10" maxlength="10" value="${cart.getThirdPartyPostalCode(shipGroupSeqId)?default(billToAccountZipCode?if_exists)}" onChange="updateShipGroup(${shipGroupSeqId}, false)"/>
                    <input name="thirdPartyCountryCode_${shipGroupSeqId}" id="thirdPartyCountryCode_${shipGroupSeqId}" type="text" class="inputBox" size="3" maxlength="3" value="${cart.getThirdPartyCountryCode(shipGroupSeqId)?default(billToAccountGeoCode?if_exists)}" onChange="updateShipGroup(${shipGroupSeqId}, false)"/>
                  </div>
                </div>
              </div>
            </#if>
          </div>

          <#-- Shipping Instructions (right) -->
          <div class="shipGroup_shippingInstructions">
            <div class="shipGroupSectionHeader">${uiLabelMap.CrmOrderShippingInstructions}</div>
            <div class="tabletext" style="clear:both">
              <#assign shippingInstructions = cart.getShippingInstructions(shipGroupSeqId)?default("")/>
              <textarea name="shippingInstructions_${shipGroupSeqId}" id="shippingInstructions_${shipGroupSeqId}" onChange="updateShipGroup(${shipGroupSeqId})">${shippingInstructions}</textarea>
            </div>
            <div style="clear:both">
              <#assign maySplit = (cart.getMaySplit(shipGroupSeqId) == "Y")/>
              <select class="inputBox" name="maySplit_${shipGroupSeqId}" id="maySplit_${shipGroupSeqId}" onChange="updateShipGroup(${shipGroupSeqId})" style="width: 100%">
                <option value="N" ${maySplit?string("selected=\"selected\"","")}>${uiLabelMap.OrderShipAllItemsTogether}</option>
                <option value="Y" ${maySplit?string("selected=\"selected\"","")}>${uiLabelMap.OrderShipItemsWhenAvailable}</option>
              </select>
            </div>
            <#if cart.getShipBeforeDate(shipGroupSeqId)?has_content>
              <#assign shipBeforeDate = getLocalizedDate(cart.getShipBeforeDate(shipGroupSeqId), "DATE")/>
            <#else>
              <#assign shipBeforeDate = ""/>
            </#if>
            <div style="text-align:right; clear:both">
              <span class="shipGroupLabel" >${uiLabelMap.OrderShipBeforeDate}:&nbsp;</span>
              <@inputDate name="shipBeforeDate_${shipGroupSeqId}" default=shipBeforeDate onChange="updateShipBeforeDate(${shipGroupSeqId})"/>
            </div>
            <#if showCheckoutGiftOptions>
              <div style="text-align:right;clear:both">
                <#assign isGift = (cart.getIsGift(shipGroupSeqId) == "Y")/>
                <span class="shipGroupLabel" >${uiLabelMap.CrmOrderIsGift}:&nbsp;</span>
                <span class="tabletext"><input type="radio" id="isGift_${shipGroupSeqId}" name="isGift_${shipGroupSeqId}" onclick="updateShipGroup(${shipGroupSeqId})" ${isGift?string("checked=\"checked\"","")}/>${uiLabelMap.CommonY}</span>
                <span class="tabletext"><input type="radio" name="isGift_${shipGroupSeqId}" onclick="updateShipGroup(${shipGroupSeqId})" ${isGift?string("","checked=\"checked\"")}/>${uiLabelMap.CommonN}</span>
                <div id="giftMessageContainer_${shipGroupSeqId}" style="clear:both;visibility:${isGift?string("visible", "hidden")}">
                  <span class="shipGroupLabel" style="margin-bottom:3px">${uiLabelMap.OrderGiftMessage}:&nbsp;</span>
                  <#assign giftMessage = cart.getGiftMessage(shipGroupSeqId)?default("")/>
                  <div class="tabletext" style="clear:both">
                    <textarea name="giftMessage_${shipGroupSeqId}" id="giftMessage_${shipGroupSeqId}" class="giftMessage" onChange="updateShipGroup(${shipGroupSeqId})">${giftMessage}</textarea>
                  </div>
                </div>
              </div>
            </#if>
          </div>
        </div>
        <div style="clear:both">&nbsp;</div>
      </#list>
    </form>
  </@frameSection>
</#if>

<script type="text/javascript">
    showOrHideShipGroupInputs(${shipGroupSeqId?if_exists});
    showOrHideEditAddressButton(${shipGroupSeqId?if_exists});
    <#if shipGroupSeqId?exists>updateShipGroup(${shipGroupSeqId}, true);</#if>
    showOrHideThirdPartyAccounts(${shipGroupSeqId?if_exists});
</script>

<#assign extraOption>
  <#if ! shippingApplies>
    <a class="subMenuButton" href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${uiLabelMap.OpentapsOrderReturnToOrder}</a>
    <a id="continueLink" class="subMenuButton" href="<@ofbizUrl>finalizeOrder?finalizeMode=init</@ofbizUrl>" style="display:${initialContinueDisplay}">${uiLabelMap.CrmOrderReviewOrder}</a>
  </#if>
</#assign>
<@frameSectionHeader title=uiLabelMap.CrmOrderPaymentSettings extra=extraOption/>
<div class="form">
<form name="paymentSetting" action="setBilling" method="post">
  <div class="quickPaymentSetting">
    <#assign doNotShow = ["EXT_BILL_3RDPTY", "EXT_BILL_3RDPTY_COD"]/>
    <#assign doNotShowIfEmpty = ["EXT_BILLACT", "FIN_ACCOUNT", "GIFT_CARD"]/>
    <#assign showNewLink = ["CREDIT_CARD", "EFT_ACCOUNT"]/>
    <#list nonExternalPaymentMethodTypes as productStorePaymentMethodType>
      <#assign productStorePaymentMethodTypeId = productStorePaymentMethodType.paymentMethodTypeId/>
      <#assign partyPaymentMethodsForType = orderPartyPaymentMethods.get(productStorePaymentMethodTypeId)?if_exists/>
      <#if ! doNotShow?seq_contains(productStorePaymentMethodTypeId) >
        <#if partyPaymentMethodsForType?has_content || (! doNotShowIfEmpty?seq_contains(productStorePaymentMethodTypeId)) >
          <#assign paymentMethodType = productStorePaymentMethodType.getRelatedOne("PaymentMethodType")/>
          <div class="paymentSettingHeader">
            ${paymentMethodType.description}
            <#assign updateUrl = ""/>
            <#if showNewLink?seq_contains(productStorePaymentMethodTypeId)>
              <#if productStorePaymentMethodTypeId == "CREDIT_CARD">
                <#assign updateUrl = "ordersEditCreditCard"/>
              <#elseif productStorePaymentMethodTypeId == "EFT_ACCOUNT">
                <#assign updateUrl = "ordersEditEftAccount"/>
              </#if>
              <#if updateUrl?has_content>
                <a href="<@ofbizUrl>${updateUrl}</@ofbizUrl>?externalLoginKey=${externalLoginKey}&partyId=${orderPartyId}&DONE_PAGE=crmsfaQuickCheckout&donePage=crmsfaQuickCheckout" class="buttontext">${uiLabelMap.CommonAdd}</a>
              </#if>
            </#if>
          </div>
          <#if partyPaymentMethodsForType?has_content>
            <#compress>
              <#list partyPaymentMethodsForType as paymentMethod>
                <div><@renderPaymentMethod paymentMethodTypeId=productStorePaymentMethodTypeId paymentMethod=paymentMethod index=paymentMethod_index /></div>
              </#list>
            </#compress>
          </#if>
        </#if>
      </#if>
    </#list>
    <#if externalPaymentMethodTypes?has_content>
      <div class="paymentSettingHeader">${uiLabelMap.CrmOrderOtherPaymentMethods}</div>
    </#if>
    <#list externalPaymentMethodTypes as productStorePaymentMethodType>
      <#assign productStorePaymentMethodTypeId = productStorePaymentMethodType.paymentMethodTypeId/>
      <#if ! doNotShow?seq_contains(productStorePaymentMethodTypeId) >
        <#assign paymentMethodType = productStorePaymentMethodType.getRelatedOneCache("PaymentMethodType")/>
        <#if (selectedPaymentMethodTypeId + "^") == (productStorePaymentMethodTypeId + "^")>
          <#assign checked = "checked=\"checked\""/>
        <#elseif defaultPaymentMethodTypeId?default("") == productStorePaymentMethodTypeId>
          <#assign checked = "checked=\"checked\""/>
        <#else>
          <#assign checked = ""/>
        </#if>
        <div>
          <input type="radio" id="productStorePaymentMethodTypeId_${productStorePaymentMethodTypeId}" name="paymentMethodTypeAndId" value="${productStorePaymentMethodTypeId}^" ${checked} onclick="updateCheckoutPayment(this.value);"/>&nbsp;${paymentMethodType.description}
        </div>
      </#if>
    </#list>
  </div>
</form>
</div>

<#macro renderPaymentMethod index paymentMethodTypeId=paymentMethodTypeId paymentMethod=paymentMethod updateUrl=updateUrl>
  <#assign inputString = ""/>
  <#if paymentMethodTypeId == "CREDIT_CARD">
    <#assign paymentMethodId = paymentMethod.paymentMethodId/>
    <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")>
    <#assign inputString = inputString + Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)/>
    <#if paymentMethod.description?has_content>
      <#assign inputString = inputString + "&nbsp;(" + paymentMethod.description + ")"/>
    </#if>
  <#elseif paymentMethodTypeId == "EFT_ACCOUNT">
    <#assign paymentMethodId = paymentMethod.paymentMethodId/>
    <#assign inputString = inputString + paymentMethod.bankName?if_exists + ":" + paymentMethod.accountNumber?if_exists/>
    <#if paymentMethod.description?has_content>
      <#assign inputString = inputString + "&nbsp;(" + paymentMethod.description + ")"/>
    </#if>
  <#elseif paymentMethodTypeId == "EXT_BILLACT">
    <#assign paymentMethodId = paymentMethod.billingAccountId/>
    <#assign inputString = inputString  + "&nbsp;" + paymentMethod.description?default("Credit Account")/>    
  </#if>
  <#if (selectedPaymentMethodTypeId + "^" + selectedPaymentMethodId) == (paymentMethodTypeId + "^" + paymentMethodId)>
    <#assign checked = "checked=\"checked\""/>
  <#elseif index == 0 && defaultPaymentMethodTypeId?default("") == paymentMethodTypeId>
    <#assign checked = "checked=\"checked\""/>
  <#else>
    <#assign checked = "" />
  </#if>
  <#if updateUrl?has_content>
    <#assign creditCardText>
      <a href="${updateUrl}?party_id=${orderPartyId}&paymentMethodId=${paymentMethodId}&DONE_PAGE=crmsfaQuickCheckout&donePage=crmsfaQuickCheckout" class="linktext" title="${uiLabelMap.AccountingEditCreditCard}">${inputString}</a>
    </#assign>
  <#else><#assign creditCardText = inputString>
  </#if>
  <input type="radio" name="paymentMethodTypeAndId" value="${paymentMethodTypeId}^${paymentMethodId}" ${checked} onclick="updateCheckoutPayment(this.value);"/>&nbsp;${creditCardText}
  <#if paymentMethodTypeId == "CREDIT_CARD">
      <#assign paymentInfo = cart.getPaymentInfo(paymentMethodId)?if_exists>
      <#if paymentInfo?exists><#assign cvv = paymentInfo.securityCode?if_exists></#if>
      &nbsp;<span class="tableheadtext">CVV</span>: <input type="text" size="5" id="cvv_${paymentMethodId}" name="cvv_${paymentMethodId}" class="inputBox" value="${cvv?if_exists}" onChange="setCVV(this.value, '${paymentMethodId}')">
  </#if>

</#macro>
