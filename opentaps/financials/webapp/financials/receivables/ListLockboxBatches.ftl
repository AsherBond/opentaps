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

<#-- Paginated list of lockbox batches found -->

<div class="subSectionBlock">
  <@paginate name="listLockBoxBatches" list=lockboxListBuilder rememberPage=false>
    <#noparse>
    <@navigationHeader/>
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.FinancialsLockboxBatchNumber orderBy="batchId"/>
                <@headerCell title=uiLabelMap.CommonDate orderBy="datetimeEntered"/>
                <@headerCell title=uiLabelMap.FinancialsLockboxOriginalAmount orderBy="batchAmount" blockClass="titleCell"/>
                <@headerCell title=uiLabelMap.FinancialsLockboxPendingAmount orderBy="outstandingAmount" blockClass="titleCell"/>
            </tr>
            <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
                <@displayLinkCell text=row.batchId href="viewLockboxBatch?lockboxBatchId=${row.lockboxBatchId}"/>
                <@displayDateCell date=row.datetimeEntered/>
                <@displayCurrencyCell amount=row.batchAmount/>
                <@displayCurrencyCell amount=row.outstandingAmount/>
            </tr>
            </#list>
        </table>
    </#noparse>
</@paginate>
</div>
