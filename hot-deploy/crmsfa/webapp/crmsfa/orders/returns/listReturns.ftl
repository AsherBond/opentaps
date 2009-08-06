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
        
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">

<@paginate name=paginateName list=findReturnsBuilder rememberPage=rememberPage>
    <#noparse>
        <@navigationHeader title=uiLabelMap.CrmReturns />
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.OrderReturnId orderBy="returnId" />
                <@headerCell title=uiLabelMap.CrmReturnFromCustomer orderBy="fromPartyId" />
                <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" />
                <@headerCell title=uiLabelMap.OpentapsDateRequested orderBy="entryDate" />
            </tr>
            <#list pageRows as return>
              <tr class="${tableRowClass(return_index)}">
                <@displayLinkCell text=return.returnId href="viewReturn?returnId=${return.returnId}" />
                <@displayCell text=return.returnPartyName />
                <@displayCell text=return.statusDescription />
                <@displayDateCell date=return.entryDate />
              </tr>
            </#list>
        </table>
    </#noparse>
</@paginate>

</div>
