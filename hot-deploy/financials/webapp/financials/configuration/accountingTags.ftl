<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<#assign disableSelectValues = {"Y": uiLabelMap.CommonDisabled, "N": uiLabelMap.CommonEnabled} />

<@sectionHeader title=uiLabelMap.FinancialsAccountingTags />
<div class="screenlet-body">
  <#list tagsByType.keySet() as type>
    <div class="screenlet">
      <div class="screenlet-header"><span class="boxhead">${type.description}</span></div>
      <@form name="deleteAccountingTagForm" url="deleteAccountingTag" enumId="" />
      <form method="post" action="<@ofbizUrl>updateAccountingTag</@ofbizUrl>" name="updateAccountingTag">
        <@inputHiddenUseRowSubmit />
        <@inputHiddenRowCount list=tagsByType.get(type) />
        <table class="listTable" style="border:0">
          <tr class="listTableHeader">
            <@displayCell text=uiLabelMap.CommonSequenceNum />
            <@displayCell text=uiLabelMap.CommonId />
            <@displayCell text=uiLabelMap.CommonName />
            <@displayCell text=uiLabelMap.CommonDescription />
            <@displayCell text=uiLabelMap.CommonEnabled />
            <td/>
            <td/>
          </tr>
          <#list tagsByType.get(type) as tag>
            <tr class="${tableRowClass(tag_index)}">
              <@inputHidden name="enumId" value=tag.enumId index=tag_index />
              <@inputTextCell name="sequenceId" default=tag.sequenceId! size=3 maxlength=3 index=tag_index />
              <@displayCell text=tag.enumId />
              <@inputTextCell name="enumCode" default=tag.enumCode! maxlength=30 index=tag_index />
              <@inputTextCell name="description" default=tag.description! maxlength=30 index=tag_index />
              <@inputSelectHashCell name="disabled" default=tag.disabled!"N" index=tag_index hash=disableSelectValues />
              <@inputHiddenRowSubmit submit=false index=tag_index/>
              <@inputSubmitIndexedCell title="${uiLabelMap.CommonUpdate}" index=tag_index/>
              <td>
              <@submitFormLink form="deleteAccountingTagForm" text=uiLabelMap.CommonRemove class="smallSubmit" enumId=tag.enumId/>
              </td>
            </tr>
          </#list>
        </table>
      </form>
    </div>
  </#list>
</div>
