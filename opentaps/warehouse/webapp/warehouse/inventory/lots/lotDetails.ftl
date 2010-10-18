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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if hasLotViewPermission>
  <@sectionHeader title=uiLabelMap.WarehouseLot>
    <#if !(isPopup?if_exists)>
      <div class="subMenuBar"><a href="<@ofbizUrl>editLotForm?lotId=${parameters.lotId}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonEdit}</a><a href="<@ofbizUrl>createLotForm</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonCreateNew}</a></div>
    </#if>
  </@sectionHeader>

    <table border="0" class="fourColumnForm">
      <tr>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductLotId}</span></td>
        <@displayCell text="${lot.lotId}" blockClass="formWidth300" />
        <td class="formWidth300">&nbsp;</td>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductCreatedDate}</span></td>
        <@displayDateCell date=lot.creationDate blockClass="formWidth300" />
      </tr>
      <tr>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductSupplier}</span></td>
        <#if supplierPartyName?exists>
          <@displayCell text="${supplierPartyName} (${lot.supplierPartyId})" blockClass="formWidth300" />
        <#else>
          <#-- This is to ensure that there are five columns in the row, regardless of -->
          <#-- whether the viewed lot has a supplier or not -->
          <@displayCell text="" blockClass="formWidth300" />
        </#if>
        <td class="formWidth300">&nbsp;</td>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductExpireDate}</span></td>
        <@displayDateCell date=lot.expirationDate blockClass="formWidth300" />
      </tr>
      <tr>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductQuantity}</span></td>
        <td class="formWidth300">
          <span class="tabletext" style="">${lot.quantity?if_exists}</span>
          <#assign uom = lot.getRelatedOne("Uom")?if_exists>
          <span class="tabletext" style="">${(uom.description)?if_exists}</span>
        </td>
      </tr>
      <tr>
        <td class="formWidth300"><span class="tableheadtext">${uiLabelMap.ProductComments}</span></td>
        <@displayCell text="${lot.comments?if_exists}" blockClass="formWidth300" style="white-space: pre" />
        <td class="formWidth300" colspan="3">&nbsp;</td>
      </tr>
      <#if isPopup?if_exists>
      <tr>
        <td class="formWidth300" colspan="5">&nbsp;</td>
      </tr>
      <tr>
        <td class="formWidth300">&nbsp;</td>
        <td><a class="buttontext" href="javascript:set_value('${parameters.lotId}')">${uiLabelMap.OpentapsDone}</a></td>
        <td class="formWidth300" colspan="3">&nbsp;</td>
      </tr>
      </#if>
    </table>
<#else>
  ${uiLabelMap.OpentapsError_PermissionDenied}
</#if>
