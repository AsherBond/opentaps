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

<#-- 
This screen lists agreements based on an agreementListBuilder.
Parameters to specify:
    agreementsPaginatorName     Name of the paginator, which should be different for each distinct agreement list (default agreements)
    agreementsListBuilder       ListBuilder for agreements
    agreementViewer             The URI of the viewAgreement page (Default viewAgreement)
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@paginate name=agreementsPaginatorName?default("agreements") list=agreementsListBuilder agreementViewer=agreementViewer?default("viewAgreement")>
<#noparse>
    <@navigationBar />
    <table class="listTable">
      <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.AccountingAgreement orderBy="description" />
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" />
        <@headerCell title=uiLabelMap.CommonFrom orderBy="fromDate" />
        <@headerCell title=uiLabelMap.CommonThru orderBy="thruDate" />
      </tr>
      <#list pageRows as agreement>
        <#assign agreementDescription = agreement.description?default("") + " (${agreement.agreementId})"/>
        <tr class="${tableRowClass(agreement_index)}">
          <@displayLinkCell href="${parameters.agreementViewer}?agreementId=${agreement.agreementId}" text=agreementDescription />
          <@displayCell text=agreement.statusDescription />
          <@displayDateCell date=agreement.fromDate format="DATE"/>
          <@displayDateCell date=agreement.thruDate format="DATE"/>
        </tr>
      </#list>
    </table>
</#noparse>
</@paginate>
