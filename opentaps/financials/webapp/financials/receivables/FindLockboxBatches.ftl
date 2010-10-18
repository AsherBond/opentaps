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

<#-- Find parameters form -->

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.FinancialsLockboxFind/>

  <form method="post" action="findLockboxBatches" name="FindLockboxForm">
    <table class="twoColumnForm">
        <@inputTextRow title=uiLabelMap.FinancialsLockboxBatchNumber name="lockboxBatchId" size="20"/>
        <@inputDateRow title=uiLabelMap.CommonDate name="datetimeEntered"/>
        <@inputTextRow title=uiLabelMap.FinancialsLockboxOriginalAmount name="batchAmount" size="15"/>
        <@inputTextRow title=uiLabelMap.FinancialsLockboxPendingAmount name="outstandingAmount" size="15"/>
        <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>

</div>
