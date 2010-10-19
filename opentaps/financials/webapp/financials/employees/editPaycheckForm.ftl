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

<#if paycheck?has_content>

<!-- Paycheck Header -->

<div class="screenlet">
  <div class="screenlet-header">
    <div class="boxhead">
        ${uiLabelMap.FinancialsPaycheck}<a href="<@ofbizUrl>viewPaycheck?paymentId=${paycheck.paymentId}</@ofbizUrl>" class="linktext" style="color:yellow;"> ${uiLabelMap.OrderNbr}${paycheck.paymentId}</a>
    </div>    
  </div>

  <div class="screenlet-body">
    <table border="0" cellpadding="2" cellspacing="0" width="100%">
	<#if (paycheck.statusId == "PMNT_NOT_PAID") && (hasUpdatePermission)>
        <form method="post" action="<@ofbizUrl>updatePaycheck</@ofbizUrl>" name="updatePaycheckForm">
        	  <@inputHidden name="paymentId" value="${paycheck.paymentId}"/>        
              <@displayRow title=uiLabelMap.CommonStatus text=paycheck.getRelatedOneCache("StatusItem").get("description", "FinancialsEntityLabel", locale) />        	  
              <@displayRow title=uiLabelMap.FinancialsPaycheckType text=paycheck.getRelatedOneCache("PaymentType").get("description", "FinancialsEntityLabel", locale)/>
              <@displayRow title=uiLabelMap.FinancialsPayToParty text=paycheck.partyIdTo />
              <@inputSelectRow name="paymentMethodId" title=uiLabelMap.FinancialsPaymentMethod list=paymentMethodList key="paymentMethodId" displayField="description" default=paycheck.paymentMethodId/>
              <tr>
		          <@displayCell text=uiLabelMap.FinancialsGrossAmount blockClass="titleCell" blockStyle="width: 100px" class="tableheadtext"/>
	          	  <@inputCurrencyCell name="amount" currencyName="currencyUomId" default=paycheck.amount defaultCurrencyUomId=paycheck.currencyUomId/>
			  </tr>	          	                
        	  <@inputDateTimeRow name="effectiveDate" title=uiLabelMap.AccountingEffectiveDate default=paycheck.effectiveDate form="updatePaycheckForm" />
              <@inputTextRow name="comments" title=uiLabelMap.CommonComments size=60 default=paycheck.comments/>
              <@inputTextRow name="paymentRefNum" title=uiLabelMap.FinancialsPaymentRefNum default=paycheck.paymentRefNum/>
              <@inputSubmitRow title=uiLabelMap.FinancialsUpdatePaycheck />
        </form>
      <#else>
    	<@inputHidden name="paymentId" value="${paycheck.paymentId}"/>        
        <@displayRow title=uiLabelMap.CommonStatus text=paycheck.getRelatedOneCache("StatusItem").get("description", "FinancialsEntityLabel", locale) />        	  
        <@displayRow title=uiLabelMap.FinancialsPaycheckType text=paycheck.getRelatedOneCache("PaymentType").get("description", "FinancialsEntityLabel", locale)/>
        <@displayRow title=uiLabelMap.FinancialsPayToParty text=paycheck.partyIdTo />
        <@displayRow title=uiLabelMap.FinancialsPaymentMethod text=paycheck.paymentMethodId />
        <@displayCurrencyRow title=uiLabelMap.FinancialsGrossAmount amount=paycheck.amount currencyUomId=paycheck.currencyUomId />
        <@displayDateRow title=uiLabelMap.AccountingEffectiveDate date=paycheck.effectiveDate />
        <@displayRow title=uiLabelMap.CommonComments text=paycheck.comments?if_exists />
        <@displayRow title=uiLabelMap.FinancialsPaymentRefNum text=paycheck.paymentRefNum?if_exists />        
      </#if>
    </table>
  </div>

</div>

</#if>





