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

<#-- 
These macros are specific to formlets.  In particular, it provides
ways to generate the navigation context, special pagination
requests, and similar things.
-->

<#macro paginationNavContext>
  <#if renderExcelButton?exists & renderExcelButton & (pageSize > 0)>
  <a class="exportExcelButton" href="renderPaginatedListAsExcel?paginatorName=${paginatorName}&amp;opentapsApplicationName=${opentapsApplicationName}" title="${uiLabelMap.OpentapsExportToExcel}"><img src="/opentaps_images/buttons/spreadsheet.png" alt="${uiLabelMap.OpentapsPaginationExportExcelButtonLabel}" />&#160;${uiLabelMap.OpentapsPaginationExportExcelButtonLabel}</a>
  </#if>
  <span class="pageNumber">Page ${pageNumber}/${totalPages}</span>
  <#assign args = "'${paginatorName}','${opentapsApplicationName}'" />
  <input type="button" class="paginationButton paginationFirstPage" value="|<" onClick="opentaps.getFirstPage(${args})"/>
  <input type="button" class="paginationButton paginationPreviousPage" value="<" onClick="opentaps.getPreviousPage(${args})"/>
  <input type="button" class="paginationButton paginationShrinkPage" value="-" onClick="opentaps.changePaginationViewSize(${args},-5)"/>
  <input type="button" class="paginationButton paginationGrowPage" value="+" onClick="opentaps.changePaginationViewSize(${args},5)"/>
  <input type="button" class="paginationButton paginationNextPage" value=">" onClick="opentaps.getNextPage(${args})"/>
  <input type="button" class="paginationButton paginationLastPage" value=">|" onClick="opentaps.getLastPage(${args})"/>
</#macro>

<#macro headerLink title orderBy orderByReverse="" blockClass="" linkClass="orderByHeaderLink">
<#if orderBy == "">
  ${title}
<#else/>
  <a href="javascript:opentaps.changePaginationOrder('${paginatorName}','${opentapsApplicationName}', '${orderBy}', '${orderByReverse}')" class="${linkClass}">${title}</a>
</#if>
</#macro>

<#macro headerCell title orderBy orderByReverse="" blockClass="" linkClass="orderByHeaderLink">
    <td class="${blockClass}"><@headerLink title=title orderBy=orderBy orderByReverse=orderByReverse blockClass=blockClass linkClass=linkClass/></td>
</#macro>

<#macro navigationHeader title="">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${title}</div>
    <div class="subMenuBar"><@paginationNavContext /></div>
  </div>
</#macro>

<#macro navigationBar>
    <div class="navigationBar"><@paginationNavContext /></div>
</#macro>

<#macro fillEmptyRows columns>
  <#if (pageSize < viewSize)>
    <#list (pageSize+1)..viewSize as row>
      <tr class="${tableRowClass(row_index + (pageSize % 2))}"><td colspan="${columns}">&nbsp;</td></tr>
    </#list>
  </#if>
</#macro>
