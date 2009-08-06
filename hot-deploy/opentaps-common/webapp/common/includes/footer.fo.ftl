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
