
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">
    <form name="testCalendarForm">
    <table class="twoColumnForm">
        <@inputDateRow name="dateTest" title="Test inputDate" weekNumbers=false/>
        <@inputDateTimeRow name="dateTimeTest" title="Test inputDateTime" popup=false/>
        <@inputDateTimeRow name="linkedDatesTest1" title="Test date1. Less then date2 in a week" linkedName="linkedDatesTest2" delta=7/>
        <@inputDateTimeRow name="linkedDatesTest2" title="Test date2. Greater then date1 in a week" linkedName="linkedDatesTest1" delta=-7/>
    </table>
    </form>
</div>