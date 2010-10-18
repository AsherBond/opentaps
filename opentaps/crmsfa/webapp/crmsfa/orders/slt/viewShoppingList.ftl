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
   Function prepares parameters and sends request for updateStatusItem to new quantity.
*/
function updateShoppingListItemQuantity(/*String*/ shoppingListId, /*String*/ shoppingListItemSeqId, /*String*/ fieldId) {
    if (!shoppingListId || !shoppingListItemSeqId || !fieldId) return;
    var quantityField = document.getElementById(fieldId);
    if (!quantityField) return;
    var qty = quantityField.value;
    document.updateShoppingListItemQuantityForm.shoppingListId.value = shoppingListId;
    document.updateShoppingListItemQuantityForm.shoppingListItemSeqId.value = shoppingListItemSeqId;
    document.updateShoppingListItemQuantityForm.quantity.value = (qty ? qty : '0');
    document.updateShoppingListItemQuantityForm.submit();
}
</script>

<#if hasViewSltPermission>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">
    <@sectionHeader title="${uiLabelMap.CrmShoppingListDetails} ${uiLabelMap.OrderNbr}${shoppingListDetails.shoppingListId}"/>
    <table class="twoColumnForm">
        <@displayRow title=uiLabelMap.CommonCreated text=shoppingListDetails.createdStamp/>
        <@displayRow title=uiLabelMap.CommonType text=shoppingListDetails.shoppingListTypeDescription?if_exists/>
        <@displayRow title=uiLabelMap.ProductProductStore text=shoppingListDetails.productStoreName?if_exists/>
        <@displayRow title=uiLabelMap.CommonName text=shoppingListDetails.listName?if_exists/>
        <@displayRow title=uiLabelMap.PartyParty text=shoppingListDetails.partyName?if_exists/>
        <@displayRow title=uiLabelMap.CommonDescription text=shoppingListDetails.description?if_exists/>
    </table>
</div>
<@form name="updateShoppingListItemQuantityForm" url="updateShoppingListItem" shoppingListId="" shoppingListItemSeqId="" quantity="" />
<div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.EcommerceListItems/>

<#if hasUpdateSltPermission>

    <@paginate name="shoppingListItems" list=shoppingListItemBuilder>
    <#noparse>
    <@navigationBar/>
    <table class="listTable">
        <tr class="listTableHeader">
            <@headerCell title=uiLabelMap.OpentapsItemID orderBy="shoppingListItemSeqId" blockClass="fieldWidth50"/>
            <@headerCell title=uiLabelMap.ProductProductId orderBy="productId" blockClass="fieldWidth100"/>
            <@headerCell title=uiLabelMap.ProductBrandName orderBy="brandName"/>
            <@headerCell title=uiLabelMap.ProductInternalName orderBy="internalName"/>
            <@headerCell title=uiLabelMap.CommonQuantity orderBy="quantity" blockClass="fieldWidth50"/>
            <td><#-- buttons --></td>
        </tr>
        <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
              <@displayCell text=row.shoppingListItemSeqId/>
              <@displayCell text=row.productId?if_exists/>
              <@displayCell text=row.brandName?if_exists/>
              <@displayCell text=row.internalName?if_exists/>
              <td>
                  <input id="quantity_${row_index}" name="quantity_${row_index}" type="text" size="5" class="inputBox" value="${row.quantity?default(0)}">
              </td>
              <@form name="removeShoppingListItemForm_${row_index}" url="removeShoppingListItem" shoppingListId="${row.shoppingListId}" shoppingListItemSeqId="${row.shoppingListItemSeqId}"/>
              <td class="alignRight" style="width: 140px;">
                  <@displayLink text=uiLabelMap.CommonUpdate href="javascript: updateShoppingListItemQuantity('${row.shoppingListId}', '${row.shoppingListItemSeqId}', 'quantity_${row_index}');" class="buttontext"/>
                  <@submitFormLinkConfirm form="removeShoppingListItemForm_${row_index}" text=uiLabelMap.CommonRemove/> 
              </td>
            </tr>
        </#list>
    </table>
    </#noparse>
    </@paginate>

<#else>

    <@paginate name="shoppingListItems" list=shoppingListItemBuilder>
    <#noparse>
    <@navigationBar/>
    <table class="listTable">
        <tr class="listTableHeader">
            <@headerCell title=uiLabelMap.OpentapsItemID orderBy="shoppingListItemSeqId" blockClass="fieldWidth50"/>
            <@headerCell title=uiLabelMap.ProductProductId orderBy="productId" blockClass="fieldWidth100"/>
            <@headerCell title=uiLabelMap.ProductBrandName orderBy="brandName"/>
            <@headerCell title=uiLabelMap.ProductInternalName orderBy="internalName"/>
            <@headerCell title=uiLabelMap.CommonQuantity orderBy="quantity" blockClass="alignRight fieldWidth50"/>
        </tr>
        <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
              <@displayCell text=row.shoppingListItemSeqId/>
              <@displayCell text=row.productId?if_exists/>
              <@displayCell text=row.brandName?if_exists/>
              <@displayCell text=row.internalName?if_exists/>
              <@displayCell text=row.quantity?default(0) blockClass="alignRight"/>
            </tr>
        </#list>
    </table>
    </#noparse>
    </@paginate>

</#if>
</div>
</#if>