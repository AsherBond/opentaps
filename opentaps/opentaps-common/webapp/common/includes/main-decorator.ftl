<#include "header.ftl">
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
<#-- 
This file controls the standard layout of all opentaps applications, including the headers, footers and so on.

There are certain parameters sthat should be set in the context to help this file work:

opentapsApplicationName:  The name of the application in lowercase letters (crmsfa, financials, etc.)
configProperties:  Map containing the config parameters from a file named ${opentapsApplication}.properties
sectionName: What section of the application we are in, which is equivalent to the tab bar name (e.g., accounts, contacts, leads in crmsfa)
-->
${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#applicationTabBar")}
<div id="nav1">
  ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#navigationHistory")}
</div>
<#include "keyboard-shortcuts.ftl"/>

<div class="centerarea">
  <#include "messages.ftl">

  <div class="contentarea">

    <#if requiredPermission?exists>
      <#if !allowed?exists || (allowed?exists && allowed)>
        <#assign allowed = Static["org.opentaps.common.security.OpentapsSecurity"].checkSectionSecurity(opentapsApplicationName, sectionName, requiredPermission, request)>
      </#if>
    </#if>

    <#if !userLogin?exists>
      <#-- always render normally when no login exists, that way the login page gets rendered first thing. 
      In practice main-body should never happen because OFBIZ will always intercept you and re-direct to login page first -->
      ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#main-body")}
    <#elseif allowed?exists && !allowed>
      <div class="tableheadtext">${uiLabelMap.OpentapsError_PermissionDenied}</div>
    <#elseif applicationSetupScreen?exists && !session.getAttribute("applicationContextSet")?default(false)>
      ${screens.render(applicationSetupScreen)}
    <#else>
      ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#main-body")}
    </#if>

  </div>
</div>

</body>
</html>
