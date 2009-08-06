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

<div class="subSectionBlock">
<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.PurchAddExistingBackupWarehouse}</div>
</div>
<form action="<@ofbizUrl>createFacilityAssoc</@ofbizUrl>" method="post" name="createFacilityAssoc">
  <@inputHidden name="facilityIdTo" value=facilityTo.facilityId />
  <@inputHidden name="facilityAssocTypeId" value="BACKUP_INVENTORY" />
  <table class="twoColumnForm">
    <@displayLinkRow title=uiLabelMap.PurchPrimaryWarehouse href="viewBackupWarehouses?facilityId=${facilityTo.facilityId}" text="${facilityTo.facilityName} (${facilityTo.facilityId})" />
    <tr>
      <@displayTitleCell title=uiLabelMap.PurchBackupWarehouse titleClass="requiredField" />
      <td>
          <select name="facilityId" class="selectBox">
              <#list warehouses as warehouse>
                  <#if !excludeWarehouses.contains(warehouse.facilityId)>
                  <option value="${warehouse.facilityId}">${warehouse.facilityName}</option>
                  </#if>
              </#list>
          </select>
      </td>
    </tr>
    <@inputTextRow name="sequenceNum" title=uiLabelMap.CommonPriority titleClass="requiredField" size=3 />
    <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFrom form="createFacilityAssoc" default=nowTimestamp titleClass="requiredField" />
    <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThru form="createFacilityAssoc" />
    <@inputSubmitRow title=uiLabelMap.OpentapsAddExisting />
  </table>
</form>
</div>

