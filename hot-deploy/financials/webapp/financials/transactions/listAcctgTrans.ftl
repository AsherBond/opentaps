<#--
 * Copyright (c) 2009 Open Source Strategies, Inc.
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

<#-- Parametrized find form for transactions. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>


<@paginate name="listAcctgTrans" list=acctgTransListBuilder rememberPage=false>
    <#noparse>
        <@navigationHeader/>
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.FinancialsTransactionId orderBy="acctgTransId"/>
                <@headerCell title=uiLabelMap.FinancialsTransactionType orderBy="acctgTransTypeId"/>
                <@headerCell title=uiLabelMap.FinancialsIsPosted orderBy="isPosted"/>
                <@headerCell title=uiLabelMap.PartyParty orderBy="partyId"/>
                <@headerCell title=uiLabelMap.FinancialsTransactionDate orderBy="transactionDate DESC"/>
                <@headerCell title=uiLabelMap.FinancialsScheduledPostingDate orderBy="scheduledPostingDate DESC"/>
                <@headerCell title=uiLabelMap.FinancialsPostedDate orderBy="postedDate DESC"/>
                <@headerCell title=uiLabelMap.FinancialsPostedAmount orderBy="postedAmount"/>
            </tr>
            <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
                <@displayLinkCell text=row.acctgTransId href="viewAcctgTrans?acctgTransId=${row.acctgTransId}"/>
                <@displayCell text=row.acctgTransTypeDescription/>
                <@displayCell text=row.isPosted/>
                <@displayCell text=row.partyNameAndId/>
                <@displayDateCell date=row.transactionDate/>
                <@displayDateCell date=row.scheduledPostingDate/>
                <@displayDateCell date=row.postedDate/>
                <@displayCell text=row.postedAmount />
            </tr>
            </#list>
        </table>
    </#noparse>
</@paginate>
