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

<#assign mustBalanceSelectValues = {"Y": uiLabelMap.CommonYes, "N": uiLabelMap.CommonNo} />

<@frameSection title=uiLabelMap.FinancialsAccountingTagsPostingChecks>
  <form method="post" action="<@ofbizUrl>updateAccountingTagPostingCheck</@ofbizUrl>" name="updateAccountingTagPostingCheck">
    <@inputHidden name="organizationPartyId" value=organizationPartyId />
    <table class="listTable" style="border:0">
      <tr class="listTableHeader">
        <@displayCell text="" />
        <@displayCell text=uiLabelMap.FinancialsAccountingTagsMustBalance />
        <@displayCell text="" />
        <@displayCell text=uiLabelMap.FinancialsAccountingTagsMustBalance />
        <@displayCell text="" />
        <@displayCell text=uiLabelMap.FinancialsAccountingTagsMustBalance />
      </tr>
      <#-- layout 3 columns -->
      <#list 1..10 as i>
        <#if (i % 3) == 1>
          <tr class="${tableRowClass(i % 6)}">
        </#if>
        <#assign typeId = "enumTypeId" + i />
        <@displayTitleCell title=i />
        <@inputSelectHashCell name="tagEnum${i}MustBalance" default=(postingCheck.get("tagEnum${i}MustBalance"))!"N" hash=mustBalanceSelectValues />
        <#if (i % 3) == 0>
          </tr>
        </#if>
      </#list>
      <@inputSubmitRow title=uiLabelMap.CommonUpdate />
    </table>
  </form>
</@frameSection>
