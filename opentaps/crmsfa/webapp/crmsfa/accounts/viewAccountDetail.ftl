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
<#if hasDeactivatePermission?exists>
  <@form name="deactivateAccountAction" url="deactivateAccount" partyId=partySummary.partyId />
</#if>

<#assign extraOptions>
  <#if hasUpdatePermission?exists>
    <@displayLink href="updateAccountForm?partyId=${partySummary.partyId}" text="${uiLabelMap.CommonEdit}" class="subMenuButton"/>
  </#if>
  <#if hasDeactivatePermission?exists>
    <@submitFormLinkConfirm form="deactivateAccountAction" text=uiLabelMap.CrmDeactivateAccount class="subMenuButtonDangerous" />
  </#if>
</#assign>

<#assign title>
  ${uiLabelMap.CrmAccount}
  <#if accountDeactivated?exists><span class="subSectionWarning">${uiLabelMap.CrmAccountDeactivated} ${getLocalizedDate(accountDeactivatedDate, "DATE_TIME")}</span></#if>
</#assign>

<@frameSection title=title extra=extraOptions>

<table class="fourColumnForm" style="border:none">
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmAccountName titleClass="requiredField" />
    <@displayCell text="${partySummary.groupName}  (${partySummary.partyId})" />
    <@displayTitleCell title=uiLabelMap.CrmParentParty />
    <#if parentParty?has_content>
      <@displayLinkCell href="viewAccount?partyId=${parentParty.partyId}" text="${parentParty.groupName} (${parentParty.partyId})" blockClass="fieldWidth50pct"/>
    </#if> 
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmLocalName />
    <@displayCell text=partySummary.groupNameLocal />
    <@displayTitleCell title=uiLabelMap.CrmSiteName />
    <@displayCell text=partySummary.officeSiteName />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmAnnualRevenue />
    <td><#if partySummary.annualRevenue?exists>
      <#-- using ..Cell gives a right alignment to the content that we do not want here -->
      <@displayCurrency amount=partySummary.annualRevenue currencyUomId=partySummary.currencyUomId />
    </#if></td>
    <@displayTitleCell title=uiLabelMap.CrmPreferredCurrency />
    <@displayCell text=partySummary.currencyUomId />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmIndustry />
    <@displayEnumerationCell enumId=partySummary.industryEnumId />
    <@displayTitleCell title=uiLabelMap.CrmNumberOfEmployees />
    <@displayCell text=partySummary.numberEmployees />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmOwnership />
    <@displayEnumerationCell enumId=partySummary.ownershipEnumId />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmSICCode />
    <@displayCell text=partySummary.sicCode />
    <@displayTitleCell title=uiLabelMap.CrmTickerSymbol />
    <@displayCell text=partySummary.tickerSymbol />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CommonDescription />
    <td colspan="3" class="tabletext">${partySummary.description?if_exists}</td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmImportantNote />
    <td colspan="3" class="tabletext">${partySummary.importantNote?if_exists}</td>
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.PartyClassifications />
    <@displayCell text=partyClassificationGroupsDisplay />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmDataSources />
    <@displayCell text=dataSourcesAsString />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmMarketingCampaigns />
    <@displayCell text=marketingCampaignsAsString />
  </tr>
  <tr>
    <@displayTitleCell title=uiLabelMap.CrmPersonResponsibleFor titleClass="requiredField" />
    <#assign responsibleForName><#if responsibleParty?exists>${responsibleParty.firstName?if_exists} ${responsibleParty.lastName?if_exists}<#else>${uiLabelMap.CommonNone}</#if></#assign>
    <td>
      <@display text=responsibleForName />
      <#if hasReassignPermission?default(false)>
        <@flexAreaHeader targetId="ReassignArea" title=uiLabelMap.CrmReassign controlClassOpen="flexAreaControlSimple_open" controlClassClosed="flexAreaControlSimple_closed" />
      </#if>
    </td>
  </tr>
  <#if hasReassignPermission?default(false)>
  <tr>
    <td/>
    <td colspan="2">
      <@flexAreaBody targetId="ReassignArea" style="border:0;margin:0;padding-left:0">
        <form method="post" action="<@ofbizUrl>reassignAccountResponsibleParty?partyId=${partySummary.partyId}</@ofbizUrl>" name="reassign">
          <@inputHidden name="accountPartyId" value="${partySummary.partyId}" />
          <@inputLookup name="newPartyId" lookup="LookupTeamMembers" form="reassign" />
          <@inputSubmit title=uiLabelMap.CrmReassign />
        </form>
      </@flexAreaBody>
    </td>
  </tr>
  </#if>

  <tr>
    <@displayTitleCell title=uiLabelMap.CrmSalesPerYear />
    <td colspan="3" class="tabletext">
      <table>
         <tr>
           <#list salesYear?default([]) as year>
           <td>${uiLabelMap.CrmSales} ${year}</td>
           </#list>
         </tr>
         <tr>
           <#list salesList?default([]) as sales>
           <td><@ofbizCurrency amount=sales.grandTotal?default(0) isoCode=partySummary.currencyUomId/></td>
           </#list>
         </tr>
       </table>
   </td>
  </tr>
  <tr>
   <#assign paymentsReceivedTitle = uiLabelMap.CrmPaymentsReceivedYear + " " + paymentReceivedYear?default("") />
   <@displayTitleCell title=paymentsReceivedTitle />
   <td><@displayCurrency amount=paymentReceived?default({}).amount?default(0) currencyUomId=partySummary.currencyUomId /></td>
  </tr>

</table>

</@frameSection>
