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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<@frameSection title=uiLabelMap.FinancialsCreatePaycheck>
  <table border="0" cellpadding="2" cellspacing="0" width="100%">
    <form action="<@ofbizUrl>createPaycheck</@ofbizUrl>" name="createPaycheckForm" method="post">
      <@inputHidden name="statusId" value="PMNT_NOT_PAID"/>
      <@inputHidden name="partyIdFrom" value="${parameters.organizationPartyId}" />
      <#if paymentType?has_content>
        <@inputHidden name="paymentTypeId" value="${paymentType.paymentTypeId}" />
        <@displayRow title=uiLabelMap.FinancialsPaycheckType text=paymentType.get("description", "FinancialsEntityLabel", locale)/>
      <#else>
        <@inputSelectRow name="paymentTypeId" title=uiLabelMap.FinancialsPaycheckType list=paycheckTypes key="paymentTypeId" displayField="description" default=paymentTypeId?if_exists/>
      </#if>
      <@inputLookupRow name="partyIdTo" title=uiLabelMap.FinancialsPayToParty form="createPaycheckForm" lookup="LookupPartyName" titleClass="requiredField"/>
      <@inputSelectRow name="paymentMethodId" title=uiLabelMap.FinancialsPaymentMethod list=paymentMethodList key="paymentMethodId" displayField="description"/>
      <@inputCurrencyRow title=uiLabelMap.FinancialsGrossAmount name="amount" currencyName="currencyUomId" defaultCurrencyUomId=defaultCurrencyUomId titleClass="requiredField"/>
      <@inputDateTimeRow name="effectiveDate" title=uiLabelMap.AccountingEffectiveDate form="createPaycheckForm" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
      <@inputTextRow name="comments" title=uiLabelMap.CommonComments size=60 />
      <@inputTextRow name="paymentRefNum" title=uiLabelMap.FinancialsPaymentRefNum />
      <@inputSubmitRow title=uiLabelMap.FinancialsCreatePaycheck />
    </form>
  </table>
</@frameSection>

<script type="text/javascript">

  partyIdToCheckIntervalMillis = 1000;
  var partyIdToInput = null;
  partyIdTo = {};

  opentaps.addOnLoad(initializePartyIdToCheck);

  var partyIdToChangeInterval = null;

  function initializePartyIdToCheck() {
    getPartyIdToValue();
    partyIdToChangeInterval = setInterval("checkForPartyIdToChange()", partyIdToCheckIntervalMillis);
    checkForPartyIdToChange();
  }

  <#-- Populate the initial value of the partyIdTo field -->
  function getPartyIdToValue() {
    var elements = document.getElementsByTagName("input");
    for (var i = 0; i < elements.length; ++i) {
      if (elements[i].getAttribute("name") == "partyIdTo") {
        partyIdToInput = elements[i];
        partyIdTo[partyIdToInput.name] = partyIdToInput.value;
      }
    }
  }

  <#-- Poll the partyIdTo field for changed values -->
  <#-- TODO: Make this a useful function for a global JS -->
  function checkForPartyIdToChange() {
    var newValue = partyIdToInput.value;
    var oldValue = partyIdTo[partyIdToInput.name];
    if ((newValue != "") && (newValue != oldValue)) {
      partyIdTo[partyIdToInput.name] = newValue;
      refreshPaymentTypeOptionsInDropdown(document.createPaycheckForm.partyIdTo.value, document.createPaycheckForm.paymentTypeId);
    }
  }

  // function to refresh the payment type options in a dropdown when the employee id is changed.
  refreshPaymentTypeOptionsInDropdown = function(partyId, paymentTypeOptionsElement) {
    // use AJAX request to get the data
    opentaps.sendRequest(
      "getPaymentTypeDataJSON",
      {"partyId" : partyId},
      function(data) {refreshPaymentTypeOptionsInDropdownResponse(paymentTypeOptionsElement, data)}
    );
  }

  // from the AJAX response, replace the given state options
  refreshPaymentTypeOptionsInDropdownResponse = function(paymentTypeOptionsElement, paymentTypeOptions) {

    // initialize array
    paymentTypeOptionsElement.options.length = 0;

    // build the payment type options
    // paymentTypeOptionsElement.options[0] = new Option('', ''); // first element is always empty
    for (i = 0; i < paymentTypeOptions.length; i++) {
      paymentTypeOption = paymentTypeOptions[i];
      paymentTypeOptionsElement.options[i] = new Option(paymentTypeOption.description, paymentTypeOption.paymentTypeId);
    }

    // by setting the length of the select option array, we can truncate it
    paymentTypeOptionsElement.options.length = paymentTypeOptions.length + 1;
 }

</script>
