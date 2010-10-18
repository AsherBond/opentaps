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


<#if requestParameters.lookupFlag?default("N") == "Y">

  <#if selectedFeatures?has_content>
    <div class="subSectionBlock">
      <@sectionHeader title=uiLabelMap.SelectableFeatures /> 
      <table class="listTable">
      <#list selectedFeatures as selectedFeature>
        <tr><@displayCell text="${selectedFeature.productFeatureTypeId} = ${selectedFeature.description!} [${selectedFeature.productFeatureId}]" /></tr>
      </#list>
      </table>
    </div>
  </#if>

  
  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.ManufacturingBillOfMaterials />
    <table class="listTable">
      <tr class="listTableHeader">
        <th width="10%" align="left">${uiLabelMap.ManufacturingProductLevel}</th>
        <th width="20%" align="left">${uiLabelMap.ProductProductId}</th>
        <th width="10%" align="left">&nbsp;</th>
        <th width="40%" align="left">${uiLabelMap.ProductProductName}</th>
        <th width="10%" align="right">${uiLabelMap.CommonQuantity}</th>
        <th width="10%" align="right">&nbsp;</th>
      </tr>
      <#if tree?has_content>
        <#list tree as node>
          <tr class="${tableRowClass(node_index)}">
            <td>
              <table cellspacing="1">
                <tr>
                  <td>${node.depth}</td>
                  <#list 0..(node.depth) as level>
                    <td bgcolor="red">&nbsp;&nbsp;</td>
                  </#list>
                </tr>
              </table>
            </td>
            <td>${node.product.productId}</td>
            <td>
              <#if node.product.isVirtual?default("N") == "Y">
                Virtual
              </#if>
              ${(node.ruleApplied.ruleId)!}
            </td>
            <td>${node.product.internalName?default("&nbsp;")}</td>
            <td align="right">${node.quantity}</td>
            <td align="right"><a href="<@ofbizUrl>EditProductBom?productId=${(node.product.productId)!}&productAssocTypeId=${(node.bomTypeId)!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
          </tr>
        </#list>          
      <#else/> <#-- from if tree?has_content -->
        <tr>
          <th colspan="4">${uiLabelMap.CommonNoElementFound}.</th>
        </tr>        
      </#if>
    </table>
  </div>

  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.ProductEstimatedCosts /> 
    <table class="listTable">
      <tr class="listTableHeader">
        <th width="18%" align="left">${uiLabelMap.ProductProductId}</th>
        <th width="50%" align="left">${uiLabelMap.ProductProductName}</th>
        <th width="8%" align="right">${uiLabelMap.CommonQuantity}</th>
        <th width="8%" align="right">${uiLabelMap.ProductQoh}</th>
        <th width="8%" align="right">${uiLabelMap.FormFieldTitle_cost}</th>
        <th width="8%" align="right">${uiLabelMap.CommonTotalCost}</th>
      </tr>
      <#if productsData?has_content>
        <#list productsData as productData>
          <#assign node = productData.node/>
          <tr class="${tableRowClass(productData_index)}">
            <td align="left"><a href="/catalog/control/EditProduct?productId=${node.product.productId}" class="buttontext">${node.product.productId}</a></td>
            <td align="left">${node.product.internalName?default("&nbsp;")}</td>
            <td align="right">${node.quantity}</td>
            <td align="right">${productData.qoh!uiLabelMap.CommonNA}</td>
            <#if productData.unitCost?? && (productData.unitCost gt 0)>
              <td align="right">${productData.unitCost}</td>
              <td align="right">${productData.totalCost!}</td>
            <#else/>
              <td align="right"><a href="/catalog/control/EditProductCosts?productId=${node.product.productId}" class="buttontext">${uiLabelMap.CommonNA}</a></td>
              <td align="right">${uiLabelMap.CommonNA}</td>
            </#if>
          </tr>
        </#list>
      <#else/> <#-- from productsData?has_content -->
        <tr>
          <td colspan="6"><h3>${uiLabelMap.CommonNoElementFound}.</h3></td>
        </tr>
      </#if>
    </table>
  </div>

</#if> <#-- from if requestParameters.lookupFlag?default("N") == "Y" -->
