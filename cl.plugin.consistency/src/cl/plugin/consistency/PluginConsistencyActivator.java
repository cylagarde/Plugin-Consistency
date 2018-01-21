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

    if (isPluginConsistencyActivated())
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
  public void activate()
  {
    //
    if (checkPluginConsistencyResourceChangeListener == null)
    {
      initCheckPluginConsistencyResourceChangeListener();
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.addResourceChangeListener(checkPluginConsistencyResourceChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    Util.launchConsistencyCheck(getPluginConsistency());
  }

  /**
   * Desactivate ResourceChangeListener
   */
  public void desactivate()
  {
    //
    if (checkPluginConsistencyResourceChangeListener != null)
    {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.removeResourceChangeListener(checkPluginConsistencyResourceChangeListener);
      checkPluginConsistencyResourceChangeListener = null;
    }
  }

  /**
   * Set the plugin consistency
   * @param pluginConsistency
   */
  public void setPluginConsistency(PluginConsistency pluginConsistency)
  {
    this.pluginConsistency = pluginConsistency;
  }

  /**
   * Return the plugin consistency
   */
  public PluginConsistency getPluginConsistency()
  {
    if (pluginConsistency == null)
    {
      String consistency_file_path = getConsistencyFilePath();
      File consistencyFile = consistency_file_path == null? null : new File(consistency_file_path);
      pluginConsistency = Util.loadAndUpdateConsistencyFile(consistencyFile, false);
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
    {
      imageRegistry.put(img.getKey(), imageDescriptorFromPlugin(img.pluginId, img.path));
    }
  }

  /**
   * Returns an image descriptor for the image
   * @param img the image path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(Images img)
  {
    return plugin.getImageRegistry().getDescriptor(img.getKey());
  }

  /**
   * Returns an image for the image key
   * @param imageKey The image key
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
   * @return
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

}
