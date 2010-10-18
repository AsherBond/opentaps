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

<div class="subSectionBlock">
    <#assign sectionTitleLabel = reportHeaderUiLabel?if_exists/>
    <#assign title>${uiLabelMap.get(sectionTitleLabel)}<#if reportDatasource?has_content>&nbsp;(${reportDatasource?size} ${uiLabelMap.AmazonProductErrorReportProblems})</#if></#assign>
	<@sectionHeader title=title/>
    <#if reportDatasource?has_content>
        <table class="listTable" cellspacing="0" cellpadding="2" width="100%">
            <tr class="listTableHeader">
            <#list columnOrder as headerTitle>
            <#assign columnTitleUiLabel = headerTitle.get("title")?if_exists/>
                <@displayCell text=uiLabelMap.get(columnTitleUiLabel)/>
            </#list>
            </tr>
            <#list reportDatasource as item>
                <tr class="${tableRowClass(item_index)}">
                    <#list columnOrder as columnDescriptor>
                    	<@displayCell text=item.get(columnDescriptor.get("field")) blockStyle="width: ${columnDescriptor.width?default('20%')};"/>
                    </#list>
                </tr>
            </#list>
        </table>
    <#else>
	    <#-- "no errors" section -->
	    <table><tr><@displayCell text="${uiLabelMap.AmazonProductErrorReportNoErrors}"/></tr></table>
    </#if>
</div>