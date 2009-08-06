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

<#macro infoSepBar>
  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
</#macro>

<#macro infoRowNested title>
  <tr>
    <td align="right" valign="top" width="15%">
      <div class="tabletext">&nbsp;<b>${title}</b></div>
    </td>
    <td width="5">&nbsp;</td>
    <td align="left" valign="top" width="80%">
      <#nested/>
    </td>
  </tr>
</#macro>

<#macro infoRow title content>
  <@infoRowNested title=title>
    <div class="tabletext">${content}</div> 
  </@infoRowNested>
</#macro>
