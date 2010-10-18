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


<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if !giftCard?exists>
  <form method="post" action="<@ofbizUrl>createGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform">
    <#assign submitLabel = uiLabelMap.CommonCreate />
<#else>
  <form method="post" action="<@ofbizUrl>updateGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform">
    <@inputHidden name="paymentMethodId" value=paymentMethodId />
    <#assign submitLabel = uiLabelMap.CommonSave />
</#if>
  <@inputHidden name="partyId" value=partyId />
  <table class="twoColumnForm" style="border:0">
    <@inputTextRow title=uiLabelMap.AccountingCardNumber titleClass="requiredField" name="cardNumber" default=(giftCardData.cardNumber)! size=20 />
    <@inputTextRow title=uiLabelMap.AccountingPinNumber name="pinNumber" default=(giftCardData.pinNumber)! size=10 />
    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingExpirationDate titleClass="requiredField" />
      <td>
        <#assign expMonth = "">
        <#assign expYear = "">
        <#if giftCardData?exists && giftCardData.expireDate?exists>
          <#assign expDate = giftCardData.expireDate>
          <#if (expDate?exists && expDate.indexOf("/") gt 0)>
            <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
            <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
          </#if>
        </#if>
        <select name="expMonth" class="inputBox">
          <option><#if tryEntity>${expMonth?if_exists}<#else>${requestParameters.expMonth?if_exists}</#if></option>
          ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
        </select>
        <select name="expYear" class="inputBox">
          <option><#if tryEntity>${expYear?if_exists}<#else>${requestParameters.expYear?if_exists}</#if></option>
          ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
        </select>
      </td>
    </tr>

    <@inputTextRow title=uiLabelMap.CommonDescription name="description" default=(paymentMethodData.description)! />

    <#-- Ofbiz service does not set the billing address on gift cards
       <#include "commonEditBillingAddress.ftl"/>
       -->

    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <@inputSubmit title=submitLabel />
        <@inputConfirm title=uiLabelMap.CommonCancel href="${donePage}?partyId=${partyId}"/>
      </td>
    </tr>
  </table>
</form>

