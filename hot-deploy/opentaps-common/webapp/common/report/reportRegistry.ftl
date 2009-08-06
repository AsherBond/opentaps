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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- AG15012008 : the report table height is fixed. We should make the table height depend on the number of reports -->
<div class="tabletext" style="margin-bottom:250px;">
    <#if reportsGroupedList?has_content>
        <#list reportsGroupedList as group>
            <p><b>${group.description}</b>
            <#assign reports = group.reports/>
            <ul class="bulletList">
            <#list reports?default([]) as report>
                <li>
                  <a href="<@ofbizUrl>setupReport?reportId=${report.reportId}</@ofbizUrl>">${report.shortName}</a><#if report.description?has_content>: ${report.description}</#if>                
                </li>
            </#list>
            </ul>
            </p>
        </#list>
    </#if>

</div>
