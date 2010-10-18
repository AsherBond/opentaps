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
<#--/* @author: Michele Orru' (michele.orru@integratingweb.com) */-->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

 <#-- TODO: find the way to import CSS from css component folder
  For now we are getting CSS from the opentaps_css directory inside opentaps-common.
 -->

<link rel="stylesheet" href="/opentaps_css/opentaps-packed.css" type="text/css"/>
<link href="/opentaps_css/integratingweb/opentaps.css" rel="stylesheet" type="text/css" />
<link rel="shortcut icon" href="<@ofbizContentUrl>/opentaps_images/favicon.ico</@ofbizContentUrl>">


    <script type="text/javascript">
        function writeAppDetails(appId, appName, appDescr){
            var id = document.getElementById('appId');
            var name = document.getElementById('appName');
            var description = document.getElementById('appDescr');

            id.innerHTML = appId;
            name.innerHTML = appName;
            description.innerHTML = appDescr;
        }

        function forgotPasswd(){
            //if the errorDiv is present, increase the heigth of the container
            if(document.getElementById('errorDiv')){
               document.getElementById('container').style.height='540px';
               document.getElementById('form').style.height='380px';
            }

            var forgotPasswdForm = document.getElementById('forgotpasswd');
            forgotPasswdForm.style.display='block';
        }
    </script>


<title>${uiLabelMap.OpentapsProductName}</title>
</head>

<body>
<div id="top"></div>
<div id="container">
	<div id="header">
    	<div id="logo"><img src="<@ofbizContentUrl>${configProperties.get("opentaps.logo")}</@ofbizContentUrl>" /></div>
        <div id="title">
        	<h1>
                <span id="appId" style="color:#FF3300">${uiLabelMap.opentaps}</span>
                <span id="appName">${uiLabelMap.OpentapsProductName}</span>
            </h1>
        </div>
    </div>
