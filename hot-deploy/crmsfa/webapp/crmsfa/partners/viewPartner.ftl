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

<#-- This is the first view screen in CRMSFA that uses the one-ftl-file-per-screen pattern.  The others should be migrated to this eventually. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if validView?default(false)>

<div class="subSectionBlock">
    <#if hasUpdatePermission?exists>
        <#assign updateLink><a class='subMenuButton' href='updatePartnerForm?partyId=${partySummary.partyId}'>${uiLabelMap.CommonEdit}</a></#assign>
    </#if>
    <div class="subSectionHeader">
        <div class="subSectionTitle">${uiLabelMap.OpentapsPartner}</div>
        <div class="subMenuBar">${updateLink?if_exists}</div>
    </div>
    <div class="form">
        ${screens.render("component://crmsfa/widget/crmsfa/screens/partners/PartnerScreens.xml#viewPartnerForm")}
    </div>
</div>

<div class="subSectionBlock">
    <@include location="component://crmsfa/webapp/crmsfa/contactmech/viewprofile.ftl" />
</div>

<div class="subSectionBlock">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.AccountingAgreements}</div>
    <#if hasCreateAgreementPermission?default(false)>
      <div class="subMenuBar"><a class="subMenuButton" href="createPartnerAgreement?partyIdFrom=${partySummary.partyId}">${uiLabelMap.CommonCreateNew}</a></div>
    </#if>
  </div>
  <@include location="component://opentaps-common/webapp/common/agreements/listAgreements.ftl"/>
</div>

${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#pendingActivities")}

${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#completedActivities")}

${screens.render("component://crmsfa/widget/crmsfa/screens/content/ContentScreens.xml#contentList")}

<div class="subSectionBlock">
    <div class="subSectionHeader">
        <div class="subSectionTitle">${uiLabelMap.CrmNotes}</div>
        <#if hasUpdatePermission?exists>
        <div class="subMenuBar"><a class="subMenuButton" href="createPartnerNoteForm?partyId=${partySummary.partyId}">${uiLabelMap.CrmCreateNew}</a></div>
        </#if>
    </div>
    ${screens.render("component://crmsfa/widget/crmsfa/screens/common/NoteScreens.xml#listNotesForm")}
</div>

</#if> <#-- end validView check -->
