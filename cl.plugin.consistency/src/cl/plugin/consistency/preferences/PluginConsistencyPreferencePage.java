package cl.plugin.consistency.preferences;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
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
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import cl.plugin.consistency.Cache;
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
    //    setDescription("Plugin consistency");

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
    Composite content = new Composite(parent, SWT.NONE);

    GridLayout contentGridLayout = new GridLayout(1, false);
    contentGridLayout.marginWidth = contentGridLayout.marginHeight = 0;
    contentGridLayout.verticalSpacing = 10;
    content.setLayout(contentGridLayout);

    GridData layoutData = new GridData(GridData.FILL_BOTH);
    layoutData.widthHint = 800; // decommenter
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
    launchCheckConsistencyButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        LaunchCheckConsistencyHandler.launchConsistencyCheck(getShell(), pluginConsistency);
      }
    });
  }

  /**
   * Configure Import Consistency File
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
    reloadPluginConsistencyFileButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        String filePath = pluginConsistencyFileText.getText();
        String message = checkConsistencyFilePath(filePath, true, true);
        if (message != null)
        {
          MessageDialog.openError(getShell(), "Cannot reload plugin consistency file", message);
          return;
        }

        pluginConsistency = Util.loadAndUpdateConsistencyFile(Util.getConsistencyFile(filePath));
        pluginTabFolder.refresh();
      }
    });

    //
    Button saveModelButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    saveModelButton.setText("Save");
    saveModelButton.setToolTipText("Save model to XML file");
    saveModelButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
    saveModelButton.addSelectionListener(new SelectionAdapter()
    {
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
    findPluginConsistencyFromFileSystemButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText("Select plugin consistency file");

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
        }
      }
    });

    //
    Button findPluginConsistencyFromWorkspaceButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    findPluginConsistencyFromWorkspaceButton.setText("Workspace");
    findPluginConsistencyFromWorkspaceButton.setToolTipText("Select consistency file from workspace");
    findPluginConsistencyFromWorkspaceButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT));
    findPluginConsistencyFromWorkspaceButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle("Select plugin consistency file from workspace");
        dialog.setMessage("Select plugin consistency file :");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setAllowMultiple(false);

        String consistency_file_path = pluginConsistencyFileText.getText();
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(consistency_file_path);
        dialog.setInitialSelection(resource);

        ISelectionStatusValidator validator = selection -> {
          String message = "Not a file";
          if (selection.length == 1 && selection[0] instanceof IFile)
            message = null;
          return new Status(message == null? Status.OK : Status.ERROR, PluginConsistencyActivator.PLUGIN_ID, message);
        };
        dialog.setValidator(validator);
        if (dialog.open() == Window.OK)
        {
          IFile consistencyFile = (IFile) dialog.getFirstResult();
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

    // remove useless pluginInfo
    PluginConsistency compactPluginConsistency = pluginConsistency.compact();

    try
    {
      File pluginConsistencyFile = Util.getConsistencyFile(consistency_file_path);
      if (pluginConsistencyFile == null)
        throw new Exception("The path does not exists");

      // save
      Util.savePluginConsistency(pluginConsistency, pluginConsistencyFile);

      IProject project = Util.getWorkspaceProject(consistency_file_path);
      if (project != null)
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
    }
    catch(Exception e)
    {
      message = "Exception when saving plugin consistency file : " + e.getLocalizedMessage();
      PluginConsistencyActivator.logError(message, e);
      MessageDialog.openError(getShell(), "Error", message);
      return null;
    }

    return compactPluginConsistency;
  }

  /**
   * Check consistency file path
   * @param consistency_file_path
   */
  private String checkConsistencyFilePath(String consistency_file_path, boolean mustExists, boolean checkLoad)
  {
    if (consistency_file_path == null || consistency_file_path.isEmpty())
      return "Define a path for plugin consistency informations";

    File pluginConsistencyFile = Util.getConsistencyFile(consistency_file_path);
    if (mustExists && (pluginConsistencyFile == null || !pluginConsistencyFile.exists()))
      return "The path does not exists";

    if (checkLoad && !Util.canLoadConsistencyFile(pluginConsistencyFile))
      return "Cannot load plugin consistency file";

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
}
