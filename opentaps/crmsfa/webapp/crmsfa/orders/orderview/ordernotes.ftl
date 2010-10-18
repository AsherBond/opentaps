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

<#assign extraOptions>
  <#if security.hasEntityPermission("ORDERMGR", "_NOTE", session)>  
    <a href="<@ofbizUrl>createnewnote?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesCreateNew}</a>
  </#if>
</#assign>

<@form name="changeInternalNoteAction" url="updateOrderNote" orderId=order.orderId internalNote="" noteId=""/>
<@frameSection title=uiLabelMap.OrderNotes extra=extraOptions>
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
              <@submitFormLink form="changeInternalNoteAction" text=uiLabelMap.OrderNotesPrivate noteId=note.noteId internalNote="Y" />
            </#if>    
            <#if note.internalNote?if_exists == "Y">
	      <div class="tabletext">${uiLabelMap.OrderNotPrintableNote}</div>
              <@submitFormLink form="changeInternalNoteAction" text=uiLabelMap.OrderNotesPublic noteId=note.noteId internalNote="N" />
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
</@frameSection>

</#if>
