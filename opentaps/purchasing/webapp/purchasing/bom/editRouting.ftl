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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if routing?has_content>
  <@sectionHeader title=uiLabelMap.ManufacturingEditRouting />
  <form name="EditRouting" action="<@ofbizUrl>UpdateRouting</@ofbizUrl>" method="post">
    <@inputHidden name="workEffortId" value=routing.workEffortId />
    <table class="twoColumnForm">
      <@inputTextRow title=uiLabelMap.ManufacturingRoutingName name="workEffortName" default=routing.workEffortName! />
      <@inputTextRow title=uiLabelMap.CommonDescription name="description" default=routing.description! />
      <@inputTextRow title=uiLabelMap.FormFieldTitle_quantityToProduce name="quantityToProduce" default=routing.quantityToProduce! />
      <@inputSubmitRow title=uiLabelMap.CommonSave />
    </table>
  </form>
<#else>
  <@sectionHeader title=uiLabelMap.ManufacturingNewRouting />
  <form name="EditRouting" action="<@ofbizUrl>CreateRouting</@ofbizUrl>" method="post">
    <@inputHidden name="workEffortTypeId" value="ROUTING"/>
    <@inputHidden name="currentStatusId"  value="ROU_ACTIVE"/>
    <table class="twoColumnForm">
      <@inputTextRow title=uiLabelMap.ManufacturingRoutingName name="workEffortName" />
      <@inputTextRow title=uiLabelMap.CommonDescription name="description" />
      <@inputTextRow title=uiLabelMap.FormFieldTitle_quantityToProduce name="quantityToProduce" />
      <@inputSubmitRow title=uiLabelMap.CommonCreate />
    </table>
  </form>
</#if>

