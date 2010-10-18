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

import java.io.Serializable;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import mz.co.dbl.siga.framework.entity.EntityBeanConverterRegistry;
import mz.co.dbl.siga.framework.workflow.MapBasedJndiInitialContextFactory;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.funambol.security.OFBizSync4jUser;
import org.opentaps.funambol.sync.EntitySyncHandler.MergeStrategy;
import org.springframework.context.ApplicationContext;

import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.AbstractSyncSource;
import com.funambol.framework.engine.source.ContentType;
import com.funambol.framework.engine.source.MergeableSyncSource;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.engine.source.SyncSourceInfo;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.security.Sync4jPrincipal;
import com.funambol.framework.server.Sync4jDevice;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.tools.beans.LazyInitBean;

/**
 * This class represents a <i>SyncSource</i> that can sync between external device data
 *  items and OFBiz Entities
 *
 * @author Cameron Smith, Eurico da Silva - Database, Lda - www.database.co.mz
 */
public class EntitySyncSource extends AbstractSyncSource implements MergeableSyncSource, Serializable, LazyInitBean
{	
    //=== static info: can be safely shared among multiple SyncSources ===
    
    private static FunambolLogger log = null;  //TODO: can this be static?
        
    //access to OFBiz services
    protected static GenericDelegator _delegator = GenericDelegator.getGenericDelegator("default");
    protected static LocalDispatcher _dispatcher;
    
    //access to our module's helper objects
    private static EntityBeanConverterRegistry _converters;
    private static ApplicationContext _spring;
    
    //general configuration
    private static EntitySyncSourceConfig _config;
    private static boolean _staticInitCompleted = false;  //set to true once all shared state has been setup
    
    //=== sync-specific-config: changes every time ===
   
    private EntitySyncHandler _handler;  //manages record manipulation, specific to OT and record type
    
    //objects representing the user and principal performing the sync
    protected Principal _principal = null;
    GenericValue _userLogin = null;

	private TimeZone deviceTimeZone = null;

	private String deviceCharset = null;
    
    //=== Initialization ===

    /**
     * TODO: check Performance no acesso a base de dados visto que criado um syncSource para cada sincronizao.
     */
    public EntitySyncSource()
    {
        log = FunambolLoggerFactory.getLogger("funambol.server");
        
        //2. set meta info stored in super class
        info = new SyncSourceInfo(new ContentType[]{
        		new ContentType("text/x-vcard", "1.1"),
        		new ContentType("text/x-s4j-sift", "1.0"),
                new ContentType("text/x-s4j-sife", "1.0")}, 0);        
    }
    
    public void init()
    {
    	log.info("INIT -> ");       
        
        //1. setup shared state - once only per JVM - no need to synchronize as only cost of repetition is a little performance
        if(!_staticInitCompleted)
        {
            _dispatcher = GenericDispatcher.getLocalDispatcher("funambol", _delegator);

            //1.1 Get at Spring application context from JNDI
            try
            {
                System.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, MapBasedJndiInitialContextFactory.class.getName());
                InitialContext initialContext = new InitialContext();
                _spring = (ApplicationContext)initialContext.lookup("spring");
                _converters = (EntityBeanConverterRegistry)_spring.getBean("converters");
                _config = (EntitySyncSourceConfig)_spring.getBean("syncSourceConfig");
                _staticInitCompleted = true;
            }
            catch (NamingException jndiX)
            {
                log.error("Could not get Spring because ", jndiX);  //TODO: what can we throw here?
            }
        }
        
        log.info("INIT <- ");
    }

    //=== Implement SyncSource ===
    
    /**
     * Returns a string representation of this SyncSource.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());

        sb.append(" - {name: ").append(getName());
        sb.append(" type: ").append(info.getPreferredType().type);
        sb.append(" uri: ").append(getSourceURI());
        sb.append("}");
        return sb.toString();
    }

    /**
     * SyncSource's beginSync()
     * A SyncSource instance is always used in a single-threaded manner by the DSS 
     *
     * @param context the context of the sync
     */
    public void beginSync(SyncContext context) throws SyncSourceException
    {
        super.beginSync(context);

        //if for some reason our shared state was not prepared, bail immediately
        if(!_staticInitCompleted) { throw new SyncSourceException("Shared configuration has not been initialized, check logs for [ERROR] messages"); }
        
        //get hold of UserLogin corresponding to this principal
        _principal = context.getPrincipal();
        context.getPrincipal().getDevice().getTimeZone();
        Sync4jDevice device  = context.getPrincipal().getDevice();
        String timezone      = device.getTimeZone    (        )  ;
        if (device.getConvertDate()) {
            if (timezone != null && timezone.length() > 0) {
                deviceTimeZone = TimeZone.getTimeZone(timezone);
            }
        }
        deviceCharset = device.getCharset();
        
        if(_principal instanceof Sync4jPrincipal)
        {
            Sync4jUser user = ((Sync4jPrincipal)_principal).getUser();
            
            if(user instanceof OFBizSync4jUser)
            {
                _userLogin = ((OFBizSync4jUser)user).getUserLogin();
            }
            else { throw new SyncSourceException("Only accept OFBizSync4jUser, not " + user); }
        }
        else { throw new SyncSourceException("Only accept Sync4jPrincipal, not " + _principal); }

        //prepare handler - it should be marked 'prototype' so spring will always construct us a fresh one to avoid any pollution from previous state
        _handler = (EntitySyncHandler)_spring.getBean(getSourceURI() + _config.getHandlerSuffix());  //eg. "otcontact" + "Handler" -> "otcontactHandler"
        try
        {
            begin();
            _handler.init(this);
            commit();
        }
        catch(Exception initX)
        {
            abort("Could not init EntitySyncHandler", initX);
        }
                
        log.info("BEGIN SYNC FOR: " + _userLogin);
    }

    /*
     * @see SyncSource
     */
    public SyncItemKey[] getAllSyncItemKeys() throws SyncSourceException
    {        
        log.info("getAllSyncItemKeys(" +  _principal + ")");       
			
        //2. convert to ids and return
		return extractKeys(_handler.getAllKeys());
    }

    /*
     * @see SyncSource
     */
    public SyncItemKey[] getNewSyncItemKeys(Timestamp since, Timestamp until) throws SyncSourceException
    {
        log.info("getNewSyncItemKeys(" + _principal+ " , " + since + " , " + until + ")");
        
        try
        {
            return extractKeys(_handler.getNewKeys(since));
        }
        catch (GeneralException listNewX) 
        {
            abort("getNewSyncItemKeys", listNewX); return new SyncItemKey[0]; //TODO: we never get here
        }
    }

    /*
     * @see SyncSource
     */
    public SyncItemKey[] getDeletedSyncItemKeys(Timestamp since, Timestamp until) throws SyncSourceException
    {
        log.info("getDeletedSyncItemKeys(" +  _principal + " , " + since  + " , " + until + ")");

        try
        {
            return extractKeys(_handler.getDeletedKeys(since));
        }
        catch (GeneralException listDeletedX) 
        {
            abort("getDeletedSyncItemKeys", listDeletedX); return new SyncItemKey[0]; //TODO: we never get here
        }
    }

    /*
     * @see SyncSource
     */
    public SyncItemKey[] getUpdatedSyncItemKeys(Timestamp since, Timestamp until) throws SyncSourceException
    {
        log.info("getUpdatedSyncItemKeys(" + _principal+ " , " + since + " , " + until + ")");

        try 
        {
            return extractKeys(_handler.getUpdatedKeys(since));
        } 
        catch (GenericEntityException detectUpdatedX) { abort("Could not detect updated keys", detectUpdatedX); return new SyncItemKey[0]; }     
    }

    /*
     * @see SyncSource
     * 
     * for now, treat the id as partyId of a contact
     */
    public SyncItem getSyncItemFromId(SyncItemKey syncItemKey) throws SyncSourceException
    {
        log.info("getSyncItemsFromId(" + _principal + ", " + syncItemKey + ")");
        
        try
        {
            //1. load up the relevant Party and subrecords
            String partyId = syncItemKey.getKeyAsString();
            Object contact = _handler.getItemFromId(partyId); 
          
            //2. convert Contact -> vcard and return
            return createItem(partyId, _handler.convertBeanToData(contact), SyncItemState.NEW);  //TODO: what state should we set here?
        }
        catch (GeneralException getX)
        {
            abort("Could not load all data for Party " + syncItemKey, getX); return null;
        }
    }
    
    /*
     * @see SyncSource
     */
    public void removeSyncItem(SyncItemKey syncItemKey,
                               Timestamp   time       ,
                               boolean     softDelete )
    throws SyncSourceException
    {
    	String deletedItemId = syncItemKey.getKeyAsString();
        
        //2. perform deletion
        begin();     //TODO: make the handler a Spring @Transactional proxied object   
        
        try
        {
            _handler.removeItem(deletedItemId);
            
            commit();
        }
        catch(GeneralException removeX) { abort(removeX); }        
    }

    /*
     * @see SyncSource
     */
    public SyncItem updateSyncItem(SyncItem syncItem) throws SyncSourceException 
    {
        log.info("updateSyncItem params:_principal , "+syncItem.getKey().getKeyAsString());
        
                
        String partyId = syncItem.getKey().getKeyAsString();
        
        try
        {
        	//1. transformar o SyncItem vcard -> Contact -> ContactMech
        	Object obj = _handler.convertDataToBean(syncItem);
        	
        	//2.1 if we got here we are going to alter OT data so start a transaction
			begin();	
            
            //2.2 perform the update specific to this kind of record
            _handler.updateItem(partyId, obj);
			           			
			//2. store everything
            commit();				
		}
        catch (GeneralException e) 
		{
        	abort(e);
		}
        finally
        {
            	try{ TransactionUtil.cleanSuspendedTransactions(); } catch(GenericTransactionException txX) {}  //TODO: is this really necessary?
        }
            
        //TODO: cant we just return the original since this was an update?             
        return new SyncItemImpl(this                                , //syncSource
                partyId , //key
                null                                , //mappedKey
                SyncItemState.UPDATED               , //state
                syncItem.getContent()               , //content
                null                                , //format
                syncItem.getType()                  , //type
                null                                //timestamp
            );
    }
    
    /**
     * Merge the remoteItem with the data in the record represented by the given key.
     * 
     * TODO: should the merge strategy be set at handler or sync source level?  Difficult to say with only one case.
     * 
     * @param syncItemKey PK of local item to be merged
     * @param remoteItem remote item to be merged
     * @return true if remoteItem was changed, ie remote client needs to re-update with it
     */
    public boolean mergeSyncItems(SyncItemKey localKey, SyncItem remoteItem) throws SyncSourceException
    {
        //0.1 control whether or not client must update
        boolean remoteBeanChanged = false;
        String key = localKey.getKeyAsString();
        
        //0.2 prepare transaction
        begin();     //TODO: make the handler a Spring @Transactional proxied object 
        
        Object remoteBean = null;
            
        try
        {
        	//1. convert record
        	remoteBean = _handler.convertDataToBean(remoteItem);

        	//2. perform merge
            //2.1 check strategy - we may not need to do a merge 
           MergeStrategy strategy = _handler.getConfiguredMergeStrategy();
           if(strategy == MergeStrategy.server_wins)
           {
               remoteBean = _handler.getItemFromId(key);
               remoteBeanChanged = true;
           }
           else if(strategy == MergeStrategy.client_wins)
           {
               updateSyncItem(remoteItem);
           }
           else if(strategy == MergeStrategy.merge)
           {              
               remoteBeanChanged = _handler.mergeItem(localKey.getKeyAsString(), remoteBean);  //2.2 record-level merge
           }
           else { abort("Do not recognize merge strategy: " + strategy, null); }  //with current code we will never get here, but someone could add to the enum

                          
            if(remoteBeanChanged)  //2.3 server changed remote bean so we need to pass changes back to remoteItem
            {
                remoteItem.setContent(_handler.convertBeanToData(remoteBean));
                //TODO: do we also need to set its status to MERGED or something?
            }
            
            commit();  //3 all data manipulation was successful so commit our work
            
            return remoteBeanChanged;  //inform DSS
        }
        catch(GeneralException removeX) { abort(removeX); return false; }     
    }

    /**
     * @throws SyncSourceException - always
     */
    @SuppressWarnings("finally")
	protected void abort(String message, Exception e) throws SyncSourceException
    {
    	log.error(e.getMessage());
    	try 
    	{
			rollback();  //TODO: only rollback if it wasnt a TransactionException?
		}
        catch (Exception x) { log.error("Rollback failed:"+x.getMessage()); } 
    	
    	throw new SyncSourceException(message, e);    	
    }
    
    private void abort(Exception e) throws SyncSourceException
    {
        abort("NO MESSAGE", e);
    }

	/**
     * @see SyncSource
     */
    public SyncItem addSyncItem(SyncItem syncItem) throws SyncSourceException
    {
        log.info("addSyncItem("+_principal+" , "+syncItem.getKey().getKeyAsString()+")");
        showSyncItem(syncItem);

        //1. transformar o SyncItem vcard -> Contact -> ContactMech
        Object contact = null;
  
        try 
        {
            contact = _handler.convertDataToBean(syncItem);
            
            //2.1 if we got here we are going to alter OT data so start a transaction
            begin();
            
            String partyId = _handler.addItem(contact);
            
            //2. store everything TODO: it seems that the transaction is not always being rolled back
            commit();

            SyncItemImpl newSyncItem = new SyncItemImpl(
            		this                  , //syncSource
                    partyId		  , //key
                    null                  , //mappedKey
                    SyncItemState.NEW     , //state
                    syncItem.getContent() , //content
                    null                  , //format
                    syncItem.getType()    , //type
                    syncItem.getTimestamp() //timestamp
                    );
            
            //8. return the original SyncItem as it was not altered in any way    
            return newSyncItem;
        }      
        catch(GeneralException badOTX)  //TODO: does throwing an X here cause whole sync to fail?
        {
            abort(badOTX); return null;
        }
        finally
        {
                try { TransactionUtil.cleanSuspendedTransactions(); } catch(GenericTransactionException txX) {}  //TODO: is this really necessary?
        }
    }
    
    /**
     * @see SyncSource
     */
    public SyncItemKey[] getSyncItemKeysFromTwin(SyncItem syncItem) throws SyncSourceException
    {
        try
        {
        	Object item = _handler.convertDataToBean(syncItem);
            return extractKeys(_handler.getKeysFromTwin(item));
        }
        catch(GeneralException twinX)
        {
            abort("getSyncItemKeysFromTwin failed", twinX); return null;
        }
    }

    /**
     * @see SyncSource
     * TODO: do we need to pay attention to this?
     */
    public void setOperationStatus(String operation, int statusCode, SyncItemKey[] keys)
    {
        StringBuffer message = new StringBuffer("Received status code '");
        message.append(statusCode).append("' for a '").append(operation).append("'").
                append(" for this items: ");

        for (int i = 0; i < keys.length; i++) { message.append("\n- " + keys[i].getKeyAsString()); }

        log.info(message.toString());
    }

    //=== private behaviour ===
    
    private SyncItem createItem(String id, byte[] content, char state)
    {
        log.info("Creating a SyncItem with: {key= " + id + ", content= " + content + ", state= " + state + "}");

        SyncItem item = new SyncItemImpl(this, id, state);

        item.setContent(content);
        item.setType(info.getPreferredType().type);

        return item;
    }
    
    //=== behaviour for related objects to use: TODO: any protected methods should be moved to appropriate handler, or to EntitySyncSourceHelper ===
    
    private void showSyncItem(SyncItem item)
    {
        log.info("\n------SHOWING SYNC ITEM:\n-----------------" +new String(item.getContent())+item.getType()+item.getFormat());
    }
           
    /**
     * Run the given service synchronously, using the userLogin related to the current principal
     */
    protected Map runSync(String service, Map params) throws GenericEntityException, GenericServiceException
    {
        params.put("userLogin", _userLogin);  //perform authentication
        
        log.warn("about to call |" + service + "| with: " + params);
        
        return _dispatcher.runSync(service, params);
    }
    
    /**
     * Convenience method for calling findByPK
     * 
     * @throws SyncSourceException to wrap any GenericEntityExceptions thrown by the delegator
     */
    protected GenericValue findByPrimaryKey(String entity, Map params) throws GenericEntityException
    {
        try { return _delegator.findByPrimaryKey(entity, params); }
        catch (GenericEntityException entityX) 
        { 
            throw new GenericEntityException("Could not find this entity " + entity + " with these params " + params, entityX);
        }
    }
    
    /**
     * Convenience method for calling findByAnd and getting back parameterized collection
     * 
     * @throws SyncSourceException to wrap any GenericEntityExceptions thrown by the delegator
     */
    protected List<GenericValue> findByAnd(String entity, Map params) throws GenericEntityException
    {
        return _delegator.findByAnd(entity, params);
    }
    
    protected EntityBeanConverterRegistry getConverters() { return _converters; }
    
    //=== private behaviour ===
    
    /**
     * Convenience method for calling findByAnd and getting back parameterized collection
     * 
     * @throws SyncSourceException to wrap any GenericEntityExceptions thrown by the delegator
     */
    protected List<GenericValue> findByAnd(String entity, EntityExpr... criteria) throws GenericEntityException
    {
        try { return _delegator.findByAnd(entity, Arrays.asList(criteria)); }
        catch (GenericEntityException entityX) 
        { 
            throw new GenericEntityException("Could not find this entity " + entity + " with these critera " + criteria, entityX);
        }
    }
    
    /**
     * Return keys based on the given Strings
     * 
     * @param keyStrings - sequence of keys
     */
    private SyncItemKey[] extractKeys(Iterable<String> keyStrings)
    {
        ArrayList<SyncItemKey> keys = new ArrayList<SyncItemKey>();
        for(String key : keyStrings) { keys.add(new SyncItemKey(key)); }

        return keys.toArray(new SyncItemKey[0]); 
    }
   
    /**
     * OFBiz uses java.sql.Timestamps all over the place which is really annoying but anyway...
     */
    protected Timestamp now() { return new Timestamp(System.currentTimeMillis()); }
    
    //=== Transaction Control ===
    //TODO: use a TransactionStrategy here?
    void begin() throws SyncSourceException
    { 
            try { TransactionUtil.begin(); } catch(GenericTransactionException beginX) { abort("Failed to BEGIN a transaction", beginX); }
    }
    
    void commit() throws SyncSourceException
    { 
            try { TransactionUtil.commit(); } catch(GenericTransactionException beginX) { abort("Failed to COMMIT a transaction", beginX); }
    }
    
    private void rollback() throws SyncSourceException
    {  
            try { TransactionUtil.rollback(); } catch(GenericTransactionException beginX) { abort("Failed to ROLLBACK a transaction", beginX); }
    }

	public String getDeviceCharset() { return deviceCharset; }

	public void setDeviceCharset(String deviceCharset) { this.deviceCharset = deviceCharset; }

	public TimeZone getDeviceTimeZone() { return deviceTimeZone; }

	public void setDeviceTimeZone(TimeZone deviceTimeZone) { this.deviceTimeZone = deviceTimeZone; }
}
