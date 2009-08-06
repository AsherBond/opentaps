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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="Content-Script-Type" content="text/javascript"/>
    <meta http-equiv="Content-Style-Type" content="text/css"/>
    <#if screenAction?has_content>
        <title>${uiLabelMap.OpentapsComposeMessage}</title>
    <#else>
        <title>${uiLabelMap.OpentapsViewMessage}</title>
    </#if>
    
    <script src="/${opentapsApplicationName}/control/javascriptUiLabels.js" type="text/javascript"></script>
    <#assign javascripts = Static["org.opentaps.common.util.UtilConfig"].getJavascriptFiles(opentapsApplicationName, locale)>
    <#if layoutSettings?exists && layoutSettings.javaScripts?has_content>
        <#assign javascripts = javascripts + layoutSettings.javaScripts/>
    </#if>
    
    <#list javascriptFiles as javascript>
      <#if javascript?matches(".*dojo.*")>
        <#-- Unfortunately, due to Dojo's module-loading behaviour, it must be served locally -->
        <script src="${javascript}" type="text/javascript"></script>
      <#else>
        <script src="<@ofbizContentUrl>${javascript}</@ofbizContentUrl>" type="text/javascript"></script>
      </#if>
    </#list>
    
    <#list stylesheetFiles as stylesheet>
      <link rel="stylesheet" href="<@ofbizContentUrl>${stylesheet}</@ofbizContentUrl>" type="text/css"/>
    </#list>
    
</head>
<body class="internalMessage" onunload="window.helper.onClose(<#if parameters.communicationEventId?has_content>${parameters.communicationEventId}</#if>)">
