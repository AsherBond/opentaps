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

<#if shoppingCart.size() != 0>
  <#if hasParty>
    <#assign quoteLink = "<a class='subMenuButton' href='createQuoteFromCart'>${uiLabelMap.CrmSaveAsQuote}</a>" />
    <#assign finalizeLink = "<a class='subMenuButton' href='finalizeOrder?finalizeMode=init'>${uiLabelMap.OpentapsFinalizeOrder}</a>" />
  </#if>
  <#assign newCustomerLink = "<a class='subMenuButton' href='createOrderPartyForm'>${uiLabelMap.CrmCreateNewCustomer}</a>" />
</#if>

<@frameSectionTitleBar title=uiLabelMap.OrderOrders titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_orders" extra="${newCustomerLink?if_exists}${quoteLink?if_exists}${finalizeLink?if_exists}" />

<#assign createOrderExtraButtons>
<a class="toggleButton" href="createOrderMainScreen?&amp;useGwt=Y">${uiLabelMap.OpentapsGridView}</a><span class="toggleButtonDisabled">${uiLabelMap.OpentapsFullView}</span>
</#assign>

<@frameSection title=uiLabelMap.OrderCreateOrder extra=createOrderExtraButtons>
  <form name="addOrderItemForm" method="POST" action="<@ofbizUrl>addOrderItem</@ofbizUrl>" onsubmit="return addOrderItemFormSubmitHandler(document.addOrderItemForm.productId)">
    <table class="fourColumnForm">
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductProductId titleClass="requiredField"/>
        <@inputAutoCompleteProductCell name="productId" url="getAutoCompleteProductNoVirtual" errorField="productId" tabIndex="1" />
        <td rowspan="4" valign="top">
          <table>
            <#if tagTypes?has_content>
              <@accountingTagsSelectRows tags=tagTypes tabIndex=7 />
            </#if>
          </table>
        </td>
      </tr>
      <@inputTextRow title=uiLabelMap.CommonQuantity titleClass="requiredField" name="quantity" size=10 default="1" errorField="quantity" tabIndex=2 />
      <@inputDateRow title=uiLabelMap.OrderShipBeforeDate name="shipBeforeDate" errorField="shipBeforeDate" tabIndex=3 calendarTabIndex=4/>
      <@inputTextareaRow title=uiLabelMap.CommonComment name="comments" cols=40 tabIndex=5 />
      <@inputSubmitRow title=uiLabelMap.OrderAddToOrder tabIndex=100 />
    </table>
  </form>
</@frameSection>

<script type="text/javascript">

    // check if product to add has warning and if it has asks user for confirmation
    addOrderItemFormSubmitHandler = function(productIdElement) {
        if ((productIdElement) && (productIdElement.value != "")) {
            productId = productIdElement.value;
            // use AJAX request to get the data
            opentaps.sendRequest(
                        "getProductWarningsDataJSON",
                        {"productId" : productId},
                        function(data) {getProductWarningsDataJSONResponse(productIdElement, data)}
                        );
            return false;
        } else {
            // let the server handle errors
            return true;        
        }
    }

    // from the AJAX response, alert user if product has warning and then submits addOrderItemForm upon confirmation
    getProductWarningsDataJSONResponse = function(productIdElement, warnings) {
        if (warnings.length == 0) {
            document.addOrderItemForm.submit();
        } else {
            warningMessages = '${uiLabelMap.CrmProductWarningConfirmMessage}' + '\n';
            for (i=0; i<warnings.length; i++) {
              idx = i + 1;
              warningMessages = warningMessages +'(' + idx + ')' + ' ' + warnings[i] + '\n';
            }
            opentaps.confirmAction(warningMessages, '', 'addOrderItemForm');
        }       
    }

</script>
