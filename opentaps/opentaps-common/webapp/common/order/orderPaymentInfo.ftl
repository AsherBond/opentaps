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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<@import location="component://opentaps-common/webapp/common/order/infoMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#if order?has_content>

<#assign updatable = false/>
<#if !order.isCompleted() && !order.isRejected() && !order.isCancelled()>
  <#assign updatable = true/>
</#if>

<#assign extraOptions>
  <#if updatable && (order.totalMinusPaymentPrefs gt 0) >
    <a href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.AccountingReceivePayment}</a>
  </#if>
</#assign>

<@frameSection title=uiLabelMap.AccountingPaymentInformation extra=extraOptions>
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <#if order.nonCancelledPaymentPreferences?has_content || order.billingAccount?has_content || order.invoices?has_content>
        <#list order.nonCancelledPaymentPreferences as opp>
          <#assign pmBillingAddress = {}/>
          <#if outputted?default("false") == "true">
            <@infoSepBar/>
          </#if>
          <#assign outputted = "true"/>
          <#-- try the paymentMethod first; if paymentMethodId is specified it overrides paymentMethodTypeId -->
          <#if !opp.paymentMethod?has_content>
            <#if opp.isBillingAccountPayment()>
              <#assign outputted = "false"/>
            <#else/>
              <@infoRowNested title=opp.paymentMethodType.get("description",locale)?if_exists>
                <#if opp.maxAmount?has_content>
                  <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/></div>
                </#if>
                <div class="tabletext">&nbsp;[<#if opp.status?exists>${opp.status.get("description",locale)}<#else>${opp.statusId}</#if>]</div>
                <#--
                     <div class="tabletext"><@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/>&nbsp;-&nbsp;${getLocalizedDate(opp.authDate)}</div>
                     <div class="tabletext">&nbsp;<#if opp.authRefNum?exists>(${uiLabelMap.OrderReference}: ${opp.authRefNum})</#if></div>
                -->
                </td><td>
                <#if updatable && !opp.isSettled() && !opp.isReceived()>
                  <@form name="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" url="updateOrderPaymentPreference" orderId=orderId orderPaymentPreferenceId=opp.orderPaymentPreferenceId statusId="PAYMENT_CANCELLED" checkOutPaymentId="${opp.paymentMethodTypeId?if_exists}" />
                  <@submitFormLink text=uiLabelMap.CommonCancel form="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" />
                </#if>
              </@infoRowNested>
            </#if>
          <#else/>
            <#if opp.isCreditCardPayment()>
              <#assign creditCard = opp.paymentMethod.creditCard?if_exists/>
              <#if creditCard?has_content>
                <#assign pmBillingAddress = creditCard.postalAddress?if_exists>
              </#if>
              <@infoRowNested title=uiLabelMap.AccountingCreditCard>
                <#if opp.maxAmount?has_content>
                  <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/></div>
                  <hr class="sepbar"/>
                </#if>
                <div class="tabletext">
                  <#if creditCard?has_content>
                    <#if creditCard.companyNameOnCard?exists>${creditCard.companyNameOnCard}<br/></#if>
                    <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                    <#if creditCard.firstNameOnCard?has_content>${creditCard.firstNameOnCard}&nbsp;</#if>
                    <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                    ${creditCard.lastNameOnCard?default("N/A")}
                    <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                    <br/>

                    ${creditCard.cardType}
                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                      ${creditCard.cardNumber}
                    <#else/>
                      ${creditCard.cardNumberStripped}
                    </#if>
                    ${creditCard.expireDate}
                    &nbsp;[<#if oppStatusItem?exists>${opp.status..get("description",locale)}<#else>${opp.statusId}</#if>]
                    <br/>

                    <#-- Authorize and Capture transactions -->
                    <div class="tabletext">
                      <#if !opp.isSettled()>
                        <a href="<@ofbizUrl>AuthorizeTransaction?orderId=${order.orderId?if_exists}&orderPaymentPreferenceId=${opp.orderPaymentPreferenceId}&externalLoginKey=${externalLoginKey}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                      </#if>
                      <#if opp.isAuthorized()>
                        <a href="<@ofbizUrl>CaptureTransaction?orderId=${order.orderId?if_exists}&orderPaymentPreferenceId=${opp.orderPaymentPreferenceId}&externalLoginKey=${externalLoginKey}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                      </#if>
                    </div>

                  <#else/>
                    ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                  </#if>
                </div>
                <#if opp.responses?has_content>
                  <div class="tabletext">
                    <hr class="sepbar"/>
                    <#list opp.responses as gatewayResponse>
                      ${(gatewayResponse.transactionCode.get("description",locale))?default("Unknown")} :
                      ${getLocalizedDate(gatewayResponse.transactionDate)}
                      <@ofbizCurrency amount=gatewayResponse.amount isoCode=order.currencyUom/><br/>
                      (<b>${uiLabelMap.OrderReference}:</b> <a href="<@ofbizUrl>ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}&externalLoginKey=${externalLoginKey}</@ofbizUrl>" class="linktext">${gatewayResponse.referenceNum?if_exists}</a>
                        <b>${uiLabelMap.OrderAvs}:</b> ${gatewayResponse.gatewayAvsResult?default("N/A")}
                        <b>${uiLabelMap.OrderScore}:</b> ${gatewayResponse.gatewayScoreResult?default("N/A")})
                        <#if gatewayResponse_has_next><hr class="sepbar"/></#if>
                      </#list>
                    </div>
                  </#if>
                </td><td>
                <#if updatable && !opp.isSettled() && !opp.isReceived()>
                  <@form name="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" url="updateOrderPaymentPreference" orderId=orderId orderPaymentPreferenceId=opp.orderPaymentPreferenceId statusId="PAYMENT_CANCELLED" checkOutPaymentId="${opp.paymentMethodTypeId?if_exists}" />
                  <@submitFormLink text=uiLabelMap.CommonCancel form="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" />
                </#if>
              </@infoRowNested>
            <#elseif opp.isElectronicFundTransferPayment()>
              <#assign eftAccount = opp.paymentMethod.eftAccount/>
              <#if eftAccount?has_content>
                <#assign pmBillingAddress = eftAccount.postalAddress?if_exists>
              </#if>
              <@infoRowNested title=uiLabelMap.AccountingEFTAccount>
                <#if opp.maxAmount?has_content>
                  <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/></div>
                  <hr class="sepbar"/>
                </#if>
                <div class="tabletext">
                  <#if eftAccount?has_content>
                    <#if eftAccount.nameOnAccount?exists>${eftAccount.nameOnAccount?if_exists}<br/></#if>
                    <#if eftAccount.companyNameOnAccount?exists>${eftAccount.companyNameOnAccount}<br/></#if>
                    ${uiLabelMap.AccountingBankName}: ${eftAccount.bankName}, ${eftAccount.routingNumber}<br/>
                    ${uiLabelMap.AccountingAccount}#: ${eftAccount.accountNumber}
                  <#else/>
                    ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                  </#if>
                </div>
                </td><td>
                <#if updatable && !opp.isSettled() && !opp.isReceived()>
                  <@form name="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" url="updateOrderPaymentPreference" orderId=orderId orderPaymentPreferenceId=opp.orderPaymentPreferenceId statusId="PAYMENT_CANCELLED" checkOutPaymentId="${opp.paymentMethodTypeId?if_exists}" />
                  <@submitFormLink text=uiLabelMap.CommonCancel form="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" />
                </#if>
              </@infoRowNested>
            <#elseif opp.isGiftCardPayment()>
              <#assign giftCard = opp.paymentMethod.giftCard/>
              <#if giftCard?exists>
                <#assign pmBillingAddress = giftCard.postalAddress?if_exists>
              </#if>
              <@infoRowNested title=uiLabelMap.OrderGiftCard>
                <#if opp.maxAmount?has_content>
                  <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/></div>
                  <hr class="sepbar"/>
                </#if>
                <div class="tabletext">
                  <#if giftCard?has_content>
                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                      ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                      &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${opp.statusId}</#if>]
                    <#else/>
                      ${giftCardNumberStripped?default("N/A")}
                      &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${opp.statusId}</#if>]
                    </#if>
                  <#else/>
                    ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                  </#if>
                </div>
                </td><td>
                <#if updatable && !opp.isSettled() && !opp.isReceived()>
                  <@form name="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" url="updateOrderPaymentPreference" orderId=orderId orderPaymentPreferenceId=opp.orderPaymentPreferenceId statusId="PAYMENT_CANCELLED" checkOutPaymentId="${opp.paymentMethodTypeId?if_exists}" />
                  <@submitFormLink text=uiLabelMap.CommonCancel form="cancelOrderPaymentPreference_${opp.orderPaymentPreferenceId}" />
                </#if>
              </@infoRowNested>
            <#elseif opp.isPaypalPayment()>
              <@infoRowNested title=uiLabelMap.OpentapsPayPalAccount>
                <#if opp.maxAmount?has_content>
                  <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=opp.maxAmount?default(0.00) isoCode=order.currencyUom/></div>
                  <hr class="sepbar"/>
                </#if>
                <div class="tabletext">
                  <#if opp.paymentMethod.emailAddress?has_content>${opp.paymentMethod.emailAddress}</#if>
                  <#if opp.paymentMethod.description?has_content>(${opp.paymentMethod.description})</#if>
                </div>
                <#if opp.responses?has_content>
                  <div class="tabletext">
                    <hr class="sepbar"/>
                    <#list opp.responses as gatewayResponse>
                      ${getLocalizedDate(gatewayResponse.transactionDate)}
                      <@ofbizCurrency amount=gatewayResponse.amount isoCode=order.currencyUom/><br/>
                      (<b>${uiLabelMap.OrderReference}:</b> <a href="<@ofbizUrl>ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}</@ofbizUrl>" class="buttontext">${(gatewayResponse.transactionCode.get("description",locale))?default("Unknown")}: ${gatewayResponse.referenceNum?if_exists}</a><br/>
                      <b>${uiLabelMap.FormFieldTitle_gatewayCode}:</b> ${gatewayResponse.gatewayCode?default("N/A")}
                      <b>${uiLabelMap.CommonMessage}:</b> ${gatewayResponse.gatewayMessage?default("N/A")})

                      <#if gatewayResponse_has_next><hr class="sepbar"/></#if>
                    </#list>
                  </div>
                </#if>
              </@infoRowNested>
            </#if>
          </#if>
          <#if pmBillingAddress?has_content>
            <tr><td colspan="2">&nbsp;</td><td colspan="5"><hr class="sepbar"/></td></tr>
            <@infoRowNested title="">
              <div class="tabletext">
                <#if pmBillingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${pmBillingAddress.toName}<br/></#if>
                <#if pmBillingAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${pmBillingAddress.attnName}<br/></#if>
                ${pmBillingAddress.address1}<br/>
                <#if pmBillingAddress.address2?has_content>${pmBillingAddress.address2}<br/></#if>
                ${pmBillingAddress.city}<#if pmBillingAddress.stateProvinceGeoId?has_content>, ${pmBillingAddress.stateProvinceGeoId} </#if>
                ${pmBillingAddress.postalCode?if_exists}<br/>
                ${pmBillingAddress.countryGeoId?if_exists}
              </div>
            </@infoRowNested>
          </#if>
        </#list>

        <#-- billing account -->
        <#if order.billingAccount?exists>
          <#if outputted?default("false") == "true">
            <@infoSepBar/>
          </#if>
          <@infoRowNested title=uiLabelMap.AccountingBillingAccount>
            <#assign billingAccountMaxAmount = order.getBillingAccountMaxAmount()/>
            <#if billingAccountMaxAmount.signum() != 0>
              <div class="tabletext">${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=billingAccountMaxAmount isoCode=order.currencyUom/></div>
            </#if>
            <div class="tabletext">
              #${order.billingAccount.billingAccountId} - ${order.billingAccount.description?if_exists}
            </div>
          </@infoRowNested>
          <@infoSepBar />
          <@infoRow title=uiLabelMap.OrderPurchaseOrderNumber content=order.primaryPoNumber?if_exists />
        </#if>

        <#-- invoices -->
        <#if order.invoices?has_content>
          <@infoSepBar />
          <@infoRowNested title=uiLabelMap.OrderInvoices>
            <#list order.invoices as invoice>
              <div class="tabletext">${uiLabelMap.OrderNbr}
              <a href="<@ofbizUrl>invoice.pdf?invoiceId=${invoice.invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>" class="linktext" target="_blank">${invoice.invoiceId}</a> (PDF)</div>
            </#list>
          </@infoRowNested>
        </#if>
      <#else/> <#-- order.nonCancelledPaymentPreferences?has_content || order.billingAccount?has_content || invoices?has_content -->
        <tr>
          <td colspan="7" align="center" class="tabletext">${uiLabelMap.OrderNoOrderPaymentPreferences}</td>
        </tr>
      </#if>
      <#-- the add new credit card to order box only appears if the order is in the right state and if there is an open amount not covered by existing payment methods -->
      <#if updatable && (order.totalMinusPaymentPrefs gt 0.0) && order.mainExternalParty.paymentMethods?has_content>
        <@infoSepBar />
        <form name="addCreditCardToOrder" method="post" action="<@ofbizUrl>addCreditCardToOrder</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <tr>
            <td width="15%" align="right" nowrap="nowrap"><div class="tableheadtext">${uiLabelMap.AccountingCreditCard} </div></td>
            <td width="5">&nbsp;</td>
            <td nowrap="nowrap">
              <select name="paymentMethodId" class="selectBox">
                <#list order.mainExternalParty.paymentMethods as paymentMethod>
                  <#if paymentMethod.isCreditCard()>
                    <#if paymentMethod.creditCard?exists>
                      <option value="${paymentMethod.creditCard.paymentMethodId}">
                        ${paymentMethod.creditCard.cardType} ${paymentMethod.creditCard.cardNumberStripped} ${paymentMethod.creditCard.expireDate}
                      </option>
                    </#if>
                  </#if>
                </#list>
              </select>
              <span class="tableheadtext">CVV</span> <@inputText name="securityCode" size=4 maxlength=4/>
              <#--<input type="button" class="smallSubmit" value="${uiLabelMap.PartyAddNewAddress}">-->
            </td>
          </tr>
          <tr>
            <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.AccountingAmount} </div></td>
            <td width="2%">&nbsp;</td>
            <td nowrap="nowrap">
              <input type="text" class="inputBox" name="amount" value="${order.totalMinusPaymentPrefs}"/>
            </td>
          </tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <div class="tabletext">&nbsp;</div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <div class="tabletext">
                <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                <#--<input type="button" value="${uiLabelMap.CrmNewCreditCard}" class="smallSubmit"/>-->
              </div>
            </td>
          </tr>
        </form>
      </#if>
    </table>
</@frameSection>

</#if> <#-- order?has_content -->
