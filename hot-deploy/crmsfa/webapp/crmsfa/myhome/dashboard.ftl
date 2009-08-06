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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

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
