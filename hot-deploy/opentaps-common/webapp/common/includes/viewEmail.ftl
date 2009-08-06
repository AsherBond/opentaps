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

<#if communicationEvent?exists>

<div class="form">

<#macro listRolesAndAddresses label roleList addressList>
  <div class="formRow">
    <span class="formLabelLeftSmall">${label}</span>
    <span class="formInputSpanBig">
      <#if roleList?has_content>
        <#list roleList as role>
            ${Static["com.opensourcestrategies.crmsfa.party.PartyHelper"].createViewPageLink(role.partyId, delegator, externalLoginKey)}
            <#if role.contactMechId?exists && role.contactMechId?has_content>
                <#assign infoString = role.getRelatedOne("ContactMech").get("infoString")/>
                &lt;${infoString}&gt; <#-- This is the standard convention for email addresses -->
                <#if addressList.contains(infoString)><#assign nothing = addressList.remove(infoString)/></#if>
            </#if>
            <#if role_has_next><br /></#if>
        </#list>
      </#if>
      <#if roleList?has_content && addressList?has_content><br /></#if>
      <#if addressList?has_content>
        <#list addressList?sort as address>
            <span style="margin-left:0.75em">${address}</span>
            <#if address_has_next><br /></#if>
        </#list>
      </#if>
    </span>
  </div>
</#macro>

  <@listRolesAndAddresses uiLabelMap.CommonFrom fromRoles fromAddresses/>
  <@listRolesAndAddresses uiLabelMap.CommonTo toRoles toAddresses/>
  <@listRolesAndAddresses uiLabelMap.CrmEmailCC ccRoles ccAddresses/>
  <@listRolesAndAddresses uiLabelMap.CrmEmailBCC ccRoles bccAddresses/>

  <#if communicationEvent.datetimeEnded?has_content>
  <div class="formRow">
    <span class="formLabelLeftSmall">${uiLabelMap.CommonDate}</span>
    <@displayDate class="formInputSpanBig" date=communicationEvent.datetimeEnded />
    </span>
  </div>
  </#if>
  
  <div class="formRow">
    <span class="formLabelLeftSmall">${uiLabelMap.PartySubject}</span>
    <span class="formInputSpanBig">${communicationEvent.subject?if_exists}</span>
  </div>

  <#if attachments?has_content>
  <div class="formRow">
    <span class="formLabelLeftSmall">${uiLabelMap.CrmEmailAttachments}</span>
    <span class="formInputSpanBig"><#list attachments as attachment><a href="/partymgr/control/ViewSimpleContent?contentId=${attachment.contentId}" class="linktext">${attachment.contentName}</a><br><br></#list>
    </span>
  </div>
  </#if>

  <#if communicationEvent.contentMimeTypeId?exists && communicationEvent.contentMimeTypeId == "text/html">
    <#assign textModeClass = "smallSubmit">
    <#assign htmlModeClass = "smallSubmitDisabled">
    <#assign textOnClick = "javascript:text_mode()">
    <#assign htmlOnClick = "">
  </#if>

  <div class="formRow">
    <span class="formFullRowText">
    <#if (communicationEvent.contentMimeTypeId?has_content) && (communicationEvent.contentMimeTypeId == "text/plain")>
    <textarea class="inputBox" cols="100" rows="20" readonly>${communicationEvent.content?if_exists}</textarea>
    <#else>
    <#-- wrap html emails around a little box -->
    <div style="white-space:normal">
    ${communicationEvent.content?if_exists}
    </div>
    </#if>
    </span>
  </div>

  <div class="spacer">&nbsp;</div>
</div>

<#else>

<div class="tabletext">${uiLabelMap.CrmEmailDeleted}</div>

</#if>

