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
<#if !creditCard?exists>
  <form method="post" action="<@ofbizUrl>createCreditCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editcreditcardform">
    <#assign submitLabel = uiLabelMap.CommonCreate />
<#else>
  <form method="post" action="<@ofbizUrl>updateCreditCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editcreditcardform">
    <@inputHidden name="paymentMethodId" value=paymentMethodId />
    <#assign submitLabel = uiLabelMap.CommonSave />
</#if>
  <@inputHidden name="partyId" value=partyId />
  <table class="twoColumnForm" style="border:0">
    <@inputTextRow title=uiLabelMap.AccountingCompanyNameCard name="companyNameOnCard" default=(creditCardData.companyNameOnCard)! />
    <#assign prefix={
             uiLabelMap.CommonTitleMr:uiLabelMap.CommonTitleMr,
             uiLabelMap.CommonTitleMrs:uiLabelMap.CommonTitleMrs,
             uiLabelMap.CommonTitleMs:uiLabelMap.CommonTitleMs,
             uiLabelMap.CommonTitleDr:uiLabelMap.CommonTitleDr
             }/>
    <@inputSelectHashRow title=uiLabelMap.AccountingPrefixCard name="titleOnCard" hash=prefix default=(creditCardData.titleOnCard)! required=false />
    <@inputTextRow title=uiLabelMap.AccountingFirstNameCard titleClass="requiredField" name="firstNameOnCard" default=(creditCardData.firstNameOnCard)! />
    <@inputTextRow title=uiLabelMap.AccountingMiddleNameCard name="middleNameOnCard" default=(creditCardData.middleNameOnCard)! size=15 />
    <@inputTextRow title=uiLabelMap.AccountingLastNameCard titleClass="requiredField" name="lastNameOnCard" default=(creditCardData.lastNameOnCard)! size=20 />
    <#assign suffix={
             "Jr.":"Jr.",
             "Sr.":"Sr.",
             "I":"I",
             "II":"II",
             "III":"III",
             "IV":"IV",
             "V":"V"
             }/>
    <@inputSelectHashRow title=uiLabelMap.AccountingSuffixCard name="suffixOnCard" hash=suffix default=(creditCardData.suffixOnCard)! required=false />

    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingCardType titleClass="requiredField" />
      <td>
        <select name="cardType" class="inputBox">
          <option>${(creditCardData.cardType)!}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
        </select>
      </td>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingCardNumber titleClass="requiredField" />
      <td>
        <#if creditCardData?has_content>
          <#-- create a display version of the card where all but the last four digits are * -->
          <#assign cardNumberDisplay = "">
          <#assign cardNumber = creditCardData.cardNumber?if_exists>
          <#if cardNumber?has_content>
            <#assign size = cardNumber?length - 4>
            <#if (size gt 0)>
              <#list 0 .. size-1 as foo>
                <#assign cardNumberDisplay = cardNumberDisplay + "*">
              </#list>
              <#assign cardNumberDisplay = cardNumberDisplay + cardNumber[size .. size + 3]>
              <#else>
                <#-- but if the card number has less than four digits (ie, it was entered incorrectly), display it in full -->
                <#assign cardNumberDisplay = cardNumber>
            </#if>
          </#if>
        </#if>
        <@inputText size=20 maxlength=30 name="cardNumber" default=cardNumberDisplay! />
      </td>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.AccountingExpirationDate titleClass="requiredField" />
      <td>
        <#assign expMonth = "">
        <#assign expYear = "">
        <#if creditCard?exists>
          <#assign expDate = creditCard.expireDate>
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

    <#include "commonEditBillingAddress.ftl"/>

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

