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

<#-- ============================================================= -->

<#-- This assignment allows the editcontactmech page to go to the donePage -->
<#-- Notes on how to use:  if you have a link that executes a service and needs to return to this page on "success" or "error", then set up the URL
     as normal and add extra parameters partyId=${partySummary.partyId} and donePage=${donePage}.  If the link goes to a different page which
     needs to speficy the done page to come back here, then use DONE_PAGE=${donePageEscaped} instead.  -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if parameters.partyId?exists>
<#assign donePageEscaped = donePage + "?partyId%3d" + parameters.partyId>
</#if>

<#assign extraOptions>
  <#if hasUpdatePermission?exists && hasUpdatePermission == true>
    <@selectAction name="createNewContactMechTarget" prompt="${uiLabelMap.CommonCreateNew}">
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=POSTAL_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsAddress}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=TELECOM_NUMBER&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsPhoneNumber}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=EMAIL_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.CommonEmail}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=WEB_ADDRESS&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.OpentapsWebUrl}"/>
      <@action url="${editContactMechPage}?partyId=${partySummary.partyId}&amp;preContactMechTypeId=SKYPE&amp;DONE_PAGE=${donePageEscaped}" text="${uiLabelMap.CrmSkypeContact}"/>
    </@selectAction>
  </#if>
</#assign>

<@frameSection title=uiLabelMap.PartyContactInformation extra=extraOptions>
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

      <#list userLogins as userLogin>
        <tr>

          <#-- contact type -->

            <td>
              <div class="tabletext"><b>${uiLabelMap.CrmUserLogin}</b>
              </div>
            </td>

            <#-- contact information -->
            <td>
              <div class="tabletext">
                <#if userLogin.userLoginId?has_content>${userLogin.userLoginId}
                  <#if userLogin.enabled?has_content && userLogin.enabled == "N" >
                    <b>(${uiLabelMap.CommonDisabled})</b>
                  <#else>
                    <b>(${uiLabelMap.CommonEnabled})</b>
                  </#if>
                </#if>
              </div>
            </td>

            <#-- purposes -->
            <td>
            </td>

            <td>
               <#if hasPassPermission?exists>
                  <a href="<@ofbizUrl>viewPartyPassword?partyId=${partySummary.partyId}&userLoginId=${userLogin.userLoginId}&DONE_PAGE=${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>&nbsp;
               </#if>
            </td>

         </tr>
      </#list>

      <#list contactMeches as contactMechMap>
          <#assign contactMech = contactMechMap.contactMech>
          <#assign partyContactMech = contactMechMap.partyContactMech>
          <tr>

            <#-- contact type -->

            <td>
              <div class="tabletext"><b>${contactMechMap.contactMechType.get("description",locale)}</b>
              </div>
            </td>

            <#-- contact information -->
            <td>
              <#if "POSTAL_ADDRESS" == contactMech.contactMechTypeId && contactMechMap.postalAddress?exists>
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
                    <#if postalAddress.postalCodeExt?has_content>-${postalAddress.postalCodeExt}</#if>
                    <#if postalAddress.directions?has_content><br/>[${postalAddress.directions}]</#if>
                    <#if postalAddress.countryGeoId?default("") == "USA">
                    <#assign query = postalAddress.address1?default("")?replace(" ", "+") + "+" + postalAddress.city?default("")?replace(" ", "+") + "+" + postalAddress.stateProvinceGeoId?default("") + "+" + postalAddress.postalCode?default("") />
                    (<a href="http://maps.google.com/?q=${query}" class="linktext" target="_blank">${uiLabelMap.CrmMapIt}</a>)
                    <#else>
                    <#if postalAddress.countryGeoId?has_content><br/><@displayGeoName geoId=postalAddress.countryGeoId /></#if>
                    </#if>
                  </div>
              <#elseif "TELECOM_NUMBER" == contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <#assign voipEnabled = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("voip.properties", "voip.enabled")>
                  <div class="tabletext">
                  <#if voipEnabled?default("") == "Y">
                  <a href="<@ofbizUrl>logTaskFormAndDial?workEffortPurposeTypeId=WEPT_TASK_PHONE_CALL&contactMechIdTo=${contactMech.contactMechId}&internalPartyId=${parameters.partyId?if_exists}&donePage=${donePage?if_exists}</@ofbizUrl>" class="linktext">
                  </#if>	
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                    <#if partyContactMech.extension?has_content>${uiLabelMap.PartyContactExt}&nbsp;${partyContactMech.extension}</#if>
                  <#if voipEnabled?default("") == "Y"> 
                    </a>
                  </#if>	  
                  </div>
                  <#if telecomNumber.askForName?has_content>
                  <div class="tabletext"><span class="tableheadtext">${uiLabelMap.CrmPhoneAskForName}:</span> ${telecomNumber.askForName}</div>
                  </#if>
              <#elseif "EMAIL_ADDRESS" == contactMech.contactMechTypeId>
                  <div class="tabletext">
                    <a href="<@ofbizUrl>writeEmail?contactMechIdTo=${contactMech.contactMechId}&internalPartyId=${parameters.partyId?if_exists}&donePage=${donePage?if_exists}</@ofbizUrl>" class="linktext">${contactMech.infoString?if_exists}</a>&nbsp;
                    </div>
                  </div>
              <#elseif "WEB_ADDRESS" == contactMech.contactMechTypeId>
                  <div class="tabletext">
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="linktext">${contactMech.infoString?if_exists}</a>
                  </div>
              <#elseif "SKYPE" == contactMech.contactMechTypeId>
                  <div class="tabletext">
                    <a href="skype:${contactMech.infoString?if_exists}?call" class="linktext">${contactMech.infoString?if_exists}</a>&nbsp;<img src="http://mystatus.skype.com/smallicon/${contactMech.infoString?if_exists}" style="vertical-align:middle"/>
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
                        ${contactMechPurposeType.get("description",locale)}
                      <#else>
                        ${uiLabelMap.PartyMechPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"
                      </#if>
                      <#if partyContactMechPurpose.thruDate?has_content>
                      (${uiLabelMap.CommonExpire}: ${getLocalizedDate(partyContactMechPurpose.thruDate)})
                      </#if>
                    </div>
              </#list>
            </td>
            <td>
               <#if hasUpdatePermission?exists>
                  <form name="deleteContactMechForm${contactMechMap_index}" method="post" action="<@ofbizUrl>deleteContactMech</@ofbizUrl>">
                      <@inputHidden name="partyId" value="${partySummary.partyId}"/>
                      <@inputHidden name="contactMechId" value="${contactMech.contactMechId}"/>
                      <@inputHidden name="donePage" value="${donePage}"/>
                  </form>
                  <a href="<@ofbizUrl>${editContactMechPage}?partyId=${partySummary.partyId}&contactMechId=${contactMech.contactMechId}&DONE_PAGE=${donePageEscaped}</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/edit.gif</@ofbizContentUrl>" width="22" height="21" border="0" alt="${uiLabelMap.CommonUpdate}"/></a>&nbsp;
                  <a href="javascript:document.deleteContactMechForm${contactMechMap_index}.submit()"><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonExpire}"/></a>&nbsp;&nbsp;
               </#if>
               <#if "POSTAL_ADDRESS" == contactMech.contactMechTypeId && contactMechMap.postalAddress?exists && !("Y" == disableRequestCatalog!)>
                  <br/><br/>
                  <@form name="createCatalogRequestForPartyForm_${contactMechMap_index}" url="createCatalogRequestForParty" partyId="${partySummary.partyId}" fromPartyId="${partySummary.partyId}" fulfillContactMechId="${contactMech.contactMechId}" custRequestTypeId="RF_CATALOG" statusId="CRQ_SUBMITTED" donePage=donePage! />
                  <@submitFormLink form="createCatalogRequestForPartyForm_${contactMechMap_index}" text="${uiLabelMap.CrmCreateCatalogRequest}" class="buttontext"/>
               </#if>
            </td>
          </tr>
      </#list>

    </table>

  <#else>
    <div class="tabletext">${uiLabelMap.PartyNoContactInformation}</div>
  </#if>
</@frameSection>
