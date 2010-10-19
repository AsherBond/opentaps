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
package org.opentaps.dataimport.domain;

import java.util.List;
import org.ofbiz.base.util.Debug;
import java.util.Locale;
import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.DataImportGlAccount;
import org.opentaps.base.entities.GlAccount;
import org.opentaps.base.entities.GlAccountClassTypeMap;
import org.opentaps.base.entities.GlAccountOrganization;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.dataimport.AccountingDataImportRepositoryInterface;
import org.opentaps.domain.dataimport.GlAccountImportServiceInterface;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.entity.hibernate.Transaction;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Import General Ledger accounts via intermediate DataImportGlAccount entity.
 */
public class GlAccountImportService extends DomainService implements GlAccountImportServiceInterface {
    
    private static final String MODULE = GlAccountImportService.class.getName();
    // session object, using to store/search pojos.
    private Session session;
    public String organizationPartyId;
    public int importedRecords;
    
    public GlAccountImportService() {
        super();
    }

    public GlAccountImportService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    /** {@inheritDoc} */
    public int getImportedRecords() {
        return importedRecords;
    }
    
    /** {@inheritDoc} */
    public void importGlAccounts() throws ServiceException{        
        try {
            this.session = this.getInfrastructure().getSession();
            
            AccountingDataImportRepositoryInterface imp_repo = this.getDomainsDirectory().getDataImportDomain().getAccountingDataImportRepository();
            LedgerRepositoryInterface ledger_repo  =this.getDomainsDirectory().getLedgerDomain().getLedgerRepository();
            
            List<DataImportGlAccount> dataforimp = imp_repo.findNotProcessesDataImportGlAccountEntries();
            
            int imported = 0;
            Transaction imp_tx1 = null;
            Transaction imp_tx2 = null;
            for(int i = 0; i < dataforimp.size(); i++){
                DataImportGlAccount rawdata = dataforimp.get(i);
                //import accounts as many as possible
                try{
                    imp_tx1 = null;
                    imp_tx2 = null;
                    
                    //begin importing raw data item
                    GlAccount glAccount = new GlAccount();
                    glAccount.setGlAccountId(rawdata.getGlAccountId());
                    glAccount.setParentGlAccountId(rawdata.getParentGlAccountId());     
                    glAccount.setAccountName(rawdata.getAccountName());
                    if(rawdata.getClassification() != null){
                        //decode account clasificationt to type Id and class Id
                        GlAccountClassTypeMap glAccountClassTypeMap = ledger_repo.findOne(GlAccountClassTypeMap.class, 
                                ledger_repo.map(GlAccountClassTypeMap.Fields.glAccountClassTypeKey, rawdata.getClassification()));
                        glAccount.setGlAccountTypeId(glAccountClassTypeMap.getGlAccountTypeId());
                        glAccount.setGlAccountClassId(glAccountClassTypeMap.getGlAccountClassId());
                    }
                    imp_tx1 = this.session.beginTransaction();
                    ledger_repo.createOrUpdate(glAccount);
                    imp_tx1.commit();

                    if(this.organizationPartyId != null){
                        //map organization party to GL accounts
                        GlAccountOrganization glAccountOrganization = new GlAccountOrganization();
                        glAccountOrganization.setOrganizationPartyId(this.organizationPartyId);
                        glAccountOrganization.setGlAccountId(rawdata.getGlAccountId());
                        glAccountOrganization.setFromDate(UtilDateTime.nowTimestamp());
                        
                        imp_tx2 = this.session.beginTransaction();
                        ledger_repo.createOrUpdate(glAccountOrganization);
                        imp_tx2.commit();
                    }
                    
                    String message = "Successfully imported General Ledger account [" + rawdata.getGlAccountId() + "].";                   
                    this.storeImportGlAccountSuccess(rawdata, imp_repo);
                    Debug.logInfo(message, MODULE);
                    
                    imported = imported + 1;
                     
                }catch(Exception ex){
                    String message = "Failed to import General Ledger account [" + rawdata.getGlAccountId() + "], Error message : " + ex.getMessage();
                    storeImportGlAccountError(rawdata, message, imp_repo);
                    
                    //rollback all if there was an error when importing item
                    if(imp_tx1 != null){
                        imp_tx1.rollback();
                    }
                    if(imp_tx2 != null){
                        imp_tx2.rollback();
                    }
                    
                    Debug.logError(ex, message, MODULE);
                    throw new ServiceException(ex.getMessage());
                }
            }
            
            this.importedRecords = imported;
            
        } catch (InfrastructureException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } catch (RepositoryException ex) {
            Debug.logError(ex, MODULE);
            throw new ServiceException(ex.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Helper method to store GL account import succes into <code>DataImportGlAccount</code> entity row.
     * @param rawdata item of <code>DataImportGlAccount</code> entity that was successfully imported
     * @param imp_repo repository of accounting
     * @throws org.opentaps.foundation.repository.RepositoryException
     */
    private void storeImportGlAccountSuccess(DataImportGlAccount rawdata, AccountingDataImportRepositoryInterface imp_repo) throws RepositoryException {
        // mark as success
        rawdata.setImportStatusId(StatusItemConstants.Dataimport.DATAIMP_IMPORTED);
        rawdata.setImportError(null);
        rawdata.setProcessedTimestamp(UtilDateTime.nowTimestamp());
        imp_repo.createOrUpdate(rawdata);
    }

    /**
     * Helper method to store GL account import error into <code>DataImportGlAccount</code> entity row.
     * @param rawdata item of <code>DataImportGlAccount</code> entity that was unsuccessfully imported
     * @param message error message
     * @param imp_repo repository of accounting
     * @throws org.opentaps.foundation.repository.RepositoryException
     */
    private void storeImportGlAccountError(DataImportGlAccount rawdata, String message, AccountingDataImportRepositoryInterface imp_repo) throws RepositoryException {
        // store the exception and mark as failed
        rawdata.setImportStatusId(StatusItemConstants.Dataimport.DATAIMP_FAILED);
        rawdata.setImportError(message);
        rawdata.setProcessedTimestamp(UtilDateTime.nowTimestamp());
        imp_repo.createOrUpdate(rawdata);
    }

}
