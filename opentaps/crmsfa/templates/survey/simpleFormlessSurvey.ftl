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
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-->


<#--
This template is for rendering a survey with form inputs and without
any form context such as <form> tags or submit buttons. Its intended
use is for inclusion in bigger forms that need an automaticially generated
survey as a subsection.

The format takes on a simple two column table.  It is recommended to use CSS
selectrs to format the contents.

This is a simplification of genericsurvey.ftl.
-->

<#if additionalFields?has_content>
  <#list additionalFields.keySet() as field>
    <input type="hidden" name="${field}" value="${additionalFields.get(field)}">
  </#list>
</#if>

<#-- update response -->
<#if surveyResponseId?has_content>
  <input type="hidden" name="surveyResponseId" value="${surveyResponseId}">
</#if>

<#-- survey ID -->
<input type="hidden" name="surveyId" value="${survey.surveyId}">

<#if surveyQuestionAndAppls?exists>
  <#list surveyQuestionAndAppls as question>

    <#-- get an answer from the answerMap -->
    <#if surveyAnswers?has_content>
      <#assign answer = surveyAnswers.get(question.surveyQuestionId)?if_exists>
    </#if>

    <tr>
      <#-- seperator options -->
      <#if question.surveyQuestionTypeId == "SEPERATOR_TEXT">
        <td colspan="2">${question.question?if_exists}</td>
      <#elseif question.surveyQuestionTypeId == "SEPERATOR_LINE">
        <td colspan="2"><hr class="sepbar"></td>
      <#else>
        <#-- standard question options -->
        <td>${question.question?if_exists}</td>
        <td>
          <#if question.surveyQuestionTypeId == "BOOLEAN">
            <#assign selectedOption = (answer.booleanResponse)?default("")>
            <select class="selectBox" name="answers_${question.surveyQuestionId}">
              <#if question.requiredField?default("N") != "Y">
                <option value=""></option>
              </#if>
              <option <#if "Y" == selectedOption>SELECTED</#if>>Y</option>
              <option <#if "N" == selectedOption>SELECTED</#if>>N</option>
            </select>
          <#elseif question.surveyQuestionTypeId == "TEXTAREA">
            <textarea class="textAreaBox" cols="40" rows="5" name="answers_${question.surveyQuestionId}">${(answer.textResponse)?if_exists}</textarea>
          <#elseif question.surveyQuestionTypeId == "TEXT_SHORT">
            <input type="text" size="15" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "TEXT_LONG">
            <input type="text" size="35" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "EMAIL">
            <input type="text" size="30" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "URL">
            <input type="text" size="40" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "DATE">
            <input type="text" size="12" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "CREDIT_CARD">
            <input type="text" size="20" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "GIFT_CARD">
            <input type="text" size="20" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_CURRENCY">
            <input type="text" size="6" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.currencyResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_FLOAT">
            <input type="text" size="6" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.floatResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_LONG">
            <input type="text" size="6" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.numericResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "PASSWORD">
            <input type="password" size="30" class="inputBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "OPTION">
            <#assign options = question.getRelated("SurveyQuestionOption", sequenceSort)?if_exists>
            <#if options?size == 1>
               <#assign option = options?first>
               <input type="hidden" name="answers_${question.surveyQuestionId}" value="${option.surveyOptionSeqId}">
               ${option.description?if_exists}
            <#else>    
            <#assign selectedOption = (answer.surveyOptionSeqId)?default("_NA_")>
            <select class="selectBox" name="answers_${question.surveyQuestionId}">
              <#if question.requiredField?default("N") != "Y">
                <option value=""></option>
              </#if>
              <#if options?has_content>
                <#list options as option>
                  <option value="${option.surveyOptionSeqId}" <#if option.surveyOptionSeqId == selectedOption>SELECTED</#if>>${option.description?if_exists}</option>
                </#list>
              <#else>
                <option value="">Nothing to choose</option>
              </#if>
            </select>
            </#if>
          <#else>
            Unsupported question type : ${question.surveyQuestionTypeId}
          </#if>
        </td>
      </#if>
    </tr>
    <#if question.hint?has_content>
    <tr><td colspan="2">${question.hint}</td></tr>
    </#if>
  </#list>
</#if>
