<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
