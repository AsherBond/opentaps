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

<@frameSection title=uiLabelMap.FinancialsCreateAccountingTag>
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
</@frameSection>
