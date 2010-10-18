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

<#-- This is the first view screen in CRMSFA that uses the one-ftl-file-per-screen pattern.  The others should be migrated to this eventually. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if validView?default(false)>

<div class="subSectionBlock">
    <#if hasUpdatePermission?exists>
        <#assign partnerLinks><a class="subMenuButton" href="updatePartnerForm?partyId=${partySummary.partyId}">${uiLabelMap.CommonEdit}</a></#assign>
    </#if>
    <@frameSectionHeader title=uiLabelMap.OpentapsPartner extra=partnerLinks?if_exists />
    <div class="form">
        ${screens.render("component://crmsfa/widget/crmsfa/screens/partners/PartnerScreens.xml#viewPartnerForm")}
    </div>
</div>

<div class="subSectionBlock">
    <@include location="component://crmsfa/webapp/crmsfa/contactmech/viewprofile.ftl" />
</div>

<div class="subSectionBlock">
  <#if hasCreateAgreementPermission?default(false)>
    <#assign agreementLinks><a class="subMenuButton" href="createPartnerAgreement?partyIdFrom=${partySummary.partyId}">${uiLabelMap.CommonCreateNew}</a></#assign>
  </#if>
  <@frameSectionHeader title=uiLabelMap.AccountingAgreements extra=agreementLinks?if_exists />
  <@include location="component://opentaps-common/webapp/common/agreements/listAgreements.ftl"/>
</div>

${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#pendingActivities")}

${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#completedActivities")}

${screens.render("component://crmsfa/widget/crmsfa/screens/content/ContentScreens.xml#contentList")}

<div class="subSectionBlock">
  <#if hasUpdatePermission?exists>
    <#assign noteLinks><a class="subMenuButton" href="createPartnerNoteForm?partyId=${partySummary.partyId}">${uiLabelMap.CrmCreateNew}</a></#assign>
  </#if>
  <@frameSectionHeader title=uiLabelMap.CrmNotes extra=noteLinks?if_exists />
  ${screens.render("component://crmsfa/widget/crmsfa/screens/common/NoteScreens.xml#listNotesForm")}
</div>

</#if> <#-- end validView check -->
