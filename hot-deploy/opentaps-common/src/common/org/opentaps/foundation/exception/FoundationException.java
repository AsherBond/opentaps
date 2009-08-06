package org.opentaps.foundation.exception;

import org.ofbiz.base.util.GeneralException;
import org.opentaps.common.util.UtilMessage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
* Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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

/**
 * This class is the base Exception class for all opentaps domain-related exceptions.  It is designed to allow you to do the following:
 *  1.  Set a flag to indicate whether a rollback is required or not
 *  2.  Set the error message expansion with messageLabel and messageContext
 *  3.  Set locale and return the error message
 *
 * This class should be extended to be more specific cases, for example:
 * FoundationException -> EntityException -> specific entity-related exceptions like EntityNotFoundException
 */
public class FoundationException extends GeneralException {

    private boolean requiresRollback = true;
    private String messageLabel = null;
    private Map messageContext = new HashMap();
    private Locale locale = Locale.getDefault();

    public FoundationException() {
        super();
    }

    public FoundationException(String message) {
        super(message);
    }

    public FoundationException(Throwable exception) {
        super(exception);
        // this is what enables exception messages to be passed from one layer to the next,
        // ie if I throw EntityNotFoundException -> RepositoryException -> ServiceException, the messages are kept.
        if (exception instanceof FoundationException) {
            setMessageLabel(((FoundationException) exception).getMessageLabel());
            setLocale(((FoundationException) exception).getLocale());
            setMessageContext(((FoundationException) exception).getMessageContext());
            setRequiresRollback(((FoundationException) exception).isRequiresRollback());
        }
    }
    
    public FoundationException(String messageLabel, Locale locale) {
        setMessageLabel(messageLabel);
        setLocale(locale);
    }

    public FoundationException(String messageLabel, Map messageContext) {
        setMessageLabel(messageLabel);
        setMessageContext(messageContext);
    }

    public FoundationException(String messageLabel, Map messageContext, Locale locale) {
        setMessageLabel(messageLabel);
        setLocale(locale);
        setMessageContext(messageContext);
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setRequiresRollback(boolean requiresRollback) {
        this.requiresRollback = requiresRollback;
    }

    public void setMessageLabel(String messageLabel) {
        this.messageLabel = messageLabel;
    }

    public void setMessageContext(Map messageContext) {
        this.messageContext = messageContext;
    }

    public String getMessageLabel() {
        return messageLabel;
    }

    public Map getMessageContext() {
        return messageContext;
    }

    public boolean isRequiresRollback() {
        return requiresRollback;
    }

    public String getMessage() {
        if (messageLabel == null) {
            return super.getMessage();
        } else {
            return (UtilMessage.expandLabel(messageLabel, locale, messageContext));
        }
    }

    public String getMessage(Locale locale) {
        setLocale(locale);
        return getMessage();
    }
}
