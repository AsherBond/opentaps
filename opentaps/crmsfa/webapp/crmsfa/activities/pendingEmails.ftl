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

<#assign toggleInboundView>
<a href="<@ofbizUrl>${currentPage}?viewPrefTypeId=${viewPrefOther}</@ofbizUrl>" class="subMenuButton"><#if viewPref == "EMAILS_MINE_OWNED">${uiLabelMap.CrmActivitiesAllAssigned}<#else>${uiLabelMap.CrmActivitiesOwnedByMe}</#if></a>
</#assign>

<script type="text/javascript">
<!--
  function getContactType(idx) {
    var typeSelect = document.updatePendingInboundEmailAssocs['type_o_'+idx];
    return typeSelect.options[typeSelect.selectedIndex].value;
  }

  function getCreateResponse(response) {
    opentaps.changeLocation("<@ofbizUrl>${currentPage}</@ofbizUrl>","");
  }

  function assignEmailAddressRow(idx) {
    var type = getContactType(idx);
    var company = document.updatePendingInboundEmailAssocs['company_o_'+idx].value;
    var first_name = document.updatePendingInboundEmailAssocs['first_name_o_'+idx].value;
    var last_name = document.updatePendingInboundEmailAssocs['last_name_o_'+idx].value;
    var name = document.updatePendingInboundEmailAssocs['name_o_'+idx].value;
    var email = document.updatePendingInboundEmailAssocs['email_o_'+idx].value;
    var submit_button = document.getElementById('create_contact_o_'+idx);
    submit_button.disabled = true;

    var company_valid = false;
    var first_name_valid = false;
    var last_name_valid = false;
    var name_valid = false;

    if( "LEAD" == type ) {
      company_valid = (company != "");
    } else {
      company_valid = true;
    }
    if( "ACCOUNT" == type || "PARTNER" == type ) {
      last_name_valid = true;
      first_name_valid = true;
      name_valid = (name != "");
    } else {
      last_name_valid = (last_name != "");
      first_name_valid = (first_name != "");
      name_valid = true;
    }

    if(company_valid && name_valid && first_name_valid && last_name_valid) {
      var targetUrl = '';
      var context;
      if( "ACCOUNT" == type ) {
        // ACCOUNT -> createAccount
        context = {"accountName" : name, "primaryEmail" : email };
        targetUrl = '<@ofbizUrl>createAccountJSON</@ofbizUrl>';
      } else if( "CONTACT" == type ) {
        // CONTACT -> createContact
        context = {"firstName" : first_name, "lastName" : last_name, "primaryEmail" : email };
        targetUrl = '<@ofbizUrl>createContactJSON</@ofbizUrl>';
      } else if( "LEAD" == type ) {
        // LEAD    -> createLead
        context = {"companyName" : company, "firstName" : first_name, "lastName" : last_name, "primaryEmail" : email };
        targetUrl = '<@ofbizUrl>createLeadJSON</@ofbizUrl>';
      } else if( "PARTNER" == type ) {
        // PARTNER -> createPartner
        context = {"partnerName" : name, "primaryEmail" : email };
        targetUrl = '<@ofbizUrl>createPartnerJSON</@ofbizUrl>';
      }

      if ( targetUrl != '' ) {
        opentaps.sendRequest(targetUrl, context, getCreateResponse, {target:  'paginate_pendingInboundEmails', containerClass: 'paginate_pendingInboundEmails', size: 28} );
      } else {
        alert("Unknown Type " + type);
        submit_button.disabled = false;
      }

    } else {
      alert("All fields are required.");
      submit_button.disabled = false;
    }
  }

  function contactTypeChanged(idx) {
    var type = getContactType(idx);
    var company = document.getElementById('company_o_'+idx);
    var first_name = document.getElementById('first_name_o_'+idx);
    var last_name = document.getElementById('last_name_o_'+idx);
    var name = document.getElementById('name_o_'+idx);
    if( "LEAD" == type ) {
      company.show();
    } else {
      company.hide();
    }
    if( "ACCOUNT" == type || "PARTNER" == type ) {
      name.show();
      last_name.hide();
      first_name.hide();
    } else {
      name.hide();
      last_name.show();
      first_name.show();
    }
  }
-->
</script>

<@flexAreaClassic targetId="pendingInboundEmails" title=uiLabelMap.CrmActivitiesPendingInboundEmails save=true defaultState="open" style="border:none; margin:0; padding:0" headerContent=toggleInboundView>
<@paginate name="pendingInboundEmails" list=inboundEmails teamMembers=teamMembers currentPage=currentPage>
  <#noparse>
  <@navigationBar/>
  <form name="updatePendingInboundEmailAssocs" method="POST" action="<@ofbizUrl>updatePendingInboundEmailAssocs</@ofbizUrl>">
    <table class="listTable">
      <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.CommonDate orderBy="datetimeEnded"/>
        <@headerCell title=uiLabelMap.CommonFrom orderBy="partyFromName"/>
        <@headerCell title=uiLabelMap.PartySubject orderBy="subject"/>
        <@headerCell title=uiLabelMap.CommonAssocs orderBy=""/>
        <td>&nbsp;</td> <#-- some buttons -->
        <@headerCell title=uiLabelMap.OpentapsOwner orderBy=""/>
        <@inputMultiSelectAllCell form="updatePendingInboundEmailAssocs"/>
      </tr>

      <#-- turn this into a multi form -->
      <@inputHiddenUseRowSubmit/>
      <@inputHiddenRowCount list=pageRows/>

      <#-- used for creating new contacts for unassociated email addresses -->
      <#assign contactTypes = {"LEAD":uiLabelMap.CrmLead, "CONTACT":uiLabelMap.CrmContact, "ACCOUNT":uiLabelMap.PartyAccount, "PARTNER":uiLabelMap.OpentapsPartner } >

      <#list pageRows as row>  <#-- passed in from pagination -->
        <@inputHidden name="workEffortId" value=row.workEffortId index=row_index/>
        <@inputHidden name="communicationEventId" value=row.communicationEventId index=row_index/>
        <@inputHidden name="donePage" value="${parameters.currentPage}" index=row_index/>   
        <tr class="${tableRowClass(row_index)}">
          <@displayDateCell date=row.datetimeEnded/>
          <td>
            <#if row.partyIdFromUrl?exists>
                <a href="${row.partyIdFromUrl?if_exists}">${row.partyFromName?default("")} (${row.partyIdFrom})</a>
            <#else>
                <span class="flexAreaContainer_closed" id="create_party_${row_index}_flexAreaControl" style="top:0;padding:0;margin:0;border:0;background:none;">${row.fromString?default("")}</span><a href="javascript:opentaps.expandCollapse('create_party_${row_index}')" class="buttontext">Add</a>
            </#if>
          </td> 
          <td><@displayLink href="viewActivity?workEffortId=${row.workEffortId}&amp;fromPage=${parameters.currentPage}" text=row.subject class="linktext"/></td> 
          <td><#if row.assocOrder?has_content>${uiLabelMap.OrderOrder}: <a href="<@ofbizUrl>orderview?orderId=${row.assocOrder.orderId}</@ofbizUrl>">${row.assocOrder.orderId}</a><br/></#if>
          <#if row.assocCase?has_content>${uiLabelMap.CrmCase}: <a href="<@ofbizUrl>viewCase?custRequestId=${row.assocCase.custRequestId}</@ofbizUrl>">${row.assocCase.custRequestId}</a></#if>
          </td>
          <td nowrap="nowrap" style="white-space: nowrap; width:1%">
             <a href="<@ofbizUrl>writeEmail?workEffortId=${row.workEffortId}&amp;communicationEventId=${row.communicationEventId}&amp;action=reply</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_reply.png</@ofbizContentUrl>" alt="${uiLabelMap.PartyReply}" title="${uiLabelMap.PartyReply}" border="0"></a>
             <a href="<@ofbizUrl>writeEmail?workEffortId=${row.workEffortId}&amp;communicationEventId=${row.communicationEventId}&amp;action=forward</@ofbizUrl>"><img src="<@ofbizContentUrl>/opentaps_images/buttons/bb_mail_new_forward.png</@ofbizContentUrl>" alt="${uiLabelMap.OpentapsEmailForward}" title="${uiLabelMap.OpentapsEmailForward}" border="0"></a>
             <#if row.hasDeleteEmailPermission == "true">
               <@inputConfirmImage src="/opentaps_images/buttons/glass_buttons_red_X.png" title=uiLabelMap.CrmDeleteEmail href="deleteActivityEmail?communicationEventId=${row.communicationEventId}&amp;workEffortId=${row.workEffortId}&amp;delContentDataResource=Y&amp;donePage=${parameters.currentPage}" />
             </#if>
          </td>
          <td nowrap="nowrap" style="white-space: nowrap; width:1%">
            <#if parameters.teamMembers?has_content>
            <select id="newOwnerPartyId_o_${row_index}" name="newOwnerPartyId_o_${row_index}" class="inputBox">
            <option value="" <#if !(row.activityOwnerPartyId?has_content)> selected="selected"</#if>></option>
            <#list parameters.teamMembers as option>
            {option.partyId}
              <#if option.get("partyId") == (row.activityOwnerPartyId?default(""))>
              <#assign selected = "selected"><#else><#assign selected = ""></#if>
              <option ${selected} value="${option.get("partyId")}">
              ${option.get("firstName")}&nbsp;${option.get("lastName")}
              </option>
            </#list>
            </select>
            </#if>
          </td>
          <td nowrap="nowrap" style="white-space: nowrap; width:1%">
            <#if row.hasChangeOwnerPermission == "true">
              <@inputMultiCheck index=row_index/>
            </#if>
          </td>
        </tr>
        <#if !row.partyIdFromUrl?exists>
        <tr id="create_party_${row_index}" class="${tableRowClass(row_index)}" style="display:none">
          <td>&nbsp;</td>
          <td colspan="6">
            <table>
              <@inputHidden        name="email"      value=row.fromString?default("") index=row_index />
              <@inputSelectHashRow name="type"       title=uiLabelMap.CrmCreateNew    index=row_index hash=contactTypes onChange="contactTypeChanged(${row_index});" />
              <@inputTextRow       name="first_name" title=uiLabelMap.PartyFirstName  index=row_index rowId="first_name" />
              <@inputTextRow       name="last_name"  title=uiLabelMap.PartyLastName   index=row_index rowId="last_name" />
              <@inputTextRow       name="name"       title=uiLabelMap.PartyName       index=row_index rowId="name" hidden=true />
              <@inputTextRow       name="company"    title=uiLabelMap.OpentapsCompanyName  index=row_index rowId="company" />
              <@inputButtonRow     title=uiLabelMap.CommonCreate onClick="assignEmailAddressRow(${row_index});return false;" id="create_contact_o_${row_index}" />
            </table>
          </td>
        </tr>
        </#if>
      </#list>
      <#if pageRows?size != 0>
      <tr>
        <td colspan="4">&nbsp;</td>
        <td><div id="buttonsBar"><@inputSubmit title=uiLabelMap.CrmReassign /></td></td>
        <td><div id="buttonsBar"><@inputSubmit title=uiLabelMap.CommonDelete onClick="document.forms['updatePendingInboundEmailAssocs'].action='deleteEmails'; submitFormWithSingleClick(this)" /></div></td>
        <td>&nbsp;</td>
      </tr>
      </#if>
    </table>
  </form>
  </#noparse>
</@paginate>

</@flexAreaClassic>
