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
 *  
-->

<#macro listGlAccounts name glAccounts defaultGlAccountId>
<select name="${name}" class="inputBox">
<#list glAccounts as glAccount>
  <option value="${glAccount.glAccountId}" <#if glAccount.glAccountId == defaultGlAccountId>selected="true"</#if>>${glAccount.accountCode} ${glAccount.accountName}</option>
</#list>
</select>
</#macro>


<#if security.hasEntityPermission("FINANCIALS", "_TX_VIEW", userLogin)>
<ul class="bulletList">
<li class="tabletext"><form method="post" action="<@ofbizUrl>settleUndepositedPayments</@ofbizUrl>">
<input type="hidden" name="paymentOrRefund" value="PAYMENT"/>
${uiLabelMap.FinancialsSettlePaymentsFrom} 
<@listGlAccounts name="undepositedReceiptGlAccountId" glAccounts=undepositedReceiptsAccounts defaultGlAccountId=defaultUndepositedReceiptsAccount.glAccountId/>
${uiLabelMap.CommonTo}
<@listGlAccounts name="settlementGlAccountId" glAccounts=settlementAccounts defaultGlAccountId=defaultSettlementAccount.glAccountId/>

<input type="submit" name="settleButton" value="${uiLabelMap.FinancialsSettle}" class="smallSubmit"/>
</form>


<li class="tabletext"><form method="post" action="<@ofbizUrl>settleCreditCardPayments</@ofbizUrl>"> 
<input type="hidden" name="paymentOrRefund" value="PAYMENT"/>
${uiLabelMap.FinancialsSettlePaymentsFrom} 
<select name="cardType" class="inputBox">
<#list creditCardTypes as creditCardType>
    <option value="${creditCardType.cardType}">${creditCardType.cardType}</option>
</#list>
</select>
${uiLabelMap.CommonTo}
<@listGlAccounts name="settlementGlAccountId" glAccounts=settlementAccounts defaultGlAccountId=defaultSettlementAccount.glAccountId/>

<input type="submit" name="settleButton" value="${uiLabelMap.FinancialsSettle}" class="smallSubmit"/>
</form>
<li class="tabletext"><form method="post" action="<@ofbizUrl>settleCreditCardPayments</@ofbizUrl>"> 
<input type="hidden" name="paymentOrRefund" value="REFUND"/>
${uiLabelMap.FinancialsSettleRefundsFrom} 
<select name="cardType" class="inputBox">
<#list creditCardTypes as creditCardType>
    <option value="${creditCardType.cardType}">${creditCardType.cardType}</option>
</#list>
</select>

${uiLabelMap.CommonTo}
<@listGlAccounts name="settlementGlAccountId" glAccounts=settlementAccounts defaultGlAccountId=defaultSettlementAccount.glAccountId/>

<input type="submit" name="settleButton" value="${uiLabelMap.FinancialsSettle}" class="smallSubmit"/>
</form>

</li>
</ul>
</#if>

