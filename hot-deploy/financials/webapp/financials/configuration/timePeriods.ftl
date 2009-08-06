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
 *  
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- TODO: probably some kind of permission checking to see that this userLogin can view such and such reports -->

<div class="tabletext">

<script type="text/javascript">
function submitReconcile(form) {
  form.action="<@ofbizUrl>ReconcileGlAccountOrganization</@ofbizUrl>";
  form.submit();
}

function submitViewBalance(form) {
  form.action="<@ofbizUrl>viewGlAccountBalance</@ofbizUrl>";
  form.submit();
}
</script>

<#if closedTimePeriods?has_content>
<p>The following time periods have been closed:
<ul type="circle">
<#list closedTimePeriods as timePeriod>
<li>${timePeriod.periodName?if_exists} <#if timePeriod.periodNum?has_content>${timePeriod.periodNum?string("####")}</#if> (${timePeriod.getRelatedOne("PeriodType").description} ${uiLabelMap.CommonFrom} ${timePeriod.fromDate} ${uiLabelMap.CommonTo} ${timePeriod.thruDate})
</#list>
</ul></p>
<p>
<#else>
<p>There are currently no closed time periods.</p>
</#if>

<#if (openTimePeriodsSortedByThruDate?has_content) && (openTimePeriodsSortedByThruDate.size() > 0)>
<#assign timePeriod = openTimePeriodsSortedByThruDate.get(0)>
<a href="<@ofbizUrl>closeAllTimePeriods?organizationPartyId=${organizationPartyId}&customTimePeriodId=${timePeriod.customTimePeriodId}</@ofbizUrl>">
Close time periods ending ${timePeriod.thruDate}
</a>
</p>
<#else>
<p>There are no time periods which can be closed.</p>
</#if>

<#if openTimePeriods?has_content>
<p>The following time periods are open:
<ul type="circle">
<#list openTimePeriods as timePeriod>
<li>${timePeriod.periodName?if_exists} <#if timePeriod.periodNum?has_content>${timePeriod.periodNum?string("####")}</#if> (${timePeriod.getRelatedOne("PeriodType").description} ${uiLabelMap.CommonFrom} ${getLocalizedDate(timePeriod.fromDate, "DATE_ONLY")} ${uiLabelMap.CommonTo} ${getLocalizedDate(timePeriod.thruDate, "DATE_ONLY")})
</#list>
</ul></p>
<p>
<#else>
<p>There are currently no open time periods.</p>
</#if>
</div>
