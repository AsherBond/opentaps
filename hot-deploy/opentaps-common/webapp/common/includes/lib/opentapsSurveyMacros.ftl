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

<#-- 
Macros to help display Survey responses.

To use these macros in your page, first put this at the top:

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsSurveyMacros.ftl"/>
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/flexAreaMacros.ftl"/>

<#-- Generic survey display.  Unfortunately this requires a call to a java method to get the response data. -->
<#macro displaySurveyResponse surveyResponseId>
    <#assign surveyResponses = Static["org.opentaps.common.util.UtilSurvey"].getSurveyResponses(surveyResponseId, delegator) >
    <@flexArea targetId=surveyResponseId title="Survey Responses">
    <table class="surveyResponse">
    <#list surveyResponses as response>
      <tr>
        <td class="surveyQuestion">${response.question?if_exists}</td>
        <td class="surveyAnswer">
          <#if response.surveyQuestionTypeId == "OPTION">
            ${response.description?if_exists}
          <#elseif response.surveyQuestionTypeId == "NUMBER_CURRENCY">
            Currency Response not implemented yet.
          <#elseif response.surveyQuestionTypeId == "BOOLEAN">
            ${response.booleanResponse?if_exists}
          <#elseif response.surveyQuestionTypeId.startsWith("NUMBER_")>
            ${response.numericResponse?default(response.floatResponse?default(textResponse?default("")))}
          <#else>
            ${response.textResponse?if_exists}
          </#if>
        </td>
      </tr>
    </#list>
    </table>
    </@flexArea>
</#macro>
