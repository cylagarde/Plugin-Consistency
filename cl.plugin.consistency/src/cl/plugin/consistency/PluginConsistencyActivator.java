package cl.plugin.consistency;

import java.io.File;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import cl.plugin.consistency.model.PluginConsistency;

/**
 * The activator class controls the plug-in life cycle
 */
public class PluginConsistencyActivator extends AbstractUIPlugin
{
  // The plug-in ID
  public static final String PLUGIN_ID = "cl.plugin.consistency"; //$NON-NLS-1$

  public static final String CONSISTENCY_FILE_PATH = "CONSISTENCY_FILE_PATH";
  public static final String CONSISTENCY_ACTIVATION = "CONSISTENCY_ACTIVATION";

  // The shared instance
  private static PluginConsistencyActivator plugin;

  // The shared instance
  private static IBundleProjectService bundleProjectService;

  private PluginConsistency pluginConsistency;
  private String lastPluginConsistencyFilePath = null;
  private long lastModifiedPluginConsistencyFile = -1;

  /**
   *
   */
  private IResourceChangeListener checkPluginConsistencyResourceChangeListener;

  /**
   * The constructor
   */
  public PluginConsistencyActivator()
  {
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception
  {
    super.start(context);
    plugin = this;

    //
    ServiceReference<IBundleProjectService> ref = context.getServiceReference(IBundleProjectService.class);
    bundleProjectService = context.getService(ref);

    //    if (isPluginConsistencyActivated())
    activate();
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception
  {
    //
    desactivate();

    plugin = null;
    bundleProjectService = null;
    checkPluginConsistencyResourceChangeListener = null;

    super.stop(context);
  }

  /**
   * Activate ResourceChangeListener
   */
  void activate()
  {
    //
    if (checkPluginConsistencyResourceChangeListener == null)
    {
      initCheckPluginConsistencyResourceChangeListener();
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.addResourceChangeListener(checkPluginConsistencyResourceChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    //    Util.launchConsistencyCheck(getPluginConsistency(), null);
  }

  /**
   * Desactivate ResourceChangeListener
   */
  void desactivate()
  {
    if (checkPluginConsistencyResourceChangeListener != null)
    {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.removeResourceChangeListener(checkPluginConsistencyResourceChangeListener);
      checkPluginConsistencyResourceChangeListener = null;
    }
  }

  /**
   * Return the plugin consistency
   */
  public PluginConsistency getPluginConsistency()
  {
    return getPluginConsistency(true, true);
  }

  private PluginConsistency getPluginConsistency(boolean loadIfNull, boolean reloadIfModified)
  {
    String consistency_file_path = getConsistencyFilePath();
    File consistencyFile = consistency_file_path == null? null : Util.getConsistencyFile(consistency_file_path);
    if (consistencyFile == null)
      pluginConsistency = null;
    else if ((loadIfNull && pluginConsistency == null) || (reloadIfModified && !consistency_file_path.equals(lastPluginConsistencyFilePath) || consistencyFile.lastModified() != lastModifiedPluginConsistencyFile))
    {
      lastPluginConsistencyFilePath = consistency_file_path;
      lastModifiedPluginConsistencyFile = consistencyFile.lastModified();
      pluginConsistency = Util.loadAndUpdateConsistencyFile(consistencyFile);
    }

    return pluginConsistency;
  }

  /**
   * Return the consistency file path
   */
  public String getConsistencyFilePath()
  {
    String consistency_file_path = getPreferenceStore().getString(CONSISTENCY_FILE_PATH);
    return consistency_file_path;
  }

  /**
   * Set the consistency file path
   */
  public void setConsistencyFilePath(String consistency_file_path)
  {
    getPreferenceStore().putValue(CONSISTENCY_FILE_PATH, consistency_file_path);
    pluginConsistency = null;
    lastPluginConsistencyFilePath = null;
    lastModifiedPluginConsistencyFile = -1;
  }

  /**
   * Return if plugin is activated
   */
  public boolean isPluginConsistencyActivated()
  {
    boolean activation = getPreferenceStore().getBoolean(CONSISTENCY_ACTIVATION);
    return activation;
  }

  /**
   * Initialize ImageRegistry
   */
  @Override
  protected void initializeImageRegistry(ImageRegistry imageRegistry)
  {
    //
    for(Images img : Images.values())
      imageRegistry.put(img.getKey(), imageDescriptorFromPlugin(img.pluginId, img.path));
  }

  /**
   * Returns an image descriptor for the image
   * @param img the image key
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(Images img)
  {
    return plugin.getImageRegistry().getDescriptor(img.getKey());
  }

  /**
   * Returns an image for the image key
   * @param img The image key
   * @return the image
   */
  public static Image getImage(Images img)
  {
    return plugin.getImageRegistry().get(img.getKey());
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static PluginConsistencyActivator getDefault()
  {
    return plugin;
  }

  /**
   * Gets the bundleProjectService.
   *
   * @return the bundleProjectService
   */
  public static final IBundleProjectService getBundleProjectService()
  {
    return bundleProjectService;
  }

  /**
   */
  private void initCheckPluginConsistencyResourceChangeListener()
  {
    checkPluginConsistencyResourceChangeListener = new CheckPluginConsistencyResourceChangeListener();
  }

  /**
   * Convenience method for logging exceptions in the workbench
   * @param severity the severity
   * @param message the message to display
   * @param e the exception thrown
   */
  public static void log(int severity, String message, Throwable e)
  {
    ILog log = plugin != null? plugin.getLog() : Platform.getLog(FrameworkUtil.getBundle(PluginConsistencyActivator.class));
    log = log != null? log : IDEWorkbenchPlugin.getDefault().getLog();

    log.log(new Status(severity, PLUGIN_ID, message, e));
  }

  /**
   * Convenience method for logging events in the workbench
   * @param severity the severity
   * @param message the message to display
   */
  public static void log(int severity, String message)
  {
    log(severity, message, null);
  }

  /**
   * Convenience method for logging errors caused by exceptions in the workbench
   * @param message the message to display
   * @param e the exception thrown
   */
  public static void logError(String message, Throwable e)
  {
    log(IStatus.ERROR, message, e);
  }

  /**
   * Convenience method for logging errors caused by exceptions in the workbench
   * @param message the message to display
   */
  public static void logError(String message)
  {
    log(IStatus.ERROR, message);
  }

  /**
   * Convenience method for logging warnings caused by exceptions in the workbench
   * @param message the message to display
   */
  public static void logWarning(String message)
  {
    log(IStatus.WARNING, message);
  }

  /**
   * Convenience method for logging information in the workbench
   * @param message the message to display
   */
  public static void logInfo(String message)
  {
    log(IStatus.INFO, message);
  }
}
