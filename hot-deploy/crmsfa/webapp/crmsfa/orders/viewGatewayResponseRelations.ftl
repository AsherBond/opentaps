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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="post"  action=""  class="basic-form" onSubmit="javascript:submitFormDisableSubmits(this)" name="ViewGatewayResponseRelations">
    <table cellspacing="0">
        <tr>
            <@displayCell text="${uiLabelMap.OrderOrderId}" blockClass="label" style="font-size: 100%;"/>
            <@displayLinkCell href="orderview?orderId=${orderId}" text=orderId/>
        </tr>
        <tr>
            <@displayCell text="${uiLabelMap.CrmOrderPaymentPreferenceId}" blockClass="label" style="font-size: 100%;"/>
            <@displayCell text=orderPaymentPreferenceId/>
        </tr>
    </table>
</form>
