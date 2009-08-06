    opentaps Financials Module
    --------------------------

    This application is a Financials module for the opentaps/OFBiz applications suite and
performs such functions as posting transactions to the general ledger, accounts receivables
and payables, and financial reporting.

    To use it, put this entire module and all its sub-directories in the hot-deploy/ directory
of your opentaps/OFBiz suite.  Then load the seed data from the financials/data directory.  

    If you have problems with 
$ ant
then edit the financials/build.xml file and change the line for
      <property name="ofbiz.dir" value="../../"/>
to the absolute path of your ofbiz/ directory.

    By default, opentaps releases have overriden the references to accounting screens for invoices, payments,
and billing accounts with those to financials.  If you are running an SVN version of OFBIZ, you can do it with
$ patch -p 0 < hot-deploy/financials/replace-ofbiz-accounting.patch
    
    If you are using Eclipse, edit the .classpath file in your ofbiz/ directory and add the line
    <classpathentry kind="src" path="hot-deploy/financials/src"/>
so Eclipse would load the src/ directory as Java files.

    Licensing
    --------

    To help support the ongoing development of this application, it is released under a dual open source/commercial
licensing model.    
    
    This application is released free of charge to you under the Honest Public License (HPL), a modified
version of the GNU General Public License (GPL) with additional clarification for hosting and for software 
as a service.  The HPL allows you to use, modify, and re-distribute it, provided you meet its requirements.

    The HPL is a very different license than the MIT/Apache License of OFBiz.  Please take the time to
understand its differences and observe them in your use of both the Financials module and of larger
applications with which you have combined it.

    If you do not wish to be bound by the HPL in your use of this application, we also offer
commercial licenses which meet the needs of users, vendors, and services providers.  Please contact
us at info@opensourcestrategies.com for additional information.  Proceeds from the sale of commercial licenses 
help support further development of our open source applications and our communities.

    For other resources, including project task tracker and bug tracker, please visit
http://www.opensourcestrategies.com/ofbiz/accounting.php
 
