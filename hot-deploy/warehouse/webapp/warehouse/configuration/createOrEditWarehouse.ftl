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

<#if facility.facilityId?exists>
  <#assign title = uiLabelMap.WarehouseViewWarehouse />
  <#assign target = "updateWarehouse" />
  <#assign submitName = uiLabelMap.CommonUpdate />
<#else>
  <#assign title = uiLabelMap.WarehouseCreateNewWarehouse />
  <#assign target = "createWarehouse" />
  <#assign submitName = uiLabelMap.CommonCreate />
</#if>

<div class="sectionHeader">${title}</div>

<form name="createOrEditWarehouse" method="post" action="<@ofbizUrl>${target}</@ofbizUrl>">
  <#if facility.facilityId?exists>
  <input type="hidden" name="facilityId" value="${facility.facilityId}"/>
  </#if>

<div class="form">

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.CommonName}</span>
    <span class="formInputSpan">
      <input type="text" name="facilityName" class="inputBox" value="${facility.facilityName?default("")}" size="40"/>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.ProductFacilityType}</span>
    <span class="formInputSpan">
        <select name="facilityTypeId" class="inputBox">
          <#list facilityTypes as type>
            <#if facility.facilityTypeId?default("") == type.facilityTypeId><#assign selected = "selected"><#else><#assign selected = ""></#if>
            <option ${selected} value="${type.facilityTypeId}">${type.description}</option>
          </#list>
        </select>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabel">${uiLabelMap.ProductSquareFootage}</span>
    <span class="formInputSpan">
      <input type="text" name="squareFootage" class="inputBox" size="6" value="${facility.squareFootage?default("")}"/>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.ProductFacilityOwner}</span>
    <span class="formInputSpan">
        <input type="text" class="inputBox" name="ownerPartyId" value="${facility.ownerPartyId?if_exists}" size="10"/>
        <a href="javascript:call_fieldlookup2(document.createOrEditWarehouse.ownerPartyId,'LookupPartyName');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"></a>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.ProductInventoryItemType}</span>
    <span class="formInputSpan">
        <select name="defaultInventoryItemTypeId" class="inputBox">
          <#list inventoryItemTypes as type>
            <#if facility.defaultInventoryItemTypeId?default("") == type.inventoryItemTypeId><#assign selected = "selected"><#else><#assign selected = ""></#if>
            <option ${selected} value="${type.inventoryItemTypeId}">${type.description}</option>
          </#list>
        </select>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabel">${uiLabelMap.ProductFacilityDefaultWeightUnit}</span>
    <span class="formInputSpan">
        <select name="defaultWeightUomId" class="inputBox">
          <option value="">None</option>
          <#list weightUomList as weight>
            <#if facility.defaultWeightUomId?default("") == weight.uomId><#assign selected = "selected"><#else><#assign selected = ""></#if>
            <option ${selected} value="${weight.uomId}">${weight.description}</option>
          </#list>
        </select>
    </span>
  </div>


  <div class="formRow">
    <span class="formLabel">${uiLabelMap.WarehouseDefaultDaysToShip}</span>
    <span class="formInputSpan">
      <input type="text" name="defaultDaysToShip" class="inputBox" size="6" value="${facility.defaultDaysToShip?default("")}"/>
    </span>
  </div>

  <div class="formRow">
    <span class="formInputSpan">
      <input type="submit" class="smallSubmit" value="${submitName}"></input>
    </span>
  </div>

  <div class="spacer">&nbsp;</div>
</div>

</form>
