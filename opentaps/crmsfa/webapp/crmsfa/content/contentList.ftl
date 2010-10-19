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

<#-- A parametrized list of content attachments. -->

<#assign addContentUrlTarget = ""/>
<#assign uploadContentTarget = ""/>
<#assign updateContentTarget = ""/>
<#assign downloadLink = ""/>
<#assign objectIdParam = ""/>
<#if !donePage?exists>
  <#assign donePage = parameters.donePage/>
</#if>
<#if donePage == "viewAccount">
  <#assign objectIdParam = "partyId=" + parameters.partyId?default("")/>
  <#assign objectIdParamJs = "partyId':'" + parameters.partyId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForAccount?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForAccount?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForAccountForm?" + objectIdParam/>
  <#assign downloadLink = "downloadPartyContent?" + objectIdParam/>
<#elseif donePage == "viewContact">
  <#assign objectIdParam = "partyId=" + parameters.partyId?default("")/>
  <#assign objectIdParamJs = "partyId':'" + parameters.partyId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForContact?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForContact?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForContactForm?" + objectIdParam/>
  <#assign downloadLink = "downloadPartyContent?" + objectIdParam/>
<#elseif donePage == "viewLead">
  <#assign objectIdParam = "partyId=" + parameters.partyId?default("")/>
  <#assign objectIdParamJs = "partyId':'" + parameters.partyId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForLead?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForLead?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForLeadForm?" + objectIdParam/>
  <#assign downloadLink = "downloadPartyContent?" + objectIdParam/>
<#elseif donePage == "viewPartner">
  <#assign objectIdParam = "partyId=" + parameters.partyId?default("")/>
  <#assign objectIdParamJs = "partyId':'" + parameters.partyId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForPartner?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForPartner?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForPartnerForm?" + objectIdParam/>
  <#assign downloadLink = "downloadPartyContent?" + objectIdParam/>
<#elseif donePage == "viewCase">
  <#assign objectIdParam = "custRequestId=" + parameters.custRequestId?default("")/>
  <#assign objectIdParamJs = "custRequestId':'" + parameters.custRequestId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForCase?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForCase?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForCaseForm?" + objectIdParam/>
  <#assign downloadLink = "downloadCaseContent?" + objectIdParam/>
<#elseif donePage == "viewOpportunity">
  <#assign objectIdParam = "salesOpportunityId=" + parameters.salesOpportunityId?default("")/>
  <#assign objectIdParamJs = "salesOpportunityId':'" + parameters.salesOpportunityId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForOpportunity?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForOpportunity?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForOpportunityForm?" + objectIdParam/>
  <#assign downloadLink = "downloadOpportunityContent?" + objectIdParam/>
<#elseif donePage == "viewActivity">
  <#assign objectIdParam = "workEffortId=" + parameters.workEffortId?default("")/>
  <#assign objectIdParamJs = "workEffortId':'" + parameters.workEffortId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForActivity?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForActivity?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForActivityForm?" + objectIdParam/>
  <#assign downloadLink = "downloadActivityContent?" + objectIdParam/>
<#elseif donePage == "orderview">
  <#assign objectIdParam = "orderId=" + parameters.orderId?default("")/>
  <#assign objectIdParamJs = "orderId':'" + parameters.orderId?default("")/>
  <#assign addContentUrlTarget = "addContentUrlForOrder?" + objectIdParam/>
  <#assign uploadContentTarget = "uploadContentForOrder?" + objectIdParam/>
  <#assign updateContentTarget = "updateContentForOrderForm?" + objectIdParam/>
  <#assign downloadLink = "downloadOrderContent?" + objectIdParam/>
<#elseif donePage == "ViewQuote">
  <#assign objectIdParam = "quoteId=" + parameters.quoteId?default("")>
  <#assign objectIdParamJs = "quoteId':'" + parameters.quoteId?default("")>
  <#assign addContentUrlTarget = "addContentUrlForQuote?" + objectIdParam>
  <#assign uploadContentTarget = "uploadContentForQuote?" + objectIdParam>
  <#assign updateContentTarget = "updateContentForQuoteForm?" + objectIdParam>
  <#assign downloadLink = "downloadQuoteContent?" + objectIdParam>
</#if>

<#assign extraOptions>
  <#if hasUpdatePermission?exists && hasUpdatePermission>
    <#if addContentUrlTarget != "" >
      <a class="subMenuButton" href="<@ofbizUrl>${addContentUrlTarget}</@ofbizUrl>">${uiLabelMap.CrmAddUrl}</a>
    </#if>
    <#if uploadContentTarget != "" >
      <a class="subMenuButton" href="<@ofbizUrl>${uploadContentTarget}</@ofbizUrl>">${uiLabelMap.CrmUploadFile}</a>
    </#if>
  </#if>
</#assign>

<@frameSectionHeader title=uiLabelMap.CrmContentList extra=extraOptions/>

<table class="crmsfaListTable">
  <tbody>
    <tr class="crmsfaListTableHeader">
      <td class="tableheadtext">${uiLabelMap.CommonName}</td>
      <td class="tableheadtext">${uiLabelMap.OpentapsContentClassification}</td>
      <td class="tableheadtext">${uiLabelMap.CommonDescription}</td>
      <td class="tableheadtext">${uiLabelMap.ProductCreatedDate}</td>
      <#if hasUpdatePermission?exists && hasUpdatePermission>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
      </#if>
    </tr>

    <#if content?exists && content.size() != 0>
    <#assign rowCount = 0/>
    <#list content as item>
      <#assign rowClass = "rowLightGray"/>
      <#if rowCount % 2 == 0><#assign rowClass = "rowWhite"/></#if>

      <#assign hyperlink = false/>
      <#assign file = false/>
      <#assign data = item.getRelatedOne("DataResource")/>
      <#assign classification = item.getRelatedOneCache("Enumeration")?if_exists/>
      <#if data?exists && data.objectInfo?has_content>
        <#if item.contentTypeId == "HYPERLINK"><#assign hyperlink = true/>
        <#elseif item.contentTypeId == "FILE"><#assign file = true/>
        </#if>
      </#if>

    <tr class="${rowClass}">
      <td class="tabletext">
        <#if hyperlink><a target="top" class="linktext" href="${data.objectInfo}"></#if>
        <#if file><a class="linktext" href="<@ofbizUrl>${downloadLink}&contentId=${item.contentId}</@ofbizUrl>"></#if>
        ${item.contentName?if_exists}
        <#if (hyperlink || file)></a></#if>
      </td>
      <td class="tabletext">${classification.description?if_exists}</td>
      <td class="tabletext">${item.description?if_exists}</td>
      <td class="tabletext"><@displayDate date=item.createdDate/></td>
      <#if hasUpdatePermission?exists && hasUpdatePermission>
      <td class="tabletext"><@form name="removeContent_${item.contentId}" url="removeContent" contentId="${item.contentId}" partyId="" custRequestId="" workEffortId="" salesOpportunityId="" orderId="" quoteId="" donePage="${donePage}"/>
      <a href="<@ofbizUrl>${updateContentTarget}&contentId=${item.contentId}&contentTypeId=${item.contentTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
      <a class="buttontext" href="javascript:opentaps.submitForm('removeContent_${item.contentId}', null, {'${objectIdParamJs}'});">${uiLabelMap.CommonRemove}</a
      </#if>
    </tr>

      <#assign rowCount = rowCount + 1/>
    </#list>
    </#if>

  </tbody>
</table>
