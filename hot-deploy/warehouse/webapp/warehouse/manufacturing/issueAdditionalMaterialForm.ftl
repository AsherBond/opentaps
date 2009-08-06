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

<form action="<@ofbizUrl>issueAdditionalMaterialCheck</@ofbizUrl>" method="post" name="issueAdditionalMaterial">

<table class="twoColumnForm">
    <@inputHidden name="productionRunId" value=parameters.productionRunId />
    <@inputSelectRow name="workEffortId" title=uiLabelMap.WorkEffortTask list=tasks key="workEffortId" displayField="workEffortName" titleClass="requiredField" />
    <@inputLookupRow name="productId" title=uiLabelMap.ProductProduct lookup="LookupProduct" form="issueAdditionalMaterial" titleClass="requiredField" />
    <@inputTextRow name="quantity" title=uiLabelMap.CommonQuantity size=6 titleClass="requiredField" />
    <@inputSelectRow name="reasonEnumId" title=uiLabelMap.OrderReason list=reasons key="enumId" displayField="description" required=false />
    <@inputTextareaRow name="description" title=uiLabelMap.CommonDescription />
    <tr>
        <td></td>
        <td><@inputForceComplete title=uiLabelMap.WarehouseIssueAdditionalMaterial forceTitle=uiLabelMap.WarehouseForceIssueAdditionalMaterial form="issueAdditionalMaterial" /></td>
    </tr>
</table>

</form>
