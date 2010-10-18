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

<#if quote?exists && canEditQuote>

<script type="text/javascript">
function addQuantityRow() {
  var table = document.getElementById("addItemTable");
  var rowCount = document.addItemToQuote.rowCount.value;

  // we need Add button on last line and Remove one on other lines 
  for (i = rowCount; i != 0; i--) {
    addButtonEl = document.getElementById('addButtonEl_' + i);
    if (addButtonEl) {
      var quantityRowId = 'quantityRow_' + i;
      opentaps.replaceNode(addButtonEl, opentaps.createAnchor('addButtonEl_' + i, 'javascript:removeQuantityRow("' + quantityRowId + '")', '<img src="/opentaps_images/buttons/glass_buttons_red_X.png" border="0" alt="${uiLabelMap.CommonRemove}"/>'));
    }
  }

  rowCount ++;
  document.addItemToQuote.rowCount.value = rowCount;
  var addButtonRow = document.getElementById("addButtonRow");
  var row = opentaps.createTableRow('quantityRow_' + rowCount);
  row.appendChild(opentaps.createTableCell());
  var cell = opentaps.createTableCell();
  cell.appendChild(opentaps.createInput(null, 'quantities_o_' + rowCount, 'text', 'inputBox', null, null, 10));
  row.appendChild(cell);
  var cell2 = opentaps.createTableCell();
  cell2.appendChild(opentaps.createInput(null, 'unitPrices_o_' + rowCount, 'text', 'inputBox', null, null, 10));
  row.appendChild(cell2);
  var cell3 = opentaps.createTableCell();
  cell3.appendChild(opentaps.createAnchor('addButtonEl_' + rowCount, 'javascript:addQuantityRow()', '<img src="/opentaps_images/buttons/glass_buttons_lumen_desi_01_plus_24x24.png" border="0" alt="${uiLabelMap.CommonAdd}"/>'));
  row.appendChild(cell3);
  table.insertBefore(row, addButtonRow);
}

function removeQuantityRow(/*String*/ id) {
  var quantityRow = document.getElementById(id);
  opentaps.removeNode(quantityRow);
}
</script>


<@frameSection title=uiLabelMap.CrmAddItems>
    <form method="post" action="<@ofbizUrl>createQuoteItem</@ofbizUrl>" name="addItemToQuote">
      <@inputHidden name="quoteId" value="${quote.quoteId}"/>
      <@inputHidden name="partyId" value=quote.partyId/>
      <@inputHidden name="rowCount" value="1"/>
      <table class="twoColumn">
        <tbody id="addItemTable">
          <tr>
            <@displayTitleCell title=uiLabelMap.ProductProduct />
            <td colspan="3">
              <@inputAutoCompleteProduct name="productId" />
            </td>
          </tr>
          <tr>
            <td/>
            <td><span class="tableheadtext"><b>${uiLabelMap.CommonQuantity}</b></span></td>
            <td><span class="tableheadtext"><b>${uiLabelMap.OrderUnitPrice}</b></span></td>
            <td/>
          </tr>
          <tr id="quantityRow_1">
            <td/>
            <@inputTextCell name="quantities_o_1" size="10" />
            <@inputTextCell name="unitPrices_o_1" size="10" />
            <td><a id="addButtonEl_1" href="javascript:addQuantityRow()"><img src="/opentaps_images/buttons/glass_buttons_lumen_desi_01_plus_24x24.png" border="0" alt="${uiLabelMap.CommonAdd}"/></a></td>            
          </tr>
          <tr id="addButtonRow"><td colspan="4"/></tr>
          <tr>
            <@displayTitleCell title=uiLabelMap.CommonDescription />
            <td colspan="3">
              <@inputText name="description" />
            </td>
          </tr>
          <tr>
            <td align="right" valign="top" class="titleCell"><span class="tableheadtext">${uiLabelMap.CommonComments}</span></td>
            <td colspan="3"><textarea rows="4" cols="50" name="comments" class="inputBox"></textarea></td>
          </tr>
          <tr>
            <td/>
            <td colspan="3">
              <@inputSubmit title=uiLabelMap.CommonAdd />
            </td>
          </tr>
        </tbody>
      </table>
    </form>
</@frameSection>

</#if>
