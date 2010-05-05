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

<#-- This file may contain code which has been modified from that included with the Apache-licensed OFBiz application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if session.getAttribute("facilityId")?exists>

<#assign extraOptions>
<a href="<@ofbizUrl>EditContactMech</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonAddNew}</a>
</#assign>

<@frameSection title=uiLabelMap.PartyContactInformation extra=extraOptions>

<#if contactMeches?has_content>
    <table width="100%" border="0" cellpadding="0">
      <#list contactMeches as contactMechMap>
          <#assign contactMech = contactMechMap.contactMech>
          <#assign facilityContactMech = contactMechMap.facilityContactMech>
          <tr>
            <td align="right" valign="top" width="10%">
              <div class="tabletext">&nbsp;<b>${contactMechMap.contactMechType.get("description",locale)}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#list contactMechMap.facilityContactMechPurposes as facilityContactMechPurpose>
                  <#assign contactMechPurposeType = facilityContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                    <div class="tabletext">
                      <#if contactMechPurposeType?has_content>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.ProductPurposeTypeNotFoundWithId}: "${facilityContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      <#if facilityContactMechPurpose.thruDate?has_content>
                      (${uiLabelMap.CommonExpire}: ${getLocalizedDate(facilityContactMechPurpose.thruDate)})
                      </#if>
                    </div>
              </#list>
              <#if "POSTAL_ADDRESS" = contactMech.contactMechTypeId>
                  <#assign postalAddress = contactMechMap.postalAddress>
                  <div class="tabletext">                    
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1?if_exists}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2?if_exists}<br/></#if>
                    ${postalAddress.city?if_exists},
                    ${postalAddress.stateProvinceGeoId?if_exists}
                    ${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                  </div>
                  <#if (postalAddress?has_content && !postalAddress.countryGeoId?has_content) || postalAddress.countryGeoId = "USA">
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                      </#if>
                  </#if>
              <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div class="tabletext">
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber?if_exists}
                    <#if facilityContactMech.extension?has_content>${uiLabelMap.CommonExt} ${facilityContactMech.extension}</#if>
                  </div>
              <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <a href="mailto:${contactMech.infoString?if_exists}" class="buttontext">${uiLabelMap.CommonSendEmail}</a>
                  </div>
              <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="buttontext">${uiLabelMap.CommonOpenPageNewWindow}</a>
                  </div>
              <#else>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                  </div>
              </#if>
              <#if facilityContactMech.thruDate?has_content><div class="tabletext"><b>${uiLabelMap.CommonUpdatedEffectiveThru}:&nbsp;${getLocalizedDate(facilityContactMech.thruDate)}</b></div></#if>
            </td>
            <td width="5">&nbsp;</td>
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
              <td align="right" valign="top">
                <a href="<@ofbizUrl>EditContactMech?facilityId=${facilityId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>
              </td>
            </#if>
            <#if security.hasEntityPermission("PARTYMGR", "_DELETE", session)>
              <td align="right" valign="top">
                <a href="<@ofbizUrl>deleteFacilityContactMech/ViewContactMechs?facilityId=${facilityId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonExpire}</a>
              </td>
            </#if>
          </tr>
          <tr><td colspan="7"><hr class="sepbar"></td></tr>          
      </#list>
    </table>
<#else>
  <div class="tabletext">${uiLabelMap.CommonNoContactInformationOnFile}.</div>
</#if>

</@frameSection>

</#if>
