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

<#if hasUpdatePermission>
           
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if invoiceItem?exists><#assign formAction = "updateInvoiceItem"><#else><#assign formAction = "createInvoiceItem"></#if>

<form method="post" action="<@ofbizUrl>${formAction}</@ofbizUrl>" name="${formAction}">
  <@inputHidden name="invoiceId" value="${invoice.invoiceId}"/>
  <@inputHidden name="validateAccountingTags" value="True"/>

<#if invoiceItem?exists>
  <@inputHidden name="invoiceItemSeqId" value="${invoiceItem.invoiceItemSeqId}"/>
  <div class="screenlet">
    <div class="screenlet-header">
      <div class="boxhead">
        Update Invoice #<a href="<@ofbizUrl>viewInvoice?invoiceId=${invoice.invoiceId}</@ofbizUrl>" class="buttontext">${invoice.invoiceId}</a> ${uiLabelMap.FinancialsInvoiceItemSeqId} ${invoiceItem.invoiceItemSeqId}
      </div>
    </div>
    <div class="screenlet-body">
      <table>
        <@inputSelectRow name="invoiceItemTypeId" title=uiLabelMap.CommonType list=invoiceItemTypes displayField="description" default=invoiceItem.invoiceItemTypeId />
        <@inputTextRow name="description" title=uiLabelMap.CommonDescription size="60" default=invoiceItem.description />
        <tr>
          <@displayTitleCell title=uiLabelMap.FinancialsOverrideGlAccount />
          <td ><@inputAutoCompleteGlAccount name="overrideGlAccountId" id="overrideGlAccountId" default=invoiceItem.overrideGlAccountId/></td>
          </tr>
          <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId default=invoiceItem.productId />
      </table>
      <table>
        <tr>
          <@displayCell text=uiLabelMap.CommonQuantity blockClass="titleCell" blockStyle="width: 200px" class="tableheadtext"/>
          <@inputTextCell name="quantity" size=4 default=invoiceItem.quantity />
          <@displayCell text=uiLabelMap.CommonAmount blockClass="titleCell" blockStyle="width: 100px" class="tableheadtext"/>
          <@inputCurrencyCell name="amount" currencyName="uomId" default=invoiceItem.amount defaultCurrencyUomId=parameters.orgCurrencyUomId disableCurrencySelect=true/>
        </tr>
        <tr>
          <@displayCell text=uiLabelMap.FinancialsIsTaxable blockClass="titleCell" blockStyle="width: 200px" class="tableheadtext"/>
          <@inputIndicatorCell name="taxableFlag" default=invoiceItem.taxableFlag />
          <@displayCell text=uiLabelMap.AccountingTaxAuthority blockClass="titleCell" blockStyle="width: 100px" class="tableheadtext"/>
          <@inputSelectTaxAuthorityCell list=taxAuthorities required=false defaultGeoId=invoiceItem.taxAuthGeoId defaultPartyId=invoiceItem.taxAuthPartyId />
        </tr>

        <#if tagTypes?has_content>
          <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=invoiceItem />
        </#if>
        <@inputSubmitRow title=uiLabelMap.CommonUpdate />
      </table>
    </div>
  </div>
<#else>
  <div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.FinancialsNewInvoiceItem}</div></div>
    <div class="screenlet-body">
      <table border="0" cellpadding="2" cellspacing="0" width="100%">
        <@inputSelectRow name="invoiceItemTypeId" title=uiLabelMap.CommonType list=invoiceItemTypes displayField="description" required=false />
        <@inputTextRow name="description" title=uiLabelMap.CommonDescription size="60" />
        <tr>
          <@displayTitleCell title=uiLabelMap.FinancialsOverrideGlAccount />
          <td ><@inputAutoCompleteGlAccount name="overrideGlAccountId" id="overrideGlAccountId" default=glAccountId/></td>
        </tr>
        <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId />
      </table>
      <table>
        <tr>
          <@displayCell text=uiLabelMap.CommonQuantity blockClass="titleCell" blockStyle="width: 200px" class="tableheadtext" />
          <@inputTextCell name="quantity" size=4 />
          <@displayCell text=uiLabelMap.CommonAmount blockClass="titleCell" blockStyle="width: 100px" class="tableheadtext" />
          <@inputCurrencyCell name="amount" currencyName="uomId" defaultCurrencyUomId=parameters.orgCurrencyUomId disableCurrencySelect=true/>
        </tr>
        <tr>
          <@displayCell text=uiLabelMap.FinancialsIsTaxable blockClass="titleCell" blockStyle="width: 200px" class="tableheadtext"/>
          <@inputIndicatorCell name="taxableFlag" default="N"/>
          <@displayCell text=uiLabelMap.AccountingTaxAuthority blockClass="titleCell" blockStyle="width: 100px" class="tableheadtext" />
          <@inputSelectTaxAuthorityCell list=taxAuthorities required=false/>
        </tr>

        <#if !disableTags?exists && tagTypes?has_content>
          <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=lastItem! />
        </#if>
        <@inputSubmitRow title=uiLabelMap.CommonAdd/>
      </table>
    </div>
  </div>
</#if>

</form>

<#else>
<#-- TODO The error here depends on what happened:  either invoice, invoiceItem or hasUpdatePermission was missing/false -->
</#if>
