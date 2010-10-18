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

<@sectionHeader title=uiLabelMap.PageTitleLookupGlAccount />

<div class="subSectionBlock">
    <form method="get" target="" name="findGlAccountForm">
        <@inputHidden name="performFind" value="Y"/>
        <table class="twoColumnForm">
            <@inputTextRow name="accountCode" title=uiLabelMap.FinancialsGLAccountCode />
            <@inputTextRow name="accountName" title=uiLabelMap.FinancialsGLAccountName />
            <@inputSubmitRow title=uiLabelMap.CommonFind />
        </table>
    </form>
</div>

<@sectionHeader title="${uiLabelMap.AccountingGlAccounts}"/>
<div class="subSectionBlock">
<@paginate name="glAccountLookup" list=glAccountListBuilder rememberPage=false sectionName=sectionName>
    <#noparse>
    <@navigationBar/>
    <table class="listTable">
        <tr class="listTableHeader">
            <@headerCell title=uiLabelMap.FinancialsGLAccountCode orderBy="accountCode"/>
            <@headerCell title=uiLabelMap.FinancialsGLAccountName orderBy="accountName"/>
        <tr>
        <#list pageRows as glAccount>
        <tr class="class="${tableRowClass(glAccount_index)}">
            <#if parameters.sectionName?default("") == "lookup">
                <@displayLinkCell href="javascript:set_value('${glAccount.glAccountId}')" text=glAccount.accountCode blockClass="fieldWidth25pct"/>
            <#elseif opentapsApplicationName?default("") == "financials">
                <@displayLinkCell href="AccountActivitiesDetail?glAccountId=${glAccount.glAccountId}" text="${supplier.compositeName?if_exists} (${supplier.partyId})" blockClass="fieldWidth25pct"/>
            <#else>
                <@displayCell text=glAccount.glAccountId blockClass="fieldWidth25pct" />
            </#if>
            <@displayCell text=glAccount.accountName />
        </tr>
        </#list>
    </table>
    </#noparse>
</@paginate>
</div>
