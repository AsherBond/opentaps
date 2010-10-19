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
