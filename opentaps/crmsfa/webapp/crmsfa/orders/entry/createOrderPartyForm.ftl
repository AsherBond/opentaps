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

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmCreateCustomerForOrder}</div>
    <div class="subMenuBar"><@inputConfirm href="createOrderMainScreen" title=uiLabelMap.OpentapsOrderReturnToOrder confirmText="Are you sure?  Customer information will not be saved."class="subMenuButtonDangerous"/><a class="subMenuButton" href="javascript:document.createOrderPartyForm.submit()">${uiLabelMap.CrmOrderSaveCustomerAndContinue}</a></div>
</div>

<form action="<@ofbizUrl>createOrderPartyValidation</@ofbizUrl>" name="createOrderPartyForm" method="post">

<div class="subSectionBlock">
<table class="fourColumnForm">
    <tr>
        <@displayCell text=uiLabelMap.CrmCompanyName class="tableheadtext" blockClass="titleCell" />
        <@inputTextCell name="groupName"/>
    </tr>
    <tr>
        <@displayCell text=uiLabelMap.PartyFirstName class="tableheadtext" blockClass="titleCell" />
        <@inputTextCell name="firstName"/>
        <@displayCell text=uiLabelMap.PartyLastName class="tableheadtext" blockClass="titleCell" />
        <@inputTextCell name="lastName"/>
    <tr>
        <@displayCell text=uiLabelMap.PartyPhoneNumber class="tableheadtext" blockClass="titleCell" />
        <td>
            <@inputText name="primaryPhoneCountryCode" size=6 default=configProperties.defaultCountryCode/>
            <@inputText name="primaryPhoneAreaCode" size=6/>
            <@inputText name="primaryPhoneNumber" size=10/>
            <span>${uiLabelMap.PartyContactExt}</span>
            <@inputText name="primaryPhoneExtension" size=10/>
        </td>
    </tr>
    <tr>
        <@displayCell text=uiLabelMap.PartyEmailAddress class="tableheadtext" blockClass="titleCell" />
        <@inputTextCell name="primaryEmail" size=30/>
    </tr>
  </table>
</div>

<#-- This DIV id is used to gray out the contents of this form. -->
<div id="billingAddressContainer">
<div class="subSectionBlock">
<div class="screenlet-header"><span class="boxhead">${uiLabelMap.OpentapsBillingAddress}</span></div>
<table class="twoColumnForm">
    <@inputTextRow name="billingToName"   title=uiLabelMap.PartyToName       titleClass="requiredField" />
    <@inputTextRow name="billingAttnName" title=uiLabelMap.PartyAddrAttnName />
    <@inputTextRow name="billingAddress1" title=uiLabelMap.PartyAddressLine1 titleClass="requiredField" />
    <@inputTextRow name="billingAddress2" title=uiLabelMap.PartyAddressLine2 />
    <tr>
        <@displayCell text=uiLabelMap.OpentapsCityStateCountry class="requiredField" blockClass="titleCell" />
        <td>
            <input type="text" class="inputBox" name="billingCity" size="20" value="${parameters.billingCity?default("")}">
            <@inputStateCountry stateInputName="billingStateProvinceGeoId" countryInputName="billingCountryGeoId" />
        </td>
    </tr>
    <tr>
      <td class="titleCell" valign="top"><span class="requiredField">${uiLabelMap.PartyPostalCode}</span></td>
      <td>
        <@inputText name="billingPostalCode"/>-
        <@inputText name="billingPostalCodeExt" size="5"/>
        <#if validatePostalAddresses>
          <div id="billingPostalAddressValidationContainer" class="tabletext">
              <@flexArea targetId="billingAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false><div>lskdjf</div></@flexArea>
          </div>
          <script type="text/javascript">
              opentaps.addListenerToNode(document.forms['createOrderPartyForm'].billingPostalCode, 'onchange', function(){opentaps.postalAddress.validationListener('billingAddressValidationError', 'billingAddress1', 'billingAddress2', 'billingCity', 'billingStateProvinceGeoId', 'billingCountryGeoId', 'billingPostalCode', 'billingPostalCodeExt', document.forms['createOrderPartyForm'])})
          </script>
        </#if>
      </td>
    </tr>
</table>
</div>
</div>

<#-- The general prefix implies a shipping and general address -->
<div id="generalAddressContainer">
<div class="subSectionBlock">
<div class="screenlet-header"><span class="boxhead">${uiLabelMap.OrderShippingAddress}</span></div>
<table class="twoColumnForm">
    <tr>
        <td>
            <@display text=uiLabelMap.OpentapsShippingSameAsBilling class="tableheadtext"/>
            <#assign billingFlexAreaState="open"/>
            <#-- Because a checkbox parameter is only passed if it's checked, it is difficult to tell if this is the first page load (checkbox should be checked by default)
                 or if we have returned due to an error and the checkbox was previously unchecked, so we'll check if billingCountryGeoId is present which will represent the latter scenario -->
            <#if (!parameters.billingCountryGeoId?exists) || parameters.generalSameAsBillingAddress?default("") == "Y">
                <#assign billingFlexAreaState="closed"/>
            </#if>
            <input type="checkbox" name="generalSameAsBillingAddress" value="Y" onClick="opentaps.expandCollapse('generalAddress')"<#if billingFlexAreaState=="closed"> checked</#if>>
        </td>
    </tr>
	<tr>
	    <td>
        <@flexArea title="" targetId="generalAddress" style="border:none; padding:0; margin-right:0" controlClassOpen="twoColumnForm" controlClassClosed="hidden" state="${billingFlexAreaState}" enabled=false>
	        <table>
                <@inputTextRow name="generalToName" title=uiLabelMap.PartyToName         titleClass="requiredField" />
                <@inputTextRow name="generalAttnName" title=uiLabelMap.PartyAddrAttnName />
                <@inputTextRow name="generalAddress1" title=uiLabelMap.PartyAddressLine1 titleClass="requiredField" />
                <@inputTextRow name="generalAddress2" title=uiLabelMap.PartyAddressLine2 />
                <tr>
                    <@displayCell text=uiLabelMap.OpentapsCityStateCountry class="requiredField" blockClass="titleCell" />
                    <td>
                        <input type="text" class="inputBox" name="generalCity" size="20" value="${parameters.generalCity?default("")}">,
                        <@inputStateCountry stateInputName="generalStateProvinceGeoId" countryInputName="generalCountryGeoId" />
                    </td>
                </tr>
                <tr>
                    <td class="titleCell" valign="top"><span class="requiredField">${uiLabelMap.PartyPostalCode}</span></td>
                    <td>
                        <@inputText name="generalPostalCode"/>-
                        <@inputText name="generalPostalCodeExt" size="5"/>
                        <#if validatePostalAddresses>
                          <div id="generalPostalAddressValidationContainer" class="tabletext">
                              <@flexArea targetId="generalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false><div>lskdjf</div></@flexArea>
                          </div>
                          <script type="text/javascript">
                              opentaps.addListenerToNode(document.forms['createOrderPartyForm'].generalPostalCode, 'onchange', function(){opentaps.postalAddress.validationListener('generalAddressValidationError', 'generalAddress1', 'generalAddress2', 'generalCity', 'generalStateProvinceGeoId', 'generalCountryGeoId', 'generalPostalCode', 'generalPostalCodeExt', document.forms['createOrderPartyForm'])})
                          </script>
                        </#if>
                    </td>
                </tr>
            </table>
        </@flexArea>
        </td>
    </tr>
</table>
</div>
</div>

<div class="screenlet-header"><span class="boxhead">${uiLabelMap.CrmNewCreditCard}</span></div>
<table class="twoColumnForm">
<tr>
  <@displayCell text=uiLabelMap.AccountingCardType class="tableheadtext" blockClass="tabletextright" />
  <td>
    <select name="cardType" class="selectBox">
      <@include location="component://common/webcommon/includes/cctypes.ftl"/>
    </select>
  </td>
</tr>
<tr>
  <@inputTextRow name="cardNumber" title=uiLabelMap.AccountingCardNumber />
</tr>
<tr>
  <@displayCell text=uiLabelMap.AccountingExpirationDate class="tableheadtext" blockClass="tabletextright" />
  <td>
    <select name="expMonth" id="expMonth" class="selectBox">
      <@include location="component://common/webcommon/includes/ccmonths.ftl"/>
    </select>
    <select name="expYear" id="expYear" class="selectBox">
      <@include location="component://common/webcommon/includes/ccyears.ftl"/>
    </select>
    <script type="text/javascript">
      <#if parameters.expMonth?has_content>
        opentaps.replaceFormElementValue(document.getElementById('expMonth'), '${parameters.expMonth}');    
      </#if>
      <#if parameters.expYear?has_content>
        opentaps.replaceFormElementValue(document.getElementById('expYear'), '${parameters.expYear}');    
      </#if>
    </script>
  </td>
</tr>
</table>

</form>
