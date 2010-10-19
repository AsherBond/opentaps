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
<script type="text/javascript">

/*
   Function prepares parameters and sends request for setQuoteItemOption.
*/
function setFinalizeQuoteItemOption(/*String*/ quoteId, /*String*/ quoteItemSeqId, /*String*/ quoteItemOptionSeqId) {
	document.processingQuoteItemOptionForm.action = "setFinalizeQuoteItemOption";
    document.processingQuoteItemOptionForm.quoteId.value = quoteId;
    document.processingQuoteItemOptionForm.quoteItemSeqId.value = quoteItemSeqId;
    document.processingQuoteItemOptionForm.quoteItemOptionSeqId.value = quoteItemOptionSeqId;
    document.processingQuoteItemOptionForm.submit();
}


</script>
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if quote?exists>
<#assign readyToFinalize = true />
<@frameSection title=uiLabelMap.OrderOrderQuoteItems>
  <!-- add for javascript function setFinalizeQuoteItemOption -->
  <@form name="processingQuoteItemOptionForm" url="" quoteId="" quoteItemSeqId="" quoteItemOptionSeqId="" />
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign="bottom">
        <td width="10%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductItem}</b></span></td>
        <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductProduct}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.ProductQuantity}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderOrderQuoteUnitPrice}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonSubtotal}</b></span></td>
        <td width="1%">&nbsp;</td>
      </tr>
      <#assign totalQuoteAmount = 0.0/>
      <#list quoteItems as quoteItem>
        <#if quoteItem.productId?exists>
          <#assign product = quoteItem.getRelatedOne("Product")/>
        </#if>
        <#assign quoteItemAmount = quoteItem.quoteUnitPrice?default(0) * quoteItem.quantity?default(0)/>
        <#assign quoteItemAdjustments = quoteItem.getRelated("QuoteAdjustment")/>
        <#assign totalQuoteItemAdjustmentAmount = 0.0/>
        <#list quoteItemAdjustments as quoteItemAdjustment>
          <#assign totalQuoteItemAdjustmentAmount = quoteItemAdjustment.amount?default(0) + totalQuoteItemAdjustmentAmount/>
        </#list>
        <#assign totalQuoteItemAmount = quoteItemAmount + totalQuoteItemAdjustmentAmount/>
        <#assign totalQuoteAmount = totalQuoteAmount + totalQuoteItemAmount/>

        <#-- cehck if the item is ready to be finalized -->
        <#assign itemReadyToFinalize = true />
        <#if !quoteItem.productId?has_content || quoteItem.quantity?default(0) == 0 || !quoteItem.quoteUnitPrice?has_content>
          <#assign readyToFinalize = false />
          <#assign itemReadyToFinalize = false />
          <#assign itemStyle = "style=\"color:red\"" />
        </#if>

        <tr><td colspan="6"><hr class="sepbar"/></td></tr>

        <tr>
          <form method="post" action="<@ofbizUrl>finalizeQuoteItem</@ofbizUrl>" name="finalizeQuoteItem_${quoteItem.quoteItemSeqId}">
            <td valign="top">
              <div class="tabletext" ${itemStyle?if_exists}>${quoteItem.quoteItemSeqId}</div>
            </td>
            <td valign="top">
              <div class="tabletext">
                <@inputHidden name="quoteId" value=quoteItem.quoteId />
                <@inputHidden name="quoteItemSeqId" value=quoteItem.quoteItemSeqId />
                <@inputAutoCompleteProduct name="productId" id="productId_${quoteItem.quoteItemSeqId}" default=quoteItem.productId?if_exists size="10"/>
                <@inputText name="description" default=quoteItem.description?if_exists />
                <a href="javascript:document.finalizeQuoteItem_${quoteItem.quoteItemSeqId}.submit();"><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/save.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonSave}"/></a>
                <br/>
                <@inputTextarea name="comments" rows=3 cols=50 default=quoteItem.comments?if_exists />
              </div>
            </td>
            <td align="right" valign="top"><div class="tabletext" ${itemStyle?if_exists}>${quoteItem.quantity?if_exists}</div></td>
            <td align="right" valign="top"><div class="tabletext" ${itemStyle?if_exists}><@ofbizCurrency amount=quoteItem.quoteUnitPrice?default(0) isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext" ${itemStyle?if_exists}><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext" ${itemStyle?if_exists}><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></div></td>
          </form>
        </tr>

        <#-- now show alternate options per line item -->
        <#assign options = quoteItem.getRelated("QuoteItemOption", ["quoteItemOptionSeqId"]) />
        <#list options as option>
          <#assign optionAmount = option.quoteUnitPrice?default(0) * option.quantity?default(0)/>
          <#assign totalOptionAmount = optionAmount + totalQuoteItemAdjustmentAmount/>
          <tr>
            <td/>
            <td align="right"><div class="tabletext" style="font-size: xx-small;">${uiLabelMap.CrmQuoteItemOption} ${option.quoteItemOptionSeqId}</div></td>
            <td align="right"><div class="tabletext" style="font-size: xx-small;">${option.quantity?if_exists}</div></td>
            <td align="right">
              <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=option.quoteUnitPrice?default(0) isoCode=quote.currencyUomId/></div>
            </td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalOptionAmount isoCode=quote.currencyUomId/></div></td>
            <#if canEditQuote>
              <@displayLinkCell text=uiLabelMap.OpentapsChoose href="javascript:setFinalizeQuoteItemOption('${option.quoteId}','${option.quoteItemSeqId}','${option.quoteItemOptionSeqId}');" class="buttontext" />
            </#if>
          </tr>
        </#list>

        <#-- now show adjustment details per line item -->
        <#list quoteItemAdjustments as quoteItemAdjustment>
          <#assign adjustmentType = quoteItemAdjustment.getRelatedOne("OrderAdjustmentType")/>
          <tr>
            <td align="right" colspan="4"><div class="tabletext" style="font-size: xx-small;"><b>${adjustmentType.get("description",locale)?if_exists}</b></div></td>
            <td align="right">
              <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=quoteItemAdjustment.amount isoCode=quote.currencyUomId/></div>
            </td>
            <td/>
          </tr>
        </#list>
      </#list>

      <tr><td colspan="6"><hr class="sepbar"/></td></tr>

      <tr>
        <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.CommonSubtotal}</b></div></td>
        <td align="right"><div class="tabletext"><@ofbizCurrency amount=totalQuoteAmount isoCode=quote.currencyUomId/></div></td>
      </tr>

      <#assign totalQuoteHeaderAdjustmentAmount = 0.0/>
      <#if quoteAdjustments?has_content>
        <tr><td colspan="4"></td><td colspan="2"><hr class="sepbar"/></td></tr>

        <#list quoteAdjustments as quoteAdjustment>
          <#assign adjustmentType = quoteAdjustment.getRelatedOne("OrderAdjustmentType")/>
          <#if !quoteAdjustment.quoteItemSeqId?exists>
            <#assign totalQuoteHeaderAdjustmentAmount = quoteAdjustment.amount?default(0) + totalQuoteHeaderAdjustmentAmount/>
            <tr>
              <td align="right" colspan="5"><div class="tabletext"><b>${adjustmentType.get("description",locale)?if_exists}</b></div></td>
              <td align="right"><div class="tabletext"><@ofbizCurrency amount=quoteAdjustment.amount isoCode=quote.currencyUomId/></div></td>
            </tr>
          </#if>
        </#list>
      </#if>
      <#assign grandTotalQuoteAmount = totalQuoteAmount + totalQuoteHeaderAdjustmentAmount/>

      <tr><td colspan="4"></td><td colspan="2"><hr class="sepbar"/></td></tr>

      <tr>
        <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderGrandTotal}</b></div></td>
        <td align="right">
          <div class="tabletext"><@ofbizCurrency amount=grandTotalQuoteAmount isoCode=quote.currencyUomId/></div>
        </td>
      </tr>

      <#if readyToFinalize>
        <tr><td colspan="4"></td><td colspan="2"><hr class="sepbar"/></td></tr>
        <tr>
          <td align="right" colspan="5"/>
          <td align="right">
          <@form name="finalizeQuoteStatusForm" url="finalizeQuoteStatus" quoteId="${quote.quoteId}" statusId="QUO_FINALIZED" finalizeMode="init"/>
          <@submitFormLink form="finalizeQuoteStatusForm" text=uiLabelMap.OrderCreateOrder/> 
        </tr>
      </#if>

    </table>
</@frameSection>

</#if>
