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
<div class="form">
<div class="tabletext" style="margin-bottom: 30px;">
<p class="tableheadtext">${uiLabelMap.FinancialsPaychecks}</p>
<ul class="bulletList">
 <li class="tabletext"><a href="<@ofbizUrl>createPaycheckForm</@ofbizUrl>">${uiLabelMap.FinancialsCreatePaycheck}</a></li>
</ul>
<ul class="bulletList">
 <li class="tabletext"><a href="<@ofbizUrl>findPaycheck</@ofbizUrl>">${uiLabelMap.FinancialsFindPaycheck}</a></li>
</ul>
<#if checkPaymentMethods?has_content>
    <ul class="bulletList">
    <#list checkPaymentMethods as paymentMethod>
        <li class="tabletext"><a href="<@ofbizUrl>listPaychecksToPrint?paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.FinancialsPrintPaychecksFor} ${paymentMethod.description?if_exists} (${paymentMethod.paymentMethodId})</a></li>
    </#list>
    </ul>
</#if>
</div>
