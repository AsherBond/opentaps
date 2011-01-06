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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- Determine what the send and save targets should be.  Note that some types of email have no save feature. -->
<#if marketingCampaigns?exists>
    <#assign sendTarget = "sendMarketingCampaignEmail">
<#else>
    <#assign sendTarget = "sendActivityEmail">
    <#assign saveTarget = "saveActivityEmail">
</#if>

<#assign htmlMode = ! (parameters.contentMimeTypeId?default("text/html") == "text/plain")>

<script type="text/javascript">
/*<![CDATA[*/
  <#if saveTarget?exists>
    function save_for_later() {
      document.writeEmailForm.action = "<@ofbizUrl>${saveTarget}</@ofbizUrl>";
      document.writeEmailForm.submit();
    }
  </#if>

  function getMergedForm() {
    var toEmailEl = document.getElementById('toEmail');
    var toEmail = toEmailEl ? toEmailEl.value : null;
    var templateEl = document.getElementById('mergeFormId');
    var mergeFormId = templateEl ? templateEl.options[templateEl.selectedIndex].value : null;
    if (! mergeFormId) return false;

    var context = {"mergeFormId" : mergeFormId };
    if (toEmail) context['toEmail'] = toEmail;
    var orderEl = document.getElementById('orderId');
    if (orderEl && orderEl.value) context['orderId'] = orderEl.value;
    <#-- For marketing emails the substitution is only completed during sending, so do not highlight template error on load -->
    <#if marketingCampaigns?exists>context['highlightTags'] = 'N'; </#if>

    opentaps.sendRequest('<@ofbizUrl>getMergedFormForEmailJSON</@ofbizUrl>', context, replaceText, {target: 'templateSpinner'}, true, 10000);

  }

  function replaceText(/* Array */ data) {
    if (! data) return;
    if (data.mergeFormText) opentaps.replaceHtmlEditorValue('htmlArea', data.mergeFormText);
    if (! data.subject) return;
    var subjInput = document.getElementById('subject');
    if (! subjInput) return;
    subjInput.value = data.subject ;
  }

/*]]>*/
</script>

<div class="form">

<#-- for the remove attachement button -->
<@form name="removeEmailAttachmentForm" url="removeEmailAttachment" workEffortId="" communicationEventId="" contentId="" dataResourceId="" fromDate="" />

<form name="writeEmailForm" id="writeEmailForm" method="POST" action="<@ofbizUrl>${sendTarget}</@ofbizUrl>" ${marketingCampaigns?exists?string("","enctype=\"multipart/form-data\"")}>
  <@inputHidden name="contactMechIdTo" value=parameters.contactMechIdTo?if_exists />
  <@inputHidden name="partyId" value=partyId?if_exists /> <#-- for passing to viewAccount/viewContact/viewLead when form finishes -->
  <@inputHidden name="orderId" value=parameters.orderId?if_exists />
  <@inputHidden name="donePage" value=parameters.donePage?if_exists />
  <@inputHidden name="contentMimeTypeId" value=parameters.contentMimeTypeId?default("text/html") />
  <@inputHidden name="datetimeStarted" value=getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()) /> <#-- for now we hardcode the start time -->
  <#-- these our set if we have a saved email -->
  <@inputHidden name="communicationEventId" value=parameters.communicationEventId?if_exists />
  <@inputHidden name="workEffortId" value=parameters.workEffortId?if_exists />
  <#-- pass-through parameters for donePage, so we can go to, for instance, viewContactList?contactListId=${parameters.contactListId} -->
  <#if parameters.contactListId?exists>
  <@inputHidden name="contactListId" value=parameters.contactListId />
  </#if>
  <#-- for switching HTML/TEXT and keeping the "from" mail selection -->
  <@inputHidden name="fromEmail" value=parameters.fromEmail?if_exists />
  <#-- original CommunicationEvent, used to relate emails when replying or forwarding -->
  <@inputHidden name="origCommEventId" value=parameters.origCommEventId?if_exists />

  <#if marketingCampaigns?exists>
    <@inputHidden name="emailType" value="MKTG_CAMPAIGN" />

    <#-- marketingCampaignId also doubles as a pass-through parameter for the donePage -->
    <div class="formRow">
      <span class="formLabelRequired">${uiLabelMap.CrmMarketingCampaign}</span>
      <span class="formInputSpan">
        <select name="marketingCampaignId" class="inputBox">
          <#list marketingCampaigns as marketingCampaign>
            <#assign selected = ""/>
            <#if parameters.marketingCampaignId?exists && parameters.marketingCampaignId == marketingCampaign.marketingCampaignId><#assign selected = "selected"/></#if>
            <option value="${marketingCampaign.marketingCampaignId}" ${selected?if_exists}>${marketingCampaign.campaignName}</option>
          </#list>
        </select>
      </span>
    </div>
  </#if>

  <#-- everything should have a from email -->
  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.CommonFrom}</span>
    <span class="formInputSpan">
      <select name="contactMechIdFrom" class="inputBox" id="fromEmailSelect">
        <#list userEmailAddresses as email>
          <#assign selected = ""/>
          <#if parameters.fromEmail?exists && parameters.fromEmail == email.infoString><#assign selected = "selected"></#if>
          <option value="${email.contactMechId}" ${selected?if_exists}>${email.infoString}</option>
        </#list>
      </select>
    </span>
  </div>

  <#-- for activities -->
  <#if !marketingCampaigns?exists>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.CommonTo}</span>
    <span class="formInputSpan">
      <input type="text" name="toEmail" id="toEmail" class="inputBox" size="60" value="<#if toAddresses?exists>${toAddresses}<#else>${parameters.toEmail?if_exists}</#if>"></input>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabel">${uiLabelMap.CrmEmailCC}</span>
    <span class="formInputSpan">
      <input type="text" name="ccEmail" class="inputBox" size="60" value="<#if ccAddresses?exists>${ccAddresses}<#else>${parameters.ccEmail?if_exists}</#if>"></input>
    </span>
  </div>

  <div class="formRow">
    <span class="formLabel">${uiLabelMap.CrmEmailBCC}</span>
    <span class="formInputSpan">
      <input type="text" name="bccEmail" class="inputBox" size="60" value="<#if bccAddresses?exists>${bccAddresses}<#else>${parameters.bccEmail?if_exists}</#if>"></input>
    </span>
  </div>

  <#if displayLinks?default(false)>
  <div class="formRow">
    <#if displayOpportunityLink?default(false)>
    <span class="formLabel">${uiLabelMap.CrmOpportunity}</span>
    <span class="formInputSpan">
      <input type="text" name="salesOpportunityId" class="inputBox" size="20" maxlength="20" value="${parameters.salesOpportunityId?if_exists}"></input>
      <a title="${uiLabelMap.CrmLookupOpportunities}" href="javascript:call_fieldlookup2(document.writeEmailForm.salesOpportunityId, 'LookupOpportunities');">
        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"></img>
      </a>
    </#if>
    <#if displayCaseLink?default(false)>
      <span class="formLabel" style="float:none; width:auto">${uiLabelMap.CrmCase}</span>
      <input type="text" name="custRequestId" class="inputBox" size="20" maxlength="20" value="${parameters.custRequestId?if_exists}"></input>
      <a title="${uiLabelMap.CrmLookupCases}" href="javascript:call_fieldlookup2(document.writeEmailForm.custRequestId, 'LookupCases');">
        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"></img>
      </a>
    </#if>
    <#if displayOrderLink?default(false)>
      <span class="formLabel" style="float:none; width:auto">${uiLabelMap.OrderOrder}</span>
      <input type="text" name="orderId" id="orderId" class="inputBox" size="20" maxlength="20" value="${parameters.orderId?if_exists}"></input>
      <a title="${uiLabelMap.CrmFindOrders}" href="javascript:call_fieldlookup2(document.writeEmailForm.orderId, 'LookupSalesOrders');">
        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"></img>
      </a>
    </span>
    </#if>
    </span>
  </div>
  </#if>

  </#if>
  
  <#if templates?has_content && displayTemplates?default(false)>
      <div class="formRow">
        <span class="formLabel">${uiLabelMap.CrmTemplate}</span>
        <span class="formInputSpan">
          <select name="mergeFormId" id="mergeFormId" class="inputBox" onchange="getMergedForm()">
            <option value=""></option>
            <#list templates as template>
              <option value="${template.mergeFormId}" <#if template.mergeFormId == parameters.mergeFormId?default("")>selected="selected"</#if>>${template.mergeFormName}</option>
            </#list>
          </select>
          <span id="templateSpinner">&nbsp;</span>
        </span>
      </div>
  </#if>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.PartySubject}</span>
    <span class="formInputSpan">
      <input type="text" id="subject" name="subject" class="inputBox" size="60" value="${parameters.subject?if_exists}"></input>
    </span>
  </div>

  <#if attachments?has_content>
  <div class="formRow">
    <span class="formLabel">${uiLabelMap.CrmEmailAttachments}</span>
    <span class="formInputSpan">
      <table>
        <#list attachments as attachment>
          <tr>
            <td>
              <a href="/partymgr/control/ViewSimpleContent?contentId=${attachment.contentId}" class="buttontext">${attachment.contentName}</a>
            </td>
            <td>
              <#assign removeAttText><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonRemove}"/></#assign>
              <@submitFormLink form="removeEmailAttachmentForm" workEffortId=parameters.workEffortId communicationEventId=attachment.communicationEventId contentId=attachment.contentId dataResourceId=attachment.dataResourceId! fromDate=attachment.fromDate?datetime text=removeAttText class=""/>
            </td>
          </tr>
        </#list>
      </table>
    </span>
  </div>
  </#if>

  <div class="formRow">
    <span class="formLabelRequired">${uiLabelMap.CommonMessage}</span>
    <span class="formInputSpan">
      <#assign content = parameters.content?default(templateText?default(""))/>
      <@htmlTextArea textAreaId="htmlArea" name="content" class="inputBox" value=content rows=15 cols=90/>
    </span>
  </div>

<#if ! marketingCampaigns?exists>
    
      <script type="text/javascript">
          attachmentCount = 1 ;
          function addAttachment() {
              var divId = 'attachment_' + attachmentCount ;
              var html = '<div id="' + divId + '"><input name="uploadedFile_' + attachmentCount + '" type="file" class="inputBox" size="60"/><a onClick=removeAttachment("attachment_' + attachmentCount + '")><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CrmEmailAttachmentRemove?js_string}"/></a>' ;
              new Insertion.Before( 'addButton' , html ) ;
              attachmentCount++ ;
          }
          
          function removeAttachment( toRemove ) {
            $(toRemove).remove() ;
          }
      </script>
      
      <input type="hidden" name="contentTypeId" value="FILE"/>
      <div class="formRow">
        <span class="formLabel">${uiLabelMap.CrmEmailAttachments}</span>
        <span class="formInputSpan">
          <div id="attachmentPaths">
            <div id="attachment_0">
              <input name="uploadedFile_0" type="file" class="inputBox" size="60"/>
              <a onClick="removeAttachment('attachment_0')">
              <img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CrmEmailAttachmentRemove}"/>
              </a>                 
            </div>
            <input type="button" class="smallSubmit" onClick="addAttachment()" value="${uiLabelMap.CrmEmailAttachmentAdd}" id="addButton"></input>
          </div>
        </span>        
      </div>

</#if>

  <div class="formRow">
    <span class="formInputSpan">
      <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSend}"></input>
      <#if saveTarget?exists>
        <input type="button" class="smallSubmit" value="${uiLabelMap.CrmSaveForLater}" onClick="javascript:save_for_later()"></input>
      </#if>
    </span>
  </div>

  <div class="spacer">&nbsp;</div>
</form>
</div>
