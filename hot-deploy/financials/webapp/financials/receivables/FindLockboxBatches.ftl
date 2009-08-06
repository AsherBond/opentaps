<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
