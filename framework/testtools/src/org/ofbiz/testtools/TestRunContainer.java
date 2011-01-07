/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
/* This file has been modified by Open Source Strategies, Inc. */
package org.ofbiz.testtools;
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Map;

import javolution.util.FastMap;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class TestRunContainer implements Container {

    public static final String module = TestRunContainer.class.getName();
    public static final String logDir = "runtime/logs/test-results/";

    protected String configFile = null;
    protected String component = null;
    protected String suiteName = null;
    protected String testCase = null;
    protected String logLevel = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                // arguments can prefix w/ a '-'. Just strip them off
                if (argument.startsWith("-")) {
                    int subIdx = 1;
                    if (argument.startsWith("--")) {
                        subIdx = 2;
                    }
                    argument = argument.substring(subIdx);
                }

                // parse the arguments
                if (argument.indexOf("=") != -1) {
                    String argumentName = argument.substring(0, argument.indexOf("="));
                    String argumentVal = argument.substring(argument.indexOf("=") + 1);

                    if ("component".equalsIgnoreCase(argumentName)) {
                        this.component = argumentVal;
                    }
                    if ("suitename".equalsIgnoreCase(argumentName)) {
                        this.suiteName = argumentVal;
                    }
                    if ("case".equalsIgnoreCase(argumentName)) {
                        this.testCase = argumentVal;
                    }
                    if ("loglevel".equalsIgnoreCase(argumentName)) {
                        this.logLevel = argumentVal;
                    }
                }
            }
        }

        // make sure the log dir exists
        File dir = new File(logDir);
        if (!dir.exists())
            dir.mkdir();
    }

    public boolean start() throws ContainerException {
        // configure log4j output logging
        if (logLevel != null) {
            int llevel = Debug.getLevelFromString(logLevel);

            for (int v = 0; v < 9; v++) {
                if (v < llevel) {
                    Debug.set(v, false);
                } else {
                    Debug.set(v, true);
                }
            }
        }

        // get the tests to run
        JunitSuiteWrapper jsWrapper = new JunitSuiteWrapper(component, suiteName, testCase);
        if (jsWrapper.getAllTestList().size() == 0) {
            throw new ContainerException("No tests found (" + component + " / " + suiteName + " / " + testCase + ")");
        }

        long suiteCount = jsWrapper.getModelTestSuites().size();
        long testCount = 0;
        for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {
            TestSuite suite = modelSuite.makeTestSuite();
            testCount += suite.countTestCases();
        }

        long suiteIdx = 1;
        long testIdx = 1;

        boolean failedRun = false;
        for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {
            Delegator testDelegator = modelSuite.getDelegator();
            TestSuite suite = modelSuite.makeTestSuite();
            JUnitTest test = new JUnitTest();
            test.setName(suite.getName());

            // create the XML logger
            JunitXmlListener xml;
            try {
                xml = new JunitXmlListener(new FileOutputStream(logDir + suite.getName() + ".xml"));
            } catch (FileNotFoundException e) {
                throw new ContainerException(e);
            }

            // per-suite results
            TestResult results = new TestResult();
            results.addListener(new JunitListener(testCount, testIdx));
            results.addListener(xml);

            // add the suite to the xml listener
            Debug.log("[JUNIT:Suite:Start] Starting test suite: [" + suiteIdx + "/" + suiteCount + "] " + suite.getName(), module);
            xml.startTestSuite(test);
            // run the tests
            suite.run(results);
            test.setCounts(results.runCount(), results.failureCount(), results.errorCount());
            // rollback all entity operations performed by the delegator
            testDelegator.rollback();
            xml.endTestSuite(test);
            Debug.log("[JUNIT:Suite:End] Ending test suite: [" + suiteIdx + "/" + suiteCount + "] " + suite.getName(), module);
            if (!results.wasSuccessful()) {
                failedRun = true;
            }

            // display the results
            Debug.log("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: " +
                    results.failureCount() + " # Errors: " + results.errorCount(), module);
            if (Debug.importantOn()) {
                Debug.log("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", module);
                Enumeration<?> err = results.errors();
                if (!err.hasMoreElements()) {
                    Debug.log("None");
                } else {
                    while (err.hasMoreElements()) {
                        Object error = err.nextElement();
                        Debug.log("--> " + error, module);
                        if (error instanceof TestFailure) {
                            Debug.log(((TestFailure) error).trace());
                        }
                    }
                }
                Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
                Debug.log("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", module);
                Enumeration<?> fail = results.failures();
                if (!fail.hasMoreElements()) {
                    Debug.log("None");
                } else {
                    while (fail.hasMoreElements()) {
                        Object failure = fail.nextElement();
                        Debug.log("--> " + failure, module);
                        if (failure instanceof TestFailure) {
                            Debug.log(((TestFailure) failure).trace());
                        }
                    }
                }
                Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
            }
            suiteIdx++;
            testIdx += results.runCount();
        }

        if (failedRun) {
            throw new ContainerException("Test run was unsuccessful");
        }
        return true;
    }

    public void stop() throws ContainerException {
    }

    public static String getProgress(long total, long idx) {
        if (total == 0) {
            return "NA";
        }
        double p = 100.0 * idx / total;
        DecimalFormat fmt = new DecimalFormat("0.00");
        return fmt.format(p) + "%";
    }

    class JunitXmlListener extends XMLJUnitResultFormatter {

        Map<String, Long> startTimes = FastMap.newInstance();

        public JunitXmlListener(OutputStream out) {
            this.setOutput(out);
        }

        @Override
        public void startTestSuite(JUnitTest suite) {
            startTimes.put(suite.getName(), System.currentTimeMillis());
            super.startTestSuite(suite);
        }

        @Override
        public void endTestSuite(JUnitTest suite) throws BuildException {
            long startTime = startTimes.get(suite.getName());
            suite.setRunTime((System.currentTimeMillis() - startTime));
            super.endTestSuite(suite);
        }
    }

    class JunitListener implements TestListener {

        public JunitListener(long count, long startIdx) {
            super();
            this.testCount = count;
            this.testIdx = startIdx;
        }

        private long testCount;
        private long testIdx;
        private long startTime;

        public void addError(Test test, Throwable throwable) {
            Debug.logWarning(throwable, "[JUNIT (error)] - " + getTestName(test) + " : " + throwable.toString(), module);
        }

        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            Debug.logWarning(assertionFailedError, "[JUNIT (failure)] - " + getTestName(test) + " : " + assertionFailedError.getMessage(), module);
        }

        public void endTest(Test test) {
            Debug.logInfo("[JUNIT (end)] : [" + testIdx + "/" + testCount + "][" + getProgress(testCount, testIdx) + "] " + getTestName(test) + " finished. (in " + (System.currentTimeMillis() - startTime) + " ms)", module);
            testIdx++;
        }

        public void startTest(Test test) {
            Debug.logInfo("[JUNIT (start)] : [" + testIdx + "/" + testCount + "][" + getProgress(testCount, testIdx) + "] " + getTestName(test) + " starting...", module);
            startTime = System.currentTimeMillis();
        }

        private String getTestName(Test test) {
            if (test instanceof TestCase) {
                return ((TestCase)test).getName();
            } else {
                return test.getClass().getName();
            }
         }
    }
}
