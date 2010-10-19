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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<#--
 *  Copyright (c) 2002-2005 The Open For Business Project - www.ofbiz.org
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
 * @author     Johan Isacsson
 * @author     David E. Jones
 * @author     Andy Zeneski
 * @author     Olivier Heintz (olivier.heintz@nereide.biz)
 * @created    May 26 2003
 */
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- ============================================================= -->

<#-- This assignment allows the editcontactmech page to go to the donePage -->
<#-- Notes on how to use:  if you have a link that executes a service and needs to return to this page on "success" or "error", then set up the URL
     as normal and add extra parameters partyId=${partySummary.partyId} and donePage=${donePage}.  If the link goes to a different page which
     needs to speficy the done page to come back here, then use DONE_PAGE=${donePageEscaped} instead.  -->
<#if parameters.partyId?exists>
<#assign donePageEscaped = donePage + "?partyId%3d" + parameters.partyId>
</#if>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.PartyContactInformation}</div>
  <#if hasUpdatePermission?exists>
  <div class="subMenuBar">
    <@selectAction name="myProfileContactMech" prompt="${uiLabelMap.CommonCreateNew}">
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=POSTAL_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsAddress}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=TELECOM_NUMBER&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsPhoneNumber}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=EMAIL_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.CommonEmail}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=WEB_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsWebUrl}"/>
    </@selectAction>
  </div>
  </#if>
</div>

<div class="form">
  <#if contactMeches?has_content>

    <table class="contactTable">

      <tr>
        <th><span class="tableheadtext">${uiLabelMap.PartyContactType}</span></th>
        <th><span class="tableheadtext">${uiLabelMap.PartyContactInformation}</span></th>
        <th><span class="tableheadtext">${uiLabelMap.CommonPurpose}</span></th>
        <#if hasUpdatePermission?exists>
        <th><span class="tableheadtext">&nbsp;</span></th>
        </#if>
      </tr>

      <#list contactMeches as contactMechMap>
          <#assign contactMech = contactMechMap.contactMech>
          <#assign partyContactMech = contactMechMap.partyContactMech>
          <tr>

            <#-- contact type -->

            <td>
              <div class="tabletext"><b>${contactMechMap.contactMechType.description}</b>
              </div>
            </td>

            <#-- contact information -->
            <td>
              <#if "POSTAL_ADDRESS" = contactMech.contactMechTypeId>
                  <#assign postalAddress = contactMechMap.postalAddress>
                  <div class="tabletext">
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.PartyAddrToName}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1?if_exists}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city?if_exists},
                     <#if postalAddress.stateProvinceGeoId?has_content>
                      <@displayGeoName geoId=postalAddress.stateProvinceGeoId />
                    </#if>
                    ${postalAddress.postalCode?if_exists}
                    <div style="margin: 3px;">
                    <#if postalAddress.countryGeoId?default("") == "USA">
                    <#assign query = postalAddress.address1?default("")?replace(" ", "+") + "+" + postalAddress.city?default("")?replace(" ", "+") + "+" + postalAddress.stateProvinceGeoId?default("") + "+" + postalAddress.postalCode?default("") />
                    <a href="http://maps.google.com/?q=${query}" class="buttontext" target="_blank">${uiLabelMap.OpentapsMapIt}</a>
                    <#else>
                    <#if postalAddress.countryGeoId?has_content><br/><@displayGeoName geoId=postalAddress.countryGeoId /></#if>
                    </#if>
                    <@form name="createCatalogRequestForPartyForm_${contactMechMap_index}" url="createCatalogRequestForParty" partyId="${partySummary.partyId}" fromPartyId="${partySummary.partyId}" fulfillContactMechId="${contactMech.contactMechId}" custRequestTypeId="RF_CATALOG" donePage="${donePage}" statusId="CRQ_SUBMITTED" />
                    <@submitFormLink form="createCatalogRequestForPartyForm_${contactMechMap_index}" text="${uiLabelMap.OpentapsCreateCatalogRequest}" class="buttontext"/>
                    </div>
                  </div>
              <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div class="tabletext">
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                    <#if partyContactMech.extension?has_content>${uiLabelMap.PartyContactExt}&nbsp;${partyContactMech.extension}</#if>
                  </div>
                  <#if telecomNumber.askForName?has_content>
                  <div class="tabletext"><span class="tableheadtext">${uiLabelMap.OpentapsPhoneAskForName}:</span> ${telecomNumber.askForName}</div>
                  </#if>
              <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    <a href="<@ofbizUrl>writeEmail?contactMechIdTo=${contactMech.contactMechId}&internalPartyId=${parameters.partyId?if_exists}&donePage=${donePage?if_exists}</@ofbizUrl>" class="linktext">${contactMech.infoString?if_exists}</a>&nbsp;
                    </div>
                  </div>
              <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="linktext">${contactMech.infoString?if_exists}</a>
                  </div>
              <#else>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                  </div>
              </#if>
              <#if partyContactMech.thruDate?has_content><div class="tabletext"><b>${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${getLocalizedDate(partyContactMech.thruDate)}</b></div></#if>
            <#if (partyContactMech.allowSolicitation?default("") == "N")><div class="tabletext"><font color="red"><b>${uiLabelMap.OpentapsDoNotSolicit}</b></font></div></#if>
            </td>

            <#-- purposes -->

            <td>
              <#list contactMechMap.partyContactMechPurposes as partyContactMechPurpose>
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                    <div class="tabletext">
                      <#if contactMechPurposeType?has_content>
                        ${contactMechPurposeType.description}
                      <#else>
                        ${uiLabelMap.PartyMechPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"
                      </#if>
                      <#if partyContactMechPurpose.thruDate?has_content>
                      (${uiLabelMap.CommonExpire}: ${getLocalizedDate(partyContactMechPurpose.thruDate)})
                      </#if>
                    </div>
              </#list>
            </td>

            <#if hasUpdatePermission?exists>
            <td>
                 <form name="deleteContactMechForm${contactMechMap_index}" method="post" action="<@ofbizUrl>deleteContactMech</@ofbizUrl>">
                      <@inputHidden name="partyId" value="${partySummary.partyId}"/>
                      <@inputHidden name="contactMechId" value="${contactMech.contactMechId}"/>
                      <@inputHidden name="donePage" value="${donePage}"/>
                  </form>
                  <a href="<@ofbizUrl>${editContactMechPage}?partyId=${partySummary.partyId}&contactMechId=${contactMech.contactMechId}&DONE_PAGE=${donePageEscaped}</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/edit.gif</@ofbizContentUrl>" width="22" height="21" border="0" alt="${uiLabelMap.CommonUpdate}"/></a>&nbsp;
                  <a href="javascript:document.deleteContactMechForm${contactMechMap_index}.submit()"><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonExpire}"/></a>&nbsp;&nbsp;
            </td>
            </#if>

          </tr>
        
      </#list>

    </table>

  <#else>
    <div class="tabletext">${uiLabelMap.PartyNoContactInformation}</div>
  </#if>
</div>
