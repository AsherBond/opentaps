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
