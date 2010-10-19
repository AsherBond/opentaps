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

<div class="screenlet">
    <div class="screenlet-header">
        <span class="boxhead">${uiLabelMap.FinancialsSelectOrganization}</span>
    </div>
    <div class="screenlet-body">
        ${screens.render("component://financials/widget/financials/screens/common/CommonScreens.xml#selectOrganizationScreen")}
    </div>
    <#if security.hasEntityPermission("FINANCIALS", "_CONFIG", userLogin) && unconfiguredOrganizations?has_content>
        <div class="screenlet-header">
            <span class="boxhead">${uiLabelMap.FinancialsConfigureNewOrganization}</span>
        </div>
        <div class="screenlet-body">
            ${screens.render("component://financials/widget/financials/screens/common/CommonScreens.xml#selectOrganizationToConfigureScreen")}
        </div>
    </#if>
</div>
