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

<div class="subSection">

  <div class="subSectionBlock">
    <div class="subSectionHeader">
      <div class="subSectionTitle">${uiLabelMap.WarehouseChooseWarehouse}</div>
    </div>
    
    <div class="form">
    <#if facilities.size() != 0>
      <form method="post" action="<@ofbizUrl>setFacility</@ofbizUrl>">
        <select name="facilityId" class="selectBox">
          <#list facilities as facility>
          <option value="${facility.facilityId}">${facility.facilityName}</option>
          </#list>
        </select>
        <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSelect}"/>
      </form>
    </#if>
  
    <#if hasCreateWarehousePermission>
    <p><a href="<@ofbizUrl>createWarehouseForm</@ofbizUrl>" class="tabletext">${uiLabelMap.WarehouseCreateNewWarehouse}</a></p>
    </#if>
   </div>
    
  </div>
  
</div>
