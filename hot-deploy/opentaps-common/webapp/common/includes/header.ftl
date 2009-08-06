<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
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
<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Leon Torres (leon@opensourcestrategies.com)
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="Content-Script-Type" content="text/javascript"/>
    <meta http-equiv="Content-Style-Type" content="text/css"/>
    <title><#if pageTitleLabel?exists>${uiLabelMap.get(pageTitleLabel)} |</#if> ${configProperties.get(opentapsApplicationName+".title")}</title>

    <#assign appName = Static["org.ofbiz.base.util.UtilHttp"].getApplicationName(request)/>

    <#list Static["org.opentaps.common.util.UtilConfig"].getStylesheetFiles(opentapsApplicationName) as stylesheet>
      <link rel="stylesheet" href="<@ofbizContentUrl>${stylesheet}</@ofbizContentUrl>" type="text/css"/>
    </#list>

    <#-- here is where the dynamic CSS goes, for changing theme color, etc. To activate this, define sectionName = 'section' -->
    <#if sectionName?exists>
      <#assign bgcolor = Static["org.opentaps.common.util.UtilConfig"].getSectionBgColor(opentapsApplicationName, sectionName)/>
      <#assign fgcolor = Static["org.opentaps.common.util.UtilConfig"].getSectionFgColor(opentapsApplicationName, sectionName)/>
      <style type="text/css">
h1, h2, .gwt-screenlet-header, .sectionHeader, .subSectionHeader, .subSectionTitle, .formSectionHeader, .formSectionHeaderTitle, .screenlet-header, .boxhead, .boxtop, div.boxtop, .toggleButtonDisabled {color: ${fgcolor}; background-color: ${bgcolor};}
div.sectionTabBorder, ul.sectionTabBar li.sectionTabButtonSelected a {color: ${fgcolor}; background-color: ${bgcolor};}
      </style>

      <script type="text/javascript">
        var bgColor = '${bgcolor?default("")?replace("#", "")}';
      </script>
      <script src="/${appName}/control/javascriptUiLabels.js" type="text/javascript"></script>

      <#assign javascripts = Static["org.opentaps.common.util.UtilConfig"].getJavascriptFiles(opentapsApplicationName, locale)/>

      <#if layoutSettings?exists && layoutSettings.javaScripts?has_content>
        <#assign javascripts = javascripts + layoutSettings.javaScripts/>
      </#if>

      <#list javascripts as javascript>
        <#if javascript?matches(".*dojo.*")>
          <#-- Unfortunately, due to Dojo's module-loading behaviour, it must be served locally -->
          <script src="${javascript}" type="text/javascript" djConfig="isDebug: false, parseOnLoad: true <#if Static["org.ofbiz.base.util.UtilHttp"].getLocale(request)?exists>, locale: '${Static["org.ofbiz.base.util.UtilHttp"].getLocale(request).getLanguage()}'</#if>"></script>
        <#else>
          <script src="<@ofbizContentUrl>${javascript}</@ofbizContentUrl>" type="text/javascript"></script>
        </#if>
      </#list>
    </#if>

    <#if gwtScripts?exists>
      <meta name="gwt:property" content="locale=${locale}"/>
    </#if>
</head>


<body>
  <#assign callInEventIcon = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("asterisk.properties", "asterisk.icon.callInEvent")>
  <#if gwtScripts?exists>
    <#list gwtScripts as gwtScript>
      <@gwtModule widget=gwtScript />
    </#list>
    <#-- Bridge between server data and GWT widgets -->
    <script type="text/javascript" language="javascript">
      <#-- expose base permissions to GWT -->
      <#if user?has_content>
        var securityUser = new Object();
        <#list user.permissions as permission>
          securityUser["${permission}"] = true;
        </#list>
      </#if>
      <#-- set up the OpentapsConfig dictionary (see OpentapsConfig.java) -->
      var OpentapsConfig = {
      <#if configProperties.defaultCountryCode?has_content>
        defaultCountryCode: "${configProperties.defaultCountryCode}",
      </#if>
      <#if configProperties.defaultCountryGeoId?has_content>
        defaultCountryGeoId: "${configProperties.defaultCountryGeoId}",
      </#if>
      <#if configProperties.defaultCurrencyUomId?has_content>
        defaultCurrencyUomId: "${configProperties.defaultCurrencyUomId}",
      </#if>
      <#if callInEventIcon?has_content>
        callInEventIcon: "${callInEventIcon}",
      </#if>      
        applicationName: "${opentapsApplicationName}"
      };
    </script>
  </#if>

  <div style="float: left; margin-left: 10px; margin-top: 5px; margin-bottom: 10px;">
    <img alt="${configProperties.get(opentapsApplicationName+".title")}" src="<@ofbizContentUrl>${configProperties.get("opentaps.logo")}</@ofbizContentUrl>"/>
  </div>
  <div align="right" style="margin-left: 300px; margin-right: 10px; margin-top: 10px;">

    <div class="insideHeaderText">
      <#if person?has_content>
        ${uiLabelMap.CommonWelcome}&nbsp;${person.firstName?if_exists}&nbsp;${person.lastName?if_exists}
      <#elseif partyGroup?has_content>
        ${uiLabelMap.CommonWelcome}&nbsp;${partyGroup.groupName?if_exists}
      <#else>
      </#if>
      <#if requestAttributes.userLogin?has_content>
        <#if enableInternalMessaging?default(false)>${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#newMessageSummary")}</#if>
        <a href="<@ofbizUrl>myProfile</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyProfile}</a>
        <a href="<@ofbizUrl>logout</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonLogout}</a>
      </#if>
    </div>
    <#if applicationSetupFacility?has_content>
      <div class="insideHeaderSubtext">
        <b>${uiLabelMap.OpentapsWarehouse}</b>:&nbsp;${applicationSetupFacility.facilityName}&nbsp; (<@displayLink text="${uiLabelMap.CommonChange}" href="selectFacilityForm"/>)
      </div>
    </#if>
    <#if applicationSetupOrganization?has_content>
      <div class="insideHeaderSubtext">
        <b>${uiLabelMap.ProductOrganization}</b>:&nbsp;${applicationSetupOrganization.groupName}&nbsp; (<@displayLink text="${uiLabelMap.CommonChange}" href="selectOrganizationForm"/>)
      </div>
    </#if>
    <div class="gwtAsteriskNotification" id="gwtAsteriskNotification">
    </div>
    <#assign helpUrl = Static["org.opentaps.common.util.UtilCommon"].getUrlContextHelpResource(delegator, appName, parameters._CURRENT_VIEW_, screenState?default(""))!/>
      <div class="liveHelp">
        <#if keyboardShortcuts?has_content>
          <a class="buttontext" style="vertical-align:text-top;" href="javascript:showKeyboardShortcutsHelp();" title="${uiLabelMap.OpentapsHelpShortcuts}">${uiLabelMap.OpentapsHelpShortcuts}</a>
        </#if>
        <#if helpUrl?exists && helpUrl?has_content><a class="liveHelp" href="${helpUrl}" target="_blank" title="${uiLabelMap.OpentapsHelp}"><img src="/opentaps_images/buttons/help_ofbiz_svn.gif" width="20" height="20"/></a>
        </#if>
      </div>
  </div>
  <div class="spacer"></div>

