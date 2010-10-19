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

<#-- Combinaition of createLead and survey -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#-- CSS styles placed here for convenience.  Feel free to move these somewhere else. -->
<style type="text/css">
  table.catalogRequestSurvey {
    margin-left: 20px;
  }
</style>

<p class="tabletext">Please complete the fields below to request a catalog from us.  If you are from outside the United States,
use "N/A" for the state code.</p>

<form name="catalogRequestForm" method="POST" action="<@ofbizUrl>createCatalogRequest</@ofbizUrl>">
  <input type="hidden" name="custRequestTypeId"  value="RF_CATALOG"/>
  <#if parameters.donePage?exists || donePage?exists>
  <input type="hidden" name="donePage" value="${parameters.donePage?default(donePage)}"/>
  </#if>

<table class="catalogRequestSurvey">
  <tr>
    <td class="requiredField">${uiLabelMap.PartyFirstName}</td>
    <td><input class="inputBox" name="firstName" value="${parameters.firstName?if_exists}" size="35"/></td>
  </tr>
  <tr>
    <td class="requiredField">${uiLabelMap.PartyLastName}</td>
    <td><input class="inputBox" name="lastName" value="${parameters.lastName?if_exists}" size="35"/></td>
  </tr>
  <tr>
    <td>${uiLabelMap.CrmCompanyName}</td>
    <td><input class="inputBox" name="companyName" value="${parameters.companyName?if_exists}" size="35"/></td>
  </tr>
  <tr>
    <td class="requiredField">${uiLabelMap.PartyAddressLine1}</td>
    <td><input class="inputBox" name="generalAddress1" id="generalAddress1" value="${parameters.generalAddress1?if_exists}" size="35"/></td>
  </tr>
  <tr>
    <td>${uiLabelMap.PartyAddressLine2}</td>
    <td><input class="inputBox" name="generalAddress2" id="generalAddress2" value="${parameters.generalAddress2?if_exists}" size="35"/></td>
  </tr>
  <tr>
    <td class="requiredField" valign="top">
      ${uiLabelMap.PartyCity},
      State, 
      Zip
    </td>
    <td>
      <input class="inputBox" name="generalCity" id="generalCity" value="${parameters.generalCity?if_exists}"/>, 
      <select name="generalStateProvinceGeoId" id="generalStateProvinceGeoId" class="selectBox">
          <option value=""></option>
        <#list states as state>
          <option value="${state.geoId}">${state.abbreviation}</option>
        </#list>
          <option value="">N/A</option>  <!-- in case the customer is not from the USA -->
      </select>
      <input class="inputBox" name="generalPostalCode" id="generalPostalCode" value="${parameters.generalPostalCode?if_exists}" size="12"/>-
      <input class="inputBox" name="generalPostalCodeExt" id="generalPostalCodeExt" value="${parameters.generalPostalCodeExt?if_exists}" size="5"/>
      <#if validatePostalAddresses>
        <@flexArea targetId="postalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false></@flexArea>
      </#if>
    </td>
  </tr>
  <tr>
    <td  class="requiredField">${uiLabelMap.PartyCountry}</td>
    <td>
      <select name="generalCountryGeoId" id="generalCountryGeoId" class="selectBox">
        <#list countries as country>
          <option value="${country.geoId}" <#if country.geoId=="USA">selected</#if>>${country.geoName}</option>
        </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td>${uiLabelMap.PartyPhoneNumber}</td>
    <td>
      <input class="inputBox" type="text" name="primaryPhoneCountryCode" value="${configProperties.defaultCountryCode?if_exists}" size="2"/>
      (<input class="inputBox" type="text" name="primaryPhoneAreaCode" value="${parameters.primaryPhoneAreaCode?if_exists}" size="3"/>)
      <input class="inputBox" type="text" name="primaryPhoneNumber" value="${parameters.primaryPhoneNumber?if_exists}" size="8"/>
    </td>
  </tr>
  <#if validatePostalAddresses>
    <script type="text/javascript">
        opentaps.addListenerToNode(document.getElementById('generalPostalCode'), 'onchange', function(){opentaps.postalAddress.validationListener('postalAddressValidationError', 'generalAddress1', 'generalAddress2', 'generalCity', 'generalStateProvinceGeoId', 'generalCountryGeoId', 'generalPostalCode', 'generalPostalCodeExt')})
    </script>
  </#if>
  <tr>
    <td>${uiLabelMap.PartyEmailAddress}</td>
    <td><input class="inputBox" name="primaryEmail" value="${parameters.primaryEmail?if_exists}" size="35"/></td>
  </tr>

  ${surveyWrapper.render("/opentaps/crmsfa/templates/survey/simpleFormlessSurvey.ftl")}

  <tr>
    <td>How did you hear abut us?</td>
    <td>
      <select name="marketingCampaignId" class="selectBox">
        <option value=""></option>
        <#list campaigns as campaign>
        <option value="${campaign.marketingCampaignId}" <#if (parameters.marketingCampaignId?has_content) && (parameters.marketingCampaignId==campaign.marketingCampaignId)>SELECTED</#if> >${campaign.campaignName}</option>
        </#list>
      </select>
    </td>
  </tr>

  <tr>
    <td>${uiLabelMap.CommonComments}</td>
    <td><textarea name="comments" class="textAreaBox" cols="40" rows="5"></textarea></td>
  </tr>

  <tr>
    <td></td>
    <td><input class="smallSubmit" type="submit" value="Request Catalog" onclick="javascript:submitFormDisableButton(this)"/></td>
  </tr>
</table>

</form>

<p><a href="<@ofbizUrl>/main</@ofbizUrl>" class="tabletext">Go back</a></p>
