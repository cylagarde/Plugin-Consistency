
package cl.plugin.consistency.preferences;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.CheckPluginConsistencyResourceChangeListener;
import cl.plugin.consistency.Images;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.handlers.LaunchCheckConsistencyHandler;
import cl.plugin.consistency.model.PluginConsistency;

/**
 * This class represents a preference page that is contributed to the Preferences dialog.
 */
public class PluginConsistencyPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
  public static final String PLUGIN_CONSISTENCY_FILE_EXTENSION = "pcf";
  //
  public PluginConsistency pluginConsistency;

  //
  Button activateButton;
  Text pluginConsistencyFileText;
  PluginTabFolder pluginTabFolder;
  final Cache cache = new Cache();

  /**
   * Constructor
   */
  public PluginConsistencyPreferencePage()
  {
    setPreferenceStore(PluginConsistencyActivator.getDefault().getPreferenceStore());
    noDefaultAndApplyButton();

    // dont use PluginConsistencyActivator.getDefault().getPluginConsistency();
    String consistency_file_path = getPreferenceStore().getString(PluginConsistencyActivator.CONSISTENCY_FILE_PATH);
    File consistencyFile = Util.getConsistencyFile(consistency_file_path);
    pluginConsistency = Util.loadAndUpdateConsistencyFile(consistencyFile);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  @Override
  public void init(IWorkbench workbench)
  {
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent)
  {
    Bundle pluginConsistencyBundle = FrameworkUtil.getBundle(PluginConsistencyPreferencePage.class);
    String version = pluginConsistencyBundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    version = version.substring(0, version.lastIndexOf('.'));
    setTitle(getTitle() + " v" + version);
    Composite content = new Composite(parent, SWT.NONE);

    GridLayout contentGridLayout = new GridLayout(1, false);
    contentGridLayout.marginWidth = contentGridLayout.marginHeight = 0;
    contentGridLayout.verticalSpacing = 10;
    content.setLayout(contentGridLayout);

    GridData layoutData = new GridData(GridData.FILL_BOTH);
    layoutData.widthHint = 900; // decommenter
    layoutData.heightHint = 500; // commenter
    content.setLayoutData(layoutData);

    configureActivate(content);

    //
    configureImportConsistencyFile(content);

    //
    pluginTabFolder = new PluginTabFolder(this, content, SWT.FLAT | SWT.DOUBLE_BUFFERED);

    return null;
  }

  /**
   * Configure Activate checkbox
   *
   * @param content
   */
  private void configureActivate(Composite content)
  {
    //
    Composite activateComposite = new Composite(content, SWT.NONE);
    GridLayout activateGridLayout = new GridLayout(3, false);
    activateGridLayout.marginWidth = activateGridLayout.marginHeight = 0;
    activateGridLayout.verticalSpacing = 0;
    activateComposite.setLayout(activateGridLayout);
    activateComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    //
    activateButton = new Button(activateComposite, SWT.CHECK);
    activateButton.setText("Automatic plugin consistency check when manifest is modified");
    boolean activation = PluginConsistencyActivator.getDefault().isPluginConsistencyActivated();
    activateButton.setSelection(activation);

    //
    Label spaceLabel = new Label(activateComposite, SWT.NONE);
    spaceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    //
    Button launchCheckConsistencyButton = new Button(activateComposite, SWT.NONE);
    launchCheckConsistencyButton.setImage(Images.LAUNCH_CHECK_CONSISTENCY.getImage());
    launchCheckConsistencyButton.setToolTipText("Launch check consistency");
    launchCheckConsistencyButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        LaunchCheckConsistencyHandler.launchConsistencyCheck(getShell(), pluginConsistency);
      }
    });
  }

  /**
   * Configure Import Consistency File
   *
   * @param content
   */
  private void configureImportConsistencyFile(Composite content)
  {
    Composite pluginConsistencyFileComposite = new Composite(content, SWT.NONE);

    GridLayout pluginConsistencyFileGridLayout = new GridLayout(6, false);
    pluginConsistencyFileGridLayout.marginWidth = pluginConsistencyFileGridLayout.marginHeight = 0;
    pluginConsistencyFileGridLayout.horizontalSpacing = 7;
    pluginConsistencyFileComposite.setLayout(pluginConsistencyFileGridLayout);

    //    pluginConsistencyFileComposite.setLayout(new GridLayout(3, false));
    pluginConsistencyFileComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(pluginConsistencyFileComposite, SWT.NONE).setText("Import");
    pluginConsistencyFileText = new Text(pluginConsistencyFileComposite, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
    pluginConsistencyFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    String consistency_file_path = getPreferenceStore().getString(PluginConsistencyActivator.CONSISTENCY_FILE_PATH);
    pluginConsistencyFileText.setText(consistency_file_path);

    //
    Button reloadPluginConsistencyFileButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    reloadPluginConsistencyFileButton.setText("Reload");
    reloadPluginConsistencyFileButton.setToolTipText("Reload model from XML file");
    reloadPluginConsistencyFileButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_SYNCED));
    reloadPluginConsistencyFileButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        String filePath = pluginConsistencyFileText.getText();
        String message = checkConsistencyFilePath(filePath, true, true);
        if (message == null)
        {
          pluginConsistency = Util.loadAndUpdateConsistencyFile(Util.getConsistencyFile(filePath));
          pluginTabFolder.refresh();
        }
        else
          resetPluginConsistency("reload", message);
      }
    });

    //
    Button saveModelButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    saveModelButton.setText("Save");
    saveModelButton.setToolTipText("Save model to XML file");
    saveModelButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
    saveModelButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        checkAndSavePluginConsistency();
      }
    });

    //
    Button findPluginConsistencyFromFileSystemButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    findPluginConsistencyFromFileSystemButton.setText("File system");
    findPluginConsistencyFromFileSystemButton.setToolTipText("Select consistency file from file system");
    findPluginConsistencyFromFileSystemButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE));
    findPluginConsistencyFromFileSystemButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText("Select plugin consistency file");
        String[] filterExtensions = new String[]{"*." + PLUGIN_CONSISTENCY_FILE_EXTENSION, "*.*"};
        dialog.setFilterExtensions(filterExtensions);
        dialog.setFilterNames(new String[]{"Plugin consistency file (" + filterExtensions[0] + ")", "All files (" + filterExtensions[1] + ")"});

        String consistency_file_path = pluginConsistencyFileText.getText();
        File file = Util.getConsistencyFile(consistency_file_path);
        if (file != null)
        {
          dialog.setFilterPath(file.getParent());
          dialog.setFileName(file.getName());
        }

        String filePath = dialog.open();
        if (filePath != null)
        {
          pluginConsistencyFileText.setText(filePath);

          String message = checkConsistencyFilePath(filePath, true, true);
          if (message == null)
          {
            boolean result = MessageDialog.openQuestion(getShell(), "Load plugin consistency file", "Do you want to load plugin consistency file?");
            if (result)
            {
              pluginConsistency = Util.loadAndUpdateConsistencyFile(new File(filePath));
              pluginTabFolder.refresh();
            }
          }
          else
            resetPluginConsistency("load", message);
        }
      }
    });

    //
    Button findPluginConsistencyFromWorkspaceButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    findPluginConsistencyFromWorkspaceButton.setText("Workspace");
    findPluginConsistencyFromWorkspaceButton.setToolTipText("Select consistency file from workspace");
    findPluginConsistencyFromWorkspaceButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT));
    findPluginConsistencyFromWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        WorkspaceResourceDialog dialog = new WorkspaceResourceDialog(getShell()) {
          @Override
          protected String acceptFilename(String filename)
          {
            String message = super.acceptFilename(filename);
            if (message == null && !filename.endsWith("." + PLUGIN_CONSISTENCY_FILE_EXTENSION))
              message = "File name must have extension '." + PLUGIN_CONSISTENCY_FILE_EXTENSION + "'";
            return message;
          }
        };
        dialog.setTitle("Select plugin consistency path from workspace");
        dialog.setMessage("Select plugin consistency path :");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setAllowMultiple(false);

        String consistency_file_path = pluginConsistencyFileText.getText();
        Path path = new Path(consistency_file_path);
        if (ResourcesPlugin.getWorkspace().getRoot().exists(path))
        {
          IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
          dialog.setInitialSelection(resource);
        }

        dialog.setShowNewFolderControl(true);
        dialog.setShowFileControl(true);

        ViewerFilter filter = new ViewerFilter() {
          PluginConsistencyFileChecker pluginConsistencyFileChecker = new PluginConsistencyFileChecker();

          @Override
          public boolean select(Viewer viewer, Object parentElement, Object element)
          {
            return pluginConsistencyFileChecker.select(parentElement, element);
          }
        };
        dialog.addFilter(filter);

        //
        if (dialog.open() == Window.OK)
        {
          IFile consistencyFile = dialog.getFile();
          if (consistencyFile != null)
          {
            pluginConsistencyFileText.setText(consistencyFile.getFullPath().toString());

            String message = checkConsistencyFilePath(consistencyFile.getRawLocation().toOSString(), true, true);
            if (message == null)
            {
              boolean result = MessageDialog.openQuestion(getShell(), "Load plugin consistency file", "Do you want to load plugin consistency file?");
              if (result)
              {
                pluginConsistency = Util.loadAndUpdateConsistencyFile(consistencyFile.getRawLocation().toFile());
                pluginTabFolder.refresh();
              }
            }
            else
              resetPluginConsistency("load", message);
          }
        }
      }
    });
  }

  /**
   * @return the cache
   */
  public Cache getCache()
  {
    return cache;
  }

  /**
   * Check and save pluginConsistency into file
   */
  private PluginConsistency checkAndSavePluginConsistency()
  {
    String consistency_file_path = pluginConsistencyFileText.getText();
    String message = checkConsistencyFilePath(consistency_file_path, false, false);
    if (message != null)
    {
      MessageDialog.openError(getShell(), "Cannot save plugin consistency into file", message);
      return null;
    }

    try
    {
      File pluginConsistencyFile = Util.getConsistencyFile(consistency_file_path);
      if (pluginConsistencyFile == null)
        throw new Exception("The path does not exists");

      CheckPluginConsistencyResourceChangeListener.savePluginConsistencyAndRefreshIntoWorkspace(pluginConsistency, pluginConsistencyFile);

      return pluginConsistency;
    }
    catch(Exception e)
    {
      message = "Exception when saving plugin consistency file : " + e.getLocalizedMessage();
      PluginConsistencyActivator.logError(message, e);
      MessageDialog.openError(getShell(), "Error", message);
      return null;
    }
  }

  /**
   * Check consistency file path
   *
   * @param consistency_file_path
   */
  private String checkConsistencyFilePath(String consistency_file_path, boolean mustExists, boolean checkLoad)
  {
    if (consistency_file_path == null || consistency_file_path.isEmpty())
      return "Define a path for plugin consistency informations";

    File pluginConsistencyFile = Util.getConsistencyFile(consistency_file_path);
    if (mustExists && (pluginConsistencyFile == null || !pluginConsistencyFile.exists()))
      return "The path does not exists";

    if (checkLoad)
      return Util.canLoadConsistencyFile(pluginConsistencyFile);

    return null;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk()
  {
    // try to save
    PluginConsistency compactPluginConsistency = checkAndSavePluginConsistency();
    if (compactPluginConsistency == null)
      return false;

    //
    getPreferenceStore().setValue(PluginConsistencyActivator.CONSISTENCY_ACTIVATION, activateButton.getSelection());
    String consistency_file_path = pluginConsistencyFileText.getText();
    getPreferenceStore().setValue(PluginConsistencyActivator.CONSISTENCY_FILE_PATH, consistency_file_path);

    return super.performOk();
  }

  private void resetPluginConsistency(String verb, String message)
  {
    boolean result = MessageDialog.openQuestion(getShell(), "Error " + verb, "Cannot " + verb + " plugin consistency file : " + message + "\nDo you want to reset plugin consistency?");
    if (result)
    {
      pluginConsistency = new PluginConsistency();
      Util.updatePluginConsistency(pluginConsistency);
      pluginTabFolder.refresh();
    }
  }

  /**
   * The class <b>PluginConsistencyFileChecker</b> allows to.<br>
   */
  class PluginConsistencyFileChecker
  {
    //    String plugin_consistency_content_type_id = "cl.plugin.consistency.content-type";

    public boolean select(Object parentElement, Object element)
    {
      if (element instanceof IFile)
      {
        IFile iFile = (IFile) element;

        if (!PLUGIN_CONSISTENCY_FILE_EXTENSION.equals(iFile.getFileExtension()))
          return false;

        //  try
        //  {
        //    Optional<String> content_type =
        //      Optional.ofNullable(iFile.getContentDescription())
        //        .map(IContentDescription::getContentType)
        //        .map(IContentType::getId)
        //    // .filter(plugin_consistency_content_type_id::equals)
        //    ;
        //    System.out.println("getContentType " + content_type + " " + iFile);
        //  }
        //  catch(CoreException e)
        //  {
        //    e.printStackTrace();
        //  }

        // File file = iFile.getRawLocation().toFile();
        // return Util.canLoadConsistencyFile(file);
      }

      return true;
    }
  }
}
