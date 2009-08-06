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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

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

