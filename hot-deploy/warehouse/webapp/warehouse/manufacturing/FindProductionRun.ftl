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

<form name="findProductionRun" method="post" action="<@ofbizUrl>FindProductionRun</@ofbizUrl>">
  <table>
    <@inputHidden name="facilityId" value=parameters.facilityId />
    <@inputHidden name="workEffortPurposeTypeId" value=workEffortPurposeTypeId />
    <@inputTextRow name="workEffortId" title=uiLabelMap.ManufacturingProductionRunId />
    <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId />
    <@inputSelectHashRow name="type" title=uiLabelMap.CommonType hash={"any":uiLabelMap.CommonAny, "assembly":uiLabelMap.WarehouseAssembly, "disassembly":uiLabelMap.WarehouseDisassembly} />
    <@inputSelectRow name="currentStatusId" title=uiLabelMap.CommonStatus list=statuses key="statusId" displayField="description" required=false />
    <@inputHidden name="performFind" value="Y" />
    <@inputButtonRow title=uiLabelMap.CommonFind />
  </table>
</form>
