/*
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
 */
/* Copyright (c) Open Source Strategies, Inc. */
package org.opentaps.crmsfa.calendar;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;
import com.opensourcestrategies.crmsfa.teams.TeamHelper;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.services.GetWorkEffortEventsByPeriodService;
import org.opentaps.common.party.ViewPrefWorker;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.service.ServiceException;

/**
 * Actions for the opentpas calendar display .
 * Not the popup calendar.
 */
public final class CalendarAction {

    private CalendarAction() { }

    public static void day(Map<String, Object> context) throws ServiceException {
        final ActionContext ac = new ActionContext(context);

        TimeZone timeZone = ac.getTimeZone();
        Locale locale = ac.getLocale();

        String startParam = ac.getParameter("start");
        String facilityId = ac.getParameter("facilityId");
        String fixedAssetId = ac.getParameter("fixedAssetId");

        String eventsParam = "";
        if (facilityId != null) {
            eventsParam = "facilityId=" + facilityId;
        }
        if (fixedAssetId != null) {
            eventsParam = "fixedAssetId=" + fixedAssetId;
        }

        // get the starting hour and number of periods from the config properties
        Map<String, String> configProperties = UtilGenerics.checkMap(ac.get("configProperties"));
        Integer startHour = Integer.valueOf(configProperties.get("crmsfa.calendar.startHour"));
        if (startHour == null) {
            startHour = 0;
        }
        ac.put("startHour", startHour);
        Integer endHour = Integer.valueOf(configProperties.get("crmsfa.calendar.endHour"));
        if (endHour == null) {
            endHour = 24;
        }
        ac.put("endHour", endHour);

        Timestamp now = UtilDateTime.getDayStart(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp start = null;
        if (startParam != null) {
            start = new Timestamp(Long.parseLong(startParam));
        }
        if (start == null) {
            start = now;
        } else {
            start = UtilDateTime.getDayStart(start, timeZone, locale);
        }

        Timestamp prev = UtilDateTime.getDayStart(start, -1, timeZone, locale);
        Timestamp next = UtilDateTime.getDayStart(start, 1, timeZone, locale);

        GetWorkEffortEventsByPeriodService getEvntsSrvc = new GetWorkEffortEventsByPeriodService(ac.getUser());
        getEvntsSrvc.setInStart(start);
        getEvntsSrvc.setInNumPeriods(Integer.valueOf(24));
        getEvntsSrvc.setInFilterOutCanceledEvents(Boolean.TRUE);
        List<String> partyIds = new ArrayList<String>(UtilGenerics.checkCollection(ac.get("partyIds"), String.class));
        getEvntsSrvc.setInEntityExprList(UtilActivity.getDefaultCalendarExprList(partyIds));
        getEvntsSrvc.setInPartyIds(partyIds);
        getEvntsSrvc.setInFacilityId(facilityId);
        getEvntsSrvc.setInFixedAssetId(fixedAssetId);
        getEvntsSrvc.setInPeriodType(Integer.valueOf(Calendar.HOUR));
        getEvntsSrvc.setInTimeZone(timeZone);
        getEvntsSrvc.setInLocale(locale);
        getEvntsSrvc.runSync(ac.getInfrastructure());

        ac.put("periods", getEvntsSrvc.getOutPeriods());
        ac.put("maxConcurrentEntries", getEvntsSrvc.getOutMaxConcurrentEntries());
        ac.put("start", start);
        ac.put("prev", prev);
        ac.put("next", next);
        ac.put("now", now);
        ac.put("eventsParam", eventsParam);
    }

    public static void week(Map<String, Object> context) throws ServiceException {
        final ActionContext ac = new ActionContext(context);

        TimeZone timeZone = ac.getTimeZone();
        Locale locale = ac.getLocale();

        String startParam = ac.getParameter("start");

        String facilityId = ac.getParameter("facilityId");
        String fixedAssetId = ac.getParameter("fixedAssetId");

        String eventsParam = "";
        if (facilityId != null) {
            eventsParam = "facilityId=" + facilityId;
        }
        if (fixedAssetId != null) {
            eventsParam = "fixedAssetId=" + fixedAssetId;
        }

        Timestamp now = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp start = null;
        if (startParam != null) {
            start = new Timestamp(Long.parseLong(startParam));
        }
        if (start == null) {
            start = now;
        } else {
            start = UtilDateTime.getWeekStart(start, timeZone, locale);
        }

        Timestamp prev = UtilDateTime.getDayStart(start, -7, timeZone, locale);
        Timestamp next = UtilDateTime.getDayStart(start, 7, timeZone, locale);
        Timestamp end = UtilDateTime.getDayStart(start, 6, timeZone, locale);

        GetWorkEffortEventsByPeriodService getEvntsSrvc = new GetWorkEffortEventsByPeriodService(ac.getUser());
        getEvntsSrvc.setInStart(start);
        getEvntsSrvc.setInNumPeriods(Integer.valueOf(7));
        List<String> partyIds = new ArrayList<String>(UtilGenerics.checkCollection(ac.get("partyIds"), String.class));
        getEvntsSrvc.setInEntityExprList(UtilActivity.getDefaultCalendarExprList(partyIds));
        getEvntsSrvc.setInPartyIds(partyIds);
        getEvntsSrvc.setInFacilityId(facilityId);
        getEvntsSrvc.setInFixedAssetId(fixedAssetId);
        getEvntsSrvc.setInFilterOutCanceledEvents(Boolean.TRUE);
        getEvntsSrvc.setInPeriodType(Integer.valueOf(Calendar.DATE));
        getEvntsSrvc.setInTimeZone(timeZone);
        getEvntsSrvc.setInLocale(locale);
        getEvntsSrvc.runSync(ac.getInfrastructure());

        ac.put("periods", getEvntsSrvc.getOutPeriods());
        ac.put("maxConcurrentEntries", getEvntsSrvc.getOutMaxConcurrentEntries());
        ac.put("start", start);
        ac.put("end", end);
        ac.put("prev", prev);
        ac.put("next", next);
        ac.put("now", now);
        ac.put("eventsParam", eventsParam);

    }

    public static void month(Map<String, Object> context) throws ServiceException {
        final ActionContext ac = new ActionContext(context);

        TimeZone timeZone = ac.getTimeZone();
        Locale locale = ac.getLocale();

        String startParam = ac.getParameter("start");

        String facilityId = ac.getParameter("facilityId");
        String fixedAssetId = ac.getParameter("fixedAssetId");

        String eventsParam = "";
        if (facilityId != null) {
            eventsParam = "facilityId=" + facilityId;
        }
        if (fixedAssetId != null) {
            eventsParam = "fixedAssetId=" + fixedAssetId;
        }

        Timestamp now = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), timeZone, locale);
        Timestamp start = null;
        if (startParam != null) {
            start = new Timestamp(Long.parseLong(startParam));
        }
        if (start == null) {
            start = now;
        } else {
            start = UtilDateTime.getMonthStart(start, timeZone, locale);
        }

        Calendar tempCal = Calendar.getInstance(timeZone, locale);
        tempCal.setTime(new java.util.Date(start.getTime()));
        Integer numDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Timestamp prev = UtilDateTime.getMonthStart(start, -1, timeZone, locale);
        Timestamp next = UtilDateTime.getDayStart(start, numDays + 1, timeZone, locale);
        Timestamp end = UtilDateTime.getDayStart(start, numDays, timeZone, locale);

        //Find out what date to get from
        Timestamp getFrom = null;
        Integer prevMonthDays =  tempCal.get(Calendar.DAY_OF_WEEK) - tempCal.getFirstDayOfWeek();
        if (prevMonthDays < 0) {
            prevMonthDays = 7 + prevMonthDays;
        }
        tempCal.add(Calendar.DATE, -(prevMonthDays));
        numDays += prevMonthDays;
        getFrom = new Timestamp(tempCal.getTime().getTime());

        GetWorkEffortEventsByPeriodService getEvntsSrvc = new GetWorkEffortEventsByPeriodService(ac.getUser());
        getEvntsSrvc.setInStart(getFrom);
        getEvntsSrvc.setInNumPeriods(Integer.valueOf(numDays));
        List<String> partyIds = new ArrayList<String>(UtilGenerics.checkCollection(ac.get("partyIds"), String.class));
        getEvntsSrvc.setInPartyIds(partyIds);
        getEvntsSrvc.setInEntityExprList(UtilActivity.getDefaultCalendarExprList(partyIds));
        getEvntsSrvc.setInFacilityId(facilityId);
        getEvntsSrvc.setInFixedAssetId(fixedAssetId);
        getEvntsSrvc.setInFilterOutCanceledEvents(Boolean.TRUE);
        getEvntsSrvc.setInPeriodType(Integer.valueOf(Calendar.DATE));
        getEvntsSrvc.setInTimeZone(timeZone);
        getEvntsSrvc.setInLocale(locale);
        getEvntsSrvc.runSync(ac.getInfrastructure());

        ac.put("periods", getEvntsSrvc.getOutPeriods());
        ac.put("maxConcurrentEntries", getEvntsSrvc.getOutMaxConcurrentEntries());
        ac.put("start", start);
        ac.put("end", end);
        ac.put("prev", prev);
        ac.put("next", next);
        ac.put("now", now);
        ac.put("eventsParam", eventsParam);
    }

    public static void calendarCommon(Map<String, Object> context) throws GenericEntityException {
        final ActionContext ac = new ActionContext(context);

        // determine the user's preferred find using findActivePartiesViewPrefTypeId (optional feature)
        GenericValue userLogin = ac.getUserLogin();
        String calendarPref = ViewPrefWorker.getViewPreferenceString(userLogin, "MY_OR_TEAM_CALENDAR");

        // save the value in the context
        ac.put("MY_OR_TEAM_CALENDAR", calendarPref);

        // whether the user can see team calendars
        Boolean hasTeamCalviewPermission = ac.getSecurity().hasPermission("CRMSFA_TEAM_CALVIEW", userLogin);
        ac.put("hasTeamCalviewPermission", hasTeamCalviewPermission);

        // determine the set of parties whom we will be getting events for (default is user login's)
        if ("TEAM_VALUES".equals(calendarPref) && hasTeamCalviewPermission) {
            ac.put("partyIds", TeamHelper.getTeamMembersForPartyId(userLogin.getString("partyId"), ac.getDelegator()));
        } else {
            ac.put("partyIds", UtilMisc.<String>toList(userLogin.getString("partyId")));
        }
    }
}
