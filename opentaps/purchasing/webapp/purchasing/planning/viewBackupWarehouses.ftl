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
<div class="form">
<form method="get" action="<@ofbizUrl>viewBackupWarehouses</@ofbizUrl>">
  <span class="tableheadtext">${uiLabelMap.PurchPrimaryWarehouse}:&nbsp;</span>
  <@inputSelect list=warehouses name="facilityId" key="facilityId" displayField="facilityName" required=true />
  <@inputSubmit title=uiLabelMap.CommonSelect />
</form>
</div>
</div>

<#if backups?exists>

<div class="subSectionBlock">
<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.PurchBackupWarehouses}</div>
    <div class="subMenuBar"><a class="subMenuButton" href="<@ofbizUrl>addBackupWarehouseForm?facilityIdTo=${parameters.facilityId}</@ofbizUrl>">${uiLabelMap.OpentapsAddExisting}</a><a class="subMenuButton" href="<@ofbizUrl>createBackupWarehouseForm?facilityIdTo=${parameters.facilityId}</@ofbizUrl>">${uiLabelMap.OpentapsAddNew}</a></div>
</div>
<table class="listTable">
    <tr class="listTableHeader">
        <td>${uiLabelMap.OpentapsWarehouse}</td>
        <td>${uiLabelMap.CommonFrom}</td>
        <td>${uiLabelMap.CommonThru}</td>
        <td>${uiLabelMap.CommonPriority}</td>
        <td></td>
    </tr>
    <#list backups as backup>
        <tr class="${tableRowClass(backup_index)}">
          <form action="<@ofbizUrl>updateFacilityAssoc</@ofbizUrl>" method="post" name="backupWarehouse_${backup.facilityId}">
            <@inputHidden name="facilityId" value=backup.facilityId />
            <@inputHidden name="facilityIdTo" value=backup.facilityIdTo />
            <@inputHidden name="fromDate" value=getLocalizedDate(backup.fromDate) />
            <@inputHidden name="facilityAssocTypeId" value="BACKUP_INVENTORY" />
            <@displayCell text=backup.facilityName />
            <@displayDateCell date=backup.fromDate />
            <@inputDateTimeCell name="thruDate" default=backup.thruDate form="backupWarehouse_${backup.facilityId}" />
            <@inputTextCell name="sequenceNum" default=backup.sequenceNum size=3 />
          </form>
          <form action="<@ofbizUrl>updateFacilityAssoc</@ofbizUrl>" method="post" name="removeBackupWarehouse_${backup.facilityId}">
            <@inputHidden name="facilityId" value=backup.facilityId />
            <@inputHidden name="facilityIdTo" value=backup.facilityIdTo />
            <@inputHidden name="fromDate" value=getLocalizedDate(backup.fromDate) />
            <@inputHidden name="thruDate" value=getLocalizedDate(nowTimestamp) />
            <@inputHidden name="facilityAssocTypeId" value="BACKUP_INVENTORY" />
          </form>
          <td align="right">
            <@displayLink class="buttontext" href="javascript:document.backupWarehouse_${backup.facilityId}.submit()" text=uiLabelMap.CommonUpdate/>
            <@inputConfirm title=uiLabelMap.CommonRemove form="removeBackupWarehouse_${backup.facilityId}" />
          </td>
        </tr>
    </#list>
</table>
</div>

</#if>
