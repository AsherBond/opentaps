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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">
  function submitPreview(form) {    
    form.cardType.value = form.settleFrom.value.substring(3);
    form.settlementGlAccountId.value = form.settleTo.value;
    form.undepositedReceiptGlAccountId.value = form.settleFrom.value.substring(3);
    if(form.settleFrom.value.substring(0,3) == "UD:") {
      form.action="<@ofbizUrl>settleUndepositedPayments</@ofbizUrl>"
    } else {
      form.action="<@ofbizUrl>settleCreditCardPayments</@ofbizUrl>"
    } 
    form.submit();
  }

  function submitSettle(form) {
    form.submit();
  }

  function changeListFrom(form) {
    var displayUD = "inline";

    if(form.paymentOrRefund.value == "REFUND") {
      displayUD = "none";
    }
    for (var i=0; i < form.settleFrom.length; i++) {    
       if(form.settleFrom[i].value.substring(0,3) == "UD:") {
         form.settleFrom[i].style.display = displayUD;
         if(displayUD == "none") {
           form.settleFrom[i+1].selected = true;
         } else {
           form.settleFrom[0].selected = true;  
         }         
       }      
    }
  }
</script>

<#macro listGlAccounts name glAccounts defaultGlAccountCode>
<select name="${name}" class="inputBox">
<#list glAccounts as glAccount>
  <option value="${glAccount.glAccountId}" <#if glAccount.accountCode == defaultGlAccountCode> selected="selected"</#if>>${glAccount.accountCode} ${glAccount.accountName}</option>
</#list>
</select>
</#macro>

<#if security.hasEntityPermission("FINANCIALS", "_TX_VIEW", userLogin)>

<form method="POST" name="createSettlePayments" action="<@ofbizUrl>createSettlementAcctgTrans</@ofbizUrl>"> 
  <input type="hidden" name="submitType" value=""/>
  <input type="hidden" name="cardType" value="" />
  <input type="hidden" name="settlementGlAccountId" value="" />
  <input type="hidden" name="undepositedReceiptGlAccountId" value="" />
  <div class="form" style="border:0">
    <table class="twoColumnForm" style="border:0">
      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsSettlementType />
        <td>
        <select name="paymentOrRefund" id="paymentOrRefund" class="inputBox" default="REFUND" onChange="javascript:changeListFrom(this.form)" >
          <option value="PAYMENT" <#if requestParameters.paymentOrRefund?if_exists == "PAYMENT"> selected="selected" </#if> >Payment </option>
          <option value="REFUND" <#if requestParameters.paymentOrRefund?if_exists == "REFUND"> selected="selected" </#if> >Refund  </option>
        </select>
        </td>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsSettlePaymentMethod />
        <td>
        <select name="settleFrom" class="inputBox">
          <#if undepositedReceiptsAccounts?has_content>
            <#list undepositedReceiptsAccounts as glAccount>
              <#if requestParameters.paymentOrRefund?if_exists == "REFUND">
                <option style="display:none" value="UD:${glAccount.glAccountId}" <#if requestParameters.settleFrom?exists && requestParameters.settleFrom.substring(3) == glAccount.glAccountId > selected="selected" </#if> >${glAccount.accountName}</option>
              <#else>
                <option style="display:inline" value="UD:${glAccount.glAccountId}" <#if requestParameters.settleFrom?exists && requestParameters.settleFrom.substring(3) == glAccount.glAccountId > selected="selected" </#if> >${glAccount.accountName}</option>
              </#if>
            </#list>
          </#if>
          <#list creditCardTypes as creditCardType>
            <option value="CC:${creditCardType.cardType}" <#if requestParameters.settleFrom?exists && requestParameters.settleFrom.substring(3) == creditCardType.cardType > selected="selected" </#if> >${creditCardType.cardType}</option>
          </#list>
        </select>
        </td>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsSettlementAccount />
        <td>
        <@listGlAccounts name="settleTo" glAccounts=settlementAccounts defaultGlAccountCode=requestParameters.settleTo?if_exists />
        </td>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsTransactionDate />
        <#if requestParameters.transactionDatePrev?exists >
            <@inputDateTimeCell name="transactionDate" default=Static["org.ofbiz.base.util.UtilDateTime"].toTimestamp(requestParameters.transactionDatePrev) />    
        <#else>
            <@inputDateTimeCell name="transactionDate" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
        </#if>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonAmount titleClass="requiredField" />
        <@inputTextCell size="10" name="amount" default=""/>
      </tr>

      <tr><td colspan="2">&nbsp;</td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <input name="settleButton" type="submit" class="smallSubmit" value="${uiLabelMap.FinancialsSettle}" onClick="javascript:submitSettle(this.form)"/>              
              &nbsp;
              <input name="previewButton" type="button" class="smallSubmit" value="${uiLabelMap.FinancialsSettlePreview}" onClick="javascript:submitPreview(this.form)"/>
            </td>
          </tr>
    </table>

  </div>
</form>

</#if>
