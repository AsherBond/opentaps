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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<#if order?has_content>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.OrderNotes}</div>
          </td>
          <td valign="middle" align="right">
            <#if security.hasEntityPermission("ORDERMGR", "_NOTE", session)>  
              <a href="<@ofbizUrl>createnewnote?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesCreateNew}</a>
            </#if>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <#if order.notes?has_content>
            <table width="100%" border="0" cellpadding="1">
              <#list order.notes as note>
                <tr>
                  <td align="left" valign="top" width="35%">
                    <#if note.noteParty?has_content>
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonBy}: </b>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, true)}</div>
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonAt}: </b>${getLocalizedDate(note.noteDateTime)}</div>
                    <#else>
                      ${uiLabelMap.PartyUnknown}
                    </#if> 
                  </td>
                  <td align="left" valign="top" width="50%">
                    <div class="tabletext">${note.noteInfo?replace("(\r\n|\r|\n|\n\r)", "<br/>", "r")?if_exists}</div>
                  </td>
                  <td align="right" valign="top" width="15%">
                    <#if note.internalNote?if_exists == "N">
	                    <div class="tabletext">${uiLabelMap.OrderPrintableNote}</div>
                      <a href="<@ofbizUrl>updateOrderNote?orderId=${orderId}&noteId=${note.noteId}&internalNote=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesPrivate}</a>
                    </#if>    
                    <#if note.internalNote?if_exists == "Y">
	                    <div class="tabletext">${uiLabelMap.OrderNotPrintableNote}</div>
                      <a href="<@ofbizUrl>updateOrderNote?orderId=${orderId}&noteId=${note.noteId}&internalNote=N</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesPublic}</a>
                    </#if>    
                  </td>
                </tr>
                <#if note_has_next>          
                  <tr><td colspan="3"><hr class="sepbar"></td></tr>
                </#if>
              </#list>
            </table>
            <#else>            
              <div class="tabletext">&nbsp;${uiLabelMap.OrderNoNotes}.</div>
            </#if>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

</#if>
