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

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.FinancialsPendingLockboxBatches>
    <div class="subMenuBar">
      <@displayLink href="findLockboxBatches" text=uiLabelMap.FinancialsLockboxFind class="subMenuButton"/>
    </div>
  </@sectionHeader>
  <#if batches?has_content>
    <div>
      <table class="listTable">
        <tr class="listTableHeader">
          <@displayCell text=uiLabelMap.FinancialsLockboxBatchNumber />
          <@displayCell text=uiLabelMap.CommonDate />
          <@displayCell text=uiLabelMap.FinancialsLockboxOriginalAmount blockClass="titleCell" />
          <@displayCell text=uiLabelMap.FinancialsLockboxPendingAmount blockClass="titleCell"  />
        </tr>
        <#list batches as batch>
          <tr class="${tableRowClass(batch_index)}">
            <@displayLinkCell text=batch.batchId href="viewLockboxBatch?lockboxBatchId=${batch.lockboxBatchId}" />
            <@displayDateCell date=batch.datetimeEntered />
            <@displayCurrencyCell amount=batch.batchAmount />
            <@displayCurrencyCell amount=batch.outstandingAmount />
          </tr>
        </#list>
      </table>
    </div>
  <#else>
    <div class="form">
      <div class="tabletext">${uiLabelMap.FinancialsNoPendingLockboxBatch}</div>
    </div>
  </#if>
</div>

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.FinancialsUploadLockboxFile/>
  <div class="form">
    <form method="post" action="<@ofbizUrl>uploadLockboxFile</@ofbizUrl>" name="uploadLockboxFile" enctype="multipart/form-data">
      <@inputHidden name="organizationPartyId" value=organizationPartyId />
      <table>
        <@inputFileRow title=uiLabelMap.FinancialsLockboxFile name="uploadedFile" size=70/>
        <@inputSubmitRow title=uiLabelMap.CrmUploadFile/>
      </table>
    </form>
  </div>
</div>
