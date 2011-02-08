package org.opentaps.tests.webapp;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class FakeHttpSession implements HttpSession {

    private Map attributes = new HashMap();

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Enumeration getAttributeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getCreationTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastAccessedTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxInactiveInterval() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getValue(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getValueNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void invalidate() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isNew() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void putValue(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    @Override
    public void removeValue(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    @Override
    public void setMaxInactiveInterval(int arg0) {
        // TODO Auto-generated method stub

    }

}
