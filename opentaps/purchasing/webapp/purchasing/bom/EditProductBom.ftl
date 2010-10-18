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

<script type="text/javascript">
/*<![CDATA[*/
function lookupBom() {
    document.searchbom.productId.value=document.editProductAssocForm.productId.value;
    document.searchbom.productAssocTypeId.value=document.editProductAssocForm.productAssocTypeId.options[document.editProductAssocForm.productAssocTypeId.selectedIndex].value;
    document.searchbom.submit();
}
/*]]>*/
</script>

<#assign title>
${uiLabelMap.ManufacturingBillOfMaterials} <#if product?exists>${(product.internalName)!} </#if>[${uiLabelMap.CommonId}:${productId!}]
</#assign>

<div class="subSectionBlock">
  <@sectionHeader title=title>
    <div class="subMenuBar">
      <#if product?has_content>
        <div class="subMenuBar">
          <a href="<@ofbizUrl>BomSimulation</@ofbizUrl>?productId=${productId}&bomType=${productAssocTypeId}" class="subMenuButton">${uiLabelMap.ManufacturingBomSimulation}</a>
        </div>
      </#if>
    </div>
  </@sectionHeader>

  <form name="searchform" action="<@ofbizUrl>EditProductBom</@ofbizUrl>#topform" method="get">
    <a name="topform"></a>
    <@inputHidden name="UPDATE_MODE" value=""/>
    <table class="fourColumnForm">
      <tr>
        <@displayTitleCell title=uiLabelMap.ManufacturingBomType />
        <@inputSelectCell name="productAssocTypeId" default="MANUF_COMPONENT" list=assocTypes ; assocType>
          ${(assocType.get("description", locale))!}
        </@inputSelectCell>

        <@displayTitleCell title=uiLabelMap.ProductProductId />
        <td>
          <@inputAutoCompleteProduct name="productId" id="search_productId" form="searchform" />
          <span><a href="javascript:document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingShowBOMAssocs}</a></span>
        </td>
      </tr>

      <tr>
        <td colspan="2">&nbsp;</td>

        <@displayTitleCell title=uiLabelMap.ManufacturingCopyToProductId />
        <td>
          <@inputAutoCompleteProduct name="copyToProductId" id="search_copyToProductId" form="searchform" />
          <span><a href="javascript:document.searchform.action='UpdateProductBom';document.searchform.UPDATE_MODE.value='COPY';document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingCopyBOMAssocs}</a></span>
        </td>
      </tr>
    </table>
  </form>
</div>

<div class="subSectionBlock">
  <form action="<@ofbizUrl>UpdateProductBom</@ofbizUrl>" method="post" name="editProductAssocForm">
    <#if !(productAssoc?exists)>
      <@inputHidden name="UPDATE_MODE" value="CREATE"/>
      <table class="twoColumnForm">
        <@inputSelectRow title=uiLabelMap.ManufacturingBomType name="productAssocTypeId" default="MANUF_COMPONENT" list=assocTypes ; assocType>
          ${(assocType.get("description", locale))!}
        </@inputSelectRow>

        <@inputAutoCompleteProductRow title=uiLabelMap.ProductProductId name="productId" id="edit_productId" form="editProductAssocForm" />
        <@inputAutoCompleteProductRow title=uiLabelMap.ManufacturingProductIdTo name="productIdTo" id="edit_productIdTo" form="editProductAssocForm" />
        <@inputDateRow   title=uiLabelMap.CommonFromDate name="fromDate" />
    <#else/> <#-- from if !(productAssoc?exists) -->
      <#assign curProductAssocType = productAssoc.getRelatedOneCache("ProductAssocType")/>
      <@inputHidden name="UPDATE_MODE"        value="UPDATE"/>
      <@inputHidden name="productId"          value="${productId!}"/>
      <@inputHidden name="productIdTo"        value="${productIdTo!}"/>
      <@inputHidden name="productAssocTypeId" value="${productAssocTypeId!}"/>
      <@inputHidden name="fromDate"           value="${fromDate!}"/>
      <table class="twoColumnForm">
        <@displayRow title=uiLabelMap.ProductProductId text=productId! />
        <@displayRow title=uiLabelMap.ManufacturingProductIdTo text=productIdTo! />
        <#assign bomTypeText>
          <#if curProductAssocType?exists>${(curProductAssocType.get("description",locale))!}<#else> ${productAssocTypeId!}</#if>
        </#assign>
        <@displayRow title=uiLabelMap.ManufacturingBomType text=bomTypeText />
        <@displayDateRow title=uiLabelMap.CommonFromDate date=fromDate! format="DATE" />
    </#if> <#-- from if !(productAssoc?exists) -->
      <@inputDateRow title=uiLabelMap.CommonThruDate name="thruDate" default=(productAssoc.thruDate)! ignoreParameters=usesValue />
      <@inputTextRow title=uiLabelMap.CommonSequenceNum name="sequenceNum" default=(productAssoc.sequenceNum)! ignoreParameters=usesValue />
      <@inputTextRow title=uiLabelMap.ManufacturingReason name="reason" default=(productAssoc.reason)! ignoreParameters=usesValue />
      <@inputTextRow title=uiLabelMap.ManufacturingInstruction name="instruction" default=(productAssoc.instruction)! ignoreParameters=usesValue />
      <@inputTextRow title=uiLabelMap.ManufacturingQuantity name="quantity" default=(productAssoc.quantity)! ignoreParameters=usesValue />
      <@inputTextRow title=uiLabelMap.ManufacturingScrapFactor name="scrapFactor" default=(productAssoc.scrapFactor)! ignoreParameters=usesValue />
      <@inputSelectRow title=uiLabelMap.ManufacturingFormula name="estimateCalcMethod" required=false list=formulae key="customMethodId" default=(productAssoc.estimateCalcMethod)! ignoreParameters=usesValue ; formula>
        ${formula.get("description",locale)!}
      </@inputSelectRow>
      <@inputLookupRow title=uiLabelMap.ManufacturingRoutingTask name="routingWorkEffortId" default=(productAssoc.routingWorkEffortId)! form="editProductAssocForm" lookup="LookupRoutingTask" ignoreParameters=usesValue />
      <@inputLookupRow title=uiLabelMap.PurchBomComponentOnlyForRouting name="specificRoutingWorkEffortId" default=(productAssoc.specificRoutingWorkEffortId)! form="editProductAssocForm" lookup="LookupRouting" ignoreParameters=usesValue />
      <tr>
        <td>&nbsp;</td>
        <td>
          <@inputSubmit title=(!productAssoc?exists)?string(uiLabelMap.CommonAdd, uiLabelMap.CommonEdit) />
          <#if productAssoc??>
            <@displayLink href="EditProductBom?productId=${productId!}&productAssocTypeId=${productAssocTypeId!}" text=uiLabelMap.CommonCancel class="buttontext" />
          </#if>
        </td>
      </tr>
    </table>
  </form>
</div>

<#if productId?exists && product?exists>
  <a name="components"></a>
  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.ManufacturingProductComponents />
    <table class="listTable">
      <tr class="listTableHeader">
        <@displayCell text=uiLabelMap.ProductProductId />
        <@displayCell text=uiLabelMap.ProductProductName />
        <@displayCell text=uiLabelMap.CommonFromDate />
        <@displayCell text=uiLabelMap.CommonThruDate />
        <@displayCell text=uiLabelMap.CommonSequenceNum />
        <@displayCell text=uiLabelMap.CommonQuantity />
        <@displayCell text=uiLabelMap.ManufacturingScrapFactor />
        <@displayCell text=uiLabelMap.ManufacturingFormula />
        <@displayCell text=uiLabelMap.ManufacturingRoutingTask />
        <@displayCell text=uiLabelMap.PurchBomComponentOnlyForRouting />
        <td>&nbsp;</td>
        <td>&nbsp;</td>
      </tr>
      <#list assocFromProducts as assocFromProduct>
        <#assign listToProduct = assocFromProduct.getRelatedOneCache("AssocProduct")/>
        <#assign curProductAssocType = assocFromProduct.getRelatedOneCache("ProductAssocType")/>
        <#assign linkToProduct>EditProductBom?productId=${(assocFromProduct.productIdTo)!}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)!}#components</#assign>
        <#assign deleteLink>UpdateProductBom?UPDATE_MODE=DELETE&productId=${productId}&productIdTo=${(assocFromProduct.productIdTo)!}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)!}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</#assign>
        <#assign editLink>EditProductBom?productId=${productId}&productIdTo=${(assocFromProduct.productIdTo)!}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)!}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</#assign>
        <tr class="${tableRowClass(assocFromProduct_index)}">
          <@displayLinkCell text=(assocFromProduct.productIdTo)! href=linkToProduct class="buttontext"/>
          <td><#if listToProduct?exists><@displayLink text=(listToProduct.internalName)! href=linkToProduct class="buttontext" /></#if>&nbsp;</td>
          <@displayDateCell date=(assocFromProduct.fromDate)! compareTo=nowDate highlightAfter=true format="DATE" />
          <@displayDateCell date=(assocFromProduct.thruDate)! compareTo=nowDate highlightBefore=true format="DATE" />
          <@displayCell text=(assocFromProduct.sequenceNum)! />
          <@displayCell text=(assocFromProduct.quantity)! />
          <@displayCell text=(assocFromProduct.scrapFactor)! />
          <@displayCell text=(assocFromProduct.estimateCalcMethod)! />
          <@displayLinkCell text=(assocFromProduct.routingWorkEffortId)! href="EditRoutingTask?workEffortId=${(assocFromProduct.routingWorkEffortId)!}" />
          <@displayLinkCell text=(assocFromProduct.specificRoutingWorkEffortId)! href="EditRouting?workEffortId=${(assocFromProduct.specificRoutingWorkEffortId)!}" />
          <@displayLinkCell text=uiLabelMap.CommonDelete href=deleteLink class="buttontext"/>
          <@displayLinkCell text=uiLabelMap.CommonEdit href=editLink class="buttontext" />
        </tr>
      </#list>
    </table>
  </div>
       
  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.ManufacturingProductComponentOf /> 
    <table class="listTable">
      <tr class="listTableHeader">
        <@displayCell text=uiLabelMap.ProductProductId />
        <@displayCell text=uiLabelMap.ProductProductName />
        <@displayCell text=uiLabelMap.CommonFromDate />
        <@displayCell text=uiLabelMap.CommonThruDate />
        <@displayCell text=uiLabelMap.CommonQuantity />
        <td>&nbsp;</td>
      </tr>
      <#list assocToProducts as assocToProduct>
        <#assign listToProduct = assocToProduct.getRelatedOneCache("MainProduct")/>
        <#assign curProductAssocType = assocToProduct.getRelatedOneCache("ProductAssocType")/>
        <#assign linkToProduct>EditProductBom?productId=${(assocToProduct.productId)!}&productAssocTypeId=${(assocToProduct.productAssocTypeId)!}#components</#assign>
        <#assign deleteLink>UpdateProductBom?UPDATE_MODE=DELETE&productId=${(assocToProduct.productId)!}&productIdTo=${(assocToProduct.productIdTo)!}&productAssocTypeId=${(assocToProduct.productAssocTypeId)!}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocToProduct.getTimestamp("fromDate").toString())}&useValues=true</#assign>
        <tr class="${tableRowClass(assocToProduct_index)}">
          <@displayLinkCell text=(assocToProduct.productId)! href=linkToProduct class="buttontext"/>
          <td><#if listToProduct?exists><@displayLink text=(listToProduct.internalName)! href=linkToProduct class="buttontext" /></#if>&nbsp;</td>
          <@displayDateCell date=(assocToProduct.fromDate)! compareTo=nowDate highlightAfter=true format="DATE" />
          <@displayDateCell date=(assocToProduct.thruDate)! compareTo=nowDate highlightBefore=true format="DATE" />
          <@displayCell text=(assocToProduct.quantity)! />
          <@displayLinkCell text=uiLabelMap.CommonDelete href=deleteLink class="buttontext"/>
        </tr>
      </#list>
    </table>

    <br/>
    <div class="tabletext">
    <b>NOTE</b>: <b style="color: red;">Red</b> date/time entries denote that the current time is before the From Date or after the Thru Date. If the From Date is <b style="color: red;">red</b>, association has not started yet; if Thru Date is <b style="color: red;">red</b>, association has expired (<u>and should probably be deleted</u>).
    </div>
  </#if> <#-- from if productId?exists && product?exists -->
