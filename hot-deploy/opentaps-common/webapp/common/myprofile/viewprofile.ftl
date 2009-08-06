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

<#-- This page is for viewing certain profile details pertinent to users of the system. It is not to be confused with contactmech/viewprofile.ftl. -->

<div class="sectionHeader"><div class="sectionHeaderTitle">${uiLabelMap.PartyTheProfileOf}
    ${person.personalTitle?if_exists}
    ${person.firstName?if_exists}
    ${person.middleName?if_exists}
    ${person.lastName?if_exists}
    ${person.suffix?if_exists}
</div>
</div>

<!-- personal information block -->
<div class="subSectionBlock">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.PartyPersonalInformation}</div>
    <div class="subMenuBar"><a href="<@ofbizUrl>updateMyProfileForm</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonEdit}</a></div>
  </div>

  <div class="form">
    <div class="formRow">
      <span class="formLabel">${uiLabelMap.PartyName}</span>
      <span class="formInputSpan">
        ${person.personalTitle?if_exists}
        ${person.firstName?if_exists}
        ${person.middleName?if_exists}
        ${person.lastName?if_exists}
        ${person.suffix?if_exists}
      </span>
    </div>

    <div class="formRow">
      <span class="formLabel">${uiLabelMap.OpentapsUserLocale}</span>
      <span class="formInputSpan">${locale.getDisplayName(locale)}</span>
    </div>
    <div class="spacer">&nbsp;</div>
  </div>
</div>

<!-- login and password management -->
<div class="subSectionBlock">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CommonUsername} &amp; ${uiLabelMap.CommonPassword}</div>
    <div class="subMenuBar"><a href="<@ofbizUrl>changePasswordForm</@ofbizUrl>" class="subMenuButton">${uiLabelMap.OpentapsChangePassword}</a></div>
  </div>
  <div class="form">
    <div class="formRow">
      <span class="formLabel">${uiLabelMap.CommonUsername}</span>
      <span class="formInputSpan">${userLogin.userLoginId}</span>
    </div>
    <div class="spacer">&nbsp;</div>
  </div>
</div>
