package io.undertow.servlet.api;

import io.undertow.servlet.core.ConvergedSessionManagerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.sip.SipServletRequest;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.annotation.ConcurrencyControlMode;
import org.mobicents.servlet.sip.core.MobicentsSipServlet;
import org.mobicents.servlet.sip.core.descriptor.MobicentsSipServletMapping;
import org.mobicents.servlet.sip.core.security.MobicentsSipLoginConfig;
import org.mobicents.servlet.sip.ruby.SipRubyController;

public class ConvergedDeploymentInfo extends DeploymentInfo implements Cloneable {
    private static final Logger logger = Logger.getLogger(ConvergedDeploymentInfo.class);

    private final Map<String, ServletInfo> sipServlets = new HashMap<>();
    private SessionManagerFactory sessionManagerFactory = new ConvergedSessionManagerFactory();

    // sip-xml meta:
    protected String applicationName;
    protected String description;
    protected String smallIcon;
    protected String largeIcon;
    protected int proxyTimeout;
    protected int sipApplicationSessionTimeout;
    // Issue 1200 this is needed to be able to give a default servlet handler if we are not in main-servlet servlet
    // selection case
    // by example when creating a new sip application session from a factory from an http servlet
    private String servletHandler;
    protected boolean isMainServlet;
    private String mainServlet;
    protected transient MobicentsSipLoginConfig sipLoginConfig;
    protected transient Method sipApplicationKeyMethod;
    protected ConcurrencyControlMode concurrencyControlMode;
    protected transient List<String> sipApplicationListeners = new CopyOnWriteArrayList<String>();
    protected transient List<MobicentsSipServletMapping> sipServletMappings = new ArrayList<MobicentsSipServletMapping>();
    private transient SipRubyController rubyController;
    protected transient Map<String, MobicentsSipServlet> childrenMap;
    protected transient Map<String, MobicentsSipServlet> childrenMapByClassName;

    public ConvergedDeploymentInfo(){
        //default constructor
    }
    
    //using reflection to copy fields fast:
    public ConvergedDeploymentInfo(DeploymentInfo info) {
        Class fromClass = info.getClass();
        Class toClass = super.getClass().getSuperclass();
        for(Field fromField : fromClass.getDeclaredFields()){
            for(Field toField : toClass.getDeclaredFields()){
                if(toField.getType() == fromField.getType() &&
                        toField.getName().equals(fromField.getName()))
                {
                    toField.setAccessible(true);
                    fromField.setAccessible(true);
                    try {
                        toField.set(this, fromField.get(info));
                    } catch (IllegalArgumentException | IllegalAccessException e1) {
                        e1.printStackTrace();
                    }
                    fromField.setAccessible(false);
                    toField.setAccessible(false);
                }
            }
        }
    }

    public ConvergedDeploymentInfo addSipServlets(final ServletInfo... servlets) {
        for (final ServletInfo servlet : servlets) {
            sipServlets.put(servlet.getName(), servlet);
            return this;

        }
        return this;
    }

    public Map<String, ServletInfo> getSipServlets() {
        return Collections.unmodifiableMap(sipServlets);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSmallIcon() {
        return smallIcon;
    }

    public void setSmallIcon(String smallIcon) {
        this.smallIcon = smallIcon;
    }

    public String getLargeIcon() {
        return largeIcon;
    }

    public void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }

    public int getProxyTimeout() {
        return proxyTimeout;
    }

    public void setProxyTimeout(int proxyTimeout) {
        this.proxyTimeout = proxyTimeout;
    }

    public int getSipApplicationSessionTimeout() {
        return sipApplicationSessionTimeout;
    }

    public void setSipApplicationSessionTimeout(int sipApplicationSessionTimeout) {
        this.sipApplicationSessionTimeout = sipApplicationSessionTimeout;
    }

    public String getServletHandler() {
        return servletHandler;
    }

    public void setServletHandler(String servletHandler) {
        this.servletHandler = servletHandler;
    }

    public boolean isMainServlet() {
        return isMainServlet;
    }

    public void setMainServlet(boolean isMainServlet) {
        this.isMainServlet = isMainServlet;
    }

    public String getMainServlet() {
        return mainServlet;
    }

    public void setMainServlet(String mainServlet) {
        this.mainServlet = mainServlet;
    }

    public void addSipApplicationListener(String listener) {
        sipApplicationListeners.add(listener);
        // TODO:fireContainerEvent("addSipApplicationListener", listener);
    }

    public void removeSipApplicationListener(String listener) {
        sipApplicationListeners.remove(listener);

        // Inform interested listeners
        // TODO:fireContainerEvent("removeSipApplicationListener", listener);
    }

    public void addSipServletMapping(MobicentsSipServletMapping sipServletMapping) {
        sipServletMappings.add(sipServletMapping);
        isMainServlet = false;
        if (servletHandler == null) {
            servletHandler = sipServletMapping.getServletName();
        }
    }

    public List<MobicentsSipServletMapping> findSipServletMappings() {
        return sipServletMappings;
    }

    public MobicentsSipServletMapping findSipServletMappings(SipServletRequest sipServletRequest) {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking sip Servlet Mapping for following request : " + sipServletRequest);
        }
        for (MobicentsSipServletMapping sipServletMapping : sipServletMappings) {
            if (sipServletMapping.getMatchingRule().matches(sipServletRequest)) {
                return sipServletMapping;
            } else {
                logger.debug("Following mapping rule didn't match : servletName => "
                        + sipServletMapping.getServletName() + " | expression = "
                        + sipServletMapping.getMatchingRule().getExpression());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void removeSipServletMapping(MobicentsSipServletMapping sipServletMapping) {
        sipServletMappings.remove(sipServletMapping);
    }

    public String[] findSipApplicationListeners() {
        return sipApplicationListeners.toArray(new String[sipApplicationListeners.size()]);
    }

    public Method getSipApplicationKeyMethod() {
        return sipApplicationKeyMethod;
    }

    public void setSipApplicationKeyMethod(Method sipApplicationKeyMethod) {
        this.sipApplicationKeyMethod = sipApplicationKeyMethod;
    }

    public ConcurrencyControlMode getConcurrencyControlMode() {
        return concurrencyControlMode;
    }

    public void setConcurrencyControlMode(ConcurrencyControlMode concurrencyControlMode) {
        this.concurrencyControlMode = concurrencyControlMode;
    }

    public MobicentsSipLoginConfig getSipLoginConfig() {
        return sipLoginConfig;
    }

    public void setSipLoginConfig(MobicentsSipLoginConfig sipLoginConfig) {
        this.sipLoginConfig = sipLoginConfig;
    }

    public SipRubyController getRubyController() {
        return rubyController;
    }

    public void setRubyController(SipRubyController rubyController) {
        this.rubyController = rubyController;
    }

    public Map<String, MobicentsSipServlet> getChildrenMap() {
        return childrenMap;
    }

    public void setChildrenMap(Map<String, MobicentsSipServlet> childrenMap) {
        this.childrenMap = childrenMap;
    }

    public Map<String, MobicentsSipServlet> getChildrenMapByClassName() {
        return childrenMapByClassName;
    }

    public void setChildrenMapByClassName(Map<String, MobicentsSipServlet> childrenMapByClassName) {
        this.childrenMapByClassName = childrenMapByClassName;
    }

    @Override
    public ConvergedDeploymentInfo clone() {
        Object parent = null;
        try {
            parent = super.clone("");
            ConvergedDeploymentInfo info = (ConvergedDeploymentInfo) parent;
            return info;
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public SessionManagerFactory getSessionManagerFactory() {
        return sessionManagerFactory;
    }

    public DeploymentInfo setSessionManagerFactory(final SessionManagerFactory sessionManagerFactory) {
        this.sessionManagerFactory = sessionManagerFactory;
        return this;
    }

    public List<String> getSipApplicationListeners() {
        return sipApplicationListeners;
    }

    public List<MobicentsSipServletMapping> getSipServletMappings() {
        return sipServletMappings;
    }

}
