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

<#if billingAccount?exists> 

<form name="receiveBillingAccountPayment" method="post" action="<@ofbizUrl>receiveBillingAccountPayment</@ofbizUrl>">
  <input type="hidden" name="billingAccountId" value="${billingAccount.billingAccountId}"/>
  <input type="hidden" name="partyIdTo" value="${parameters.organizationPartyId}"/>

<div class="screenlet">
  <div class="screenlet-header">
    <div class="boxhead">${uiLabelMap.FinancialsReceivePaymentFor} ${uiLabelMap.FinancialsCustomerBillingAccount} (${billingAccount.billingAccountId})</div>
  </div>

  <div class="screenlet-body">
    <table border="0" cellpadding="2" cellspacing="0">
      <tr>
        <td class="tableheadtext" style="color: #AA0000" align="right" width="20%">Received from Party</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <input type="text" name="partyIdFrom" size="20" value="${defaultBillToParty.partyId?default("")}" class="inputBox"></input>
          <a href="javascript:call_fieldlookup2(document.receiveBillingAccountPayment.partyIdFrom, 'LookupPartyName');">
            <img src="/images/fieldlookup.gif" width="16" height="16" border="0" alt="Lookup"></img>
          </a>
        </td>
      </tr>
      <tr>
        <td class="tableheadtext" style="color: #AA0000" align="right" width="20%">Payment Type</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <select name="paymentTypeId" class="selectBox">
            <option value="CUSTOMER_PAYMENT">Customer Payment</option>
            <option value="CUSTOMER_DEPOSIT">Customer Deposit</option>
          </select>
        </td>
      </tr>
      <tr>
        <td class="tableheadtext" style="color: #AA0000" align="right" width="20%">Payment Method</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <select name="paymentMethodTypeId" class="inputBox">
            <#list paymentMethodTypes as paymentMethodType>
            <option value="${paymentMethodType.paymentMethodTypeId}">${paymentMethodType.description}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td class="tableheadtext" style="color: #AA0000" align="right" width="20%">Amount</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <input type="text" name="amount" size="6" value="" class="inputBox"></input>
          <select name="currencyUomId" class="inputBox">
            <#list currencies as currency>
            <#assign selected=""/>
            <#if parameters.orgCurrencyUomId == currency.uomId><#assign selected="selected"/></#if>
            <option value="${currency.uomId}" ${selected}>${currency.uomId}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td class="tableheadtext" align="right" width="20%">Effective Date</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <@inputDateTime name="effectiveDate"/>
        </td>
      </tr>
      <tr>
        <td class="tableheadtext" align="right" width="20%">Reference Number</td>
        <td>&nbsp;</td>
        <td class="tabletext" width="80%" align="left">
          <input type="text" name="paymentRefNum" size="30" value="" class="inputBox"></input>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
        <td><input type="submit" name="formSubmitButton" value="Submit" class="smallSubmit"></input>
        </tr>
      </table>
    </div>
  </div>
</form>

<#else>
Please select a billing account.
</#if>
