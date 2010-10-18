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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if partySummary?exists>
  <#assign isCreate = false />
<#else>
  <#assign isCreate = true />
  <#assign partySummary = {} />
</#if>

<div class="subSectionBlock">

<#if isCreate>
  <form name="createAccountForm" method="post" action="<@ofbizUrl>createAccount</@ofbizUrl>" id="createAccountForm">
    <@inputHidden name="created" value="true" />
<#else>
  <form name="createAccountForm" method="post" action="<@ofbizUrl>updateAccount</@ofbizUrl>" id="createAccountForm">
    <@inputHidden name="partyId" value=partySummary.partyId />
</#if>

<table class="fourColumnForm">

  <tr>
    <@displayTitleCell title=uiLabelMap.CrmAccountName titleClass="requiredField" />
    <@inputTextCell name="accountName" maxlength=60 default=partySummary.groupName />
    <@displayTitleCell title=uiLabelMap.CrmParentParty />
    <@inputLookupCell name="parentPartyId" form="createAccountForm" lookup="LookupAccounts" default=partySummary.parentPartyId />
  </tr>
  <#if duplicateAccountsWithName?has_content>
    <tr>
      <td/>
      <td colspan="3"><span class="errorMessage">${uiLabelMap.CrmAccountsWithDuplicateName}:</span> <#list duplicateAccountsWithName as dup><@displayPartyLink partyId=dup.partyId /><#if dup_has_next>, </#if></#list></td>
    </tr>
  </#if>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmLocalName />
    <@inputTextCell name="groupNameLocal" maxlength=100 default=partySummary.groupNameLocal />
    <@displayTitleCell title=uiLabelMap.CrmSiteName />
    <@inputTextCell name="officeSiteName" maxlength=100 default=partySummary.officeSiteName />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmAnnualRevenue />
    <@inputTextCell name="annualRevenue" size=15 maxlength=15 default=partySummary.annualRevenue />
    <@displayTitleCell title=uiLabelMap.CrmPreferredCurrency />
    <@inputCurrencySelectCell defaultCurrencyUomId=partySummary.currencyUomId useDescription=true />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmIndustry />
    <@inputEnumerationCell name="industryEnumId" enumTypeId="PARTY_INDUSTRY" default=partySummary.industryEnumId />
    <@displayTitleCell title=uiLabelMap.CrmNumberOfEmployees />
    <@inputTextCell name="numberEmployees" default=partySummary.numberEmployees />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmOwnership />
    <@inputEnumerationCell name="ownershipEnumId" enumTypeId="PARTY_OWNERSHIP" default=partySummary.ownershipEnumId />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmSICCode />
    <@inputTextCell name="sicCode" default=partySummary.sicCode />
    <@displayTitleCell title=uiLabelMap.CrmTickerSymbol />
    <@inputTextCell name="tickerSymbol" default=partySummary.tickerSymbol />
  </tr>

  <#if isCreate>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmDataSource />
    <@inputSelectCell  name="dataSourceId" list=sourcesList displayField="description" required=false />
    <@displayTitleCell title=uiLabelMap.CrmMarketingCampaign />
    <@inputSelectCell  name="marketingCampaignId" list=marketingCampaignsList displayField="campaignName" required=false />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmInitialTeam />
    <@inputSelectCell  name="initialTeamPartyId" key="partyId" list=teamsList displayField="groupName" required=false />
  </tr>
  </#if>

  <tr>
    <@displayTitleCell title=uiLabelMap.CommonDescription />
    <@inputTextareaCell name="description" default=partySummary.description />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmImportantNote />
    <@inputTextareaCell name="importantNote" default=partySummary.importantNote />
  </tr>

  <#if isCreate>
  <tr><td colspan="4" class="boxhead">${uiLabelMap.PartyContactInformation}</td></tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyCountryCode />
    <@inputTextCell name="primaryPhoneCountryCode" size=6 maxlength=6 default=configProperties.defaultCountryCode />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyAreaCode />
    <@inputTextCell name="primaryPhoneAreaCode" size=6 maxlength=10 />
    <@displayTitleCell title=uiLabelMap.PartyPhoneNumber />
    <@inputTextCell name="primaryPhoneNumber" size=15 maxlength=15 />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyExtension />
    <@inputTextCell name="primaryPhoneExtension" size=10 maxlength=10 />
    <@displayTitleCell title=uiLabelMap.CrmPhoneAskForName />
    <@inputTextCell name="primaryPhoneAskForName" />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyEmailAddress />
    <@inputTextCell name="primaryEmail" />
    <@displayTitleCell title=uiLabelMap.CrmWebUrl />
    <@inputTextCell name="primaryWebUrl" />
  </tr>
  <tr><td colspan="4" class="boxhead">${uiLabelMap.CrmPrimaryAddress}</td></tr>
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
    <@displayTitleCell title=uiLabelMap.CommonCountry />
    <td><@inputCountry name="generalCountryGeoId" stateInputName="generalStateProvinceGeoId" /></td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyZipCode />
    <@inputTextCell name="generalPostalCode" />
    <@displayTitleCell title=uiLabelMap.PartyState />
    <td><@inputState name="generalStateProvinceGeoId" countryInputName="generalCountryGeoId" /></td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmPostalCodeExt />
    <@inputTextCell name="generalPostalCodeExt" size=5 />
  </tr>
  </#if>

  <@inputHidden name="forceComplete" value="N"/>
  <#if requestAttributes.duplicateAccountsWithName?exists>
  <tr> 
  <td >&nbsp;</td>
  
  <td>
    <@inputSubmit title=uiLabelMap.CrmCreateAccount />
    &nbsp;
    <@submitFormLinkConfirm form="createAccountForm" text=uiLabelMap.CrmCreateAccountIgnoreDuplicate forceComplete="Y"/>
  </td>
  </tr>
  <#else>
  <#if isCreate>
    <@inputSubmitRow title=uiLabelMap.CrmCreateAccount />
  <#else>
    <@inputSubmitRow title=uiLabelMap.CommonSave />
  </#if>
  </#if>
</table>
</form>
</div>

<#if isCreate && validatePostalAddresses>
  <div id="postalAddressValidationContainer" class="tabletext">
      <@flexArea targetId="postalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false><div>lskdjf</div></@flexArea>
  </div>
  <script type="text/javascript">
      opentaps.addListenerToNode(document.forms['createAccountForm'].generalPostalCode, 'onchange', function(){opentaps.postalAddress.validationListener('postalAddressValidationError', 'generalAddress1', 'generalAddress2', 'generalCity', 'generalStateProvinceGeoId', 'generalCountryGeoId', 'generalPostalCode', 'generalPostalCodeExt', document.forms['createAccountForm'])})
      document.forms['createAccountForm'].generalPostalCodeExt.parentNode.appendChild(document.getElementById('postalAddressValidationContainer'));
      document.forms['createAccountForm'].generalPostalCodeExt.parentNode.parentNode.childNodes[1].vAlign = 'top';
  </script>
</#if>
