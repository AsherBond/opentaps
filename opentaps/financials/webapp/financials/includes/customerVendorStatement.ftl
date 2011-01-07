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
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#include "generateStatement.ftl" />

<#if transactions?exists>
<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<table style="" width="98%">
  <tr>
      <td>
      <table style="">
	  <tr>
	    <td class="tableheadtext" align="center" colspan="4">${uiLabelMap.FinancialsStatementFor} ${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, partyId, false)} (${partyId?default("N/A")}) (<a href="<@ofbizUrl>customerVendorStatement.pdf?partyId=${partyId?if_exists}&amp;fromDate=${getLocalizedDate(fromDate)}&amp;thruDate=${getLocalizedDate(thruDate)}&amp;glFiscalTypeId=${requestParameters.glFiscalTypeId?if_exists}&amp;reportType=${reportType}</@ofbizUrl>"class="linktext">PDF</a>)
	    </td>
	  </tr>
	
	  <@generateStatement transactions=transactions beginningBalance=beginningBalance />
	
	</table>
	</td>
	<td align="center">

     <img src="<@ofbizUrl>showChart?chart=${chartImage?html}</@ofbizUrl>" style="margin-right: 15px; "/>

	</td>
  </tr>
</table>	

</#if>
