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

<#-- Footer for all pdfs.  You can render this with <@opentapsFooterFO/>.  -->

<#escape x as x?xml>
<fo:static-content flow-name="xsl-region-after">
    <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->
    <fo:block font-size="10pt" text-align="center">${uiLabelMap.CommonPage} <fo:page-number/> ${uiLabelMap.CommonOf} <fo:page-number-citation ref-id="theEnd"/></fo:block>
    <fo:block font-size="10pt"/>
    <#list salesInvoiceFooterText?default("")?split("\n") as footerText>
      <fo:block font-size="8pt" text-align="center">${footerText}</fo:block>
    </#list>
</fo:static-content>
</#escape>
