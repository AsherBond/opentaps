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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>>

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
