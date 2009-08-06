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

<@sectionHeader title=uiLabelMap.FinancialsCreateAccountingTag />
<div class="screenlet-body">
  <form method="post" action="<@ofbizUrl>createAccountingTag</@ofbizUrl>" name="createAccountingTag">
    <table class="twoColumnForm" style="border:0">
      <@inputSelectRow title=uiLabelMap.CommonType name="enumTypeId" list=tagTypes key="enumTypeId" ; tag>
        ${tag.description}
      </@inputSelectRow>
      <@inputTextRow title=uiLabelMap.CommonSequenceNum name="sequenceId" size=3 maxlength=3 />
      <@inputTextRow title=uiLabelMap.CommonId name="enumId" />
      <@inputTextRow title=uiLabelMap.CommonName name="enumCode" titleClass="requiredField" />
      <@inputTextRow title=uiLabelMap.CommonDescription name="description" size=60  titleClass="requiredField"/>
      <@inputSubmitRow title=uiLabelMap.CommonCreate />
    </table>
  </form>
</div>
