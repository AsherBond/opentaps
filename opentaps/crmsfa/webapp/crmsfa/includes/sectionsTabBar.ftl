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
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@version    $Id: $
-->

<#-- This file contains the sections tab bar -->

<#if userLogin?exists>

<#-- what CSS styles to use for unselected and selected tabs -->
<#assign unselectedClass = {"col" : "tabdownblock", "left" : "tabdownleft", "center" : "tabdowncenter", "right" : "tabdownright", "link" : "tablink"}>
<#assign selectedClass = {"col" : "mainblock", "left" : "tabupleft", "center" : "tabupcenter", "right" : "tabupright", "link" : "tablinkselected"}>

<table align="center" width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr bgcolor="#FFFFFF">
    <td><div class="appbarleft"></div></td>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>

          <#-- crmSections is set in webapp/crmsfa/WEB-INF/actions/includes/main-decorator.bsh -->
          <#list crmSections as section> 
            <#if section.isExternal?exists><#assign prefix = ""><#assign suffix="?${externalKeyParam}"><#else><#assign prefix="/crmsfa/control"><#assign suffix=""></#if>
            <#if section.hasPermission>
              <#if sectionName?exists && section.sectionName == sectionName>
                <#assign class = selectedClass>
              <#else>
                <#assign class = unselectedClass>
              </#if>
              <td class="${class.col}">
                <table width="100%" border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td class="${class.left}">
                      <a href="${response.encodeURL(prefix + section.uri + suffix)}" title="" class="${class.link}">
                        <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" border="0"/>
                      </a>
                    </td>
                    <#-- this row has to be on one line so there is no padding around the text -->
                    <td nowrap="nowrap" class="${class.center}"><a href="${response.encodeURL(prefix + section.uri + suffix)}" title="" class="${class.link}">${section.uiLabel}</a></td>
                    <td class="${class.right}">
                      <a href="${response.encodeURL(prefix + section.uri + suffix)}" title="" class="${class.link}">
                        <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" border="0"/>
                      </a>
                    </td>
                  </tr>

                  <#-- colored bar underneath the tab -->
                  <#if sectionName?exists && section.sectionName != sectionName>
                  <tr><td colspan="3"><div class="tabseparatordown"></div></td></tr>
                  <#else>
                  <tr><td colspan="3"><div class="tabseparatorup"></div></td></tr>
                  </#if>
                </table>
              </td>
            </#if>
          </#list>

          <#-- right side fill -->
          <td style="width: 100%;">
              <div class="tabdownblock">&nbsp;</div>
              <div class="tabseparatordown"></div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</#if>

