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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if hasDashViewPermissions>
  <div style="text-align: left">
    <img src="<@ofbizUrl>showChart?chart=${leadChartImage?if_exists?html}</@ofbizUrl>" style="margin-left: 10px" />
    <img src="<@ofbizUrl>showChart?chart=${opportunitiesbyStageImage?if_exists?html}</@ofbizUrl>" style="margin-left: 10px" />
  </div>

  <div style="text-align: left; margin-top: 20px">
    <img src="<@ofbizUrl>showChart?chart=${openCasesImage?if_exists?html}</@ofbizUrl>" style="float: left; margin-left: 10px" />
    <div style="height: 300px; width: 385px; overflow: hidden; overflow-x: hidden; overflow-y: auto">
      <img src="<@ofbizUrl>showChart?chart=${activitiesByTeamMemberImage?if_exists?html}</@ofbizUrl>" style="margin-left: 10px" />
    </div>
  </div>
</#if>

<div class="spacer"></div>
