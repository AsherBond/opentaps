<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<form name="createProductionRun" method="post" action="<@ofbizUrl>createProductionRun</@ofbizUrl>">
  <table>
    <@inputHidden name="facilityId" value=parameters.facilityId />
    <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId titleClass="requiredField" />
    <@inputTextRow name="quantity" title=uiLabelMap.ManufacturingQuantity titleClass="requiredField" size=6 />
    <@inputDateTimeRow name="startDate" title=uiLabelMap.ManufacturingStartDate titleClass="requiredField" default=startDate />
    <@inputLookupRow name="routingId" title=uiLabelMap.ManufacturingRoutingId lookup="LookupRouting" form="createProductionRun"/>
    <@inputTextRow name="workEffortName" title=uiLabelMap.ManufacturingProductionRunName size=30 />
    <@inputTextRow name="description" title=uiLabelMap.CommonDescription size=50 />
    <@inputButtonRow title=uiLabelMap.CommonCreate />
  </table>
</form>
