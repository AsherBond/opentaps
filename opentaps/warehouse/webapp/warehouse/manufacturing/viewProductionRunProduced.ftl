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

<#-- This file contains the single or multi form for producing (allocating to inventory) the output products of a production run. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if opentapsProductionRun.canProduce()>

  <#if opentapsProductionRun.isAssembly()> <#-- this is the legacy form -->

    <form name="ProductionRunProduce" action="<@ofbizUrl>productionRunProduce</@ofbizUrl>" method="post">
      <@inputHidden name="workEffortId" value=productionRunData.workEffortId />
      
      <#assign produced = opentapsProductionRun.productProduced/>
      <#assign producedId = produced.productId />

      <#assign toProduceMap = opentapsProductionRun.getProductsToProduce()/>
      <#assign producedMap = opentapsProductionRun.getProductsProduced()/>

      <#assign quantityToProduce = toProduceMap.get(producedId)/>
      <#assign quantityProduced = producedMap.get(producedId)?default(0)/>
      <#assign quantityCanProduce = quantityToProduce - quantityProduced/>

      <table class="twoColumnForm" style="border-top:0">
        <@displayRow title=uiLabelMap.ManufacturingQuantityProduced text=quantityProduced />
        <@displayCell class="tableheadtext" blockClass="titleCell" />
        <td class="tabletext">
          <table>
            <#if quantititesByLot?has_content>
              <#list quantitiesByLot.keySet() as lotId>
                <tr>
                  <td>
                    <#-- lotId may be a null key -->
                    <#if lotId?exists>
                      <a href="<@ofbizUrl>lotDetails?lotId=${lotId}</@ofbizUrl>">${lotId}</a>:
                    <#else/>
                      ${uiLabelMap.CommonNone}:
                    </#if>
                  </td>
                  <td>${quantitiesByLot.get(lotId)}</td>
                </tr>
              </#list>
            </#if>
          </table>
        </td>
        <@displayRow title=uiLabelMap.ManufacturingQuantityRejected text=productionRunData.quantityRejected/>
        <@inputTextRow name="quantity" title=uiLabelMap.ManufacturingProduceQuantity default=quantityCanProduce ignoreParameters=true/>
        <@inputSelectRow title=uiLabelMap.ProductInventoryItemType name="inventoryItemTypeId" list=inventoryItemTypes displayField="description" required=true default="NON_SERIAL_INV_ITEM" />
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.WarehouseLot}</span></td>
          <td nowrap="nowrap">
            <input type="text" size="20" maxlength="20" name="lotId" class="inputBox">
            <a href="javascript:call_fieldlookup2(document.ProductionRunProduce.lotId,'LookupLot');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"/></a>
            <a href="javascript:call_fieldlookup2(document.ProductionRunProduce.lotId,'createLotPopupForm');" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
          </td>
        </tr>
        <@inputSubmitRow title=uiLabelMap.CommonAdd/>
      </table>
    </form>

  <#else/><#-- from if opentapsProductionRun.isAssembly() ; Disassembly produces multiple products and calls a different service -->

    <form name="ProductionRunProduce" action="<@ofbizUrl>productionRunProduceDisassembly</@ofbizUrl>" method="post">
      <@inputHidden name="productionRunId" value=productionRun.workEffortId />

      <table class="listTable">
        <tr class="listTableHeader">
          <td>${uiLabelMap.ProductProduct}</td>
          <td>${uiLabelMap.ManufacturingQuantityProduced}</td>
          <td>${uiLabelMap.ManufacturingQuantityToProduce}</td>
          <td>${uiLabelMap.ProductInventoryItemType}</td>
          <td>${uiLabelMap.WarehouseLot}</td>
          <td><@inputMultiSelectAll form="ProductionRunProduce"/></td>
        </tr>

        <#assign toProduceMap = opentapsProductionRun.getProductsToProduce()/>
        <#assign producedMap = opentapsProductionRun.getProductsProduced()/>
        <#assign productIds = toProduceMap.keySet()/>
        <#list productIds as productId>
          <#assign quantityToProduce = toProduceMap.get(productId)/>
          <#assign quantityProduced = producedMap.get(productId)?default(0)/>
          <#assign quantityCanProduce = quantityToProduce - quantityProduced/>
          <#assign thisProduct = opentapsProductionRun.getProduct(productId)?if_exists>
          <#if thisProduct?has_content>
            <#assign productName = thisProduct.internalName?default("") + " (${productId})"/>
          <#else/>
            <#assign productName = productId/>
          </#if>

          <@inputHidden name="workEffortId" value=productionRunData.workEffortId index=productId_index />
          <@inputHidden name="productId" value=productId index=productId_index />

          <tr class="${tableRowClass(productId_index)}">
            <@displayCell text=productName />
            <@displayCell text=quantityProduced />
            <#if (quantityCanProduce gt 0)>
              <@inputTextCell name="quantity" default=quantityCanProduce size=6 index=productId_index />
              <@inputSelectCell name="inventoryItemTypeId" list=inventoryItemTypes displayField="description" required=true default="NON_SERIAL_INV_ITEM" index=productId_index />
              <td nowrap="nowrap">
                <input type="text" size="20" maxlength="20" name="lotId_o_${productId_index}" class="inputBox"/>
                <a href="javascript:call_fieldlookup2(document.ProductionRunProduce.lotId_o_${productId_index},'LookupLot');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"/></a>
                <a href="javascript:call_fieldlookup2(document.ProductionRunProduce.lotId_o_${productId_index},'createLotPopupForm');" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
              </td>
              <@inputMultiCheckCell index=productId_index />
            <#else/>
              <@inputHiddenRowSubmit index=productId_index submit=false/>
              <@displayCell text=0/>
              <td></td>
              <td></td>
            </#if>
          </tr>
        </#list>

        <tr>
          <td colspan="5"></td>
          <@inputSubmitCell title=uiLabelMap.CommonAdd />
        </tr>

        <@inputHiddenUseRowSubmit />
        <@inputHiddenRowCount list=productIds />
      </table>
    </form>

  </#if> <#-- from if opentapsProductionRun.isAssembly() -->

<#elseif opentapsProductionRun.isDisassembly()> <#-- from if opentapsProductionRun.canProduce() ; Cannot produce, so just display what the production is if we are disassembling. -->

    <table class="listTable">
      <tr class="listTableHeader">
          <td>${uiLabelMap.ProductProduct}</td>
          <td>${uiLabelMap.ManufacturingQuantityProduced}</td>
          <td>${uiLabelMap.ManufacturingQuantityToProduce}</td>
      </tr>

      <#assign toProduceMap = opentapsProductionRun.getProductsToProduce()>
      <#assign producedMap = opentapsProductionRun.getProductsProduced()>
      <#assign productIds = toProduceMap.keySet()>
      <#list productIds as productId>
        <#assign quantityToProduce = toProduceMap.get(productId)>
        <#assign quantityProduced = producedMap.get(productId)?default(0)>
        <#assign quantityCanProduce = quantityToProduce - quantityProduced>
        <#if (quantityCanProduce < 0)><#assign quantityCanProduce = 0></#if>
        <#assign thisProduct = opentapsProductionRun.getProduct(productId)?if_exists>
        <#if thisProduct?has_content>
          <#assign productName = thisProduct.internalName?default("") + " (${productId})">
        <#else>
          <#assign productName = productId>
        </#if>

        <tr class="${tableRowClass(productId_index)}">
          <@displayCell text=productName />
          <@displayCell text=quantityProduced />
          <@displayCell text=quantityCanProduce />
        </tr>
      </#list>
    </table>

</#if>
