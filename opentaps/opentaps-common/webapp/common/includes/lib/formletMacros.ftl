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

<@import location="component://opentaps-common/webapp/common/includes/lib/flexAreaMacros.ftl"/>

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
  <#if !viewAll>
    <input type="button" class="paginationButton paginationFirstPage" value="" title="${uiLabelMap.pagerFirstPage}" onClick="opentaps.getFirstPage(${args})"/>
    <input type="button" class="paginationButton paginationPreviousPage" value="" title="${uiLabelMap.pagerPreviousPage}" onClick="opentaps.getPreviousPage(${args})"/>
    <input type="button" class="paginationButton paginationShrinkPage" value="" title="${uiLabelMap.pagerShrinkPage}" onClick="opentaps.changePaginationViewSize(${args},-5)"/>
    <input type="button" class="paginationButton paginationGrowPage" value="" title="${uiLabelMap.pagerGrowPage}" onClick="opentaps.changePaginationViewSize(${args},5)"/>
    <input type="button" class="paginationButton paginationNextPage" value="" title="${uiLabelMap.pagerNextPage}" onClick="opentaps.getNextPage(${args})"/>
    <input type="button" class="paginationButton paginationLastPage" value="" title="${uiLabelMap.pagerLastPage}" onClick="opentaps.getLastPage(${args})"/>
  </#if>
  <input type="button" class="paginationButton <#if viewAll>paginationUntoggleViewAll<#else>paginationToggleViewAll</#if>" value="" title="${uiLabelMap.pagerToggleViewAll}" onClick="opentaps.togglePaginationViewAll(${args})"/>
</#macro>

<#macro headerLink title orderBy orderByReverse="" blockClass="" linkClass="orderByHeaderLink">
<#if orderBy == "">
  ${title}
<#else/>
  <a href="javascript:opentaps.changePaginationOrder('${paginatorName}','${opentapsApplicationName}', '${orderBy}', '${orderByReverse}')" class="${linkClass}">${title}</a>
</#if>
</#macro>

<#macro headerCell title orderBy orderByReverse="" blockClass="" linkClass="orderByHeaderLink" width="" align="">
    <td class="${blockClass}"<#if width?has_content> width="${width}"</#if><#if align?has_content> align="${align}"</#if>><@headerLink title=title orderBy=orderBy orderByReverse=orderByReverse blockClass=blockClass linkClass=linkClass/></td>
</#macro>

<#macro navigationHeader title="">
  <@sectionHeader title=title>
    <@paginationNavContext />
  </@sectionHeader>
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
