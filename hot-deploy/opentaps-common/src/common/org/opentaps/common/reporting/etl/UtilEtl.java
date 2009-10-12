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

package org.opentaps.common.reporting.etl;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.location.ComponentLocationResolver;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilDate;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleJobException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.version.BuildVersion;

/**
 * Utility class to deal with ETL jobs.
 */
public final class UtilEtl {

    private UtilEtl() { }

    public static final String MODULE = UtilEtl.class.getName();

    private static final String STRING_KITCHEN = "Kitchen";

    /**
     * Runs an ETL job.
     * @param jobFilename the name of the job, eg: "sales_tax_statement_etl_job.kjb"
     * @param reportsPath the path to the job, eg: "component://financials/webapp/financials/reports/repository"
     * @exception KettleException if an error occurs
     */
    public static void runJob(String jobFilename, String reportsPath) throws KettleException {
        EnvUtil.environmentInit();

        RepositoryMeta repinfo  = null;
        UserInfo       userinfo = null;
        Job            job      = null;

        StringBuffer optionRepname, optionUsername, optionPassword, optionJobname, optionDirname, optionFilename, optionLoglevel;
        StringBuffer optionLogfile, optionLogfileOld, optionListdir, optionListjobs, optionListrep, optionNorep, optionVersion;

        LogWriter log = LogWriter.getInstance(LogWriter.LOG_LEVEL_BASIC);

        String jobDirPath = "";
        String jobFilePath = "";
        String jobFilenameTemplate = "/${jobFilename}";
        try {
            jobDirPath = ComponentLocationResolver.getBaseLocation(reportsPath).toString();
            jobFilePath = ComponentLocationResolver.getBaseLocation(FlexibleStringExpander.expandString(reportsPath + jobFilenameTemplate, UtilMisc.toMap("jobFilename", jobFilename))).toString();
        } catch (MalformedURLException muex) {
            log.logError(STRING_KITCHEN, "Error resolving ETL files path.", muex);
        }

        optionRepname = new StringBuffer();
        optionUsername = new StringBuffer();
        optionPassword = new StringBuffer();
        optionJobname = new StringBuffer();
        optionDirname = new StringBuffer();
        optionFilename = new StringBuffer(jobFilePath);
        optionLoglevel = new StringBuffer();
        optionLogfile = new StringBuffer();
        optionLogfileOld = new StringBuffer();
        optionListdir = new StringBuffer();
        optionListdir = new StringBuffer();
        optionListjobs = new StringBuffer();
        optionListrep = new StringBuffer();
        optionNorep = new StringBuffer();
        optionVersion = new StringBuffer();

        String kettleRepname  = Const.getEnvironmentVariable("KETTLE_REPOSITORY", null);
        String kettleUsername = Const.getEnvironmentVariable("KETTLE_USER", null);
        String kettlePassword = Const.getEnvironmentVariable("KETTLE_PASSWORD", null);

        if (!Const.isEmpty(kettleRepname)) {
            optionRepname = new StringBuffer(kettleRepname);
        }
        if (!Const.isEmpty(kettleUsername)) {
            optionUsername = new StringBuffer(kettleUsername);
        }
        if (!Const.isEmpty(kettlePassword)) {
            optionPassword = new StringBuffer(kettlePassword);
        }

        LogWriter.setConsoleAppenderDebug();

        if (Const.isEmpty(optionLogfile) && !Const.isEmpty(optionLogfileOld)) {
            // if the old style of logging name is filled in, and the new one is not
            // overwrite the new by the old
            optionLogfile = optionLogfileOld;
        }

        if (Const.isEmpty(optionLogfile)) {
            log = LogWriter.getInstance(LogWriter.LOG_LEVEL_BASIC);
        } else {
            log = LogWriter.getInstance(optionLogfile.toString(), true, LogWriter.LOG_LEVEL_BASIC);
        }

        if (!Const.isEmpty(optionLoglevel)) {
            log.setLogLevel(optionLoglevel.toString());
            log.logMinimal(STRING_KITCHEN, "Logging is at level : " + log.getLogLevelDesc());
        }

        if (!Const.isEmpty(optionVersion)) {
            BuildVersion buildVersion = BuildVersion.getInstance();
            log.logBasic("Pan", "Kettle version " + Const.VERSION + ", build " + buildVersion.getVersion() + ", build date : " + buildVersion.getBuildDate());
            //if (a.length==1) System.exit(6);
        }

        // Start the action...
        //
        if (!Const.isEmpty(optionRepname) && !Const.isEmpty(optionUsername)) {
            log.logDetailed(STRING_KITCHEN, "Repository and username supplied");
        }

        log.logMinimal(STRING_KITCHEN, "Start of run.");

        /* Load the plugins etc.*/
        try {
            StepLoader.init();
        } catch (KettleException e) {
            log.logError(STRING_KITCHEN, "Error loading steps... halting Kitchen!", e);
        }
        StepLoader stepLoader = StepLoader.getInstance();

        /* Load the plugins etc.*/
        try {
            JobEntryLoader.init();
        } catch (KettleException e) {
            log.logError(STRING_KITCHEN, "Error loading job entries & plugins... halting Kitchen!", e);
            return;
        }

        Date start, stop;
        Calendar cal;
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        cal = Calendar.getInstance();
        start = cal.getTime();

        log.logDebug(STRING_KITCHEN, "Allocate new job.");
        JobMeta jobMeta = new JobMeta(log);

        // In case we use a repository...
        Repository repository = null;

        try {
            // Read kettle job specified on command-line?
            if (!Const.isEmpty(optionRepname) || !Const.isEmpty(optionFilename)) {
                log.logDebug(STRING_KITCHEN, "Parsing command line options.");
                if (!Const.isEmpty(optionRepname) && !"Y".equalsIgnoreCase(optionNorep.toString())) {
                    log.logDebug(STRING_KITCHEN, "Loading available repositories.");
                    RepositoriesMeta repsinfo = new RepositoriesMeta(log);
                    if (repsinfo.readData()) {
                        log.logDebug(STRING_KITCHEN, "Finding repository [" + optionRepname + "]");
                        repinfo = repsinfo.findRepository(optionRepname.toString());
                        if (repinfo != null) {
                            // Define and connect to the repository...
                            log.logDebug(STRING_KITCHEN, "Allocate & connect to repository.");
                            repository = new Repository(log, repinfo, userinfo);
                            if (repository.connect("Kitchen commandline")) {
                                RepositoryDirectory directory = repository.getDirectoryTree(); // Default = root

                                // Find the directory name if one is specified...
                                if (!Const.isEmpty(optionDirname)) {
                                    directory = repository.getDirectoryTree().findDirectory(optionDirname.toString());
                                }

                                if (directory != null) {
                                    // Check username, password
                                    log.logDebug(STRING_KITCHEN, "Check supplied username and password.");
                                    userinfo = new UserInfo(repository, optionUsername.toString(), optionPassword.toString());
                                    if (userinfo.getID() > 0) {
                                        // Load a job
                                        if (!Const.isEmpty(optionJobname)) {
                                            log.logDebug(STRING_KITCHEN, "Load the job info...");
                                            jobMeta =  new JobMeta(log, repository, optionJobname.toString(), directory);
                                            log.logDebug(STRING_KITCHEN, "Allocate job...");
                                            job = new Job(log, stepLoader, repository, jobMeta);
                                        } else if ("Y".equalsIgnoreCase(optionListjobs.toString())) {
                                            // List the jobs in the repository
                                            log.logDebug(STRING_KITCHEN, "Getting list of jobs in directory: " + directory);
                                            String[] jobnames = repository.getJobNames(directory.getID());
                                            for (int i = 0; i < jobnames.length; i++) {
                                                log.logError(jobnames[i], MODULE);
                                            }
                                        } else if ("Y".equalsIgnoreCase(optionListdir.toString())) {
                                            // List the directories in the repository
                                            String[] dirnames = repository.getDirectoryNames(directory.getID());
                                            for (int i = 0; i < dirnames.length; i++) {
                                                log.logError(dirnames[i], MODULE);
                                            }
                                        }
                                    } else {
                                        log.logError("ERROR: Can't verify username and password.", MODULE);
                                        userinfo = null;
                                        repinfo = null;
                                    }
                                } else {
                                    log.logError("ERROR: Can't find the supplied directory [" + optionDirname + "]", MODULE);
                                    userinfo = null;
                                    repinfo = null;
                                }
                            } else {
                                log.logError("ERROR: Can't connect to the repository.", MODULE);
                            }
                        } else {
                            log.logError("ERROR: No repository provided, can't load job.", MODULE);
                        }
                    } else {
                        log.logError("ERROR: No repositories defined on this system.", MODULE);
                    }
                }

                // Try to load if from file anyway.
                if (!Const.isEmpty(optionFilename) && job == null) {
                    jobMeta = new JobMeta(log, optionFilename.toString(), null, null);
                    job = new Job(log, stepLoader, null, jobMeta);
                }
            } else if ("Y".equalsIgnoreCase(optionListrep.toString())) {
                RepositoriesMeta ri = new RepositoriesMeta(log);
                if (ri.readData()) {
                    log.logError("List of repositories:", MODULE);
                    for (int i = 0; i < ri.nrRepositories(); i++) {
                        RepositoryMeta rinfo = ri.getRepository(i);
                        log.logError("#" + (i + 1) + " : " + rinfo.getName() + " [" + rinfo.getDescription() + "] ", MODULE);
                    }
                } else {
                    log.logError("ERROR: Unable to read/parse the repositories XML file.", MODULE);
                }
            }
        } catch (KettleException e) {
            job = null;
            jobMeta = null;
            log.logError("Processing stopped because of an error: " + e.getMessage(), MODULE);
        }

        if (job == null) {
            if (!"Y".equalsIgnoreCase(optionListjobs.toString())
                    && !"Y".equalsIgnoreCase(optionListdir.toString())
                    && !"Y".equalsIgnoreCase(optionListrep.toString())
            ) {
                log.logError("ERROR: Kitchen can't continue because the job couldn't be loaded.", MODULE);
            }
        }

        Result result = null;

        //int returnCode=0;

        try {
            job.getJobMeta().setArguments(null);
            job.initializeVariablesFrom(null);

            // set the path to where the transformation files are located
            job.setVariable("transformationsPath", jobDirPath);

            // set all parameters as internal variables
            job.getJobMeta().setInternalKettleVariables(job);

            result = job.execute(); // Execute the selected job.
            job.endProcessing("end", result);  // The bookkeeping...
        } catch (KettleJobException je) {
            if (result == null) {
                result = new Result();
            }
            result.setNrErrors(1L);

            try {
                job.endProcessing("error", result);
            } catch (KettleJobException je2) {
                log.logError(job.getName(), "A serious error occured : " + je2.getMessage());
                //returnCode = 2;
            }
        } finally {
            if (repository != null) {
                repository.disconnect();
            }
        }

        log.logMinimal(STRING_KITCHEN, "Finished!");

        if (result != null && result.getNrErrors() != 0) {
            log.logError(STRING_KITCHEN, "Finished with errors");
            //returnCode = 1;
        }
        cal = Calendar.getInstance();
        stop = cal.getTime();
        String begin = df.format(start).toString();
        String end = df.format(stop).toString();

        log.logMinimal(STRING_KITCHEN, "Start=" + begin + ", Stop=" + end);
        long millis = stop.getTime() - start.getTime();
        log.logMinimal(STRING_KITCHEN, "Processing ended after " + (millis / 1000) + " seconds.");
    }

    /**
     * Runs an ETL transformation.
     * 
     * @param transUrl transformation file, should be in component url format.
     * @throws KettleException
     * @throws MalformedURLException 
     */
    public static void runTrans(String location, String[] arguments) throws KettleException, MalformedURLException {

        String path = ComponentLocationResolver.getBaseLocation(location).toString();
        Debug.logInfo("Starting transformation at location" + path, MODULE);

        EnvUtil.environmentInit();
        StepLoader.init();

        TransMeta transMeta = new TransMeta(path);
        transMeta.setArguments(arguments);

        Trans trans = new Trans(transMeta);

        trans.prepareExecution(transMeta.getArguments());
        trans.startThreads();
        trans.waitUntilFinished();
        trans.endProcessing("end");

        if (trans.getErrors() > 0) {
            Debug.logError("There were errors during transformation execution.", MODULE);
        }
    }

    /**
     * This method allow look up surrogate key in dimension entity under certain conditions.
     * 
     * @param entityName Entity name
     * @param surrogateKeyName Name of dimension key field
     * @param lookupConditions Conditions to use together with <code>GenericDelegator</code> methods
     * @param <code>GenericDelegator</code> instance
     * @return
     *   surrogate key value - if only record found under the conditions<br>
     *   0 - in all other cases
     */
    public static Long lookupDimension(String entityName, String surrogateKeyName, EntityCondition lookupConditions, GenericDelegator delegator) {
        if (UtilValidate.isEmpty(entityName) || lookupConditions == null) {
            throw new IllegalArgumentException();
        }

        try {
            List<GenericValue> results = delegator.findByCondition(entityName, lookupConditions, UtilMisc.toSet(surrogateKeyName), null);
            if (results.size() == 1) {
                return EntityUtil.getFirst(results).getLong(surrogateKeyName);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return 0L;
        }

        return 0L;
    }

    /**
     * @throws GenericEntityException 
     * 
     */
    public static void setupDateDimension(GenericDelegator delegator, TimeZone timeZone, Locale locale) throws GenericEntityException {
        int YEARS = 100;
        Timestamp startDate = UtilDate.toTimestamp("1970-01-01 00:00:00.0", timeZone, locale);
        Timestamp endDate = UtilDateTime.adjustTimestamp(startDate, Calendar.YEAR, YEARS, timeZone, locale);

        long sequentialKey = 1L;
        Timestamp current = startDate;

        DateFormat dayOfMonthFmt = new SimpleDateFormat("dd");
        DateFormat weekOfYearFmt = new SimpleDateFormat("ww");
        DateFormat monthOfYearFmt = new SimpleDateFormat("MM");
        DateFormat yearNumberFmt = new SimpleDateFormat("yyyy");
        DateFormat nameDayFmt = new SimpleDateFormat("E");
        DateFormat nameMonthFmt = new SimpleDateFormat("MMMM");

        // clear DateDim
        delegator.removeByCondition("DateDim", EntityCondition.makeCondition("dateDimId", EntityOperator.NOT_EQUAL, null));

        // default row w/ 0 key
        delegator.create("DateDim", UtilMisc.toMap("dateDimId", Long.valueOf(0L)));

        do {
            Date currentDate = new Date(current.getTime());

            GenericValue dateDim = delegator.makeValue("DateDim");
            dateDim.set("dateDimId", Long.valueOf(sequentialKey));
            dateDim.set("dayOfMonth", dayOfMonthFmt.format(currentDate));
            dateDim.set("weekOfYear", weekOfYearFmt.format(currentDate));
            dateDim.set("monthOfYear", monthOfYearFmt.format(currentDate));
            dateDim.set("yearNumber", yearNumberFmt.format(currentDate));
            dateDim.set("nameDay", nameDayFmt.format(currentDate));
            dateDim.set("nameMonth", nameMonthFmt.format(currentDate));
            Calendar cal = Calendar.getInstance(timeZone, locale);
            cal.setTime(currentDate);
            int monthNum = cal.get(Calendar.MONTH);
            if (monthNum <= 3) {
                dateDim.set("quarter", "Q1");
            } else if (monthNum <= 6) {
                dateDim.set("quarter", "Q2");
            } else if (monthNum <= 9) {
                dateDim.set("quarter", "Q3");
            } else {
                dateDim.set("quarter", "Q4");
            }
            dateDim.create();

            current = UtilDateTime.adjustTimestamp(current, Calendar.DAY_OF_YEAR, 1, timeZone, locale);
            sequentialKey++;

        } while(current.compareTo(endDate) < 0);

    }
}

