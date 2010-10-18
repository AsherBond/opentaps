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
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script language="JavaScript" type="text/javascript">
    function quicklookup(func, locationelement, facilityelement, productelement) {

        var productId = productelement.value;
        var facilityId = facilityelement.value;
        var request = "LookupProductInventoryLocation?productId=" + productId + "&facilityId=" + facilityId;
        window[func](locationelement, request);
    }
</script>

<#assign extraOptions>
  <a href="<@ofbizUrl>PickMoveStockSimple?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrint}</a>
</#assign>

<@frameSection title=uiLabelMap.ProductStockMovesNeeded extra=extraOptions>
  <form method="post" action="<@ofbizUrl>processPhysicalStockMove</@ofbizUrl>" name='selectAllForm' style='margin: 0;'>
    <@inputHidden name="facilityId" value="${facilityId?if_exists}"/>
    <@inputHidden name="_useRowSubmit" value="Y"/>
    <#assign rowCount = 0/>
    <table cellspacing="0" class="basic-table hover-bar">
      <tr class="header-row">
        <td>${uiLabelMap.ProductProductId}</td>
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.ProductFromLocation}</td>
        <td>${uiLabelMap.ProductQoh}</td>
        <td>${uiLabelMap.ProductAtp}</td>
        <td>${uiLabelMap.ProductToLocation}</td>
        <td>${uiLabelMap.ProductQoh}</td>
        <td>${uiLabelMap.ProductAtp}</td>
        <td>${uiLabelMap.ProductMinimumStock}</td>
        <td>${uiLabelMap.ProductMoveQuantity}</td>
        <td>${uiLabelMap.CommonConfirm}</td>
        <td align="right">
          ${uiLabelMap.ProductSelectAll}&nbsp;
          <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');highlightAllRows(this, 'moveInfoId_tableRow_', 'selectAllForm');">
        </td>
      </tr>
      <#if moveByOisgirInfoList?has_content || moveByPflInfoList?has_content>
        <#assign alt_row = false/>
        <#list moveByOisgirInfoList?if_exists as moveByOisgirInfo>
          <#assign product = moveByOisgirInfo.product/>
          <#assign facilityLocationFrom = moveByOisgirInfo.facilityLocationFrom/>
          <#assign facilityLocationTypeEnumFrom = (facilityLocationFrom.getRelatedOneCache("TypeEnumeration"))?if_exists/>
          <#assign facilityLocationTo = moveByOisgirInfo.facilityLocationTo/>
          <#assign targetProductFacilityLocation = moveByOisgirInfo.targetProductFacilityLocation/>
          <#assign facilityLocationTypeEnumTo = (facilityLocationTo.getRelatedOneCache("TypeEnumeration"))?if_exists/>
          <#assign totalQuantity = moveByOisgirInfo.totalQuantity/>
          <tr id="moveInfoId_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td>${product.productId}</td>
            <td>${product.internalName?if_exists}</td>
            <td>${facilityLocationFrom.areaId?if_exists}:${facilityLocationFrom.aisleId?if_exists}:${facilityLocationFrom.sectionId?if_exists}:${facilityLocationFrom.levelId?if_exists}:${facilityLocationFrom.positionId?if_exists}<#if facilityLocationTypeEnumFrom?has_content>(${facilityLocationTypeEnumFrom.description})</#if>[${facilityLocationFrom.locationSeqId}]</td>
            <td>${moveByOisgirInfo.quantityOnHandTotalFrom?if_exists}</td>
            <td>${moveByOisgirInfo.availableToPromiseTotalFrom?if_exists}</td>
            <td>${facilityLocationTo.areaId?if_exists}:${facilityLocationTo.aisleId?if_exists}:${facilityLocationTo.sectionId?if_exists}:${facilityLocationTo.levelId?if_exists}:${facilityLocationTo.positionId?if_exists}<#if facilityLocationTypeEnumTo?has_content>(${facilityLocationTypeEnumTo.description})</#if>[${facilityLocationTo.locationSeqId}]</td>
            <td>${moveByOisgirInfo.quantityOnHandTotalTo?if_exists}</td>
            <td>${moveByOisgirInfo.availableToPromiseTotalTo?if_exists}</td>
            <td>${targetProductFacilityLocation.minimumStock?if_exists}</td>
            <td>${targetProductFacilityLocation.moveQuantity?if_exists}</td>
            <td align="right">
              <@inputHidden name="productId_o_${rowCount}" value="${product.productId?if_exists}"/>
              <@inputHidden name="facilityId_o_${rowCount}" value="${facilityId?if_exists}"/>
              <@inputHidden name="locationSeqId_o_${rowCount}" value="${facilityLocationFrom.locationSeqId?if_exists}"/>
              <@inputHidden name="targetLocationSeqId_o_${rowCount}" value="${facilityLocationTo.locationSeqId?if_exists}"/>
              <@inputText name="quantityMoved_o_${rowCount}" size="6" value="${totalQuantity?string.number}"/>
            </td>
            <td align="right">
              <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'moveInfoId_tableRow_${rowCount}');">
            </td>
          </tr>
          <#assign rowCount = rowCount + 1/>
          <#assign alt_row = !alt_row/>
        </#list>
        <#list moveByPflInfoList?if_exists as moveByPflInfo>
          <#assign product = moveByPflInfo.product/>
          <#assign facilityLocationFrom = moveByPflInfo.facilityLocationFrom/>
          <#assign facilityLocationTypeEnumFrom = (facilityLocationFrom.getRelatedOneCache("TypeEnumeration"))?if_exists/>
          <#assign facilityLocationTo = moveByPflInfo.facilityLocationTo/>
          <#assign targetProductFacilityLocation = moveByPflInfo.targetProductFacilityLocation/>
          <#assign facilityLocationTypeEnumTo = (facilityLocationTo.getRelatedOneCache("TypeEnumeration"))?if_exists/>
          <#assign totalQuantity = moveByPflInfo.totalQuantity/>
          <tr id="moveInfoId_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td>${product.productId}</td>
            <td>${product.internalName?if_exists}</td>
            <td>${facilityLocationFrom.areaId?if_exists}:${facilityLocationFrom.aisleId?if_exists}:${facilityLocationFrom.sectionId?if_exists}:${facilityLocationFrom.levelId?if_exists}:${facilityLocationFrom.positionId?if_exists}<#if facilityLocationTypeEnumFrom?has_content>(${facilityLocationTypeEnumFrom.description})</#if>[${facilityLocationFrom.locationSeqId}]</td>
            <td>${moveByPflInfo.quantityOnHandTotalFrom?if_exists}</td>
            <td>${moveByPflInfo.availableToPromiseTotalFrom?if_exists}</td>
            <td>${facilityLocationTo.areaId?if_exists}:${facilityLocationTo.aisleId?if_exists}:${facilityLocationTo.sectionId?if_exists}:${facilityLocationTo.levelId?if_exists}:${facilityLocationTo.positionId?if_exists}<#if facilityLocationTypeEnumTo?has_content>(${facilityLocationTypeEnumTo.description})</#if>[${facilityLocationTo.locationSeqId}]</td>
            <td>${moveByPflInfo.quantityOnHandTotalTo?if_exists}</td>
            <td>${moveByPflInfo.availableToPromiseTotalTo?if_exists}</td>
            <td>${targetProductFacilityLocation.minimumStock?if_exists}</td>
            <td>${targetProductFacilityLocation.moveQuantity?if_exists}</td>
            <td align="right">
              <@inputHidden name="productId_o_${rowCount}" value="${product.productId?if_exists}"/>
              <@inputHidden name="facilityId_o_${rowCount}" value="${facilityId?if_exists}"/>
              <@inputHidden name="locationSeqId_o_${rowCount}" value="${facilityLocationFrom.locationSeqId?if_exists}"/>
              <@inputHidden name="targetLocationSeqId_o_${rowCount}" value="${facilityLocationTo.locationSeqId?if_exists}"/>
              <@inputText name="quantityMoved_o_${rowCount}" size="6" default="${totalQuantity?string.number}"/>
            </td>
            <td align="right">
              <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'moveInfoId_tableRow_${rowCount}');">
            </td>
          </tr>
          <#assign rowCount = rowCount + 1/>
        </#list>
        <tr>
          <td colspan="13" align="right">
            <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductConfirmSelectedMoves}</a>
          </td>
        </tr>
      <#else>
        <tr><td colspan="13"><h3>${uiLabelMap.ProductNoStockMovesNeeded}.</h3></td></tr>
      </#if>
      <#assign messageCount = 0/>
      <#list pflWarningMessageList?if_exists as pflWarningMessage>
        <#assign messageCount = messageCount + 1/>
        <tr><td colspan="13"><h3>${messageCount}:${pflWarningMessage}.</h3></td></tr>
      </#list>
    </table>
    <@inputHidden name="_rowCount" value="${rowCount}"/>
  </form>
</@frameSection>

<@frameSection title=uiLabelMap.ProductQuickStockMove>
  <form method="post" action="<@ofbizUrl>processQuickStockMove</@ofbizUrl>" name='quickStockMove'>
    <@inputHidden name="facilityId" value="${facilityId?if_exists}"/>
    <table cellspacing="0" class="basic-table hover-bar">
      <tr class="header-row">
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.ProductFromLocation}</td>
        <td>${uiLabelMap.ProductToLocation}</td>
        <td>${uiLabelMap.ProductMoveQuantity}</td>
        <td>&nbsp</td>
      </tr>
      <@inputAutoCompleteProductCell name="productId" form="quickStockMove"/>
      <@inputLookupCell name="locationSeqId" form="quickStockMove" lookup="LookupFacilityLocation"/>
      <@inputLookupCell name="targetLocationSeqId" form="quickStockMove" lookup="LookupFacilityLocation"/>
      <@inputTextCell name="quantityMoved" size="6"/>
      <tr>
        <td colspan="13" align="right">
          <a href="javascript:document.quickStockMove.submit();" class="buttontext">${uiLabelMap.ProductQuickStockMove}</a>
        </td>
      </tr>
    </table>
  </form>
</@frameSection>
