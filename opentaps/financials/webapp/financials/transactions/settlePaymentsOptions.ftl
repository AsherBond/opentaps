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

