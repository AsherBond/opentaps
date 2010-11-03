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
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones
 *@author     Olivier Heintz (olivier.heintz@nereide.biz)
 *@since      1.0
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#if !contactMech?exists>
  <#-- When creating a new contact mech, first select the type, then actually create -->
  <#if !preContactMechTypeId?has_content>
    <p class="head1">${uiLabelMap.PartyCreateNewContact}</p>
    <form method="post" action="<@ofbizUrl>editcontactmech</@ofbizUrl>" name="createcontactmechform">
      <@inputHidden name="donePage" value=donePage />
      <@inputHidden name="errorPage" value=errorPage?if_exists />
      <@inputHidden name="DONE_PAGE" value=donePageFull />
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <@displayTitleCell title=uiLabelMap.PartySelectContactType />
          <@inputSelectCell name="preContactMechTypeId" list=mechMap.contactMechTypes key="contactMechTypeId" displayField="description" />
        </tr>
      </table>
    </form>
  </#if>
</#if>

<#if mechMap.contactMechTypeId?has_content>
  <#if createContactMechLabel?exists>
    <@sectionHeader title=createContactMechLabel />
  <#else>
    <@sectionHeader title=uiLabelMap.PartyCreateNewContact />
  </#if>
  <#-- always creating a new ContactMech -->
    <form method="post" action="<@ofbizUrl>createPostalAddressAndPurpose</@ofbizUrl>" name="editcontactmechform">
      <div class="form" >
        <table class="twoColumnForm" style="border:0">
          <#if mechMap.purposeTypes?has_content && !cmNewPurposeTypeId?has_content && !contactMechPurposeTypeId?has_content && !contactMechPurposeType?exists>
            <tr>
              <@displayTitleCell title=uiLabelMap.CommonPurpose />
              <@inputSelectCell name="contactMechPurposeTypeId" list=mechMap.purposeTypes required=false key="contactMechPurposeTypeId" displayField="description" />
            </tr>
          <#elseif contactMechPurposeType?exists && !createContactMechLabel?exists>
            <tr>
              <@displayTitleCell title=uiLabelMap.CommonPurpose />
              <@displayCell text=contactMechPurposeType.description />
            </tr>
          </#if>
          <@inputHidden name="donePage" value=donePage />
          <@inputHidden name="DONE_PAGE" value=donePageFull />
          <@inputHidden name="errorPage" value=errorPage?if_exists />
          <@inputHidden name="contactMechTypeId" value=mechMap.contactMechTypeId />
          <#if orderId?exists>
            <@inputHidden name="orderId" value=orderId />
            <@inputHidden name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}" />
            <@inputHidden name="oldContactMechId" value="${oldContactMechId?if_exists}" />
          </#if>
          <#if cmNewPurposeTypeId?has_content><@inputHidden name="contactMechPurposeTypeId" value=cmNewPurposeTypeId /></#if>
          <#if preContactMechTypeId?exists><@inputHidden name="preContactMechTypeId" value=preContactMechTypeId /></#if>
          <#if contactMechPurposeTypeId?exists><@inputHidden name="contactMechPurposeTypeId" value=contactMechPurposeTypeId /></#if>
          <#if paymentMethodId?has_content><@inputHidden name="paymentMethodId" value=paymentMethodId /></#if>
    <#if "POSTAL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyToName />
        <@inputTextCell name="toName" default="${(mechMap.postalAddress.toName)?default(request.getParameter('toName')?default(defaultToName)?if_exists)}" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyAttentionName />
        <@inputTextCell name="attnName" default="${(mechMap.postalAddress.attnName)?default(request.getParameter('attnName')?if_exists)}" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyAddressLine1 titleClass="requiredField" />
        <@inputTextCell name="address1" default="${(mechMap.postalAddress.address1)?default(request.getParameter('address1')?if_exists)}" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyAddressLine2 />
        <@inputTextCell name="address2" default="${(mechMap.postalAddress.address2)?default(request.getParameter('address2')?if_exists)}" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyCity titleClass="requiredField" />
        <@inputTextCell name="city" default="${(mechMap.postalAddress.city)?default(request.getParameter('city')?if_exists)}" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyState />
        <@inputStateCountryCell address=mechMap.postalAddress />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyZipCode titleClass="requiredField" />
        <td>
            <@inputText name="postalCode" size=12 default="${(mechMap.postalAddress.postalCode)?default(request.getParameter('postalCode')?if_exists)}" />* &nbsp;
            <@inputText name="postalCodeExt" size=5 default="${(mechMap.postalAddress.postalCodeExt)?default(request.getParameter('postalCodeExt')?if_exists)}" />
            <#if validatePostalAddresses>
              <@flexArea targetId="postalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false></@flexArea>
              <script type="text/javascript">
                  opentaps.addListenerToNode(document.getElementById('postalCode'), 'onchange', function(){opentaps.postalAddress.validationListener('postalAddressValidationError', 'address1', 'address2', 'city', 'stateProvinceGeoId', 'countryGeoId', 'postalCode', 'postalCodeExt')})
              </script>
            </#if>
        </td>
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyPostalDirections />
        <@inputTextareaCell name="directions" cols=60 rows=2 default=(mechMap.postalAddress.directions)?default(request.getParameter('directions')?if_exists) />
      </tr>
    <#elseif "TELECOM_NUMBER" = mechMap.contactMechTypeId?if_exists>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyPhoneNumber />
        <td>
          <input type="text" class="inputBox" size="4" maxlength="10" name="countryCode"
           <#if (mechMap.telecomNumber.countryCode)?exists>
             value="${mechMap.telecomNumber.countryCode}">
           <#elseif request.getParameter('countryCode')?exists>
             value="${request.getParameter('countryCode')}">
           <#else>
             value="${configProperties.defaultCountryCode}">
           </#if>

          -&nbsp;<input type="text" class="inputBox" size="4" maxlength="10" name="areaCode" value="${(mechMap.telecomNumber.areaCode)?default(request.getParameter('areaCode')?if_exists)}">
          -&nbsp;<input type="text" class="inputBox" size="15" maxlength="15" name="contactNumber" value="${(mechMap.telecomNumber.contactNumber)?default(request.getParameter('contactNumber')?if_exists)}">
          &nbsp;${uiLabelMap.PartyContactExt}&nbsp;<input type="text" class="inputBox" size="6" maxlength="10" name="extension" value="${(mechMap.partyContactMech.extension)?default(request.getParameter('extension')?if_exists)}">
        </td>
      </tr>
      <tr>
        <@displayTitleCell title="" />
        <@displayCell text="[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyContactExt}]" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.CrmPhoneAskForName />
        <@inputTextCell name="askForName" default="${(mechMap.telecomNumber.askForName)?if_exists}" />
      </tr>
    <#elseif "EMAIL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
      <tr>
        <@displayTitleCell title=mechMap.contactMechType.get("description",locale) titleClass="requiredField" />
        <@inputTextCell name="emailAddress" default="${(contactMech.infoString)?default(request.getParameter('emailAddress')?if_exists)}" />
      </tr>
    <#else>
      <tr>
        <@displayTitleCell title=mechMap.contactMechType.get("description",locale) titleClass="requiredField" />
        <@inputTextCell name="infoString" default="${(contactMech.infoString)?if_exists}" />
      </tr>
    </#if>
  </table>

  <div style="margin-left: 24%;">
    <#if (orderId?exists || forCart?exists) && !disableOrderOnly?exists >
      <@inputHidden name="onlyForOrder" value="Y" />
    </#if>
    <a href="javascript:document.editcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
    <a href="<@ofbizUrl>${donePageFull}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a>
  </div>
  </form>
  </div> <#-- div class="form" -->
<#else>
  <div style="margin-left: 24%;">
    <a href="<@ofbizUrl>${donePageFull}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  </div>
</#if>
