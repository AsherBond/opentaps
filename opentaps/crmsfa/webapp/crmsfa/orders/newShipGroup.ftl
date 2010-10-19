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

<script type="text/javascript">
/*<![CDATA[*/

  // Build the third party info hash as a JS object
  // the structure is {carrierPartyId => [ { 'accountNumber' => accountNumber, 'carrierName' => carrierPartyId, 'postalCode' => postalCode, 'countryGeoCode' => countryGeoCode } ]}
  var thirdPartyInfoHash = null;
  function getThirdPartyInfoHash() {
    if (thirdPartyInfoHash != null) return thirdPartyInfoHash;

    thirdPartyInfoHash = new Array();
    var supplierInfoList = null;
    var supplierInfo = null;
    <#if thirdPartyInfo?has_content>
      <#list thirdPartyInfo.keySet() as carrierPartyId>
        supplierInfoList = new Array();
        // ${carrierPartyId}
        <#list thirdPartyInfo.get(carrierPartyId) as info>
          supplierInfo = new Array();
          <#list info.keySet() as k>
            supplierInfo['${k}'] = '${info.get(k)}';
          </#list>
          supplierInfoList.push( supplierInfo );
        </#list>
        thirdPartyInfoHash['${carrierPartyId}'] = supplierInfoList;
      </#list>
    </#if>
    return thirdPartyInfoHash;
  }

  function getSelectedCarrierPartyId() {
    var shippingMethodInput = document.getElementById('shippingMethod');
    if (!shippingMethodInput) return '';

    return shippingMethodInput.options[shippingMethodInput.selectedIndex].value.split('@')[1];
  }

  function renderCarrierPartyAccounts(/* String */ selectedCarrierPartyId) {
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount');
    var selectedValue = carrierPartyAccountSelect.options[carrierPartyAccountSelect.selectedIndex].value;

    var thirdPartyInfoHash = getThirdPartyInfoHash();
    var carrierAccounts = thirdPartyInfoHash[selectedCarrierPartyId];

    var newSelect = document.createElement('select');
    newSelect.id = 'carrierPartyAccount';
    newSelect.name = 'carrierPartyAccount';
    newSelect.className = 'inputBox';
    newSelect.onchange = function() {setThirdPartyDetails()};
    newSelect.options[0] = new Option("", "^^^", false, false);
    var z = 1;
    if (carrierAccounts) {
      for (var a = 0; a < carrierAccounts.length; a++) {
        var accounts = carrierAccounts[a];
        var accountNumber = accounts['accountNumber'];
        var postalCode = accounts['postalCode'];
        var countryGeoCode = accounts['countryGeoCode'];
        var carrierName = accounts['carrierName'];
        var optionValue = selectedCarrierPartyId + '^' + accountNumber + '^' + postalCode + '^' + countryGeoCode;
        var optionText = carrierName + ' ' + accountNumber;
        if (postalCode != null && typeof(postalCode) != 'undefined') {
          optionText += ' (';
          if (countryGeoCode != null && typeof(countryGeoCode) != 'undefined') {
            optionText += countryGeoCode;
          }
          optionText += ' ' + postalCode + ')';
        }
        var newOptionSelected = optionValue == selectedValue;
        var newOption = new Option(optionText, optionValue, false, newOptionSelected);
        newSelect.options[z] = newOption;
        z++;
      }
    }

    opentaps.replaceNode(carrierPartyAccountSelect, newSelect);
    showOrHideThirdPartyAccounts();
  }


  function checkCarrierPartyAccount(/*Boolean*/ setValues) {
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount');
    if (!carrierPartyAccountSelect) return;

    var selectedCarrierPartyId = getSelectedCarrierPartyId();
    var carrierAccountPartyId = carrierPartyAccountSelect.options[carrierPartyAccountSelect.selectedIndex].value.split('^')[0];
    if (selectedCarrierPartyId == carrierAccountPartyId) return;

    if (setValues) setThirdPartyDetails(true);
    renderCarrierPartyAccounts(selectedCarrierPartyId);
  }

  function setThirdPartyDetails(/* Boolean */ clear) {
    var thirdPartyInput = document.getElementById('carrierPartyAccount');
    var thirdPartyAccountNoInput = document.getElementById('thirdPartyAccountNumber');
    var thirdPartyPostalCodeInput = document.getElementById('thirdPartyPostalCode');
    var thirdPartyCountryCodeInput = document.getElementById('thirdPartyCountryCode');
    if (!thirdPartyInput) return;

    var carrierInfo = thirdPartyInput.options[thirdPartyInput.selectedIndex].value.split('^');
    if (thirdPartyAccountNoInput != null) thirdPartyAccountNoInput.value = clear ? '' : carrierInfo[1];
    if (thirdPartyPostalCodeInput != null) thirdPartyPostalCodeInput.value = clear ? '' : carrierInfo[2];
    if (thirdPartyCountryCodeInput != null) thirdPartyCountryCodeInput.value = clear ? '' : carrierInfo[3];
  }

  function autoSelectCarrierPartyAccount() {
    var selectedCarrierPartyId = getSelectedCarrierPartyId();
    var thirdPartyInput = document.getElementById('carrierPartyAccount');
    var thirdPartyAccountNoInput = document.getElementById('thirdPartyAccountNumber');
    var thirdPartyPostalCodeInput = document.getElementById('thirdPartyPostalCode');
    var thirdPartyCountryCodeInput = document.getElementById('thirdPartyCountryCode');
    if (!thirdPartyInput || !thirdPartyAccountNoInput || !thirdPartyPostalCodeInput || !thirdPartyCountryCodeInput || !selectedCarrierPartyId) return;

    var thirdPartyInputKey = selectedCarrierPartyId + '^' + thirdPartyAccountNoInput.value + '^' + thirdPartyPostalCodeInput.value + '^' + thirdPartyCountryCodeInput.value;
    for (var i = 0; i < thirdPartyInput.options.length; i++) {
      if (thirdPartyInput.options[i].value == thirdPartyInputKey) {
        thirdPartyInput.value = thirdPartyInputKey;
        return;
      }
    }
    thirdPartyInput.value = '^^^';
  }

  function showOrHideEditAddressButton() {
    // get the button
    var editAddressButton = document.getElementById('editContactMechButton');
    if (! editAddressButton) return;
    // get selected contact mech
    var contactMechIdInput = document.getElementById('contactMechId');
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

  function showOrHideThirdPartyAccounts() {
    var carrierPartyAccountSelect = document.getElementById('carrierPartyAccount');
    var carrierPartyAccountHelp = document.getElementById('carrierPartyAccount2');

    if (carrierPartyAccountSelect) {
      if (carrierPartyAccountSelect.options.length > 1) {
        opentaps.addClass(carrierPartyAccountHelp, 'hidden');
        opentaps.removeClass(carrierPartyAccountSelect, 'hidden');
      } else if (carrierPartyAccountSelect.options.length <= 1) {
        opentaps.removeClass(carrierPartyAccountHelp, 'hidden');
        opentaps.addClass(carrierPartyAccountSelect, 'hidden');
      }
    }
  }

  function showOrHideShippingInfo() {
    // get the select
    var shippingMethodSelect = document.getElementById('shippingMethod');
    var shippingMethodHelp = document.getElementById('shippingMethod2');
    if (! shippingMethodSelect) return;
    // get selected contact mech
    var contactMechIdInput = document.getElementById('contactMechId');
    var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
    var hide = false;
    // do not show edit button for no address
    if ("" == contactMechId) hide = true;
    // hide or show
    if (hide) {
      opentaps.addClass(shippingMethodSelect, 'hidden');
      opentaps.removeClass(shippingMethodHelp, 'hidden');
    } else {
      opentaps.removeClass(shippingMethodSelect, 'hidden');
      opentaps.addClass(shippingMethodHelp, 'hidden');
    }
  }

  function showOrHideUpsBillingDetails() {
    // get the zone to show/hide
    var upsBillingDetails = document.getElementById('upsBillingDetailsRow');
    if (! upsBillingDetails) return;

    var selectedCarrierPartyId = getSelectedCarrierPartyId();

    var hide = true;
    // do not show edit button for no address
    if ("UPS" == selectedCarrierPartyId) hide = false;
    // hide or show
    if (hide) {
      upsBillingDetails.style.visibility = 'hidden';
    } else {
      upsBillingDetails.style.visibility = 'visible';
    }
  }

  function showOrHideGiftMessage() {
    // get the zone to show/hide
    var isGift = document.getElementById('isGift');
    var giftMessage = document.getElementById('giftMessage');
    var giftMessageTitleCell = document.getElementById('giftMessageTitleCell');
    if (! isGift || ! giftMessage) return;

    var hide = ! isGift.checked;

    // hide or show
    if (hide) {
      giftMessage.style.visibility = 'hidden';
      giftMessageTitleCell.style.visibility = 'hidden';
    } else {
      giftMessage.style.visibility = 'visible';
      giftMessageTitleCell.style.visibility = 'visible';
    }
  }

  function updateShippingEstimates() {

  }

  function editCurrentAddress() {
    var url = "<@ofbizUrl>newShipGroupEditContactMech?partyId=${customerPartyId}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=newShipGroup%3ForderId%3D${orderId}&orderId=${orderId}</@ofbizUrl>";
    // get selected contact mech
    var contactMechIdInput = document.getElementById('contactMechId');
    var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
    if ("" == contactMechId || "_NA_" == contactMechId) return;
    url += "&contactMechId=" + contactMechId;
    location.href = url;
  }

/*]]>*/
</script>

<#if orderHeader?exists>
  <#assign sectionTitle >
    <@expandLabel label="CrmOrderNewShipGroupForOrder" params={"orderId", orderId, "shipGroupSeqId", shipGroupSeqId} />
  </#assign>
  <@sectionHeader title=sectionTitle />
  <form method="POST" action="<@ofbizUrl>createShipGroup</@ofbizUrl>" name="createShipGroup">
    <@inputHidden name="orderId" value=orderHeader.orderId />
    <@inputHiddenUseRowSubmit />
    <@inputHiddenRowCount list=orderItems />

    <div class="screenlet">

      <table class="fourColumnForm" style="border:0">
        <#-- Customer name (for reference, and link to details page) -->
        <tr>
          <#if customerParty?has_content>
            <@displayTitleCell title=uiLabelMap.CrmCustomer />
            <@displayPartyLinkCell partyId=customerParty.partyId />
          <#else>
            <td colspan="2">&nbsp;</td>
          </#if>
          <td valign="left"><@display text=uiLabelMap.CrmOrderShippingInstructions class="tableheadtext"/></td>
        </tr>
        <#-- Shipping Address -->
        <tr>
          <@displayTitleCell title=uiLabelMap.OpentapsShippingAddress />
          <td>
            <select id="contactMechId" name="contactMechId" class="inputBox shippingAddressAndMethod" onChange="updateShippingEstimates();showOrHideEditAddressButton();showOrHideShippingInfo()">
              <#-- the empty option only appears when no default value can be found -->
              <option value="_NA_">${uiLabelMap.CrmAddressUnknown}</option>
              <#list shippingAddresses as address>
                <option value="${address.contactMechId}" ${(contactMechId == address.get("contactMechId"))?string('selected="selected"','')}>
                  ${address.address1} - ${address.city?if_exists} <#if address.countryGeoId?has_content > - <@displayGeoName geoId=address.countryGeoId /></#if>
                </option>
              </#list>
            </select>
            <#-- Edit and New Address buttons -->
            <a class="buttontext" href="<@ofbizUrl>newShipGroupEditContactMech?partyId=${customerPartyId}&preContactMechTypeId=POSTAL_ADDRESS&DONE_PAGE=newShipGroup%3ForderId%3D${orderId}&contactMechPurposeTypeId=SHIPPING_LOCATION&orderId=${orderHeader.orderId}</@ofbizUrl>">${uiLabelMap.CommonNew}</a>
            <a id="editContactMechButton" class="buttontext" href="#" onClick="editCurrentAddress()">${uiLabelMap.CommonEdit}</a>
          </td>
          <#-- Shipping Instructions -->
          <td rowspan="2" valign="left">
            <textarea name="shippingInstructions" style="width:100%">${shippingInstructions?if_exists}</textarea>
          </td>
        </tr>
        <#-- Shipping Method -->
        <tr>
          <@displayTitleCell title=uiLabelMap.CrmOrderShippingMethod />
          <td>
            <select id="shippingMethod" name="shippingMethod" class="inputBox shippingAddressAndMethod" onChange="checkCarrierPartyAccount(true);showOrHideUpsBillingDetails()">
              <#list productStoreShipmentMethList as productStoreShipmentMethod>
                <#assign shipmentMethodTypeAndParty = productStoreShipmentMethod.shipmentMethodTypeId + "@" + productStoreShipmentMethod.partyId />
                <#if productStoreShipmentMethod.partyId?has_content || productStoreShipmentMethod?has_content>
                  <option value="${shipmentMethodTypeAndParty?if_exists}" <#if shipmentMethodTypeAndParty = shippingMethodCode?default("")>selected="selected"</#if>><#if productStoreShipmentMethod.partyId != "_NA_">${productStoreShipmentMethod.partyId?if_exists}</#if>&nbsp;${productStoreShipmentMethod.get("description",locale)?default("")}</option>
                </#if>
            </#list>
            </select>
            <span id="shippingMethod2" class="tabletext hidden">${uiLabelMap.CrmOrderSelectShippingAddressFirst}</span>
          </td>
        </tr>
        <tr>
          <#-- Third Party Accounts -->
          <@displayTitleCell title=uiLabelMap.CrmAccounts />
          <td>
            <select id="carrierPartyAccount" class="inputBox" onChange="setThirdPartyDetails()">
              <option value="^^^">&nbsp;</option>
            </select>
            <span id="carrierPartyAccount2" class="tabletext hidden">${uiLabelMap.CrmOrderNoShippingAccountForSelectedMethod}</span>
          </td>
          <#-- May split option -->
          <td>
            <select class="inputBox" name="maySplit" style="width: 100%">
              <option value="N" ${("N" == maySplit)?string("selected=\"selected\"","")}>${uiLabelMap.OrderShipAllItemsTogether}</option>
              <option value="Y" ${("Y" == maySplit)?string("selected=\"selected\"","")}>${uiLabelMap.OrderShipItemsWhenAvailable}</option>
            </select>
          </td>
        </tr>
        <tr>
          <#-- Third Party Account No -->
          <@displayTitleCell title=uiLabelMap.CrmOrderThirdPartyAccountNo />
          <@inputTextCell name="thirdPartyAccountNumber" id="thirdPartyAccountNumber" size="10" maxlength="10" default="${billToAccountNumber?if_exists}" onChange="autoSelectCarrierPartyAccount()"/>
          <#-- Is Gift option -->
          <td>
            <@display text=uiLabelMap.CrmOrderIsGift class="tableheadtext" />
            <input type="radio" name="isGift" value="Y" id="isGift" onChange="showOrHideGiftMessage()" ${(isGift == "Y")?string("checked=\"checked\"","")} />${uiLabelMap.CommonY}
            <input type="radio" name="isGift" value="N" onChange="showOrHideGiftMessage()" ${(isGift == "N")?string("checked=\"checked\"","")} />${uiLabelMap.CommonN}
          </td>
        </tr>
        <#-- Third Party Zip Code / Only for UPS -->
        <tr id="upsBillingDetailsRow">
          <@displayTitleCell title=uiLabelMap.CrmOrderThirdPartyAccountZip />
          <td>
            <@inputText name="thirdPartyPostalCode" id="thirdPartyPostalCode" size="10" maxlength="10" default="${billToAccountZipCode?if_exists}" onChange="autoSelectCarrierPartyAccount()"/>
            <@inputText name="thirdPartyCountryCode" id="thirdPartyCountryCode" size="3" maxlength="3" default="${billToAccountGeoCode?if_exists}" onChange="autoSelectCarrierPartyAccount()"/>
          </td>
          <td id="giftMessageTitleCell"><@display text=uiLabelMap.OrderGiftMessage class="tableheadtext" /></td>
        </tr>
        <tr>
          <#-- Ship before date -->
          <@displayTitleCell title=uiLabelMap.OrderShipBeforeDate />
          <@inputDateCell name="shipByDate" default=shipBeforeDate?if_exists />
          <#-- Gift message -->
          <td rowspan="2">
            <textarea name="giftMessage" class="giftMessage" id="giftMessage"  style="width:100%">${giftMessage?if_exists}</textarea>
          </td>
        </tr>
        <#-- empty row for spacing -->
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td colspan="3">
            <#if orderItems.size() != 0>
              <table class="listTable" cellspacing="0">
                <tbody>
                  <tr class="boxtop">
                    <td class="boxhead">${uiLabelMap.ProductProduct}</td>
                    <td class="boxhead"><@expandLabel label="CrmOrderShipGroupQuantity" params={"shipGroupSeqId", shipGroupSeqId} /></td>
                    <td class="boxhead">${uiLabelMap.CrmOrderNewShipGroupQuantity}</td>
                  </tr>

                  <#list orderItems as orderItem>
                    <@inputHidden name="orderId" value=orderItem.orderId index=orderItem_index />
                    <@inputHidden name="orderItemSeqId" value=orderItem.orderItemSeqId index=orderItem_index />
                    <@inputHidden name="shipGroupSeqId" value=orderItem.shipGroupSeqId index=orderItem_index />
                    <@inputHiddenRowSubmit index=orderItem_index />
                    <tr class="${tableRowClass(orderItem_index)}">
                      <@displayCell text="${orderItem.productId?default('N/A')} - ${orderItem.itemDescription?if_exists}" />
                      <@displayCell text=orderItem.remainingQty />
                      <@inputTextCell name="qtyToTransfer" index=orderItem_index size="6" />
                    </tr>
                  </#list>
                </tbody>
              </table>
            <#else>
              <@display text=uiLabelMap.OrderNoOrderItemsToDisplay />
            </#if>
          </td>
        </tr>
        <tr>
          <td/>
          <td>
            <@inputSubmit title=uiLabelMap.CommonCreate />
            <@displayLink href="orderview?orderId=${orderHeader.orderId}" text=uiLabelMap.OpentapsGoBack class="buttontext" />
          </td>
        </tr>
      </table>
    </div>
  </form>
<#-- javascript to auto process the form on load -->
<script type="text/javascript">
/*<![CDATA[*/
  showOrHideShippingInfo();
  showOrHideEditAddressButton();
  showOrHideGiftMessage();
  checkCarrierPartyAccount(false);
  showOrHideUpsBillingDetails();
  autoSelectCarrierPartyAccount();
/*]]>*/
</script>
</#if>

