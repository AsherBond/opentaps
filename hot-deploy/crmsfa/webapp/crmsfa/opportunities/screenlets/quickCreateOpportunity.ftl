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

<#assign stages = delegator.findAll("SalesOpportunityStage", Static["org.ofbiz.base.util.UtilMisc"].toList("sequenceNum"))>
<form method="post" action="/crmsfa/control/createOpportunity" name="quickCreateOpportunityForm">
<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmNewOpportunity}</div></div>
    <div class="screenlet-body">

        <span class="requiredFieldNormal">${uiLabelMap.CrmAccount}</span><br/>
        <@inputAutoCompleteAccount name="accountPartyId" size=13 /> 
        <br/>

        <span class="requiredFieldNormal">${uiLabelMap.CrmLead}</span><br/>
        <@inputLookup name="leadPartyId" lookup="LookupLeads" form="quickCreateOpportunityForm" size=15 maxlength=20/>
        <br/>

        <span class="requiredFieldNormal">${uiLabelMap.CrmOpportunityName}</span><br/>
        <@inputText name="opportunityName" size=15 maxlength=60/><br/>

        <span class="requiredFieldNormal">${uiLabelMap.CrmInitialStage}</span><br/>
        <select class="inputbox" name="opportunityStageId" size="1">
        <#list stages as stage>
          <option value="${stage.opportunityStageId}">${stage.description}</option>
        </#list>
        </select><br />
            
        <span class="tabletext">${uiLabelMap.CrmEstimatedAmount}</span><br/>
        <@inputText name="estimatedAmount" size=15 maxlength=60/><br/>

        <span class="requiredFieldNormal">${uiLabelMap.CrmEstimatedCloseDate}</span><br/>
        <@inputDate name="estimatedCloseDate" id="quickCreateOpEstimatedCloseDate"/>
        <br/>

        <#-- Useful note:  make your submit buttons of type button.  This prevents an ENTER keypress in a text field from submitting the form. -->
        <@inputSubmit title=uiLabelMap.CommonCreate/>
    </div>
</div>
</form>
