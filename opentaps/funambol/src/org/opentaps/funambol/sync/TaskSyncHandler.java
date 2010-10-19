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

package org.opentaps.funambol.sync;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.funambol.common.pim.calendar.Calendar;
import com.funambol.common.pim.calendar.CalendarContent;
import com.funambol.common.pim.calendar.Event;
import com.funambol.common.pim.calendar.Task;
import com.funambol.common.pim.common.Property;
import com.funambol.common.pim.converter.BaseConverter;
import com.funambol.common.pim.converter.CalendarToSIFE;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.common.pim.converter.TaskToSIFT;
import com.funambol.common.pim.converter.VCalendarContentConverter;
import com.funambol.common.pim.converter.VCalendarConverter;
import com.funambol.common.pim.model.VCalendar;
import com.funambol.common.pim.model.VCalendarContent;
import com.funambol.common.pim.sif.SIFCalendarParser;
import com.funambol.common.pim.xvcalendar.XVCalendarParser;
import com.funambol.foundation.exception.EntityException;
import com.funambol.framework.engine.SyncItem;
import mz.co.dbl.siga.framework.base.NotConfiguredException;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

public class TaskSyncHandler extends AbstractSyncHandler<CalendarContent> implements EntitySyncHandler<CalendarContent> {
    //represent possible statuses for Events and Tasks
    private static enum EventState {
            EVENT_SCHEDULED,
            EVENT_STARTED,
            EVENT_COMPLETED,
            EVENT_ON_HOLD,
            EVENT_CANCELLED
            }
    private static enum TaskState {
            TASK_SCHEDULED,
            TASK_STARTED,
            TASK_COMPLETED,
            TASK_ON_HOLD,
            TASK_CANCELLED
            }

    //acceptable date formats
    private static TimeZone GMT = TimeZone.getTimeZone("GMT");  //FOPn always sends times as GMT
    private static DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private static DateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static {
        DATE_TIME_FORMAT.setTimeZone(GMT);
        DATE_ONLY_FORMAT.setTimeZone(GMT);
    }

    private int _defaultStartHour, _defaultEndHour;

    //represents the mime-types of data items we can handle - correspond to types supported by FOPlugin
    private static enum Type {
        EVENTSIF("text/x-s4j-sife", EnumSet.allOf(EventState.class), Event.class, DATE_TIME_FORMAT ,"CAL_ATTENDEE"),
        EVENTCAL("text/x-vcalendar", EnumSet.allOf(EventState.class), Event.class, DATE_TIME_FORMAT ,"CAL_ATTENDEE"),
        TASKSIF("text/x-s4j-sift", EnumSet.allOf(TaskState.class), Task.class, DATE_ONLY_FORMAT, "CAL_OWNER"),
        TASKCAL("text/x-vcalendar", EnumSet.allOf(TaskState.class), Task.class, DATE_ONLY_FORMAT, "CAL_OWNER");

        private String _mimeType;
        private EnumSet _states;
        private Class _itemClass;
        private DateFormat _dateFormat;  //default format for Date->String
        private String _roleType;  //default roleTypeId

        Type(String mimeType, EnumSet states, Class itemClass, DateFormat dateFormat, String roleType) {
            _mimeType = mimeType;
            _states = states;
            _itemClass = itemClass;
            _dateFormat = dateFormat;
            _roleType = roleType;
        }

        public String getMimeType() { return _mimeType; }

        /**
         * Return ordinal of the given status.
         * @param statusId a <code>String</code> value
         * @throws NotConfiguredException if no corresponding state found
         */
        public String stateFor(String statusId) {
           int status = 0;
           for (Object state : _states) {
               status++;
               if (state.toString().equals(statusId)) {
                   return String.valueOf(status);
               }
           }

           //if we got here, we found nothing
           throw new NotConfiguredException("No state found matching " + statusId);
        }

        /**
         * Gets the first state in the list.
         * @return first state in the list
         */
        public String firstStatus() {
            return _states.iterator().next().toString();
        }

        /**
         * Return statusId for given status num.
         * @param status the status number
         * @return the statusId <code>String</code> value
         */
        public String statusFor(int status) {
            Iterator allStates = _states.iterator();
            Object state = null;
            for (int i = 0; i <= status; i++) {
                state = allStates.next();
            }

            return state.toString();
        }

        public CalendarContent newItem() {
            try {
                return (CalendarContent) _itemClass.newInstance();
            } catch (Exception newX) {
                throw new NotConfiguredException("Cannot create new instance of " + _itemClass, newX);
            }
        }

        /**
         * Parse with appropriate formatting, FOPn uses two.
         *
         * @param dateStr - if it finishes with the letter 'Z' will be treated as date-time, otherwise as date-only
         */
        public Date toDate(String dateStr) {
            try {
                if (dateStr.charAt(dateStr.length() - 1) == 'Z') {
                    return DATE_TIME_FORMAT.parse(dateStr);
                } else {
                    return DATE_ONLY_FORMAT.parse(dateStr);
                }
            } catch (ParseException e) {
                //TODO: what to throw here
                throw new RuntimeException("Cannot parse date from " + dateStr);
            }
        }

        public String fromDate(Date date) {
            return _dateFormat.format(date);
        }

        public String getRoleType() { return _roleType; }

        public String getName() {
            if (this.toString().startsWith("TASK")) {
                return "TASK";
            } else if (this.toString().startsWith("EVENT")) {
                return "EVENT";
            }
            return "";
        }
    };

    private List<GenericValue> _allTasks = null;

    //=== configuration info, should eventually be loaded from spring ===
    private Map<String, ServiceMapping> _deleteServices = new HashMap<String, ServiceMapping>();  //roleTypeId:serviceName
    private Map<String, ServiceMapping> _updateServices = new HashMap<String, ServiceMapping>();  //roleTypeId:serviceName
    private Type _type = Type.TASKSIF;  //by default we handle tasks, but Spring can override this

    //=== initialization ===

    /**
     * Set the type of record handled by this handler.
     * @param type the type of record
     */
    public void setType(String type) {
        for (Type t  : EnumSet.range(Type.EVENTSIF, Type.TASKCAL)) {
            if (t.getMimeType().equals(type)) {
                _type = t;
                return;
            }
        }

        //if we go to here, bail out because type is unsupported
        throw new NotConfiguredException("Type " + type + " is not supported by TaskSyncHandler");
    }

    //=== implement EntitySyncHandler ===

    public String addItem(CalendarContent source) throws GeneralException {
        //1a. convert basic params
        Map params = UtilMisc.toMap("workEffortTypeId", getWorkEffortType());
        _converters.getConverter("CREATE", source).toMap(source, params);

        //1b. if it is an all day event, we need to set start and end time from defaults
        //     - otherwise OT shows it in activity list but not in calendar
        if ((_type == Type.EVENTSIF || _type == Type.EVENTCAL) && source.isAllDay()) {
            params.put("estimatedStartDate", setHours((Timestamp) params.get("estimatedStartDate"), _defaultStartHour));
            params.put("estimatedCompletionDate", setHours((Timestamp) params.get("estimatedCompletionDate"), _defaultEndHour));
        }

        //2. make sure status is set
        if (params.get("currentStatusId") == null) {
            params.put("currentStatusId", _type.firstStatus());
        }

        //3. run relevant service to insert the WorkEffort
        Map effortInfo = _syncSource.runSync("createWorkEffort", params);

        //4. now create a WEPA to link sync user to the WorkEffort
        String effortId = effortInfo.get("workEffortId").toString();
        Map assingPartyParams = UtilMisc.toMap("workEffortId", effortId, "partyId", _syncPartyId);
        assingPartyParams.put("statusId", "PRTYASGN_ASSIGNED");
        assingPartyParams.put("roleTypeId", _type.getRoleType());
        _syncSource.runSync("assignPartyToWorkEffort", assingPartyParams);

        //5. finally return id of the WorkEffort we created, will serve as key
        return effortId;
    }

    public Iterable<String> getAllKeys() {
        Set<String> keys = new HashSet<String>();
        for (GenericValue party : _allTasks) {
            keys.add(party.getString("workEffortId"));
        }

        return keys;
    }

    public Iterable<String> getDeletedKeys(Timestamp since) throws GeneralException {
        Set<String> deletedKeys = new HashSet<String>();  //use a set in case any Id comes up twice
        for (GenericValue deleted : _syncSource.findByAnd("WorkEffortPartyAssignment", EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, since))) {
            deletedKeys.add(deleted.getString("workEffortId"));
        }

        return deletedKeys;
    }

    public CalendarContent getItemFromId(String key) throws GeneralException {
        //0. prepare the data objects we will work with
        GenericValue workEffortGV = _syncSource.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", key));
        CalendarContent item = _type.newItem();

        //1. prepare data common to both Task and Event
        String priority = workEffortGV.get("priority") == null ? "0" : workEffortGV.get("priority").toString();
        String percentComplete = workEffortGV.get("percentComplete") == null ? "0" : workEffortGV.get("percentComplete").toString();
        String owner = _syncSource._userLogin.getString("userLoginId");

        String currentStatusId = workEffortGV.getString("currentStatusId");
        String status = _type.stateFor(currentStatusId);
        Timestamp estEndDate = workEffortGV.getTimestamp("estimatedCompletionDate");
        item.setSummary(createProperty("Subject", workEffortGV.getString("workEffortName"))); //Subject String
        item.setPriority(createProperty("Importance", priority)); //Importance Long
        item.setDescription(createProperty("Body", workEffortGV.getString("description")));// Body String
        item.setDtStart(createProperty("StartDate", workEffortGV.getTimestamp("estimatedStartDate"))); //StartDate String
        item.setStatus(createProperty("Status", status)); //Status String
        item.setAllDay(false);  //OT has no concept of all-day events

        //2.1 Data specific to Task
        if (item instanceof Task) {
            Task task = (Task) item;

            task.setImportance(createProperty("Importance", priority));
            task.setDueDate(createProperty("DueDate", estEndDate));
            task.setDtEnd(task.getDueDate());
            task.setPercentComplete(createProperty( "PercentComplete" , percentComplete )); //PercentComplete String
            task.setDateCompleted( createProperty( "DateCompleted" ,  workEffortGV.getTimestamp("actualCompletionDate") ) );
            task.setComplete(createProperty("Complete", currentStatusId.equals(TaskState.TASK_COMPLETED.toString())));
            task.setOwner(createProperty("Owner", owner)); //Owner String
        }

        //2.2 Data specific to Event
        else if (item instanceof Event) {
            Event event = (Event) item;

            event.getLocation().setPropertyValue(workEffortGV.get("locationDesc"));
            event.setDtEnd(createProperty("DtEnd", estEndDate));
        }

        return item;
    }

    private Property createProperty(final String tag,final Object inputValue) {
        if (inputValue == null || tag == null) {
            return null;
        }
        Object propVal = null;

        Property prop = new Property();
        prop.setTag(tag);

        //special treatment for date/time
        if (inputValue instanceof Timestamp) {
            propVal = _type.fromDate((Timestamp) inputValue);
        } else {
            propVal = inputValue;
        }

        prop.setPropertyValue(propVal);
        return prop;
    }

    /**
     * We don't do anything in this method as given the Outlook UI, it would be basically impossible to register the same Task or Event twice and not notice it.
     */
    public Iterable<String> getKeysFromTwin(CalendarContent twin) throws GeneralException {
        return new LinkedList<String>();
    }

    /**
     * Return key of all WorkEfforts either CREATED or ASSIGNED TO SYNC USER since <code>since</code>.
     *
     * @param since moment of last sync
     * @return list of String (workEffortId)
     */
    public Iterable<String> getNewKeys(Timestamp since) throws GenericEntityException {
        Set<String> keys = new HashSet<String>();  //use a HashSet to handle duplicates
        for (GenericValue workEffort : _allTasks) {
            String weId = workEffort.getString("workEffortId");

            //1. the WE could be new itself
            if(EntitySyncSourceHelper.wasCreated(workEffort, since)) {
                keys.add(weId);
            } else { //2. OR the WE already existed but has just been ASSIGNED to us
                for (GenericValue wepa : (List<GenericValue>) workEffort.getRelatedByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("partyId", _syncPartyId))) {
                    if (EntitySyncSourceHelper.wasCreated(wepa, since)) {
                        keys.add(weId);
                        break;
                    }
                }
            }
        }

        _since = since;  //this line is essential for merge to work, otherwise it has no basis for comparison of times
        return keys;
    }

    public Iterable<String> getUpdatedKeys(Timestamp since) throws GenericEntityException {
        Set<String> keys = new HashSet<String>();
        for (GenericValue party : EntitySyncSourceHelper.getUpdatedTasks(_allTasks, since)) {
            keys.add(party.getString("workEffortId"));
        }

        return keys;
    }

    public boolean mergeItem(String key, CalendarContent source) throws GeneralException {
        if(_mergeStrategy == MergeStrategy.server_wins) {
            //override client
            return true;
        } else {
            //override server
            updateItem(key, source);

            //no need to overight client
            return false;
        }
    }

    public void removeItem(String key) throws GeneralException {
        List<GenericValue> wepa = _syncSource._delegator.findByAnd("WorkEffortPartyAssignment",
                UtilMisc.toMap("partyId", _syncPartyId, "workEffortId", key)
                );

        for (GenericValue gv : wepa) {
            Timestamp fromDate = gv.getTimestamp("fromDate");
            String roleTypeId = gv.getString("roleTypeId");

            Map params = UtilMisc.toMap(
                    "partyId", _syncPartyId,
                    "workEffortId", key,
                    "fromDate", fromDate,
                    "roleTypeId", roleTypeId
                    );

            _syncSource.runSync(_deleteServices.get(getWorkEffortType()).getServiceName(), params);
        }
    }

    public void updateItem(String key, CalendarContent source) throws GeneralException {
        //1. convert basic data
        Map params = UtilMisc.toMap("workEffortId", key);
        GenericValue workEffort = _syncSource.findByPrimaryKey("WorkEffort", params);
        _converters.getConverter("CREATE", source).toMap(source, params);

        //2. only permit status conversion if it is logical
        Object newStatusId = params.get("currentStatusId");
        if (newStatusId != null) {
            String newStatusPos = _type.stateFor((String) newStatusId);
            String currentStatusId =  workEffort.getString("currentStatusId");
            currentStatusId = currentStatusId == null ? _type.firstStatus(): currentStatusId;
            String currentStatusPos = _type.stateFor(currentStatusId);

            if (currentStatusPos.compareTo(newStatusPos) > 0) {
                params.put("currentStatusId", currentStatusId);
            }
        }

        //3. finally perform the update
        String serviceName = _updateServices.get(getWorkEffortType()).getServiceName();
        _syncSource.runSync(serviceName, params);
    }


    @Override
    public MergeStrategy[] getInvalidStrategy() {
        MergeStrategy[] invalid = {MergeStrategy.merge};
        return invalid;
    }

    public byte[] convertBeanToData(CalendarContent item) throws GeneralException {
        try {
            Calendar cal = new Calendar();
            if (item instanceof Task) {
                cal.setTask((Task) item);
            } else if (item instanceof Event) {
                cal.setEvent((Event) item);
            } else {
                throw new NotConfiguredException("Cannot convert unrecognized item " + item + " to raw data");
            }

            return convert(cal, _syncSource.getInfo().getPreferredType().getType()).getBytes();
        } catch (EntityException e) {
            throw new GeneralException(e);
        }
    }

    public CalendarContent convertDataToBean(SyncItem syncItem) throws GeneralException
    {
        String content = getContentFromSyncItem(syncItem);
        try {
            //0. perform the conversion from Raw data to Funambol PIM object
            CalendarContent item = convert(content, syncItem.getType());

            //1. convert data common to both Task and Calendar
            if (item.getStatus() != null) {
                String statusStr = item.getStatus().getPropertyValueAsString();
                if (statusStr != null) {
                    int status = Integer.parseInt(statusStr);  //NOT FOR EVENT
                    item.getStatus().setPropertyValue(_type.statusFor(status));
                }
            }
            if (item.getPriority() == null) {
                item.setPriority(new Property());
                item.getPriority().setPropertyValue("0");
            }
            convertPropertyValueToLong(item.getPriority());
            convertDatesToTimestamp(item);

            //2.1 convert data specific to Task
            if (item instanceof Task) {
                Task task = (Task) item;

                Property dateCompleted = task.getDateCompleted();
                if (!StringUtils.isEmpty(dateCompleted.getPropertyValueAsString())) {
                    dateCompleted.setPropertyValue(convertToTimestamp(dateCompleted.getPropertyValueAsString()));
                }

                convertPropertyValueToLong(task.getPercentComplete());
            }

            //2.2 convert data specific to Calendar
            //if it is an all day event, we need to set start and end time from defaults
            //     - otherwise OT shows it in activity list but not in calendar
            if (item instanceof Event && item.isAllDay()) {
                Property start = item.getDtStart();
                start.setPropertyValue(setHours((Timestamp) start.getPropertyValue(),  _defaultStartHour));
                Property end = item.getDtEnd();
                end.setPropertyValue(setHours((Timestamp) end.getPropertyValue(),  _defaultEndHour));
            }

            return item;
        } catch (EntityException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    protected void prepareHandler() throws GeneralException {
        _allTasks = prepareAllTasks();

        //prep update services
        _updateServices.put("EVENT" , new ServiceMapping("updateWorkEffort"));
        _updateServices.put("TASK" , new ServiceMapping("updateWorkEffort"));
        _deleteServices.put("EVENT" , new ServiceMapping("unassignPartyFromWorkEffort"));
        _deleteServices.put("TASK" , new ServiceMapping("unassignPartyFromWorkEffort"));

        //read defaults from config files
        _defaultStartHour = (int) UtilProperties.getPropertyNumber("crmsfa", "crmsfa.calendar.startHour");
        _defaultEndHour = (int) UtilProperties.getPropertyNumber("crmsfa", "crmsfa.calendar.endHour");
    }

    private void convertPropertyValueToLong(Property prop) {
        prop.setPropertyValue(Long.valueOf(prop.getPropertyValueAsString()));
    }

    /**
     * Converts a calendar in SIF-E or SIF-T format to a Calendar object IF we support this format.
     *
     * @param content as a String
     * @param contentType
     * @throws EntityException if the contentType is wrong or the conversion
     *                         attempt doesn't succeed.
     * @return a Calendar object
     */
    private CalendarContent convert(String content, String contentType) throws EntityException {
        if (contentType.equals(Type.EVENTSIF.getMimeType()) || contentType.equals(Type.TASKSIF.getMimeType())) {
            return sif2Calendar(content, contentType).getCalendarContent();
        } else if (contentType.equals(Type.EVENTCAL.getMimeType()) || contentType.equals(Type.TASKCAL.getMimeType())) {
            try {
                VCalendarContent vcc = cal2Calendar(content, contentType).getVCalendarContent();
                VCalendarContentConverter vccc = new VCalendarContentConverter(_syncSource.getDeviceTimeZone(), _syncSource.getDeviceCharset());
                return vccc.vcc2cc(vcc, true);
            } catch (ConverterException e) {
                throw new EntityException(e);
            }
        } else {
            throw new EntityException("Can't convert " + contentType + "!");
        }
    }

    /**
     * Converts a Calendar back to a streamable (vCalendar/iCalendar, SIF-E or
     * SIF-T) format.
     *
     * @param calendar
     * @param contentType
     * @throws EntityException if the contentType is wrong or the conversion
     *                         attempt doesn't succeed.
     * @return the result in the required format
     */
    private String convert(Calendar calendar, String contentType) throws EntityException {
        if (contentType.equals(Type.EVENTSIF.getMimeType()) || contentType.equals(Type.TASKSIF.getMimeType())) {
            return calendar2sif(calendar, contentType);
        } else if (contentType.equals(Type.EVENTCAL.getMimeType()) || contentType.equals(Type.TASKCAL.getMimeType())) {
            return calendar2cal(calendar, contentType);
        } else {
            throw new EntityException("Can't convert " + contentType + "!");
        }
    }

    private Calendar sif2Calendar(String xml, String sifType) throws EntityException {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder(xml.length() + 60);
            sb.append("Converting: ").append(sifType).append(" => Calendar ")
              .append("\nINPUT = {").append(xml).append('}');
            log.trace(sb.toString());
        }

        ByteArrayInputStream buffer = null;
        Calendar calendar = null;
        try {
            calendar = new Calendar();
            buffer = new ByteArrayInputStream(xml.getBytes());
            if ((xml.getBytes()).length > 0) {
                    SIFCalendarParser parser = new SIFCalendarParser(buffer);
                    calendar = parser.parse();
            } else {
                throw new EntityException("No data");
            }
        } catch (EntityException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityException("Error converting " + sifType + " to Calendar. ", e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Conversion done.");
        }
        return calendar;
    }

    private VCalendar cal2Calendar(String xml, String calType) throws EntityException {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder(xml.length() + 60);
            sb.append("Converting: ").append(calType).append(" => Calendar ")
              .append("\nINPUT = {").append(xml).append('}');
        }

        ByteArrayInputStream buffer = null;
        VCalendar calendar = null;
        try {
            calendar = new VCalendar();
            buffer = new ByteArrayInputStream(xml.getBytes());
            if ((xml.getBytes()).length > 0) {
                    XVCalendarParser parser = new XVCalendarParser(buffer);
                    calendar = parser.XVCalendar();
            } else {
                throw new EntityException("No data");
            }
        } catch (EntityException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityException("Error converting " + calType
                                    + " to Calendar. ", e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Conversion done.");
        }
        return calendar;
    }

    private String calendar2sif(Calendar calendar, String sifType) throws EntityException {
        if (log.isTraceEnabled()) {
            log.trace("Converting: Calendar => " + sifType);
        }

        try {
            BaseConverter c2xml;
            Object thing;

            if (sifType.equals(Type.EVENTSIF.getMimeType())) { // SIF-E
                c2xml = new CalendarToSIFE(_syncSource.getDeviceTimeZone(), _syncSource.getDeviceCharset());
                thing = calendar;  //TODO: should it not be Calendar.getEvent()?
            } else if (sifType.equals(Type.TASKSIF.getMimeType())) { // SIF-T
                c2xml = new TaskToSIFT(_syncSource.getDeviceTimeZone(), _syncSource.getDeviceCharset());
                thing = calendar.getTask();
            } else {
                return null;
            }

            String xml = c2xml.convert(thing);

            if (log.isTraceEnabled()) { log.trace("OUTPUT = {" + xml + "}. Conversion done."); }

            return xml;
        } catch(Exception e) {
            throw new EntityException("Error converting Calendar to " + sifType, e);
        }
    }

    private String calendar2cal(Calendar calendar, String calType) throws EntityException {
        if (log.isTraceEnabled()) {
            log.trace("Converting: Calendar => " + calType);
        }

        try {
            BaseConverter c2xml;
            Object thing;

            if (calType.equals(Type.EVENTCAL.getMimeType())) { // CAL

                c2xml = new VCalendarConverter(_syncSource.getDeviceTimeZone(), _syncSource.getDeviceCharset(), false);
                thing = calendar.getTask();
            } else if (calType.equals(Type.TASKCAL.getMimeType())) { // CAL
                c2xml = new VCalendarConverter(_syncSource.getDeviceTimeZone(), _syncSource.getDeviceCharset(), false);
                thing = calendar.getTask();
            } else {
                return null;
            }

            String xml = c2xml.convert(thing);

            if (log.isTraceEnabled()) {
                log.trace("OUTPUT = {" + xml + "}. Conversion done.");
            }

            return xml;
        } catch (Exception e) {
            throw new EntityException("Error converting Calendar to " + calType, e);
        }
    }

    /**
     * Extracts the content from a syncItem.
     *
     * @param syncItem
     * @return as a String object (same as
     *         PIMSyncSource#getContentFromSyncItem(String), but trimmed)
     */
    protected String getContentFromSyncItem(SyncItem syncItem) {
        byte[] itemContent = syncItem.getContent();

        String raw = new String(itemContent == null ? new byte[0] : itemContent);
        return raw.trim();
    }

    /**
     * Given a Date String in the format yyyy-MM-dd
     * and converts it to yyyy-mm-dd hh:mm:ss.fffffffff
     * and return the timestamp.
     */
    private Timestamp convertToTimestamp(String yearMonthDay) {
        if (StringUtils.isEmpty(yearMonthDay)) {
            return null;
        }

        return new Timestamp(_type.toDate(yearMonthDay).getTime());
    }

    /*
     * Convert dateCompleted, dateEnd and dateStart to timestamps.
     * Empty dates will be ignored.
     *
     * TODO: maybe this code should be done by a custom PropertyEditor?
     */
    private void convertDatesToTimestamp(CalendarContent task) {
        Property dateEnd = task.getDtEnd();
        Property dateStart = task.getDtStart();
        Property[] dates = {dateEnd, dateStart};

        for (Property date : dates) {
            if (!StringUtils.isEmpty(date.getPropertyValueAsString())) {
                Timestamp time = convertToTimestamp(date.getPropertyValueAsString());
                date.setPropertyValue(time);
            }
        }
    }

    /**
     * Prepare _allTasks.
     * Ideally call this only once for efficiency,
     */
    private List<GenericValue> prepareAllTasks() throws GeneralException {
        //1. get everything we are assigned to either as OWNER or ATTENDEE
        List<EntityCondition> criteria = new ArrayList<EntityCondition>();
        criteria.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, _syncPartyId));
        criteria.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, UtilMisc.toList("CAL_OWNER", "CAL_ATTENDEE")));
        List<GenericValue> allPartyAssingnment = _syncSource._delegator.findByAnd("WorkEffortPartyAssignment", criteria);

        //2. now filter the results by CALENDAR or TASK depending if workEffortTypeId matches what we are configured for
        return EntityUtil.getRelatedByAnd("WorkEffort", UtilMisc.toMap("workEffortTypeId", getWorkEffortType()), allPartyAssingnment);
    }
    //=== private behaviour ===

    /**
     * Return the correct workEffortTypeId depending on where we are configured to handle Task or Calendar.
     */
    private String getWorkEffortType() {
       return _type.getName();
    }

    /**
     * Change the hour of the givven timestamp.
     * @param dateTime the timestamp whose date and minutes should be used
     * @param hours hour in 24-hr clock to use
     * @return a new Timestamp, like the old one but with the given hours
     */
    private Timestamp setHours(Timestamp dateTime, int hours) {
       //1. convert our Timestamp to a Calendar
       java.util.Calendar dateTimeCal = new GregorianCalendar();
       dateTimeCal.setTime(dateTime);

       //2. alter the hours
       dateTimeCal.set(java.util.Calendar.HOUR_OF_DAY, hours);

       //3. convert back to a timestamp
       return new Timestamp(dateTimeCal.getTimeInMillis());
    }
}
