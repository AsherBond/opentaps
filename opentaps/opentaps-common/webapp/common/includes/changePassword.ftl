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

<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<style type="text/css">
<#assign fgcolor = "#FFFFFF"/>
<#assign bgcolor = "#000099"/>
.x-panel-tl, .x-panel-tr, .titleBar .x-panel-br, .titleBar .x-panel-bl { background-image:url(/opentaps_images/panels/corners-sprite-${bgcolor?replace("#", "")}.gif) !important; }
.x-panel-tc, .titleBar .x-panel-bc { background-image:url(/opentaps_images/panels/top-bottom-${bgcolor?replace("#", "")}.gif) !important; }
.x-panel-tl .x-panel-header, .frameSectionHeader .pageNumber {color: ${fgcolor} !important; }
.x-panel-noborder .x-panel-header-noborder { border:none !important; }
<#-- center the section titles -->
.x-panel-header {float:none !important;}
</style>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="form" style="padding-top: 125px; padding-bottom: 125px;">

<#assign username = requestParameters.USERNAME?default((sessionAttributes.autoUserLogin.userLoginId)?default(""))>

<div align="center">
    <@frameSection title="${uiLabelMap.CommonPasswordChange}" style="width: 300px; margin-left: auto; margin-right: auto;text-align: center;" innerStyle="text-align: center;">
    <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform">
      <input type="hidden" name="requirePasswordChange" value="Y"/>
      <input type="hidden" name="USERNAME" value="${username}"/>
      <table cellspacing="0">
        <tr>
          <td align="right"><@display text="${uiLabelMap.CommonCurrentPassword}&nbsp;" class="tabletext"/></td>
          <td align="left"><input type="password" name="PASSWORD" value="" size="20" class="inputBox"/></td>
        </tr>
        <tr>
          <td align="right"><@display text="${uiLabelMap.CommonNewPassword}&nbsp;" class="tabletext"/></td>
          <td align="left"><input type="password" name="newPassword" value="" size="20" class="inputBox"/></td>
        </tr>
        <tr>
          <td align="right"><@display text="${uiLabelMap.OpentapsConfirmPassword}&nbsp;" class="tabletext"/></td>
          <td align="left"><input type="password" name="newPasswordVerify" value="" size="20" class="inputBox"/></td>
        </tr>
        <tr>
          <td colspan="2" align="center">
            <input type="submit" value="${uiLabelMap.PartyChangePassword}" class="loginButton"/>
          </td>
        </tr>
      </table>
    </form>
    </@frameSection>
</div>

<script language="JavaScript" type="text/javascript">
  document.loginform.PASSWORD.focus();
</script>

</div>