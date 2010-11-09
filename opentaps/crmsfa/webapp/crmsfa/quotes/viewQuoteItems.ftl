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
   Function prepares parameters and sends request for removeQuoteItemOption.
*/
function removeQuoteItemOption(/*String*/ quoteId, /*String*/ quoteItemSeqId, /*String*/ quoteItemOptionSeqId) {
    document.processingQuoteItemOptionForm.action = "removeQuoteItemOption";
    document.processingQuoteItemOptionForm.quoteId.value = quoteId;
    document.processingQuoteItemOptionForm.quoteItemSeqId.value = quoteItemSeqId;
    document.processingQuoteItemOptionForm.quoteItemOptionSeqId.value = quoteItemOptionSeqId;
    document.processingQuoteItemOptionForm.submit();
}

/*
   Function prepares parameters and sends request for setQuoteItemOption.
*/
function setQuoteItemOption(/*String*/ quoteId, /*String*/ quoteItemSeqId, /*String*/ quoteItemOptionSeqId) {
    document.processingQuoteItemOptionForm.action = "setQuoteItemOption";
    document.processingQuoteItemOptionForm.quoteId.value = quoteId;
    document.processingQuoteItemOptionForm.quoteItemSeqId.value = quoteItemSeqId;
    document.processingQuoteItemOptionForm.quoteItemOptionSeqId.value = quoteItemOptionSeqId;
    document.processingQuoteItemOptionForm.submit();
}

/*
   Function prepares parameters and sends request for unsetQuoteItemOption.
*/
function unsetQuoteItemOption(/*String*/ quoteId, /*String*/ quoteItemSeqId) {
    document.processingQuoteItemOptionForm.action = "unsetQuoteItemOption";
    document.processingQuoteItemOptionForm.quoteId.value = quoteId;
    document.processingQuoteItemOptionForm.quoteItemSeqId.value = quoteItemSeqId;
    document.processingQuoteItemOptionForm.quoteItemOptionSeqId.value = "";
    document.processingQuoteItemOptionForm.submit();
}

/*
   Function prepares parameters and sends request for removeQuoteItem.
*/
function removeQuoteItem(/*String*/ quoteId, /*String*/ quoteItemSeqId) {
    document.processingQuoteItemOptionForm.action = "removeQuoteItem";
    document.processingQuoteItemOptionForm.quoteId.value = quoteId;
    document.processingQuoteItemOptionForm.quoteItemSeqId.value = quoteItemSeqId;
    document.processingQuoteItemOptionForm.quoteItemOptionSeqId.value = "";
    document.processingQuoteItemOptionForm.submit();
}
</script>
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if quote?exists>

  <!-- add for  setQuoteItemOption/removeQuoteItemOption/unsetQuoteItemOption/removeQuoteItem -->
  <@form name="processingQuoteItemOptionForm" url="" quoteId="" quoteItemSeqId="" quoteItemOptionSeqId="" />

  <@frameSection title=uiLabelMap.OrderOrderQuoteItems>
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign="bottom">
        <td width="10%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductItem}</b></span></td>
        <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductProduct}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.ProductQuantity}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonUnitPrice}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonSubtotal}</b></span></td>
        <td width="1%">&nbsp;</td>
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

        <tr><td colspan="6"><hr class="sepbar"/></td></tr>

        <#if canEditQuote>
        <tr>
          <form method="post" action="<@ofbizUrl>updateQuoteItem</@ofbizUrl>" name="updateQuoteItem_${quoteItem.quoteItemSeqId}">
            <td valign="top">
              <div class="tabletext" style="font-size: xx-small;">
                <#if showQuoteManagementLinks?exists && quoteItem.isPromo?default("N") == "N">
                  <a href="<@ofbizUrl>EditQuoteItem?quoteId=${quoteItem.quoteId}&amp;quoteItemSeqId=${quoteItem.quoteItemSeqId}</@ofbizUrl>" class="buttontext">${quoteItem.quoteItemSeqId}</a>
                <#else>
                  ${quoteItem.quoteItemSeqId}
                </#if>
              </div>
            </td>
            <td valign="top">
              <div class="tabletext">
                <@inputHidden name="quoteId" value=quoteItem.quoteId />
                <@inputHidden name="quoteItemSeqId" value=quoteItem.quoteItemSeqId />
                <@inputAutoCompleteProduct name="productId" id="productId_${quoteItem.quoteItemSeqId}" default=quoteItem.productId?if_exists size="10"/>
                <@inputText name="description" default=quoteItem.description?if_exists />
                <a href="javascript:document.updateQuoteItem_${quoteItem.quoteItemSeqId}.submit();"><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/save.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonSave}"/></a>
                <br/>
                <@inputTextarea name="comments" rows=3 cols=50 default=quoteItem.comments?if_exists />
              </div>
            </td>
            <td align="right" valign="top"><div class="tabletext">${quoteItem.quantity?if_exists}</div></td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=quoteItem.quoteUnitPrice?default(0) isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></div></td>
            <#if 0 != quoteItem.quoteUnitPrice?default(0) || 0 != quoteItem.quantity?default(0) >
              <td valign="top"><@displayLink text=uiLabelMap.OpentapsUnset href="javascript:unsetQuoteItemOption('${quoteItem.quoteId}','${quoteItem.quoteItemSeqId}');" class="buttontext" /></td>
            </#if>
            <td valign="top"><@displayLink text=uiLabelMap.CommonRemove href="javascript:removeQuoteItem('${quoteItem.quoteId}','${quoteItem.quoteItemSeqId}');" class="buttontext" /></td>
          </form>
        </tr>
        <#else>
        <tr>
          <td valign="top">
            <div class="tabletext" style="font-size: xx-small;">${quoteItem.quoteItemSeqId}</div>
          </td>
          <td valign="top">
            <div class="tabletext">
              <#if quoteItem.productId?exists>
                <a href="<@ofbizUrl>product?product_id=${quoteItem.productId?if_exists}</@ofbizUrl>" class="linktext">${quoteItem.productId?if_exists}</a>&nbsp;
                ${quoteItem.description?if_exists?xml}
              </#if><br/>
              <i>${quoteItem.comments?if_exists?xml}</i> 
            </div>
          </td>
          <td align="right" valign="top"><div class="tabletext">${quoteItem.quantity?if_exists}</div></td>
          <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=quoteItem.quoteUnitPrice?default(0) isoCode=quote.currencyUomId/></div></td>
          <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>
          <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></div></td>
        </tr>
        </#if>

        <#-- now show alternate options per line item -->
        <#assign options = quoteItem.getRelated("QuoteItemOption", ["quoteItemOptionSeqId"]) />
        <form method="post" action="<@ofbizUrl>updateQuoteItemOptions</@ofbizUrl>" name="updateQuoteItemOptions_${quoteItem.quoteItemSeqId}">
          <@inputHiddenRowCount list=options />
          <@inputHidden name="quoteId" value=quote.quoteId />
        <#list options as option>
          <#assign optionAmount = option.quoteUnitPrice?default(0) * option.quantity?default(0)/>
          <#assign totalOptionAmount = optionAmount + totalQuoteItemAdjustmentAmount/>
          <tr>
            <#if canEditQuote>
              <@inputHidden name="quoteId" value=option.quoteId index=option_index />
              <@inputHidden name="quoteItemSeqId" value=option.quoteItemSeqId index=option_index />
              <@inputHidden name="quoteItemOptionSeqId" value=option.quoteItemOptionSeqId index=option_index />
            </#if>
            <td/>
            <td align="right"><div class="tabletext" style="font-size: xx-small;">${uiLabelMap.OpentapsOption} ${option.quoteItemOptionSeqId}</div></td>
            <#if canEditQuote>
              <td align="right"><@inputText name="quantity" size=7 default=option.quantity?if_exists index=option_index /></td>
              <td align="right"><@inputText name="quoteUnitPrice" size=7 default=option.quoteUnitPrice?if_exists index=option_index /></td>
            <#else>
              <td align="right"><div class="tabletext" style="font-size: xx-small;">${option.quantity?if_exists}</div></td>
              <td align="right"><div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=option.quoteUnitPrice?default(0) isoCode=quote.currencyUomId/></div></td>
            </#if>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>
            <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalOptionAmount isoCode=quote.currencyUomId/></div></td>
            <#if canEditQuote>
              <td valign="top"><@displayLink text=uiLabelMap.OpentapsChoose href="javascript:setQuoteItemOption('${option.quoteId}','${option.quoteItemSeqId}','${option.quoteItemOptionSeqId}');" class="buttontext" /></td>
              <td valign="top"><@displayLink text=uiLabelMap.CommonRemove href="javascript:removeQuoteItemOption('${option.quoteId}','${option.quoteItemSeqId}','${option.quoteItemOptionSeqId}');" class="buttontext" /></td>
            </#if>
          </tr>
        </#list>
        <#if canEditQuote && options?has_content>
          <tr>
            <td colspan="3"/>
            <td align="right"><@displayLink text=uiLabelMap.CommonSave href="javascript:document.updateQuoteItemOptions_${quoteItem.quoteItemSeqId}.submit();" class="buttontext" /></td>
          </tr>
        </#if>
        </form>

        <#-- Add a new option -->
        <#if canEditQuote>
          <tr>
            <form method="post" action="<@ofbizUrl>addQuoteItemOption</@ofbizUrl>" name="addQuoteItemOption_${quoteItem.quoteItemSeqId}">
              <@inputHidden name="quoteId" value=quoteItem.quoteId />
              <@inputHidden name="quoteItemSeqId" value=quoteItem.quoteItemSeqId />            
              <td/>
              <td/>
              <td align="right"><@inputText name="quantity" size=7 /></td>
              <td align="right"><@inputText name="quoteUnitPrice" size=7 /></td>
              <td align="left"><@inputSubmit title=uiLabelMap.CrmAddOption /></td>
              <td colspan="4"/>
            </form>
          </tr>
        </#if>

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
    </table>
</@frameSection>

</#if>
