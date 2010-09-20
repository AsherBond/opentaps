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

<#-- This is a segment of viewprofile.ftl from partymgr that displays payment information.  It has been modified to include crmsfa specific logic, links and styles. -->
<#-- TODO: how do the other pages wrap the correct decorator? -->

<#if hasPaymentViewPermission>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign extraOptions>
  <#if hasPaymentUpdatePermission>
    <@selectAction name="createNewPaymentAccountTarget" prompt="${uiLabelMap.CommonCreateNew}">
      <@action url="${editEftAccountPage}?partyId=${parameters.partyId}" text="${uiLabelMap.AccountingEFTAccount}"/>
      <@action url="${editGiftCardPage}?partyId=${parameters.partyId}" text="${uiLabelMap.AccountingGiftCard}"/>
      <@action url="${editCreditCardPage}?partyId=${parameters.partyId}" text="${uiLabelMap.AccountingCreditCard}"/>
      <@action url="${editTaxAuthPartyInfo}?partyId=${parameters.partyId}" text="${uiLabelMap.OpentapsTaxAuthPartyId}"/>
      <@action url="${editShippingAccount}?partyId=${parameters.partyId}" text="${uiLabelMap.CrmShippingAccount}"/>
    </@selectAction>
  </#if>
</#assign>

<@frameSection title=uiLabelMap.CrmPaymentAndShippingAccounts extra=extraOptions>

      <#if paymentMethodValueMaps?has_content || partyCarrierAccounts?has_content || partyTaxAuthInfoList?has_content>
        <table class="basic-table" cellspacing="0">
           <#if partyTaxAuthInfoList?has_content>
              <#list partyTaxAuthInfoList as partyTaxAuthInfo>
                  <tr>
                     <td class="label">${uiLabelMap.OpentapsTaxAuthPartyId}</td>
                     <td>
                       ${partyTaxAuthInfo.partyTaxAuthName?default(partyTaxAuthInfo.taxAuthPartyId)}<#if partyTaxAuthInfo.isExempt?exists >:
                         <span class="requiredField"><#if partyTaxAuthInfo.isExempt == "Y" >${uiLabelMap.CrmExempt}<#else>${uiLabelMap.CrmNotExempt}</#if></span>
                       </#if>.<#if partyTaxAuthInfo.partyTaxId?exists> ${uiLabelMap.OpentapsTaxAuthPartyId}: ${partyTaxAuthInfo.partyTaxId}</#if>
                     </td>
                     <td>
                     <#if hasPaymentUpdatePermission>
                         <a href="<@ofbizUrl>${editTaxAuthPartyInfo}?partyId=${parameters.partyId}&taxAuthGeoId=${partyTaxAuthInfo.taxAuthGeoId}&taxAuthPartyId=${partyTaxAuthInfo.taxAuthPartyId}&fromDate=${partyTaxAuthInfo.fromDate}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                     </#if> 
                     </td>                                        
                  </tr>
              </#list>
           </#if>        
           <#if paymentMethodValueMaps?has_content>
            <#list paymentMethodValueMaps as paymentMethodValueMap>
              <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
              <tr>
                <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                  <#assign creditCard = paymentMethodValueMap.creditCard/>
                  <td class="label">
                    ${uiLabelMap.AccountingCreditCard}
                  </td>
                  <td>
                    <#if creditCard.companyNameOnCard?has_content>${creditCard.companyNameOnCard}&nbsp;</#if>
                    <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                    ${creditCard.firstNameOnCard}&nbsp;
                    <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                    ${creditCard.lastNameOnCard}
                    <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                    &nbsp;-&nbsp;
                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                      ${creditCard.cardType}
                      ${creditCard.cardNumber}
                      ${creditCard.expireDate}
                    <#else>
                      ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                    </#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${getLocalizedDate(paymentMethod.fromDate)})</#if>
                    <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(paymentMethod.thruDate)})</#if>
                  </td>
                  <td>
                    <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                      <a href="<@ofbizUrl>${editCreditCardPage}?partyId=${parameters.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                    </#if>
                  <#-- </td> -->
                <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId>
                  <#assign giftCard = paymentMethodValueMap.giftCard>
                  <td class="label" valign="top">
                    ${uiLabelMap.AccountingGiftCard}
                  </td>
                  <td>
                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                      ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                    <#else>
                      <#if giftCard?has_content && giftCard.cardNumber?has_content>
                        <#assign giftCardNumber = "">
                        <#assign pcardNumber = giftCard.cardNumber>
                        <#if pcardNumber?has_content>
                          <#assign psize = pcardNumber?length - 4>
                          <#if 0 < psize>
                            <#list 0 .. psize-1 as foo>
                              <#assign giftCardNumber = giftCardNumber + "*">
                            </#list>
                            <#assign giftCardNumber = giftCardNumber + pcardNumber[psize .. psize + 3]>
                          <#else>
                            <#assign giftCardNumber = pcardNumber>
                          </#if>
                        </#if>
                      </#if>
                      ${giftCardNumber?default("N/A")}
                    </#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${getLocalizedDate(paymentMethod.fromDate)})</#if>
                    <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(paymentMethod.thruDate)}</b></#if>
                  </td>
                  <td>
                    <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                      <a href="<@ofbizUrl>${editGiftCardPage}?partyId=${parameters.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                    </#if>
                  <#-- </td> -->
                <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                  <#assign eftAccount = paymentMethodValueMap.eftAccount>
                  <td class="label" valign="top">
                      ${uiLabelMap.PartyEftAccount}
                  </td>
                  <td>
                    ${eftAccount.nameOnAccount} - <#if eftAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${eftAccount.bankName}</#if> <#if eftAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${eftAccount.accountNumber}</#if>                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${getLocalizedDate(paymentMethod.fromDate)})</#if>
                    <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(paymentMethod.thruDate)}</#if>
                  </td>
                  <td>
                    <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                      <a href="<@ofbizUrl>${editEftAccountPage}?partyId=${parameters.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                    </#if>
                  <#-- </td> -->
                <#elseif "COMPANY_CHECK" == paymentMethod.paymentMethodTypeId>
                  <td class="label" valign="top">
                    <#-- TODO: Convert hard-coded text to UI label properties -->
                    Company Check
                  </td>
                  <td>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${getLocalizedDate(paymentMethod.fromDate)})</#if>
                    <#if paymentMethod.thruDate?has_content>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(paymentMethod.thruDate)}</#if>
                  </td>
                  <td class="button-col align-float">
                    &nbsp;
                  <#-- </td> -->
                <#elseif "EXT_PAYPAL" == paymentMethod.paymentMethodTypeId>
                  <td class="label">
                    ${uiLabelMap.OpentapsPayPalAccount}
                  </td>
                  <td>
                    <#if paymentMethod.emailAddress?has_content>${paymentMethod.emailAddress}</#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${getLocalizedDate(paymentMethod.fromDate)})</#if>
                    <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(paymentMethod.thruDate)})</#if>
                  </td>
                  <td>
                </#if>
                <#if security.hasEntityPermission("PAY_INFO", "_DELETE", session)>
                  <a href="<@ofbizUrl>deletePaymentMethod?partyId=${parameters.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}&amp;donePage=${parameters.thisRequestUri}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonExpire}</a>
                <#else>
                  &nbsp;
                </#if>
                </td> <#-- closes out orphaned <td> elements inside conditionals -->
              </tr>
            </#list>
          </#if>
          <#if partyCarrierAccounts?has_content>
            <#list partyCarrierAccounts?sort_by("carrierName") as partyCarrierAccount>
              <tr>
                <td class="label">${uiLabelMap.CrmShippingAccount}</td>
                <td>
                  ${partyCarrierAccount.carrierName?if_exists} ${partyCarrierAccount.accountNumber?if_exists}
                  <#if partyCarrierAccount.postalCode?has_content || partyCarrierAccount.countryGeoCode?has_content>
                    (${partyCarrierAccount.postalCode?if_exists} ${partyCarrierAccount.countryGeoCode?if_exists})<#if partyCarrierAccount.isDefault?default("N") == "Y"> -- (${uiLabelMap.OpentapsDefault})</#if>
                  </#if>
                </td>
                <td>
                  <@form name="updatePartyCarrierAccountHiddenForm" url="updatePartyCarrierAccount" partyId="${parameters.partyId}" carrierPartyId="${partyCarrierAccount.carrierPartyId}" fromDate="${partyCarrierAccount.fromDate}" thruDate="${now}" donePage="${parameters.thisRequestUri}" />
                  <a href="<@ofbizUrl>${editShippingAccount}?partyId=${parameters.partyId}&carrierPartyId=${partyCarrierAccount.carrierPartyId}&fromDate=${partyCarrierAccount.fromDate}&donePage=${parameters.thisRequestUri}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                  <@submitFormLink form="updatePartyCarrierAccountHiddenForm" text="${uiLabelMap.CommonExpire}" />
                </td>
              </tr>
            </#list>
          </#if>
        </table>
      <#else>
        <span class="tabletext">${uiLabelMap.PartyNoPaymentMethodInformation}</span>
      </#if>

</@frameSection>

</#if> <#-- hasPaymentViewPermission -->
