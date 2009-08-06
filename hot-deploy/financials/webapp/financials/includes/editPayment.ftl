<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">
    function checkPaymentMethodTypeId() {
        paymentMethodSelect = document.getElementById('paymentMethodId');
        paymentMethodTypeSelect = document.getElementById('paymentMethodTypeId');
        if (paymentMethodSelect != null && paymentMethodTypeSelect != null) {
            paymentMethodId = paymentMethodSelect.options[paymentMethodSelect.selectedIndex].value;
            if (paymentMethodId == '') {
                paymentMethodTypeSelect.disabled = false;
            } else {
                paymentMethodTypeSelect.disabled = true;
            }
        }
    }
</script>

<#if isDisbursement?exists>

<#-- Prepare commands for Payment status change  -->
<div class="screenlet-header">
    <div style="float: right;">
        <#if paymentValue?has_content>
          <#if paymentValue.paymentId?has_content>
            <a class="buttontext" href="<@ofbizUrl>editPayment?paymentTypeId=${paymentTypeId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a>
          </#if> 
          <a class="buttontext" href="<@ofbizUrl>viewPayment?paymentId=${paymentValue.paymentId}</@ofbizUrl>">${uiLabelMap.CommonView}</a>
          <#if paymentValue.paymentId?has_content && isDisbursement && paymentValue.statusId.equals("PMNT_NOT_PAID")>
            <a class="buttontext" href="<@ofbizUrl>setPaymentStatus?paymentId=${paymentValue.paymentId}&statusId=PMNT_SENT</@ofbizUrl>">${uiLabelMap.FinancialsPaymentStatusToSent}</a>
          </#if>
          <#if paymentValue.paymentId?has_content && !isDisbursement && paymentValue.statusId.equals("PMNT_NOT_PAID")>
            <a class="buttontext" href="<@ofbizUrl>setPaymentStatus?paymentId=${paymentValue.paymentId}&statusId=PMNT_RECEIVED</@ofbizUrl>">${uiLabelMap.FinancialsPaymentStatusToReceived}</a>
          </#if>
          <#if paymentValue.paymentId?has_content && paymentValue.statusId.equals("PMNT_NOT_PAID")>
            <a class="buttontext" href="<@ofbizUrl>setPaymentStatus?paymentId=${paymentValue.paymentId}&statusId=PMNT_CANCELLED</@ofbizUrl>">${uiLabelMap.FinancialsPaymentStatusToCanceled}</a>
          </#if>
          <#if paymentValue.paymentId?has_content && (paymentValue.statusId.equals("PMNT_RECEIVED") || paymentValue.statusId.equals("PMNT_SENT"))>
            <a class="buttontext" href="<@ofbizUrl>setPaymentStatus?paymentId=${paymentValue.paymentId}&statusId=PMNT_CONFIRMED</@ofbizUrl>">${uiLabelMap.FinancialsPaymentStatusToConfirmed}</a>
          </#if>
        </#if>  

    </div>    
    <div class="boxhead">
    <#if paymentValue?has_content>
      ${uiLabelMap.AccountingPayment} #${paymentValue.paymentId}
    <#else>
      <#if isDisbursement>
        ${uiLabelMap.FinancialsPayablesPayment}
      <#else>  
        ${uiLabelMap.FinancialsReceivablesPayment}
      </#if>  
    </#if>  
    </div>
</div>

<#if paymentValue?has_content && paymentValue.paymentId?has_content>
  <#assign formName = "updatePayment">
<#else>
  <#assign formName = "createPayment">
</#if>
<form method="post" action="<@ofbizUrl>${formName}</@ofbizUrl>" name="${formName}">

<#if paymentValue?has_content && paymentValue.paymentId?has_content>
  <@inputHidden name="paymentId" value="${paymentValue.paymentId}"/>
<#else>
  <@inputHidden name="statusId" value="PMNT_NOT_PAID"/>
</#if>

<#if isDisbursement>
  <@inputHidden name="partyIdFrom" value="${organizationPartyId?if_exists}"/>
<#else>
  <@inputHidden name="partyIdTo" value="${organizationPartyId?if_exists}"/>
</#if>

<table class="twoColumnForm" style="border:none">
  <#-- this is for not losing the original payment type when creating a payment, as it must be DISBURSEMENT or RECEIPT, but it will change when the form is submitted -->
  <@inputHidden name="paymentType" value=(paymentType)! />

  <@inputSelectRow title=uiLabelMap.AccountingPaymentType name="paymentTypeId" ignoreParameters=true default=defaultPaymentTypeId?default("CUSTOMER_PAYMENT") list=paymentTypeList key="paymentTypeId" displayField="description" />
  <tr>
    <#if isDisbursement>
      <@displayTitleCell title=uiLabelMap.FinancialsPayToParty />
      <#if toParty?exists>
        <@inputHidden name="partyIdTo" value="${toParty.partyId}"/>
        <@displayCell text="${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, toParty.partyId, false)} (${toParty.partyId})"/>
      <#else/>
        <@inputAutoCompletePartyCell name="partyIdTo" id="autoCompletePartyIdTo" />
      </#if>
    <#else/>
      <@displayTitleCell title=uiLabelMap.FinancialsReceiveFromParty />
      <#if fromParty?exists>
        <@inputHidden name="partyIdFrom" value="${fromParty.partyId}"/>
        <@displayCell text="${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, fromParty.partyId, false)} (${fromParty.partyId})"/>
      <#else/>
        <@inputAutoCompletePartyCell name="partyIdFrom" id="autoCompletePartyIdFrom" />
      </#if>
    </#if>
  </tr>

  <#if paymentValue?has_content>
    <@inputHidden name="statusId" value="${paymentValue.statusId?if_exists}" />
    <#assign statusValue = paymentValue.getRelatedOne("StatusItem")/>
    <@displayRow title=uiLabelMap.FinancialsStatusId text=statusValue.description! />  
  </#if>

  <#if paymentMethodList?exists && paymentMethodList?has_content && ( fromParty?exists || isDisbursement )>
    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingPaymentMethod />
      <td nowrap="nowrap">
        <select name="paymentMethodId" id="paymentMethodId" class="selectBox" onchange="checkPaymentMethodTypeId()">
          <#if ! isDisbursement><option value=""></option></#if>
          <#list paymentMethodList as paymentMethod>
            <#-- default to the current payment paymentMethodId, unless it has no value, in which case use the default -->
            <option value="${paymentMethod.get("paymentMethodId")?if_exists}"  <#if paymentValue?has_content && paymentValue.paymentMethodId?default("") == paymentMethod.paymentMethodId>selected="selected"<#elseif !(paymentValue?has_content) && (paymentMethod.paymentMethodId == defaultPaymentMethodId?default(""))>selected="selected"</#if>>
              <#-- assign and check that credit card/eft account actually exists.  if so, use them in the description -->
              <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")?if_exists/>
              <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")?if_exists/>
              <#if creditCard?has_content> 
                ${creditCard.cardType} ${creditCard.cardNumber[creditCard.cardNumber?length-4..creditCard.cardNumber?length-1]} ${creditCard.expireDate}
              <#elseif eftAccount?has_content> 
                ${uiLabelMap.AccountingEFTAccount} ${eftAccount.accountNumber[eftAccount.accountNumber?length-4..eftAccount.accountNumber?length-1]}
              <#else>
                ${paymentMethod.description?if_exists} (${paymentMethod.paymentMethodId})
              </#if>
            </option>
          </#list>
        </select>
      </td>
    </tr>
  </#if>

  <#if ! isDisbursement >
    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingPaymentMethodType />
      <td nowrap="nowrap">
        <select name="paymentMethodTypeId" id="paymentMethodTypeId" class="selectBox">
          <#list paymentMethodTypeList as paymentMethodType>
            <option value="${paymentMethodType.get("paymentMethodTypeId")?if_exists}"  <#if paymentValue?has_content && paymentValue.paymentMethodTypeId?default("") == paymentMethodType.paymentMethodTypeId>selected="selected"</#if>>${paymentMethodType.get("description")}</option>
          </#list>
        </select>
      </td>
    </tr>
  </#if>

  <tr>
    <@displayTitleCell title=uiLabelMap.AccountingAmount />
    <td nowrap="nowrap">
      <input type="text" class="inputBox" name="amount" value="<#if paymentValue?has_content>${paymentValue.amount?if_exists}<#elseif parameters.amount?exists>${parameters.amount}</#if>"/>
      <select name="currencyUomId" class="selectBox">
	<#list currencyUoms as currencyUom>
	  <option value="${currencyUom.uomId}" <#if currencyUomId == currencyUom.uomId>selected="selected"</#if>>${currencyUom.abbreviation}</option>
	</#list>
      </select>
    </td>
  </tr>

  <tr>
    <@displayTitleCell title=uiLabelMap.AccountingEffectiveDate />
    <td nowrap="nowrap">
      <#if paymentValue?has_content>
        <@inputDateTime name="effectiveDate" default=paymentValue.effectiveDate?if_exists/>
      <#else>
        <@inputDateTime name="effectiveDate"/>
      </#if>
    </td>
  </tr>

  <tr>
    <@displayTitleCell title=uiLabelMap.CommonComments />
    <td width="80%" colspan="4"><textarea class="textAreaBox" cols="60" rows="2" name="comments"><#if paymentValue?has_content>${paymentValue.comments?if_exists}</#if></textarea></td>
  </tr>

  <tr>
    <@displayTitleCell title=uiLabelMap.FinancialsPaymentRefNum />
    <td nowrap="nowrap"><input type="text" class="inputBox" name="paymentRefNum" value="<#if paymentValue?has_content>${paymentValue.paymentRefNum?if_exists}</#if>"/></td>
  </tr>

  <#if tagTypes?has_content>
    <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=paymentValue! />
  </#if>

  <#if paymentId?has_content>
    <@inputSubmitRow title=uiLabelMap.CommonUpdate />
  <#else>
    <@inputSubmitRow title=isDisbursement?string(uiLabelMap.FinancialsMakePayment, uiLabelMap.FinancialsReceivePayment) />
  </#if>

</table>
</form>

</#if> <#-- if isDisbursement exists -->
