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

