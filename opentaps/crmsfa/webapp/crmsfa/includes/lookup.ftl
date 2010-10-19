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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<!-- Copyright (c) 2005-2006 Open Source Strategies Inc. -->
<!-- Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org -->
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
 *@author     Leon Torres (leon@opensourcestrategies.com)
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Rev: 314 $
 *@since      3.0
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="Content-Script-Type" content="text/javascript"/>
    <meta http-equiv="Content-Style-Type" content="text/css"/>
    <title>${uiLabelMap.get(pageTitleLabel)}</title>
    <script src="/crmsfa/control/javascriptUiLabels.js" type="text/javascript"></script>     
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

    <script type="text/javascript">
        // This code inserts the value lookedup by a popup window back into the associated form element
        var re_id = new RegExp('id=(\\d+)');
        var num_id = (re_id.exec(String(window.location))
                ? new Number(RegExp.$1) : 0);
        var obj_caller = (window.opener ? window.opener.lookups[num_id] : null);
        if (obj_caller == null) 
            obj_caller = window.opener;
        
        
        // function passing selected value to calling window
        function set_value(value) {
                if (!obj_caller) return;
                window.close();
                obj_caller.target.value = value;
                if (obj_caller.targetHidden == null) return;
                obj_caller.targetHidden.value = value;
                obj_caller.target.focus();
        }
        // function passing selected value to calling window
        function set_values(value, value2) {
                set_value(value);
                if (!obj_caller.target2) return;
                if (obj_caller.target2 == null) return;
                obj_caller.target2.value = value2;
        }
        function set_multivalues(value) {
            obj_caller.target.value = value;
            var thisForm = obj_caller.target.form;
            var evalString = "";
             
    		if (arguments.length > 2 ) {
        		for(var i=1; i < arguments.length; i=i+2) {
        			evalString = "thisForm." + arguments[i] + ".value='" + arguments[i+1] + "'";
        			eval(evalString);
        		}
    		}
    		window.close();
         }
    </script>

    <script type="text/javascript">
        // This code set the timeout default value for opentaps.sendRequest
        var ajaxDefaultTimeOut = ${configProperties.get("opentaps.ajax.defaultTimeout")};
    </script>

    <#if gwtScripts?exists>
      <meta name="gwt:property" content="locale=${locale}"/>
    </#if>
</head>
<body class="lookupBody">

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
