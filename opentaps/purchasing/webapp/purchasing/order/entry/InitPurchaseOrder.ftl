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

var skipReqForAgreements = false;

/*
  AJAX handler.
  Create new <select/> of agreements, fill its options and replace existent input with new one.  
*/
function handleAgreements(/*Array*/ data) {
    if (!data) return;

    // agreement may be selected earlier
    <#if shoppingCart?has_content && shoppingCart.agreementId?has_content>
    var cartAgreementId = '${shoppingCart.agreementId}';
    <#else>
    var cartAgreementId = null;
    </#if>

    var agreementInput = document.getElementById('agreementId');
    if (!agreementInput) return;

    // creates new input
    var newSelect = document.createElement('select');
    newSelect.id = 'agreementId';
    newSelect.name = 'agreementId';
    newSelect.className = 'inputBox';
    newSelect.onchange = function() {agreementChanged();};

    if (data.length > 0) {
        newSelect.options[0] = new Option('', '', true, true);
    } else {
        // not agreements to supplier, put default message
        newSelect.options[0] = new Option('${uiLabelMap.PurchNoAgreements}', '', true, true);
    }

    // fill list of available agreements
    for (var i = 0; i < data.length; i++) {
        var agreement = data[i];
        var optionText = agreement.agreementId;
        optionText += ' - ';
        if (agreement.description != null && agreement.description.length > 0) {
            optionText += agreement.description;
        }

        var option = new Option(optionText, agreement.agreementId, false, agreement.agreementId == cartAgreementId ? true : false);
        newSelect.options[i+1] = option;

    }

    // replace agreement drop-down with new one
    opentaps.replaceNode(agreementInput, newSelect);

    // enable currency box 
    agreementChanged();
}

/*
  Send AJAX request if supplier id is changed.
*/
function supplierChanged() {

    if (skipReqForAgreements) {
        skipReqForAgreements = false;
        return;
    }

    var partyId;
    var supplierInput = document.getElementById("ComboBox_supplierPartyId");
    if (supplierInput) {
        partyId = prevSupplierId = supplierInput.value;
    } else {
        <#if parameters.partyId?has_content>
        partyId = prevSupplierId = '${parameters.partyId}';
        if (!partyId) {
            return;
        }
        <#else>
        return;
        </#if>
    };

    opentaps.sendRequest('getSupplierAgreementsJSON', {'partyId' : partyId, 'organizationPartyId' : '${organizationPartyId}'}, handleAgreements);
}

/* disable/enable currency box depending on agreement selection */
function agreementChanged() {
    var agreementField = document.getElementById('agreementId');
    if (agreementField) {
        var currencyField = document.getElementById('currencyUomId');
        if (agreementField.value == null || agreementField.value.length == 0) {
            currencyField.disabled = false;
        } else {
            currencyField.disabled = true;
        }
    }
}

var onLookupReturn = function() { supplierChanged(); skipReqForAgreements = true; };

opentaps.addOnLoad(supplierChanged());
/*]]>*/
</script>

<!-- Purchase Order Entry -->
<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
    <div class="subSectionBlock">
        <#if shoppingCart?exists>
            <#assign sectionTitle = "${uiLabelMap.OrderPurchaseOrder}&nbsp;${uiLabelMap.OrderInProgress}" />
        <#else>
            <#assign sectionTitle = "${uiLabelMap.OrderPurchaseOrder}" />
        </#if>

        <@sectionHeader title="${sectionTitle}"/>

        <#if hasParty>
          <#assign thisPartyId = shoppingCart?if_exists.orderPartyId?if_exists />
        <#else>
          <#assign thisPartyId = requestParameters.partyId?if_exists />
        </#if>

        <form method="post" name="purchOrderForm" action="<@ofbizUrl>InitializePurchaseOrder</@ofbizUrl>">
            <@inputHidden name='finalizeMode' value='type'/>
            <@inputHidden name='orderMode' value='PURCHASE_ORDER'/>
            <#if shoppingCart?has_content>
                <@inputHidden name="supplierPartyId" value=thisPartyId /> 
                <#if cartAgreement?has_content>
                    <@inputHidden name="agreementId" value=shoppingCart.agreementId />
                </#if>
            </#if>
            <table class="fourColumnForm" width="100%">
                <tr>
                    <@displayTitleCell title=uiLabelMap.PartySupplier/>
                    <#if shoppingCart?has_content>
                        <@displayCell text="${thisPartyId}"/>
                    <#else>
                        <@inputAutoCompleteSupplierCell name="supplierPartyId" default="${thisPartyId?if_exists}" onChange="supplierChanged()"/>
                    </#if>
                    <td colspan="2"></td>
                </tr>
                <tr>
                    <@displayTitleCell title=uiLabelMap.OrderOrderName/>
                    <td colspan="3">
                        <@inputText name="orderName" size="60" default=shoppingCart?if_exists.orderName?if_exists/>
                    </td>
                </tr>
                <@inputHidden name='hasAgreements' value='Y'/>
                <tr>
                    <@displayTitleCell title=uiLabelMap.OrderSelectAgreement/>
                    <#if shoppingCart?has_content>
                        <#if cartAgreement?has_content>
                            <@displayCell text="${cartAgreement.agreementId} - ${cartAgreement?if_exists.description}" />
                        <#else>
                            <td></td>
                        </#if>
                    <#else>
                        <@inputSelectCell name="agreementId" list=agreements?default([]) key="agreementId" onChange="agreementChanged();" defaultOptionText="${uiLabelMap.PurchNoAgreements}" required=false ; option>
                            ${option.agreementId} - ${option.description?if_exists}
                        </@inputSelectCell>
                    </#if>
                    <@displayTitleCell title=uiLabelMap.OrderSelectCurrencyOr/>
                    <#if cartAgreement?has_content>
                        <@displayCell text=shoppingCart?if_exists.currency?if_exists />
                    <#else>
                        <@inputCurrencySelectCell defaultCurrencyUomId=shoppingCart?if_exists.currency?if_exists/>
                    </#if>
                </tr>
                <tr>
                    <@displayTitleCell title=uiLabelMap.OrderShipAfterDateDefault/>
                    <@inputDateCell name="shipAfterDate" default=shoppingCart?if_exists.defaultShipAfterDate?if_exists/>
                    <@displayTitleCell title=uiLabelMap.OrderShipBeforeDateDefault/>
                    <@inputDateCell name="shipBeforeDate" default=shoppingCart?if_exists.defaultShipBeforeDate?if_exists/>
                </tr>
                <tr>
                    <td></td>
                    <@inputSubmitCell title="Continue"/>
                    <td colspan="2"></td>
                </tr>
            </table>
        </form>

    </div>

</#if>
