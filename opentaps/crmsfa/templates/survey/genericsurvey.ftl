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
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Revision: 1.1 $
 *@since      3.0
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

<#-- party ID -->
<#if partyId?has_content>
  <input type="hidden" name="partyId" value="${partyId}">
</#if>

<#-- survey ID -->
<input type="hidden" name="surveyId" value="${survey.surveyId}">

<div class="head1">${survey.description?if_exists}</div>
<div class="head2"><b>${survey.surveyName?if_exists}</b></div>

<#if survey.comments?has_content>
<p class="tabletext">${survey.comments}</p>
</#if>

<#if surveyQuestionAndAppls?exists>
<table width="100%" border="0" cellpadding="2" cellspacing="0">
  <#list surveyQuestionAndAppls as question>
    <#-- special formatting for select boxes -->
    <#assign align = "left">

    <#-- get an answer from the answerMap -->
    <#if surveyAnswers?has_content>
      <#assign answer = surveyAnswers.get(question.surveyQuestionId)?if_exists>
    </#if>

    <tr>
      <#-- seperator options -->
      <#if question.surveyQuestionTypeId == "SEPERATOR_TEXT">
        <td colspan="5"><div class="tabletext" align="left">${question.question?if_exists}</div></td>
      <#elseif question.surveyQuestionTypeId == "SEPERATOR_LINE">
        <td colspan="5"><hr class="sepbar"></td>
      <#else>
        <#-- standard question options -->
        <td align='left' nowrap>
          <div class="tabletext">${question.question?if_exists}</div>
        </td>
        <td width='1'>&nbsp;</td>
        <td align="${align}">
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
            <input type="text" size="15" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "TEXT_LONG">
            <input type="text" size="35" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "EMAIL">
            <input type="text" size="30" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "URL">
            <input type="text" size="40" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "DATE">
            <input type="text" size="12" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "CREDIT_CARD">
            <input type="text" size="20" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "GIFT_CARD">
            <input type="text" size="20" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_CURRENCY">
            <input type="text" size="6" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.currencyResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_FLOAT">
            <input type="text" size="6" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.floatResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "NUMBER_LONG">
            <input type="text" size="6" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.numericResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "PASSWORD">
            <input type="password" size="30" class="textBox" name="answers_${question.surveyQuestionId}" value="${(answer.textResponse)?if_exists}">
          <#elseif question.surveyQuestionTypeId == "OPTION">
            <#assign options = question.getRelated("SurveyQuestionOption", sequenceSort)?if_exists>
            <#if options?size == 1>
               <#assign option = options?first>
               <input type="hidden" name="answers_${question.surveyQuestionId}" value="${option.surveyOptionSeqId}">
               <div class="tabletext">${option.description?if_exists}</div>
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
            <div class="tabletext">Unsupported question type : ${question.surveyQuestionTypeId}</div>
          </#if>
        </td>
        <td width="90%">&nbsp;</td>
      </#if>
    </tr>
    <#if question.hint?has_content>
    <tr>
       <td colspan="5" class="tabletext"><i>${question.hint}</i></div>
    </tr>
    </#if>
  </#list>
  <tr><td colspan="4">&nbsp;</td></tr>
  <tr>
    <td colspan="3" align="center"><input type="submit" value="<#if survey.submitCaption?has_content>${survey.submitCaption}<#else>Submit</#if>" class="smallSubmit"></td>
    <td width="90%">&nbsp;</td>
  </tr>
</table>
<#else>
<h2>${uiLabelMap.CrmSurveyHasNoQuestions}</h2>
 </#if>