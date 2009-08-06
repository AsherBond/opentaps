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

package com.opensourcestrategies.crmsfa.reports;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;
import com.opensourcestrategies.crmsfa.teams.TeamHelper;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.StandardGradientPaintTransformer;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;

import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

/**
 * Charts for CRM/SFA using the JFree API.
 */
public class JFreeCrmsfaCharts {

    /**
     * Lead pipeline.  Description at http://www.opentaps.org/docs/index.php/CRMSFA_Dashboard
     * Note that this counts all the leads in the system for now.
     */
    public static String createLeadPipelineChart(GenericDelegator delegator, Locale locale) throws GenericEntityException, IOException {
        Map uiLabelMap = UtilMessage.getUiLabels(locale);
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // get all LEAD statuses that are not CONVERTED, or DEAD
        List<GenericValue> leadStatuses = ReportHelper.findLeadStatusesForDashboardReporting(delegator);

        // report number of leads for each status
        for (GenericValue status : leadStatuses) {
            String statusId = status.getString("statusId");
            long count = delegator.findCountByCondition("PartyAndStatus", new EntityExpr("statusId", EntityOperator.EQUALS, statusId), null);
            dataset.addValue(count, "", (String) status.get("description", locale));
        }

        // set up the chart
        JFreeChart chart = ChartFactory.createBarChart(
        	(String) uiLabelMap.get("CrmLeadPipeline"),
            null,
            null,
            dataset,
            PlotOrientation.HORIZONTAL,
            false,
            true,
            false
        );
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);
        chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // get a reference to the plot for further customisation...
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        // get the bar renderer to put effects on the bars
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false); // disable bar outlines

        // set up gradient paint on bar
        final GradientPaint gp = new GradientPaint(
            0.0f, 0.0f, new Color(227, 246, 206),
            0.0f, 0.0f, new Color(153, 204, 102)
        );
        renderer.setSeriesPaint(0, gp);
        
        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // save as a png and return the file name
        return ServletUtilities.saveChartAsPNG(chart, 360, 200, null);
    }

	/**
     * Opportunities by stage.  Description at
     * http://www.opentaps.org/docs/index.php/CRMSFA_Dashboard
     * Note that this counts all the opportunities in the system for now.
     */
    public static String createOpportunitiesbyStageChart(GenericDelegator delegator, Locale locale) throws GenericEntityException, IOException {
        Map uiLabelMap = UtilMessage.getUiLabels(locale);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        String currency = UtilConfig.getPropertyValue("crmsfa", "defaultCurrencyUomId");
        
        // get all sales opportunity stages that are not closed, or lost
        List<GenericValue> salesOpportunityStages = ReportHelper.findSalesOpportunityStagesForDashboardReporting(delegator);

        // report number of leads for each status
        for (GenericValue stage : salesOpportunityStages) {
            String opportunityStageId = stage.getString("opportunityStageId");
        	EntityCondition conditions = new EntityConditionList(
            	UtilMisc.toList(
            		new EntityExpr("opportunityStageId", EntityOperator.EQUALS, opportunityStageId),
            		new EntityExpr("currencyUomId", EntityOperator.EQUALS, currency)
            	),
            	EntityOperator.AND
            );
        	List<GenericValue> salesOpportunityEstimatedAmountTotals = delegator.findByConditionCache(
            	"SalesOpportunityEstimatedAmountTotalByStage",
            	conditions,
            	null,
            	null
            );
        	
        	if (salesOpportunityEstimatedAmountTotals != null && !salesOpportunityEstimatedAmountTotals.isEmpty()) {
        		GenericValue salesOpportunityEstimatedAmountTotal = salesOpportunityEstimatedAmountTotals.get(0);
        		dataset.addValue(salesOpportunityEstimatedAmountTotal.getDouble("estimatedAmountTotal"), "", (String) stage.get("description", locale));
        	}
        	else {
        		dataset.addValue(0, "", (String) stage.get("description", locale));
        	}
        }

        // set up the chart
        JFreeChart chart = ChartFactory.createBarChart(
                      (String) UtilMessage.expandLabel("CrmOpportunitiesbyStage", locale, UtilMisc.toMap("currency", currency)),
                      null,
                      null,
                      dataset,
                      PlotOrientation.HORIZONTAL,
                      false,
                      true,
                      false);
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);
        chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // get a reference to the plot for further customisation...
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        // get the bar renderer to put effects on the bars
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false); // disable bar outlines

        // set up gradient paint on bar
        final GradientPaint gp = new GradientPaint(
            0.0f, 0.0f, new Color(206, 246, 245),
            0.0f, 0.0f, new Color(51, 204, 204)
        );
        renderer.setSeriesPaint(0, gp);
       
        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // save as a png and return the file name
        return ServletUtilities.saveChartAsPNG(chart, 360, 200, null);
    }

    /**
     * Open Cases snapshot.  Description at http://www.opentaps.org/docs/index.php/CRMSFA_Dashboard
     * Note that this counts all the cases in the system for now.
     */
    public static String createOpenCasesChart(GenericDelegator delegator, Locale locale) throws GenericEntityException, IOException {
        Map uiLabelMap = UtilMessage.getUiLabels(locale);

        // create the dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // get all case statuses that are not "closed" (this is dynamic because statuses may be added at run time)
        List<GenericValue> statuses = ReportHelper.findCasesStagesForDashboardReporting(delegator);

        // Report number of cases for each status
        for (GenericValue status : statuses) {
            String statusId = status.getString("statusId");
            long count = delegator.findCountByCondition("CustRequest", new EntityExpr("statusId", EntityOperator.EQUALS, statusId), null);
            dataset.addValue(count, "", (String) status.get("description", locale));
        }

        // set up the chart
        JFreeChart chart = ChartFactory.createBarChart(
                (String) uiLabelMap.get("CrmOpenCases"),                    // chart title
                null,                                                       // domain axis label
                null,                                                       // range axis label
                dataset,                                                    // data
                PlotOrientation.HORIZONTAL,                                 // orientation
                false,                                                      // include legend
                true,                                                       // tooltips
                false                                                       // urls
        );
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);
        chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // get a reference to the plot for further customisation...
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        // get the bar renderer to put effects on the bars
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false); // disable bar outlines

        // set up gradient paint on bar
        final GradientPaint gp = new GradientPaint(
            0.0f, 0.0f, new Color(246, 227, 206),
            0.0f, 0.0f, new Color(204, 153, 102)
        );
        renderer.setSeriesPaint(0, gp);

        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // tilt the category labels so they fit
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
            CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
        );

        // save as a png and return the file name
        return ServletUtilities.saveChartAsPNG(chart, 360, 300, null);
    }

    /**
     * Team Member Activity Snapshot.  Description at http://www.opentaps.org/docs/index.php/CRMSFA_Dashboard
     * Note that this counts all the team members in the system for now.
     */
    public static String createActivitiesByTeamMemberChart(GenericDelegator delegator, Locale locale) throws GenericEntityException, IOException {
        Map uiLabelMap = UtilMessage.getUiLabels(locale);

        // create the dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // get all team members (read the TODO in this function)
        List<GenericValue> teamMembers = TeamHelper.getTeamMembersForOrganization(delegator);

        // condition to count activities
        EntityCondition conditions = new EntityConditionList( UtilMisc.toList(
                new EntityExpr("statusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED"),
                new EntityExpr("currentStatusId", EntityOperator.NOT_IN, UtilActivity.ACT_STATUSES_COMPLETED),
                EntityUtil.getFilterByDateExpr()
        ), EntityOperator.AND);

        // condition to count pending outbound emails
        EntityCondition commConditions = new EntityConditionList( UtilMisc.toList(
            new EntityExpr("communicationEventTypeId", EntityOperator.EQUALS, "EMAIL_COMMUNICATION"),
            new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "TASK"),
            new EntityExpr("workEffortPurposeTypeId", EntityOperator.EQUALS, "WEPT_TASK_EMAIL"),
            new EntityExpr("currentStatusId", EntityOperator.IN, UtilMisc.toList("TASK_SCHEDULED", "TASK_STARTED")),
            new EntityExpr("assignmentStatusId", EntityOperator.EQUALS, "PRTYASGN_ASSIGNED"),
            EntityUtil.getFilterByDateExpr()
        ), EntityOperator.AND);

        // count active work efforts for each team member
        for (GenericValue member : teamMembers) {
            String partyId = member.getString("partyId");
            EntityCondition memberCond = new EntityConditionList( UtilMisc.toList(conditions, new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND);
            long count = delegator.findCountByCondition("WorkEffortAndPartyAssign", memberCond, null);

            // subtract outbound emails
            EntityCondition commCond = new EntityConditionList( UtilMisc.toList(commConditions, new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND);
            count -= delegator.findCountByCondition("WorkEffortPartyAssignCommEvent", commCond, null);

            // bar will be the name of the team member
            StringBuffer name = new StringBuffer();
            name.append(member.get("lastName")).append(", ").append(member.get("firstName"));
            dataset.addValue(count, "", name.toString());
        }

        // set up the chart
        JFreeChart chart = ChartFactory.createBarChart(
                (String) uiLabelMap.get("CrmActivitiesByTeamMember"),       // chart title
                null,                                                       // domain axis label
                null,                                                       // range axis label
                dataset,                                                    // data
                PlotOrientation.HORIZONTAL,                                 // orientation
                false,                                                      // include legend
                true,                                                       // tooltips
                false                                                       // urls
        );
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);
        chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // get a reference to the plot for further customisation...
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        // get the bar renderer to put effects on the bars
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false); // disable bar outlines

        // set up gradient paint on bar
        final GradientPaint gp = new GradientPaint(
            0.0f, 0.0f, new Color(230, 230, 230),
            0.0f, 0.0f, new Color(153, 153, 153)
        );
        renderer.setSeriesPaint(0, gp);

        // by default the gradient is vertical, but we can make it horizontal like this
        renderer.setGradientPaintTransformer(
            new StandardGradientPaintTransformer(GradientPaintTransformType.HORIZONTAL)
        );

        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // save as a png and return the file name (vertical height depends on size of team members)
        return ServletUtilities.saveChartAsPNG(chart, 360, 100 + 25 * teamMembers.size(), null);
    }
}
