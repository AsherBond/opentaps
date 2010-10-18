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

<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified from the version included with the Apache-licensed OFBiz facility application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if illegalInventoryItem?exists>
  <div class="errorMessage">${illegalInventoryItem}</div>
</#if>

<@frameSection title="${uiLabelMap.ProductInventoryTransfer} ${uiLabelMap.CommonFrom}&nbsp;${facility?exists?string((facility.facilityName)!,'')}&nbsp;[${uiLabelMap.CommonId}:${facilityId!}]">

<#if !(inventoryItem?exists)>
  <form method="post" action="<@ofbizUrl>TransferInventoryItem</@ofbizUrl>" name="TransferInventoryItem">
    <table>
      <@inputHidden name="facilityId" value=facilityId! />
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductInventoryItemId />
        <td>
          <@inputText name="inventoryItemId" />
          <@inputSubmit title=uiLabelMap.ProductGetItem />
        </td>
      </tr>
    </table>
  </form>
<#else>
  <#if !(inventoryTransfer?exists)>
    <form method="post" action="<@ofbizUrl>CreateInventoryTransfer</@ofbizUrl>" name="transferform">
  <#else>
    <form method="post" action="<@ofbizUrl>CheckInventoryForUpdateTransfer</@ofbizUrl>" name="transferform">
      <@inputHidden name="inventoryTransferId" value=inventoryTransferId!/>
  </#if>

  <script type="text/javascript">
  /*<![CDATA[*/
    function setNow(field) { eval('document.transferform.' + field + '.value="${StringUtil.wrapString(getNow())}"'); }
  /*]]>*/
  </script>

  <table>
    <@inputHidden name="inventoryItemId" value=inventoryItemId!/>
    <@inputHidden name="facilityId" value=facilityId!/>
    <@inputHidden name="locationSeqId" value=inventoryItem.locationSeqId!/>

    <@displayLinkRow title=uiLabelMap.ProductInventoryItemId href="EditInventoryItem?inventoryItemId=${inventoryItemId!}" text=inventoryItemId! />
    <@displayRow title=uiLabelMap.ProductInventoryItemTypeId text=(inventoryItemType.get("description", locale))! />
    <@displayLinkRow title=uiLabelMap.ProductProductId href="findInventoryItem?performFind=Y&amp;productId=${(inventoryItem.productId)!}" text=(inventoryItem.productId)! />
    <@displayRow title=uiLabelMap.ProductStatus text=(inventoryStatus.get("description", locale))!("--") />
    <@displayRow title=uiLabelMap.ProductComments text=(inventoryItem.comments)!("--") />

    <tr>
      <@displayTitleCell title=uiLabelMap.ProductSerialAtpQoh/>
      <#if inventoryItem?exists && "NON_SERIAL_INV_ITEM" == (inventoryItem.inventoryItemTypeId)>
        <@displayCell text="${(inventoryItem.availableToPromiseTotal)!}&nbsp;/&nbsp;${(inventoryItem.quantityOnHandTotal)!}" />
      <#elseif inventoryItem?exists && "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)>
        <@displayCell text=(inventoryItem.serialNumber)! />
      <#elseif inventoryItem?exists>
        <@displayCell style="color: red;" text="${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)!} ${uiLabelMap.ProductUnknownSpecifyType}."/>
      </#if>
    </tr>

    <tr>
      <td colspan="2"><hr class="sepbar"></td>
    </tr>

    <tr>
      <@displayTitleCell title=uiLabelMap.ProductTransferStatus />
      <td>
        <#if statusItems?has_content>
          <select name="statusId" class="inputBox">
            <#if (inventoryTransfer.statusId)?has_content>
              <#assign curStatus = inventoryTransfer.getRelatedOneCache("StatusItem")! />
              <option value="${inventoryTransfer.statusId}" selected="selected">${(curStatus.get("description", locale))!}</option>
              <option disabled="disabled">---</option>
            </#if>
            <#assign curStatusId = parameters.statusId?default(inventoryTransfer?default({}).statusId)?default("")/>
            <#list statusItems as statusItem>
              <#-- exclude IXF_COMPLETE when creating a transfer -->
              <#if ! ((! inventoryTransfer?exists) && statusItem.statusId == "IXF_COMPLETE")>
                <option value="${(statusItem.statusId)?if_exists}" ${(!(inventoryTransfer.statusId)?has_content && curStatusId == statusItem.statusId)?string("selected=\"selected\"", "")}>${(statusItem.get("description", locale))!}</option>
              </#if>
            </#list>
          </select>
        </#if>
      </td>
    </tr>

    <tr>
      <@displayTitleCell title=uiLabelMap.ProductTransferSendDate />
      <td><@inputText name="sendDate" default="${getLocalizedDate((inventoryTransfer.sendDate)!)}" size="22" /><a href="#" onclick="setNow('sendDate')" class="buttontext">${uiLabelMap.CommonNow}</a></td>
    </tr>

    <#if !(inventoryTransfer?exists)>
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductToFacilityContainer />
        <td>
          <span class="tabletext">${uiLabelMap.ProductSelectFacility}:</span>
          <@inputSelect name="facilityIdTo" list=facilities key="facilityId" ; nextFacility>
            ${(nextFacility.facilityName)!} [${(nextFacility.facilityId)!}]
          </@inputSelect>
          <br/>
          <span class="tabletext">${uiLabelMap.ProductOrEnterContainerId}:</span>
          <@inputText name="containerIdTo" default="${(inventoryTransfer.containerIdTo)!}" size="20" maxlength="20"/>
        </td>
      </tr>

      <@inputLookupRow title=uiLabelMap.ProductToLocation name="locationSeqIdTo" lookup="LookupFacilityLocation" form="transferform" />
      <@inputTextRow title=uiLabelMap.ProductComments name="comments" size="60" maxlength="250" />

      <tr>
        <@displayTitleCell title=uiLabelMap.ProductQuantityToTransfer/>
        <#if inventoryItem?exists && "NON_SERIAL_INV_ITEM" == (inventoryItem.inventoryItemTypeId)>
          <@inputTextCell name="xferQty" size="5" default="${(inventoryItem.availableToPromiseTotal)!}" />
        <#elseif inventoryItem?exists && "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)>
          <@inputHidden name="xferQty" value="1"/>
          <@displayCell text="1"/>
        <#elseif inventoryItem?exists>
          <@displayCell style="color: red;" text="${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)!} ${uiLabelMap.ProductUnknownSpecifyType}."/>
        </#if>
      </tr>
    <#else>
      <tr>
        
        <@displayTitleCell title=uiLabelMap.ProductTransferReceiveDate />
        <td><@inputText name="receiveDate" default="${getLocalizedDate((inventoryTransfer.receiveDate)!)}" size="22" /><a href="#" onclick="setNow('receiveDate')" class="buttontext">${uiLabelMap.CommonNow}</a></td>
      </tr>

      <#assign fac = inventoryTransfer.getRelatedOneCache("ToFacility")/>
      <@displayRow title=uiLabelMap.ProductToFacilityContainer text=(fac.facilityName)! />
      <@inputLookupRow title=uiLabelMap.ProductToLocation name="locationSeqIdTo" lookup="LookupFacilityLocation?facilityId=${inventoryTransfer.facilityIdTo}" form="transferform" />
      <@inputTextRow title=uiLabelMap.ProductComments name="comments" size="60" maxlength="250" default=(inventoryTransfer.comments)! />
    </#if>

    <tr>
      <td>&nbsp;</td>
      <#if !(inventoryTransfer?exists)>
        <@inputSubmitCell title=uiLabelMap.ProductTransfer/>
      <#else>
        <#if parameters.forceComplete?default("false") == "true">
          <@inputHidden name="forceComplete" value=parameters.forceComplete?default("false") />
          <@inputConfirmCell title=uiLabelMap.WarehouseForceComplete form="transferform" />
        <#else>
          <@inputSubmitCell title=uiLabelMap.CommonUpdate/>
        </#if>
      </#if>
    </tr>

  </table>
</form>
</#if>

</@frameSection>
