    package org.jumpmind.symmetric;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.common.logging.LogFactory;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.ext.IExtensionPointManager;
import org.jumpmind.symmetric.job.IJobManager;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.NodeStatus;
import org.jumpmind.symmetric.service.IAcknowledgeService;
import org.jumpmind.symmetric.service.IBandwidthService;
import org.jumpmind.symmetric.service.IClusterService;
import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.service.IDataExtractorService;
import org.jumpmind.symmetric.service.IDataLoaderService;
import org.jumpmind.symmetric.service.IDataService;
import org.jumpmind.symmetric.service.IIncomingBatchService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IOutgoingBatchService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.service.IPullService;
import org.jumpmind.symmetric.service.IPurgeService;
import org.jumpmind.symmetric.service.IPushService;
import org.jumpmind.symmetric.service.IRegistrationService;
import org.jumpmind.symmetric.service.IRouterService;
import org.jumpmind.symmetric.service.ISecurityService;
import org.jumpmind.symmetric.service.IStatisticService;
import org.jumpmind.symmetric.service.ITriggerRouterService;
import org.jumpmind.symmetric.service.IUpgradeService;
import org.jumpmind.symmetric.util.AppUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractSymmetricEngine implements ISymmetricEngine {

    protected final ILog log = LogFactory.getLog(getClass());

    private static Map<String, ISymmetricEngine> registeredEnginesByUrl = new HashMap<String, ISymmetricEngine>();
    private static Map<String, ISymmetricEngine> registeredEnginesByName = new HashMap<String, ISymmetricEngine>();

    private ApplicationContext applicationContext;
    private JdbcTemplate jdbcTemplate;
    private boolean started = false;
    private boolean starting = false;
    private boolean setup = false;
    private IDbDialect dbDialect;
    private IJobManager jobManager;    
    
    protected AbstractSymmetricEngine() {
    }

    /**
     * Locate a {@link StandaloneSymmetricEngine} in the same JVM
     */
    public static ISymmetricEngine findEngineByUrl(String url) {
        if (registeredEnginesByUrl != null && url != null) {
            return registeredEnginesByUrl.get(url);
        } else {
            return null;
        }
    }

    /**
     * Locate a {@link StandaloneSymmetricEngine} in the same JVM
     */
    public static ISymmetricEngine findEngineByName(String name) {
        if (registeredEnginesByName != null && name != null) {
            return registeredEnginesByName.get(name.toLowerCase());
        } else {
            return null;
        }
    }

/**
     * Locate the one and only registered {@link StandaloneSymmetricEngine}.  Use {@link #findEngineByName(String)} or
     * {@link #findEngineByUrl(String) if there is more than on engine registered.
     * @throws IllegalStateException This exception happens if more than one engine is 
     * registered  
     */
    public static ISymmetricEngine getEngine() {
        int numberOfEngines = registeredEnginesByName.size();
        if (numberOfEngines == 0) {
            return null;
        } else if (numberOfEngines > 1) {
            throw new IllegalStateException("More than one SymmetricEngine is currently registered");
        } else {
            return registeredEnginesByName.values().iterator().next();
        }
    }

    public boolean isConfigured() {
        return getParameterService() != null && !StringUtils.isBlank(getParameterService().getNodeGroupId())
                && !StringUtils.isBlank(getParameterService().getExternalId())
                && !StringUtils.isBlank(getParameterService().getSyncUrl());
    }

    public synchronized void start() {
        if (!starting && !started) {
            try {
                starting = true;
                AppUtils.cleanupTempFiles();
                getParameterService().rereadParameters();
                setup();
                validateConfiguration();
                registerEngine();
                startDefaultServerJMXExport();
                Node node = getNodeService().findIdentity();
                if (node != null) {
                    log.info("RegisteredNodeStarting", node.getNodeGroupId(), node.getNodeId(), node.getExternalId());
                } else {
                    log.info("UnregisteredNodeStarting", getParameterService().getNodeGroupId(), getParameterService()
                            .getExternalId());
                }
                getTriggerService().syncTriggers();
                heartbeat(false);
                jobManager.startJobs();
                log
                        .info("SymmetricDSStarted", getParameterService().getExternalId(), Version.version(), dbDialect
                                .getName(), dbDialect.getVersion());
                started = true;
            } finally {
                starting = false;
            }
        }
    }

    public synchronized void stop() {
        log.info("SymmetricDSClosing", getParameterService().getExternalId(), Version.version(), dbDialect.getName());
        jobManager.stopJobs();
        removeMeFromMap(registeredEnginesByName);
        removeMeFromMap(registeredEnginesByUrl);
        DataSource ds = jdbcTemplate.getDataSource();
        if (ds instanceof BasicDataSource) {
            try {
                ((BasicDataSource) ds).close();
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
        applicationContext = null;
        jdbcTemplate = null;
        dbDialect = null;
        started = false;
        starting = false;
    }

    /**
     * @param overridePropertiesResource1
     *            Provide a Spring resource path to a properties file to be used for configuration
     * @param overridePropertiesResource2
     *            Provide a Spring resource path to a properties file to be used for configuration
     */
    protected void init(ApplicationContext ctx, boolean isParentContext, Properties overrideProperties,
            String overridePropertiesResource1, String overridePropertiesResource2) {
        // Setting system properties is probably not the best way to accomplish
        // this setup. Synchronizing on the class so creating multiple engines
        // is thread safe.
        synchronized (this.getClass()) {
            if (overrideProperties != null) {
                FileOutputStream fos = null;
                try {
                    File file = File.createTempFile("symmetric.", ".properties");
                    fos = new FileOutputStream(file);
                    overrideProperties.store(fos, "");
                    System.setProperty(Constants.OVERRIDE_PROPERTIES_FILE_TEMP, "file:" + file.getAbsolutePath());                    
                } catch (IOException ex) {
                    log.error(ex);
                    throw new RuntimeException(ex);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }
            System.setProperty(Constants.OVERRIDE_PROPERTIES_FILE_1, overridePropertiesResource1 == null ? ""
                    : overridePropertiesResource1);
            System.setProperty(Constants.OVERRIDE_PROPERTIES_FILE_2, overridePropertiesResource2 == null ? ""
                    : overridePropertiesResource2);
            if (isParentContext || ctx == null) {
                init(createContext(ctx));
            } else {
                init(ctx);
            }
        }
    }

    protected void init(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        dbDialect = AppUtils.find(Constants.DB_DIALECT, this);
        jobManager = AppUtils.find(Constants.JOB_MANAGER, this);
        jdbcTemplate = AppUtils.find(Constants.JDBC_TEMPLATE, this);
        
        IExtensionPointManager extMgr = (IExtensionPointManager)this.applicationContext.getBean(Constants.EXTENSION_MANAGER);
        extMgr.register();        
    }

    private ApplicationContext createContext(ApplicationContext parentContext) {
        return new ClassPathXmlApplicationContext(new String[] { "classpath:/symmetric.xml" }, parentContext);
    }

    private void removeMeFromMap(Map<String, ISymmetricEngine> map) {
        Set<String> keys = new HashSet<String>(map.keySet());
        for (String key : keys) {
            if (map.get(key).equals(this)) {
                map.remove(key);
            }
        }
    }

    /**
     * Register this instance of the engine so it can be found by other processes in the JVM.
     * 
     * @see #findEngineByUrl(String)
     */
    private void registerEngine() {
        registeredEnginesByUrl.put(getSyncUrl(), this);
        registeredEnginesByName.put(getEngineName(), this);
    }

    public String getSyncUrl() {
        Node node = getNodeService().findIdentity();
        if (node != null) {
            return node.getSyncUrl();
        } else {
            return getParameterService().getSyncUrl();
        }
    }

    /**
     * This is done dynamically because some application servers do not allow the default MBeanServer to be accessed for
     * security reasons (OC4J).
     */
    private void startDefaultServerJMXExport() {
        if (getParameterService().is(ParameterConstants.JMX_LEGACY_BEANS_ENABLED)) {
            try {
                getApplicationContext().getBean(Constants.DEFAULT_JMX_SERVER_EXPORTER);
            } catch (Exception ex) {
                log.warn("JMXBeansRegisterError ", ex.getMessage());
            }
        }
    }

    public Properties getProperties() {
        Properties p = new Properties();
        p.putAll(getParameterService().getAllParameters());
        return p;
    }

    public String getEngineName() {
        return dbDialect.getEngineName();
    }

    public synchronized void setup() {
        if (!setup) {
            setupDatabase(false);
            setup = true;
        }
    }

    public String reloadNode(String nodeId) {
        return getDataService().reloadNode(nodeId);
    }

    public String sendSQL(String nodeId, String tableName, String sql) {
        return getDataService().sendSQL(nodeId, tableName, sql);
    }

    public boolean push() {
        return ((IPushService) applicationContext.getBean(Constants.PUSH_SERVICE)).pushData();
    }

    public void syncTriggers() {
        getTriggerService().syncTriggers();
    }

    public NodeStatus getNodeStatus() {
        return getNodeService().getNodeStatus();
    }

    public boolean pull() {
        return ((IPullService) applicationContext.getBean(Constants.PULL_SERVICE)).pullData();
    }

    public void purge() {
        if (!Boolean.TRUE.toString().equalsIgnoreCase(getParameterService().getString(ParameterConstants.START_PURGE_JOB))) {
            getPurgeService().purge();
        } else {
            throw new UnsupportedOperationException("Cannot actuate a purge if it is already scheduled.");
        }
    }

    protected void setupDatabase(boolean force) {
        getConfigurationService().autoConfigDatabase(force);
        if (getUpgradeService().isUpgradeNecessary()) {
            if (getParameterService().is(ParameterConstants.AUTO_UPGRADE)) {
                try {
                    if (getUpgradeService().isUpgradePossible()) {
                        getUpgradeService().upgrade();
                        // rerun the auto configuration to make sure things are
                        // kosher after the upgrade
                        getConfigurationService().autoConfigDatabase(force);
                    } else {
                        throw new SymmetricException("SymmetricDSManualUpgradeNeeded", getNodeService()
                                .findSymmetricVersion(), Version.version());
                    }
                } catch (RuntimeException ex) {
                    log.fatal("SymmetricDSUpgradeFailed", ex);
                    throw ex;
                }
            } else {
                throw new SymmetricException("SymmetricDSUpgradeNeeded");
            }
        }

        // lets do this every time init is called.
        getClusterService().initLockTable();
    }

    public void validateConfiguration() {
        Node node = getNodeService().findIdentity();
        if (node == null && StringUtils.isBlank(getParameterService().getRegistrationUrl())) {
            throw new IllegalStateException(
                    String
                            .format(
                                    "Please set the property %s so this node may pull registration or manually insert configuration into the configuration tables.",
                                    ParameterConstants.REGISTRATION_URL));
        } else if (node != null
                && (!node.getExternalId().equals(getParameterService().getExternalId()) || !node.getNodeGroupId().equals(
                        getParameterService().getNodeGroupId()))) {
            throw new IllegalStateException(
                    "The configured state does not match recorded database state.  The recorded external id is "
                            + node.getExternalId() + " while the configured external id is "
                            + getParameterService().getExternalId() + ".  The recorded node group id is "
                            + node.getNodeGroupId() + " while the configured node group id is "
                            + getParameterService().getNodeGroupId());
        } else if (node != null && StringUtils.isBlank(getParameterService().getRegistrationUrl())
                && StringUtils.isBlank(getParameterService().getSyncUrl())) {
            throw new IllegalStateException(
                    "The sync.url property must be set for the registration server.  Otherwise, registering nodes will not be able to sync with it.");
        }
        
        long offlineNodeDetectionPeriodSeconds = getParameterService().getLong(ParameterConstants.OFFLINE_NODE_DETECTION_PERIOD_MINUTES)*60;
        long heartbeatSeconds = getParameterService().getLong(ParameterConstants.HEARTBEAT_SYNC_ON_PUSH_PERIOD_SEC);
        if (offlineNodeDetectionPeriodSeconds > 0 && offlineNodeDetectionPeriodSeconds <= heartbeatSeconds) {
            // Offline node detection is not disabled (-1) and the value is too small (less than the heartbeat)
            throw new IllegalStateException(
                "The " + ParameterConstants.OFFLINE_NODE_DETECTION_PERIOD_MINUTES + " property must be a longer period of time than the " +
                ParameterConstants.HEARTBEAT_SYNC_ON_PUSH_PERIOD_SEC + " property.  Otherwise, nodes will be taken offline before the heartbeat job has a chance to run.");
        }

        
        // TODO Add more validation checks to make sure that the system is
        // configured correctly

        // TODO Add method to configuration service to validate triggers and
        // call from here.
        // Make sure there are not duplicate trigger rows with the same name
    }

    public void heartbeat(boolean force) {
        getDataService().heartbeat(force);
    }

    public void openRegistration(String groupId, String externalId) {
        getRegistrationService().openRegistration(groupId, externalId);
    }

    public void reOpenRegistration(String nodeId) {
        getRegistrationService().reOpenRegistration(nodeId);
    }

    public boolean isRegistered() {
        return getNodeService().findIdentity() != null;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isStarting() {
        return starting;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public IConfigurationService getConfigurationService() {
        return AppUtils.find(Constants.CONFIG_SERVICE, this);
    }

    public IParameterService getParameterService() {
        return AppUtils.find(Constants.PARAMETER_SERVICE, this);
    }

    public INodeService getNodeService() {
        return AppUtils.find(Constants.NODE_SERVICE, this);
    }

    public IRegistrationService getRegistrationService() {
        return AppUtils.find(Constants.REGISTRATION_SERVICE, this);
    }

    public IUpgradeService getUpgradeService() {
        return AppUtils.find(Constants.UPGRADE_SERVICE, this);
    }

    public IClusterService getClusterService() {
        return AppUtils.find(Constants.CLUSTER_SERVICE, this);
    }

    public IPurgeService getPurgeService() {
        return AppUtils.find(Constants.PURGE_SERVICE, this);
    }

    public ITriggerRouterService getTriggerService() {
        return AppUtils.find(Constants.TRIGGER_ROUTER_SERVICE, this);
    }

    public IDataService getDataService() {
        return AppUtils.find(Constants.DATA_SERVICE, this);
    }

    public IDbDialect getDbDialect() {
        return dbDialect;
    }

    public IJobManager getJobManager() {
        return jobManager;
    }    
    
    public IOutgoingBatchService getOutgoingBatchService() {
    	return AppUtils.find(Constants.OUTGOING_BATCH_SERVICE, this);
    }
    
    public IAcknowledgeService getAcknowledgeService() {
    	return AppUtils.find(Constants.ACKNOWLEDGE_SERVICE, this);
    }
    
    public IBandwidthService getBandwidthService() {
    	return AppUtils.find(Constants.BANDWIDTH_SERVICE, this);
    }
    
    public IDataExtractorService getDataExtractorService() {
    	return AppUtils.find(Constants.DATAEXTRACTOR_SERVICE, this);
    }
    
    public IDataLoaderService getDataLoaderService() {
    	return AppUtils.find(Constants.DATALOADER_SERVICE, this);
    }
    
    public IIncomingBatchService getIncomingBatchService() {
    	return AppUtils.find(Constants.INCOMING_BATCH_SERVICE, this);
    }
    
    public IPullService getPullService() {
    	return AppUtils.find(Constants.PULL_SERVICE, this);
    }
    
    public IPushService getPushService() {
    	return AppUtils.find(Constants.PUSH_SERVICE, this);
    }
    
    public IRouterService getRouterService() {
    	return AppUtils.find(Constants.ROUTER_SERVICE, this);
    }
    
    public ISecurityService getSecurityService() {
    	return AppUtils.find(Constants.SECURITY_SERVICE, this);
    }
    
    public IStatisticService getStatisticService() {
    	return AppUtils.find(Constants.STATISTIC_SERVICE, this);
    }
    
    public ITriggerRouterService getTriggerRouterService() {
    	return AppUtils.find(Constants.TRIGGER_ROUTER_SERVICE, this);
    }

}