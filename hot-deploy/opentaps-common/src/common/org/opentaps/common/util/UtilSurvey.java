/*
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
 */
package org.opentaps.common.util;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import java.util.List;
import java.util.Map;

/**
 * Helper methods to manage Surveys.
 */
public class UtilSurvey {

    /**
     * Get the survey response and answers.  The response is a join SurveyResponseAndAnswer with SurveyQuestion and an
     * outer join on SurveyQuestionOption to get the option data.
     * 
     * It has all fields of SurveyResponseAndAnswer, the surveyQuestionTypeId and question fields of SurveyQuestion, and
     * the description field of SurveyQuestionOption.
     */
    public static List<Map> getSurveyResponses(String surveyResponseId, GenericDelegator delegator) throws GenericEntityException {
        List<Map> lines = FastList.newInstance();
        List<GenericValue> responses = delegator.findByAndCache("SurveyResponseAndAnswer", UtilMisc.toMap("surveyResponseId", surveyResponseId), UtilMisc.toList("sequenceNum"));
        for (GenericValue response : responses) {
            GenericValue question = response.getRelatedOneCache("SurveyQuestion");
            GenericValue option = response.getRelatedOneCache("SurveyQuestionOption");

            Map line = FastMap.newInstance();
            line.putAll(response.getAllFields());
            if (question != null) {
                line.put("surveyQuestionTypeId", question.get("surveyQuestionTypeId"));
                line.put("question", question.get("question"));
            }
            if (option != null) {
                line.put("description", option.get("description"));
            }
            lines.add(line);
        }
        return lines;
    }

}
