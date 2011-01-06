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

<form action="<@ofbizUrl>issueAdditionalMaterialCheck</@ofbizUrl>" method="post" name="issueAdditionalMaterial">

<table class="twoColumnForm">
    <@inputHidden name="productionRunId" value=parameters.productionRunId />
    <@inputSelectRow name="workEffortId" title=uiLabelMap.WorkEffortTask list=tasks key="workEffortId" displayField="workEffortName" titleClass="requiredField" />
    <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProduct form="issueAdditionalMaterial" titleClass="requiredField" />
    <@inputTextRow name="quantity" title=uiLabelMap.CommonQuantity size=6 titleClass="requiredField" />
    <@inputSelectRow name="reasonEnumId" title=uiLabelMap.OrderReason list=reasons key="enumId" displayField="description" required=false />
    <@inputTextareaRow name="description" title=uiLabelMap.CommonDescription />
    <tr>
        <td></td>
        <td><@inputForceComplete title=uiLabelMap.WarehouseIssueAdditionalMaterial forceTitle=uiLabelMap.WarehouseForceIssueAdditionalMaterial form="issueAdditionalMaterial" /></td>
    </tr>
</table>

</form>
