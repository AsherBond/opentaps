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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.OpentapsPartners}</div>
</div>
<div class="subSectionBlock">
  <@paginate name="listPartnersByPhone" list=crmPartyListBuilder rememberPage=false>
    <#noparse>
      <@navigationBar />
      <table class="crmsfaListTable">
        <tr class="crmsfaListTableHeader">
          <@headerCell title=uiLabelMap.OpentapsPartnerName orderBy="partyId" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="primaryEmail" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy="" blockClass="tableheadtext"/>
        </tr>
        <#list pageRows as crmPartner>
          <tr class="${tableRowClass(crmPartner_index)}">
            <#if crmPartner.sectionName?default("") == "lookup">
              <@displayLinkCell href="javascript:set_value('${crmPartner.partyId}')" text="${crmPartner.groupName?if_exists} (${crmPartner.partyId})"/>
            <#else/>
              <@displayLinkCell href="viewPartner?partyId=${crmPartner.partyId}" text="${crmPartner.groupName?if_exists} (${crmPartner.partyId})"/>
            </#if>
            <@displayCell text=crmPartner.abbrevPostalAddressByPurpose/>
            <@displayCell text=crmPartner.electronicAddressByPurpose/>
            <@displayCell text=formatTelecomNumber(crmPartner.entityValue)/>
          </tr>
        </#list>
      </table>
    </#noparse>
  </@paginate>
</div>
 

