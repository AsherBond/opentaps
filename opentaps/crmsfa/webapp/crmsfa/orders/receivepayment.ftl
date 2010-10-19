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

<#-- This file has been modified by Open Source Strategies, Inc. -->


<#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
 
  <script type="text/javascript">
  /*<![CDATA[*/
      function calculateChange(/*boolean*/ wasClicked) {
          var inputs = document.forms.paysetupform.getElementsByTagName('input');
          var orderTotal = ${order.totalMinusPaymentPrefs};
          var paymentsTotal = 0;
          for (var x = 0; x < inputs.length; x++) {
              if (! inputs[x].id.match("^payType_")) continue;
              var payment = parseFloat(inputs[x].value);
              if (! isNaN(payment)) paymentsTotal += payment;
          }

          if (paymentsTotal == 0) {
              if (wasClicked != null && wasClicked == true) {
                  alert('No payment to receive.');
              }
              return false;
          }

          var disbursementInput = document.getElementById('disbursementAmount');
          var disbursementDisplay = document.getElementById('disbursementAmountDisplay');
          if (paymentsTotal > orderTotal && disbursementInput != null) {
              var disbursementAmount = (paymentsTotal - orderTotal).toFixed(2);
              disbursementInput.value = disbursementAmount;
              disbursementDisplay.innerHTML = opentaps.formatCurrency(disbursementAmount, '${order.currencyUom?if_exists}');
              opentaps.expandCollapse('disburseChange', null, true);
          } else {
              disbursementInput.value = '';
              disbursementDisplay.innerHTML = '&nbsp;';
              opentaps.expandCollapse('disburseChange', null, null, true);
          }
          var submitButton = document.getElementById('submit');
          submitButton.onclick = function() {document.paysetupform.submit()};
          return true;
      }
  /*]]>*/
  </script>

  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

  <#assign methodsToShow = ["CASH", "MONEY_ORDER", "PERSONAL_CHECK", "COMPANY_CHECK", "CERTIFIED_CHECK", "EXT_PAYPAL", "CREDIT_CARD", "GIFT_CARD"]/>
  <form method="post" action="<@ofbizUrl>receiveOfflinePaymentsAndDisburseChange</@ofbizUrl>" name="paysetupform">    
    <input type="hidden" name="orderId" value="${order.orderId}">
    <#if requestParameters.workEffortId?exists>
    	<input type="hidden" name="workEffortId" value="${requestParameters.workEffortId}">
    </#if>
    <table cellpadding="1" cellspacing="5" border="0" style="text-align:right; width:50%">
      <tr>
        <td width="200px"><div class="tableheadtext"><u>${uiLabelMap.CrmOrderRemainingTotal}</u></div></td>
        <td align="center" width="15%"><div class="tableheadtext"><@ofbizCurrency amount=order.totalMinusPaymentPrefs isoCode=order.currencyUom/></div></td>
      </tr>    
      <tr><td>&nbsp;</td></tr>
      <tr>
        <td width="200px"><div class="tableheadtext"><u>${uiLabelMap.OrderPaymentType}</u></div></td>
        <td align="center" width="15%"><div class="tableheadtext"><u>${uiLabelMap.OrderAmount}</u></div></td>
        <td align="left" width="25%"><div class="tableheadtext"><u>${uiLabelMap.FinancialsPaymentRefNum}</u></div></td>
      </tr>    
      <#list methodsToShow as paymentMethodTypeId>
        <#list paymentMethodTypes as payType>
          <#if paymentMethodTypeId == payType.paymentMethodTypeId>
            <tr>
              <td><div class="tabletext">${payType.get("description",locale)?default(payType.paymentMethodTypeId)}</div></td>
              <td align="center" width="15%"><input type="text" size="10" id="payType_${payType_index}" name="${payType.paymentMethodTypeId}_amount" class="inputBox" onchange="calculateChange();"></td>
              <td align="left"><input type="text" size="15" name="${payType.paymentMethodTypeId}_reference" class="inputBox"></td>
            </tr>
          </#if>
        </#list>
      </#list>
    </table>
    <@flexArea title="" targetId="disburseChange" style="border:none;padding-left:0" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
      <table cellpadding="1" cellspacing="5" border="0" style="text-align:right; width:50%">
        <tr>
          <td width="200px"><div class="tableheadtext">Disburse Change</div></td>
          <td align="center" width="15%">
            <input type="hidden" id="disbursementAmount" name="disbursementAmount" value=""/>
            <span id="disbursementAmountDisplay">&nbsp;</span>
          </td>
          <td align="center" width="25%">&nbsp;</td>
        </tr>    
      </table>
    </@flexArea>
    <table cellpadding="1" cellspacing="5" border="0" style="text-align:right; width:48%; white-space: nowrap">
      <tr>
        <td width="200px">&nbsp;</td>
        <#-- this button should say "Receive" unless it's disbursing change in which case it should say "Done" -->
        <td align="left" colspan="2" width="40%"><a id="submit" onclick="calculateChange(true);" class="buttontext" href="javascript:void(0)"><#if disbursementAmount?has_content>${uiLabelMap.OpentapsDone}<#else>${uiLabelMap.CommonReceive}</#if></a>&nbsp; 
        <a href="<@ofbizUrl>orderview?orderId=${order.orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OpentapsOrderReturnToOrder}</a></td>
      </tr>    
    </table>
  </form>

  <script>
  /*<![CDATA[*/  
  // auto focus the first form input 
  <#list methodsToShow as paymentMethodTypeId>
    <#list paymentMethodTypes as payType>
      <#if paymentMethodTypeId == payType.paymentMethodTypeId>
        document.getElementById('payType_${payType_index}').focus();
        <#break/>
      </#if>
    </#list>
    <#break/>
  </#list>
  /*]]>*/
  </script>
  
<br/>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
