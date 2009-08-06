<#escape x as x?xml>
<#list groupedItems as item>
<#assign productId = item.productId?if_exists>
<fo:table-row>
  <fo:table-cell>
    <fo:block>
      <#if productId?exists>
      ${item.productId?default("N/A")} - ${item.itemDescription?if_exists}
      <#elseif item.orderItemTypeDescription?exists>
      ${orderItemTypeDescription} - ${item.itemDescription?if_exists}
      <#else>
      ${item.itemDescription?if_exists}
    </#if>
  </fo:block>
</fo:table-cell>
<fo:table-cell text-align="right"><fo:block>${item.groupQuantity}</fo:block></fo:table-cell>            
<fo:table-cell text-align="right"><fo:block>
<#if item.groupQuantity != 0>
    <@ofbizCurrency amount=(item.groupTotal/item.groupQuantity) isoCode=currencyUomId rounding=4 />
</#if>
</fo:block></fo:table-cell>
<fo:table-cell text-align="right"><fo:block>
    <#if item.statusId != "ITEM_CANCELLED">
    <@ofbizCurrency amount=item.groupTotal isoCode=currencyUomId/>
  <#else>
  <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
  </#if></fo:block></fo:table-cell>
</fo:table-row>
<#if item.groupAdjustmentTotal != 0>
<fo:table-row>
  <fo:table-cell number-columns-spanned="2"><fo:block><fo:inline font-style="italic">${uiLabelMap.OrderAdjustments}</fo:inline>: <@ofbizCurrency amount=item.groupAdjustmentTotal isoCode=currencyUomId/></fo:block></fo:table-cell>
</fo:table-row>
</#if>
</#list>
</#escape>
