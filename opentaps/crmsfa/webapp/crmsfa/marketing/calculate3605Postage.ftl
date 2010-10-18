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

<@frameSection title=uiLabelMap.CrmCalculate3605Postage>

  <form action="calculate3605Postage" method="POST">
    <@inputHidden name="contactListId" value=parameters.contactListId />
    <span class="tabletext">${uiLabelMap.CrmWeightPerPiece}:</span> <@inputText name="weightPerPiece" size="5"/>
    <@inputSubmit title="Calculate"/>
  </form>
</@frameSection>

<#if report?exists>
 <table class="crmsfaListTable">
    <tr class="headerRow">
        <@displayCell class="tableheadtext" text="Rate Zone" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Pieces" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Piece Rate" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Pieces Subtotal" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Pounds" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Pound Rate" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Pound Subtotal" />
        <@displayCell class="tableheadtext" blockClass="tabletextright" text="Grand Total" />
    </tr>
    <#list report as row>
      <tr class="${tableRowClass(row_index)}">
        <@displayCell text=row.uspsBPMRateZone />
        <@displayCell text=row.pieces?default(0) blockClass="tabletextright" />
        <@displayCell text="$${row.ratePerPiece}" blockClass="tabletextright" />
        <@displayCurrencyCell amount=row.piecesSubtotal currencyUomId="USD" />
        <@displayCell text=row.pounds?default(0) blockClass="tabletextright" />
        <@displayCell text="$${row.ratePerPound}" blockClass="tabletextright" />
        <@displayCurrencyCell amount=row.poundsSubtotal currencyUomId="USD" />
        <@displayCurrencyCell amount=row.total currencyUomId="USD" />
      </tr>
    </#list>
    <tr class="totalRow">
        <@displayCell text=uiLabelMap.CommonTotal />
        <@displayCell text=totals.totalPieces?default(0) blockClass="tabletextright" />
        <td>&nbsp;</td>
        <@displayCurrencyCell amount=totals.piecesGrandTotal currencyUomId="USD" />
        <@displayCell text=totals.totalPounds?default(0) blockClass="tabletextright" />
        <td>&nbsp;</td>
        <@displayCurrencyCell amount=totals.poundsGrandTotal currencyUomId="USD" />
        <@displayCurrencyCell amount=totals.grandTotal currencyUomId="USD" />
    </tr>
  </table>
</#if>
