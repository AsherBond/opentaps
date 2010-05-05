<#function tableRowClass rowIndex rowClassOdd="rowWhite" rowClassEven="rowLightGray">
  <#return (rowIndex % 2 == 0)?string(rowClassOdd, rowClassEven)/>
</#function>

<#macro paginatorNavContext paginatorName responseFunction="">
<#if responseFunction != ""><#assign func = ", " + responseFunction><#else><#assign func=""></#if>
    &nbsp;&nbsp;&nbsp;
  <input type="button" class="smallSubmit" value="|<" onClick="opentaps.getFirstPage('${paginatorName}' ${func})">
  <input type="button" class="smallSubmit" value="<" onClick="opentaps.getPreviousPage('${paginatorName}' ${func})">
  <input type="button" class="smallSubmit" value="5" onClick="opentaps.getPageNumber('${paginatorName}', 5 ${func})">
    <input type="button" class="smallSubmit" value="+" onClick="opentaps.changePaginationViewSize('${paginatorName}', 1 ${func})">
    <input type="button" class="smallSubmit" value="-" onClick="opentaps.changePaginationViewSize('${paginatorName}', -1 ${func})">
  <input type="button" class="smallSubmit" value=">" onClick="opentaps.getNextPage('${paginatorName}' ${func})">
  <input type="button" class="smallSubmit" value=">|" onClick="opentaps.getLastPage('${paginatorName}' ${func})">
    &nbsp;&nbsp;&nbsp;
  <input type="button" class="smallSubmit" value="Status" onClick="opentaps.changePaginationOrder('${paginatorName}', 'description' ${func})">
  <input type="button" class="smallSubmit" value="Type" onClick="opentaps.changePaginationOrder('${paginatorName}', 'statusTypeId,description' ${func})">
</#macro>



<#-- Example of a formlet to render a list of status items with special data -->
<#assign paginator = session.getAttribute("customStatusPaginator")>
<#assign pageRows = request.getAttribute("pageRows")>

<div class="subSectionHeader">
    <div class="subSectionTitle">Non Cancelled Status Items <@paginatorNavContext paginatorName='customStatusPaginator'/></div>
    <div class="subMenuBar" id="customStatusPaginatorInfo">Page ${paginator.getPageNumber()}/${paginator.getTotalPages()}</div>
</div>
<table class="listTable">
  <#list pageRows as row>
    <tr class="${tableRowClass(row_index)}"><td>${row.description}</td><td>${row.statusTypeDescription?default(row.statusTypeId)}</td></tr>
  </#list>
</table>
