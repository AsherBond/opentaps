package org.opentaps.funambol.sync;

import java.sql.Timestamp;

import mz.co.dbl.siga.framework.base.NotConfiguredException;
import mz.co.dbl.siga.framework.entity.EntityBeanConverterRegistry;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.GeneralException;
import org.opentaps.funambol.sync.EntitySyncHandler.MergeStrategy;

import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

abstract public class AbstractSyncHandler<E>
{
	protected static FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.server");
	
	protected EntitySyncSource _syncSource;  //for whom we perform the sync.
	protected EntityBeanConverterRegistry _converters;  //convert beans<->Map or GenericValue
    
	protected MergeStrategy _mergeStrategy = MergeStrategy.server_wins;  //merge strategy at all levels
	protected String _normalizeRemoveRegex = "";  //default is to strip nothing
    
    //=== sync-specific data ===
	protected Timestamp _since;  //used in merge, to know when last sync was and thus decide whether server or client wins at per-subrecord level
	protected String _syncPartyId;  //partyId of syncing user
	  
	/**
     * Default (if this method is not called): server_wins
     */
    public void setMergeStrategy(String strategy)
    {
    	if( isInvalidStrategy(strategy) )
    	{
    		String message = "Merge Strategy setting [" + strategy + "] is on the list of invalid strategies {" + StringUtils.join(getInvalidStrategy(), ",") + "}"; 
            log.fatal(message);
            throw new NotConfiguredException(message);
    	}
    	
        try
        {
            _mergeStrategy = Enum.valueOf(MergeStrategy.class, strategy);
        }
        catch(IllegalArgumentException noSuchStrategyX)
        {
            String message = "Merge Strategy setting not recognized: [" + strategy + "]. Must be one of {" + StringUtils.join(MergeStrategy.values(), ",") + "}"; 
            log.fatal(message);
            throw new NotConfiguredException(message, noSuchStrategyX);
        }
    }
    
    public MergeStrategy getConfiguredMergeStrategy() { return _mergeStrategy; }
    
    /**
     * Set the regex used to remove chars from a String when it is normalized for comparison.
     * 
     * Default (if this method is not called): "" (empty String)
     */
    public void setNormalizeRemoveRegex(String regex) { _normalizeRemoveRegex = regex; }
    	
	public final void init(EntitySyncSource syncSource) throws GeneralException
	{
		_syncSource = syncSource;
        _converters = _syncSource.getConverters();
        _syncPartyId = _syncSource._userLogin.getString("partyId");
        _since = null;  //to avoid accidents, always clear this field at start of a new sync
        
		prepareHandler();
	}
	
	abstract protected void prepareHandler()throws GeneralException;
	
	public MergeStrategy[] getInvalidStrategy()
	{
		return new MergeStrategy[0];
	}
	
	public boolean isInvalidStrategy(String strategy)
	{
		for (MergeStrategy invalid : getInvalidStrategy())
    	{
			if( strategy.equals( invalid.toString() ) )
			{
				return true;
			}
		}
		
		return false;
	}
}