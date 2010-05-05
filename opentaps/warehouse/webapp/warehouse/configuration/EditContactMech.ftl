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

<#-- This screen had several issues that required re-implementing.
1.  The facility contact mech requests conflicted with party ones, e.g. deleteContactMech, createEmailAddress, etc.
    These were changed in this file to point to Facility specific requests.
2.  Some of the links have conventions which were changed to suit warehouse better.  For instance, facilityId does not need to be passed.
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if !mechMap.facilityContactMech?exists && mechMap.contactMech?exists>
  <@frameSection title=uiLabelMap.PartyContactInfoNotBelongToYou>
    <a href="<@ofbizUrl>viewWarehouse</@ofbizUrl>" class="buttontext">${uiLabelMapCommon.Back}</a>
  </@frameSection>
<#else>
  <#if !mechMap.contactMech?exists>
    <#-- When creating a new contact mech, first select the type, then actually create -->
    <#if !preContactMechTypeId?has_content>
      <@frameSection title=uiLabelMap.PartyCreateNewContact>
        <form method="post" action="<@ofbizUrl>EditContactMech</@ofbizUrl>" name="createcontactmechform">
          <@inputHidden name="facilityId" value="${facilityId}"/>
          <@inputHidden name="DONE_PAGE" value="${donePage?if_exists}"/>
          <table class="twoColumnForm">
            <@inputSelectRow title=uiLabelMap.PartySelectContactType name="preContactMechTypeId" list=mechMap.contactMechTypes key="contactMechTypeId" displayField="description" />
            <@inputSubmitRow title=uiLabelMap.CommonCreate />
          </table>
        </form>
      </@frameSection>
    </#if>
  </#if>

  <#if mechMap.contactMechTypeId?has_content>
    <#if !mechMap.contactMech?has_content>
      <@frameSectionHeader title=uiLabelMap.PartyCreateNewContact/>
      <#if contactMechPurposeType?exists>
        <div>(${uiLabelMap.PartyMsgContactHavePurpose}<b>"${contactMechPurposeType.get("description",locale)?if_exists}"</b>)</div>
      </#if>
      <table class="twoColumnForm">
        <form method="post" action="<@ofbizUrl>${mechMap.requestName}Facility</@ofbizUrl>" name="editcontactmechform">
          <@inputHidden name="DONE_PAGE" value="${donePage}"/>
          <@inputHidden name="contactMechTypeId" value="${mechMap.contactMechTypeId}"/>
          <@inputHidden name="facilityId" value="${facilityId}"/>
          <#if preContactMechTypeId?exists><@inputHidden name="preContactMechTypeId" value="${preContactMechTypeId}"/></#if>
          <#if contactMechPurposeTypeId?exists><@inputHidden name="contactMechPurposeTypeId" value="${contactMechPurposeTypeId?if_exists}"/></#if>
          <#if paymentMethodId?exists><@inputHidden name="paymentMethodId" value="${paymentMethodId}"/></#if>
    <#else>
      <@frameSectionHeader title=uiLabelMap.PartyEditContactInformation/>
      <table class="twoColumnForm">
        <#if mechMap.purposeTypes?has_content>
          <@form name="deleteFacilityContactMechPurposeForm" url="deleteFacilityContactMechPurpose" facilityId="${facilityId}" contactMechId="" contactMechPurposeTypeId="" fromDate="" DONE_PAGE="${donePage}" useValues="true"/>
          <tr>
            <@displayTitleCell title=uiLabelMap.PartyContactPurposes />
            <td>
              <table border="0" cellspacing="1">
                <#if mechMap.facilityContactMechPurposes?has_content>
                  <#list mechMap.facilityContactMechPurposes as facilityContactMechPurpose>
                    <#assign contactMechPurposeType = facilityContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")/>
                    <tr>
                      <td>
                        <#if contactMechPurposeType?has_content>
                          <b>${contactMechPurposeType.get("description",locale)}</b>
                        <#else>
                          <b>${uiLabelMap.PartyMechPurposeTypeNotFound}: "${facilityContactMechPurpose.contactMechPurposeTypeId}"</b>
                        </#if>
                        (${uiLabelMap.CommonSince}:${getLocalizedDate(facilityContactMechPurpose.fromDate)})
                        <#if facilityContactMechPurpose.thruDate?has_content>(${uiLabelMap.CommonExpires}: ${getLocalizedDate(facilityContactMechPurpose.thruDate)}</#if>
                      </td>
                      <td>
                        <@submitFormLink form="deleteFacilityContactMechPurposeForm" text="${uiLabelMap.CommonDelete}" contactMechId="${contactMechId}" contactMechPurposeTypeId="${facilityContactMechPurpose.contactMechPurposeTypeId}" fromDate="${facilityContactMechPurpose.fromDate.toString()}" />
                      </td>
                    </tr>
                  </#list>
                </#if>

                <tr>
                  <form method="post" action="<@ofbizUrl>createFacilityContactMechPurpose?DONE_PAGE=${donePage}&useValues=true</@ofbizUrl>" name="newpurposeform">
                    <@inputHidden name="facilityId" value="${facilityId}"/>
                    <@inputHidden name="contactMechId" value="${contactMechId?if_exists}"/>
                    <@inputSelectCell name="contactMechPurposeTypeId" list=mechMap.purposeTypes key="contactMechPurposeTypeId" displayField="description" />
                  </form>
                  <td>
                    <a href="javascript:document.newpurposeform.submit()" class="buttontext">${uiLabelMap.PartyAddPurpose}</a>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </#if>
        <form method="post" action="<@ofbizUrl>${mechMap.requestName}Facility</@ofbizUrl>" name="editcontactmechform">
          <@inputHidden name="contactMechId" value="${contactMechId}"/>
          <@inputHidden name="contactMechTypeId" value="${mechMap.contactMechTypeId}"/>
          <@inputHidden name="facilityId" value="${facilityId}"/>
    </#if>

    <#if "POSTAL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
      <@inputTextRow title=uiLabelMap.PartyToName name="toName" default=(mechMap.postalAddress.toName)! />
      <@inputTextRow title=uiLabelMap.PartyAttentionName name="attnName" default=(mechMap.postalAddress.attnName)! />
      <@inputTextRow title=uiLabelMap.PartyAddressLine1 name="address1" default=(mechMap.postalAddress.address1)! titleClass="requiredField" />
      <@inputTextRow title=uiLabelMap.PartyAddressLine2 name="address2" default=(mechMap.postalAddress.address2)! />
      <@inputTextRow title=uiLabelMap.PartyCity name="city" default=(mechMap.postalAddress.city)! titleClass="requiredField" />
      <@inputStateCountryRow title=uiLabelMap.PartyState address=(mechMap.postalAddress)! titleClass="requiredField" />
      <@inputTextRow title=uiLabelMap.PartyZipCode name="postalCode" default=(mechMap.postalAddress.postalCode)! titleClass="requiredField" />
  <#elseif "TELECOM_NUMBER" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <@displayTitleCell title=uiLabelMap.PartyPhoneNumber titleClass="requiredField" />
      <td>
        <input type="text" class="inputBox" size="4" maxlength="10" name="countryCode" value="${(mechMap.telecomNumber.countryCode)?default(request.getParameter("countryCode")?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="4" maxlength="10" name="areaCode" value="${(mechMap.telecomNumber.areaCode)?default(request.getParameter("areaCode")?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="15" maxlength="15" name="contactNumber" value="${(mechMap.telecomNumber.contactNumber)?default(request.getParameter("contactNumber")?if_exists)}">
        &nbsp;${uiLabelMap.PartyContactExt}&nbsp;<input type="text" class="inputBox" size="6" maxlength="10" name="extension" value="${(mechMap.facilityContactMech.extension)?default(request.getParameter("extension")?if_exists)}">
      </td>
    </tr>
    <tr>
      <@displayTitleCell title="" />
      <@displayCell text="[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]" />
    </tr>
  <#elseif "EMAIL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <@displayTitleCell title=uiLabelMap.PartyEmailAddress titleClass="requiredField" />
      <@inputTextCell name="emailAddress" default="${(mechMap.contactMech.infoString)?default(request.getParameter('emailAddress')?if_exists)}" />
    </tr>
  <#else>
    <tr>
      <@displayTitleCell title=mechMap.contactMechType.get("description",locale) titleClass="requiredField" />
      <@inputTextCell name="infoString" default="${(mechMap.contactMech.infoString)?if_exists}" />
    </tr>
  </#if>
  <tr>
    <td>&nbsp;</td>
    <td>
      <a href="<@ofbizUrl>viewWarehouse</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.editcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
    </td>
  </tr>
  </form>
  </table>
  <#else>
    &nbsp;<a href="<@ofbizUrl>viewWarehouse</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  </#if>
</#if>
