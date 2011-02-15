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
<#assign paymentStatusChangeAction = "">

<#if payment?has_content>
  <#if hasCreatePermission>
    <#assign paymentStatusChangeAction><a class="subMenuButton" href="<@ofbizUrl>editPayment?paymentTypeId=${paymentTypeId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></#assign>
  </#if>
  <#if hasUpdatePermission>
    <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<a class="subMenuButton" href="<@ofbizUrl>viewPayment?paymentId=${payment.paymentId}</@ofbizUrl>">${uiLabelMap.CommonView}</a></#assign>
    <#if isDisbursement && payment.isNotPaid()>
      <@form name="paymentSentAction" url="setPaymentStatus" paymentId=payment.paymentId statusId="PMNT_SENT" />
      <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<@submitFormLink form="paymentSentAction" text=uiLabelMap.FinancialsPaymentStatusToSent class="subMenuButton" /></#assign>
    </#if>
    <#if !isDisbursement && payment.isNotPaid()>
      <@form name="paymentReceivedAction" url="setPaymentStatus" paymentId=payment.paymentId statusId="PMNT_RECEIVED" />
      <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<@submitFormLink form="paymentReceivedAction" text=uiLabelMap.FinancialsPaymentStatusToReceived class="subMenuButton" /></#assign>
    </#if>
    <#if payment.isNotPaid()>
      <@form name="paymentCancelledAction" url="setPaymentStatus" paymentId=payment.paymentId statusId="PMNT_CANCELLED" />
      <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<@submitFormLinkConfirm form="paymentCancelledAction" text=uiLabelMap.FinancialsPaymentStatusToCanceled class="subMenuButtonDangerous" /></#assign>
    </#if>
    <#if (payment.isReceived() || payment.isSent())>
      <@form name="paymentConfirmedAction" url="setPaymentStatus" paymentId=payment.paymentId statusId="PMNT_CONFIRMED" />
      <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<@submitFormLink form="paymentConfirmedAction" text=uiLabelMap.FinancialsPaymentStatusToConfirmed class="subMenuButton" /></#assign>
    </#if>
  </#if>
  <#if isDisbursement && ! payment.isCancelled() && ! payment.isVoided()>
    <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<a href="<@ofbizUrl>/check.pdf?paymentId=${payment.paymentId}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.AccountingPrintAsCheck}</a></#assign>
  </#if>
  <#if hasUpdatePermission && (payment.isReceived() || payment.isSent()) && !payment.lockboxBatchItemDetails?has_content>
    <@form name="paymentVoidAction" url="voidPayment" paymentId=payment.paymentId />
    <#assign paymentStatusChangeAction>${paymentStatusChangeAction}<@submitFormLinkConfirm form="paymentVoidAction" text=uiLabelMap.FinancialsPaymentVoidPayment class="subMenuButtonDangerous" /></#assign>
  </#if>
</#if>

<#assign sectionTitle>
  <#if payment?has_content>${uiLabelMap.AccountingPayment} #${payment.paymentId}<#else><#if isDisbursement>${uiLabelMap.FinancialsPayablesPayment}<#else>${uiLabelMap.FinancialsReceivablesPayment}</#if></#if>
</#assign>

<@frameSection title=sectionTitle extra=paymentStatusChangeAction?if_exists>

<#-- set as true if there is a payment and its status only allow comments / reference number to be updated -->
<#assign limitiedUpdate = false/>

<#if payment?has_content>
  <#if payment.isSent() || payment.isReceived()>
    <#-- this only allows a limited update -->
    <#assign formName = "updateSentOrReceivedPayment"/>
    <#assign limitiedUpdate = true/>
  <#else>
    <#assign formName = "updatePayment"/>
  </#if>
<#else>
  <#assign formName = "createPayment"/>
</#if>
<form method="post" action="<@ofbizUrl>${formName}</@ofbizUrl>" name="${formName}">

<#if payment?has_content>
  <@inputHidden name="paymentId" value="${payment.paymentId}"/>
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

  <#if limitiedUpdate>
    <@displayRow title=uiLabelMap.AccountingPaymentType text=(payment.paymentType.description)! />
  <#else>
    <@inputSelectRow title=uiLabelMap.AccountingPaymentType name="paymentTypeId" ignoreParameters=true default=defaultPaymentTypeId?default("CUSTOMER_PAYMENT") list=paymentTypeList key="paymentTypeId" displayField="description" />
  </#if>

  <tr>
    <#if isDisbursement>
      <@displayTitleCell title=uiLabelMap.FinancialsPayToParty />
      <#if toParty?exists>
        <@inputHidden name="partyIdTo" value="${toParty.partyId}"/>
        <@displayCell text="${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, toParty.partyId, false)} (${toParty.partyId})"/>
      <#elseif !limitiedUpdate/>
        <@inputAutoCompletePartyCell name="partyIdTo" id="autoCompletePartyIdTo" />
      </#if>
    <#else/>
      <@displayTitleCell title=uiLabelMap.FinancialsReceiveFromParty />
      <#if fromParty?exists>
        <@inputHidden name="partyIdFrom" value="${fromParty.partyId}"/>
        <@displayCell text="${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, fromParty.partyId, false)} (${fromParty.partyId})"/>
      <#elseif !limitiedUpdate/>
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
      <@displayTitleCell title=uiLabelMap.FinancialsPaymentMethod />
      <#if limitiedUpdate>
        <@displayCell text=(payment.paymentMethod.description)! />
      <#else>
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
      </#if>
    </tr>
  </#if>

  <#if ! isDisbursement >
    <tr>
      <@displayTitleCell title=uiLabelMap.CommonPaymentMethodType />
      <#if limitiedUpdate>
        <@displayCell text=(payment.paymentMethodType.description)! />
      <#else>
        <td nowrap="nowrap">
          <select name="paymentMethodTypeId" id="paymentMethodTypeId" class="selectBox">
            <#list paymentMethodTypeList as paymentMethodType>
              <option value="${paymentMethodType.get("paymentMethodTypeId")?if_exists}"  <#if paymentValue?has_content && paymentValue.paymentMethodTypeId?default("") == paymentMethodType.paymentMethodTypeId>selected="selected"</#if>>${paymentMethodType.get("description")}</option>
            </#list>
          </select>
        </td>
      </#if>
    </tr>
  </#if>

  <tr>
    <@displayTitleCell title=uiLabelMap.AccountingAmount />
    <#if limitiedUpdate>
      <@displayCurrencyCell amount=(payment.amount)!0 currencyUomId=(payment.currencyUomId)! />
    <#else>
      <td nowrap="nowrap">
        <input type="text" class="inputBox" name="amount" value="<#if paymentValue?has_content>${paymentValue.amount?if_exists}<#elseif parameters.amount?exists>${parameters.amount}</#if>"/>
        <select name="currencyUomId" class="selectBox">
          <#list currencyUoms as currencyUom>
            <#if !currencyUomId?has_content>
              <option value="" selected="selected"></option>
            </#if>
            <option value="${currencyUom.uomId}" <#if currencyUomId?has_content && currencyUomId == currencyUom.uomId>selected="selected"</#if>>${currencyUom.abbreviation}</option>
          </#list>
        </select>
      </td>
    </#if>
  </tr>

  <tr>
    <@displayTitleCell title=uiLabelMap.AccountingEffectiveDate />
    <#if limitiedUpdate>
      <@displayDateCell date=(paymentValue.effectiveDate)! />
    <#else>
      <td nowrap="nowrap">
        <#if paymentValue?has_content>
          <@inputDateTime name="effectiveDate" default=paymentValue.effectiveDate?if_exists/>
        <#else>
          <@inputDateTime name="effectiveDate"/>
        </#if>
      </td>
    </#if>
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
    <#if limitiedUpdate>
      <@accountingTagsDisplayRows tags=tagTypes entity=paymentValue! />
    <#elseif !allocatePaymentTagsToApplications>
      <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=paymentValue! />
    </#if>
  </#if>

  <#if paymentId?has_content>
    <@inputSubmitRow title=uiLabelMap.CommonUpdate />
  <#else>
    <@inputSubmitRow title=isDisbursement?string(uiLabelMap.FinancialsMakePayment, uiLabelMap.FinancialsReceivePayment) />
  </#if>

</table>
</form>
</@frameSection>

</#if> <#-- if isDisbursement exists -->
