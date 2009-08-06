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

<form name="RunBomSimulation" action="<@ofbizUrl>runBomSimulation</@ofbizUrl>" method="post">
  <table class="twoColumnForm">
    <@inputHidden name="lookupFlag" value="Y"/>
    <@inputAutoCompleteProductRow title=uiLabelMap.ProductProductId name="productId" id="bom_productId" form="RunBomSimulation" />
    <@inputSelectRow title=uiLabelMap.ManufacturingBomType name="bomType" key="productAssocTypeId" list=assocTypes default="MANUF_COMPONENT" ; assocType>
      ${(assocType.get("description", locale))!}
    </@inputSelectRow>
    <@inputDateRow title=uiLabelMap.CommonFromDate name="fromDate" />
    <@inputLookupRow title=uiLabelMap.ManufacturingRoutingId name="routingId" form="RunBomSimulation" lookup="LookupRouting" />
    <@inputTextRow title=uiLabelMap.ManufacturingQuantity name="quantity" default="1" />
    <@inputTextRow title=uiLabelMap.CommonAmount name="amount" default="0" />
    <@inputSelectHashRow title=uiLabelMap.CommonType name="type" hash={"0":uiLabelMap.ManufacturingExplosion, "1":uiLabelMap.ManufacturingExplosionSingleLevel, "2":uiLabelMap.ManufacturingExplosionManufacturing, "3":uiLabelMap.ManufacturingImplosion} />
    <@inputSelectRow title=uiLabelMap.ProductFacilityId name="facilityId" list=facilities required=false ; facility>
      ${(facility.facilityName)!} [${facility.facilityId}]
    </@inputSelectRow>
    <@inputCurrencySelectRow title=uiLabelMap.ProductCurrencyUomId useDescription=true />
    <@inputSubmitRow title=uiLabelMap.CommonSubmit />
  </table>
</form>
