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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>
    
    <fo:layout-master-set>
        <fo:simple-page-master master-name="my-page"
            margin-top="1in" margin-bottom="0in"
            margin-left="20mm" margin-right="20mm">
            <fo:region-body margin-top="1.5in" margin-bottom="1in"/>
            <fo:region-before extent="2.75in"/>
            <fo:region-after extent="1in"/>
        </fo:simple-page-master>
    </fo:layout-master-set>

    <fo:page-sequence master-reference="my-page" initial-page-number="1">
        <fo:static-content flow-name="xsl-region-before">

            <@opentapsHeaderFO>
              ${screens.render("component://opentaps-common/widget/screens/common/PDFScreens.xml#QuotePDFHeaderInfo")}
            </@opentapsHeaderFO>

            <fo:block><fo:leader/></fo:block>

        </fo:static-content>

        <@opentapsFooterFO />

        <fo:flow flow-name="xsl-region-body">
          ${screens.render("component://opentaps-common/widget/screens/common/PDFScreens.xml#MiniCatalog")}
        </fo:flow>

    </fo:page-sequence>
</fo:root>
</#escape>
