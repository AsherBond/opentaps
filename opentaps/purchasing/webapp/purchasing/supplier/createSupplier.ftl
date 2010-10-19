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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form name="createSupplierParty" method="post" action="<@ofbizUrl>createSupplierParty</@ofbizUrl>">

<table class="fourColumnForm">

  <tr><td colspan="4" class="formSectionHeaderTitle">${uiLabelMap.PurchSupplierDetails}</td></tr>
  <@inputTextRow name="partyId" title=uiLabelMap.PartyPartyId size=20 maxlength=20 />
  <@inputTextRow name="groupName" title=uiLabelMap.PurchSupplierName titleClass="requiredField" />
  <#if requestAttributes.duplicateSuppliersWithName?has_content>
    <tr>
      <td/>
      <td colspan="3"><span class="errorMessage">${uiLabelMap.PurchSuppliersWithDuplicateName}:</span> <#list requestAttributes.duplicateSuppliersWithName as dup><@displayPartyLink partyId=dup.partyId /><#if dup_has_next>, </#if></#list></td>
    </tr>
  </#if>  
  <@inputTextRow name="federalTaxId" title=uiLabelMap.OpentapsTaxAuthPartyId />
  <@inputIndicatorRow name="requires1099" title=uiLabelMap.OpentapsRequires1099 />
  <@inputIndicatorRow name="isIncorporated" title=uiLabelMap.OpentapsIsIncorporated />

  <tr><td colspan="4">&nbsp;</td></tr>
  <tr><td colspan="4" class="formSectionHeaderTitle">${uiLabelMap.PartyContactInformation}</td></tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyPhoneNumber />
    <td colspan="3">
      <input name="primaryPhoneCountryCode" size=6 maxlength=6 class="inputBox" value="${Static["org.opentaps.common.util.UtilConfig"].getPropertyValue(opentapsApplicationName, "defaultCountryCode")}" /> -
      (<input name="primaryPhoneAreaCode" size=6 maxlength=10 class="inputBox" />)
      <input name="primaryPhoneNumber" size=10 maxlength=20 class="inputBox" /> ${uiLabelMap.PartyContactExt}
      <input name="primaryPhoneExtension" size=10 maxlength=20 class="inputBox" />
    </td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyFaxNumber />
    <td colspan="3">
      <input name="primaryFaxCountryCode" size=6 maxlength=6 class="inputBox" value="${Static["org.opentaps.common.util.UtilConfig"].getPropertyValue(opentapsApplicationName, "defaultCountryCode")}" /> -
      (<input name="primaryFaxAreaCode" size=6 maxlength=10 class="inputBox" />)
      <input name="primaryFaxNumber" size=10 maxlength=20 class="inputBox" /> ${uiLabelMap.PartyContactExt}
      <input name="primaryFaxExtension" size=10 maxlength=20 class="inputBox" />
    </td>
  </tr>  
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyEmailAddress />
    <@inputTextCell name="primaryEmail" />
    <@displayTitleCell title=uiLabelMap.OpentapsWebUrl />
    <@inputTextCell name="primaryWebUrl" />
  </tr>

  <tr><td colspan="4">&nbsp;</td></tr>
  <tr><td colspan="4" class="formSectionHeaderTitle">${uiLabelMap.OpentapsAddress}</td></tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyToName />
    <@inputTextCell name="generalToName" />
    <@displayTitleCell title=uiLabelMap.PartyAttentionName />
    <@inputTextCell name="generalAttnName" />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyAddressLine1 />
    <@inputTextCell name="generalAddress1" />
    <@displayTitleCell title=uiLabelMap.PartyAddressLine2 />
    <@inputTextCell name="generalAddress2" />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyCity />
    <@inputTextCell name="generalCity" />
    <@displayTitleCell title=uiLabelMap.PartyState />
    <td><@inputState name="generalStateProvinceGeoId" countryInputName="generalCountryGeoId" /></td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyZipCode />
    <@inputTextCell name="generalPostalCode" />
    <@displayTitleCell title=uiLabelMap.CommonCountry />
    <td><@inputCountry stateInputName="generalStateProvinceGeoId" name="generalCountryGeoId" /></td>
  </tr>

  <@inputHidden name="forceComplete" value="N"/>
  <#if requestAttributes.duplicateSuppliersWithName?exists>
  <tr> 
  <td >&nbsp;</td>
  
  <td colspan=2>
    <@inputSubmit title=uiLabelMap.PurchCreateSupplier />
    &nbsp;
    <@submitFormLinkConfirm form="createSupplierParty" text=uiLabelMap.PurchCreateSupplierIgnoreDuplicate forceComplete="Y"/>
  </td>
  </tr>
  <#else>
  <@inputSubmitRow title=uiLabelMap.PurchCreateSupplier />
  </#if>


</table>

</form>

