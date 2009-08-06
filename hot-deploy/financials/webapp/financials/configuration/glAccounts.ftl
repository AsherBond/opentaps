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

<style type="text/css">
  table.trialBalanceSummary {
    float: right;
    text-align: right;
    padding: 0px;
    border-collapse: collapsed;
    white-space: nowrap;
  }
  table.trialBalanceSummary td.postedBalance {
    width: 100px;
  }
</style>

<script type="text/javascript">

// Extend opentaps.GLAccountTree to provide formatting specific to this page
dojo.declare("opentaps.TrialBalanceAccountTree", opentaps.GLAccountTree, {

    formatValueNode: function (node, value) {
        // cells for values, starting with posted balance followed by links
        var glAccountId = this.tree.store.getIdentity(node.item);
        cells = '<table class="trialBalanceSummary"><tr>';
        cells += '<td class="postedBalance">' + value + '</td>';
        cells += '<td><a href="reconcileAccounts?glAccountId=' + glAccountId + '" class="buttontext">${uiLabelMap.FinancialsReconcile}</a></td>';
        cells += '<td><a href="updateGlAccountScreen?glAccountId=' + glAccountId + '" class="buttontext">${uiLabelMap.CommonEdit}</a></td>';
        cells += '<td><a href="addSubAccountScreen?parentGlAccountId=' + glAccountId + '" class="buttontext">${uiLabelMap.FinancialsAddSubAccount}</a></td>';
        cells += '<td><a href="removeGlAccountFromOrganization?glAccountId=' + glAccountId + '" class="buttonDangerous">${uiLabelMap.FinancialsDeactivate}</a></td>';
        cells += '</tr></table>';
        return  opentaps.createSpan(null, cells, 'amount');
    }

});

</script>

<div class="screenlet-header">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.FinancialsChartOfAccountsFor} ${parameters.organizationName}</div>
    <div class="subMenuBar"><a class="buttontext" href="addNewGlAccountScreen">${uiLabelMap.FinancialsCreateNewAccount}</a><a class="buttontext" href="addExistingGlAccountScreen">${uiLabelMap.FinancialsAddExistingAccount}</a></div>
  </div>
</div>

<#if chartOfAccountsTree?exists>
  <@glAccountTree glAccountTree=chartOfAccountsTree treeId="chartOfAccounts" className="opentaps.TrialBalanceAccountTree" defaultState="collapsed"/>
</#if>
