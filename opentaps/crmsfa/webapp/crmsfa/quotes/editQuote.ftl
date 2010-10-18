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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if quote?exists><#assign formAction = "updateQuote"><#else><#assign formAction = "createQuote"></#if>

<div class="subSectionBlock">
  <div class="form">
    <form method="post" action="<@ofbizUrl>${formAction}</@ofbizUrl>" name="${formAction}">
      <table class="twoColumn">
        <#if quote?exists>
          <#-- Edit an existing quote -->
          <@inputHidden name="quoteId" value="${quote.quoteId}"/>
          <@inputTextRow name="quoteName" title=uiLabelMap.OrderOrderQuoteName default=quote.quoteName />
          <@inputHidden name="quoteTypeId" value="PRODUCT_QUOTE"/>
          <@inputHidden name="currencyUomId" value=quote.currencyUomId?if_exists/>
          <@inputAutoCompleteAccountRow name="partyId" id="partyId" title=uiLabelMap.CrmAccount titleClass="requiredField" styleClass="inputAutoCompleteQuick" default=quote.partyId?if_exists />
          <@inputLookupRow name="contactPartyId" title=uiLabelMap.CrmContact lookup="LookupContacts" form=formAction default=quote.contactPartyId?if_exists />
          <@inputSelectRow name="productStoreId" title=uiLabelMap.OrderProductStore list=productStores displayField="storeName" titleClass="requiredField" default=quote.productStoreId?default(defaultProductStoreId?if_exists) ignoreParameters=true />
          <@inputSelectRow name="salesChannelEnumId" title=uiLabelMap.OrderSalesChannel list=salesChannels displayField="description" key="enumId" default=quote.salesChannelEnumId titleClass="requiredField" />
          <@inputDateTimeRow name="validFromDate" title=uiLabelMap.CommonValidFromDate default=quote.validFromDate?if_exists />
          <@inputDateTimeRow name="validThruDate" title=uiLabelMap.CommonValidThruDate default=quote.validThruDate?if_exists />
          <@inputTextareaRow name="description" title=uiLabelMap.CommonDescription default=quote.description?if_exists />
          <tr>
            <td/>
            <td>
              <@inputSubmit title=uiLabelMap.CommonSave />
              <@displayLink text=uiLabelMap.CommonCancel href="ViewQuote?quoteId=${quote.quoteId}" class="buttontext" />
          </tr>
        <#else>
          <#-- Create a new quote -->
          <@inputTextRow name="quoteName" title=uiLabelMap.OrderOrderQuoteName />
          <@inputHidden name="createdByPartyId" value="${userLogin.partyId}"/>
          <@inputHidden name="quoteTypeId" value="PRODUCT_QUOTE"/>
          <@inputHidden name="currencyUomId" value=defaultCurrencyUomId?if_exists />
          <@inputHidden name="validFromDate" value=getNow() />
          <@inputAutoCompleteAccountRow name="partyId" id="partyId" title=uiLabelMap.CrmAccount titleClass="requiredField" styleClass="inputAutoCompleteQuick" />
          <@inputLookupRow name="contactPartyId" title=uiLabelMap.CrmContact lookup="LookupContacts" form=formAction />
          <@inputSelectRow name="productStoreId" title=uiLabelMap.OrderProductStore list=productStores displayField="storeName" titleClass="requiredField" default=defaultProductStoreId?if_exists ignoreParameters=true />
          <@inputSelectRow name="salesChannelEnumId" title=uiLabelMap.OrderSalesChannel list=salesChannels displayField="description" key="enumId" titleClass="requiredField" />
          <@inputDateTimeRow name="validThruDate" title=uiLabelMap.CommonValidThruDate />
          <@inputTextareaRow name="description" title=uiLabelMap.CommonDescription />
          <@inputSubmitRow title=uiLabelMap.CrmCreateQuote />
        </#if>
      </table>
    </form>
  </div>
</div>
