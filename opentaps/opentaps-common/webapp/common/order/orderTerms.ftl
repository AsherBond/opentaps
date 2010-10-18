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
<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<@import location="component://opentaps-common/webapp/common/order/infoMacros.ftl"/>

<#if order?has_content && order.orderTerms?has_content>
<div class="screenlet">
  <div class="screenlet-header">
    <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderTerms}</div>
  </div>
  <div class="screenlet-body">
    <table border="0" width="100%" cellspacing="0" cellpadding="0">
      <tr>
        <td width="35%" align="left"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
        <td width="15%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
        <td width="15%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
        <td width="35%" align="center"><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
      </tr>
      <tr><td colspan="4"><hr class='sepbar'></td></tr>
      <#list order.orderTerms as orderTerm>
        <tr>
          <td width="35%" align="left"><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description", locale)}</div></td>
          <td width="15%" align="center"><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
          <td width="15%" align="center"><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
          <td width="35%" align="center"><div class="tabletext">${orderTerm.description?default("")}</div></td>
        </tr>
        <tr><td colspan="4">&nbsp;</td></tr>
      </#list>
    </table>
  </div>
</div>
</#if>
