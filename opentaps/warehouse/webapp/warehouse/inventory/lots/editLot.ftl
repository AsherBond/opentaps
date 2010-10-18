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

<#assign isEditing = lot?exists>

<#if hasLotModifyPermission>
  <#if isEditing>
    <#if isPopup?if_exists>
      <#assign modifyLotUrl = "updateLotPopup" />
    <#else>
      <#assign modifyLotUrl = "updateLot" />
    </#if>
    <#assign submitButtonLabel = uiLabelMap.CommonUpdate />
  <#else>
    <#if isPopup?if_exists>
      <#assign modifyLotUrl = "createLotPopup" />
    <#else>
      <#assign modifyLotUrl = "createLot" />
    </#if>
    <#assign submitButtonLabel = uiLabelMap.CommonCreate />
  </#if>

  <#if isPopup?if_exists>
    <@sectionHeader title=uiLabelMap.WarehouseCreateNewLot />
  </#if>

  <div class="form">
    <form method="post" name="modifyLotForm" action="<@ofbizUrl>${modifyLotUrl}</@ofbizUrl>">
      <table border="0">
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.ProductLotId}</span></td>
          <#if isEditing>
            <@displayCell text="${lot.lotId}" />
            <@inputHidden name="lotId" value="${lot.lotId}" />
          <#else>
            <@inputTextCell name="lotId" />
          </#if>
          <#-- This is because if a supplier party lookup is made, while this form is -->
          <#-- itself being displayed as a popup page, then the party lookup will use -->
          <#-- the same popup window, and then when the user selects a supplier,      -->
          <#-- then that same popup window will get closed, before the user has the   -->
          <#-- chance to complete the lot creation process                            -->
          <#if isPopup?if_exists>
            <@inputTextRow name="supplierPartyId" title=uiLabelMap.ProductSupplier default="${(lot.supplierPartyId)?if_exists}" />

          <#else>
            <@inputLookupRow name="supplierPartyId" title=uiLabelMap.ProductSupplier default="${(lot.supplierPartyId)?if_exists}" form="modifyLotForm" lookup="LookupPartyName" />
          </#if>
        </tr>
        <#if isEditing>
          <@displayDateRow date=lot.creationDate title=uiLabelMap.ProductCreatedDate />
        </#if>
        <@inputDateTimeRow name="expirationDate" title=uiLabelMap.ProductExpireDate form="modifyLotForm" default="${defaultExpirationDate}" />
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.ProductQuantity}</span></td>
          <td>
            <input name="quantity" type="text" size="30" maxlength="" class="inputBox" value="${(lot.quantity)?if_exists}"/>
            <@inputSelect name="uomId" list=uoms displayField="description" default="${defaultUomId?if_exists}" />
          </td>
        </tr>
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.ProductComments}</span></td>
          <@inputTextareaCell name="comments" default="${(lot.comments)?if_exists}" />
          <td colspan="3">&nbsp;</td>
        </tr>
        <@inputSubmitRow title="${submitButtonLabel}" />
      </table>
    </form>
  </div>
<#else>
  ${uiLabelMap.OpentapsError_PermissionDenied}
</#if>
