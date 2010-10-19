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
 *  
 *  @author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- TODO: probably some kind of permission checking to see that this userLogin can view such and such reports -->

<div class="tabletext">

  <script type="text/javascript">
  function submitReconcile(form) {
    form.action="<@ofbizUrl>reconcileAccountForm</@ofbizUrl>";
    form.submit();
  }

  function submitViewBalance(form) {
    form.action="<@ofbizUrl>viewGlAccountBalance</@ofbizUrl>";
    form.submit();
  }

  // this should be a common javascript function for currency fields (use onClick or onBlur)
  function validateCurrency(element, defaultValue) {
    val = (element.value - 0);
    if (isNaN(val)) {
      val = (defaultValue - 0);
      if (isNaN(val)) {
        element.value = "0.00";
      } else {
        element.value = val.toFixed(2);
      }
    }
    element.value = val.toFixed(2);
  }
  </script>

  <#if glAccountOrgList?exists>
    <form method="POST" name="reconcileForm" action="reconcileAccountForm"> <#-- action set by javascript -->
      <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
      <div class="form" >
        <table class="twoColumnForm" style="border:0">
          <tr>
            <@displayTitleCell title=uiLabelMap.FinancialsReconcileAccount />
            <td><@inputAutoCompleteGlAccount name="glAccountId" default=glAccountId?if_exists /></td>
          </tr>
          <tr>
            <@displayTitleCell title=uiLabelMap.OpentapsAsOfDate />
            <td>
              <@inputDateTime name="reconciledDate" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()/>
            </td>
          </tr>
          <tr>
            <@displayTitleCell title=uiLabelMap.FinancialsStatementsEndingBalance />
            <td>
              <input type='text' size='25' class='inputBox' name='reconciledBalance' value='0.00' onChange="javascript:validateCurrency(this, '0.00')">
            </td>
          </tr>
          <tr><td colspan="2">&nbsp;</td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <input name="reconcile" type="button" class="smallSubmit" value="Reconcile" onClick="javascript:submitReconcile(this.form)"/>
              &nbsp;
              <input name="viewbalance" type="button" class="smallSubmit" value="${uiLabelMap.FinancialsPastReconciliations}" onClick="javascript:submitViewBalance(this.form)"/>
            </td>
          </tr>
        </table>
      </div>
    </form>
  </#if>
</div>
