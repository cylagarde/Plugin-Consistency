package cl.plugin.consistency.preferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

import cl.plugin.consistency.Activator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginConsistency;
import cl.plugin.consistency.model.util.PluginConsistencyLoader;

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

  /**
   * Constructor
   */
  public PluginConsistencyPreferencePage()
  {
    setPreferenceStore(Activator.getDefault().getPreferenceStore());
    noDefaultAndApplyButton();
    //    setDescription("Plugin consistency");

    //
    String consistency_file_path = getPreferenceStore().getString(Activator.CONSISTENCY_FILE_PATH);
    File consistencyFile = new File(consistency_file_path);
    pluginConsistency = Util.loadAndUpdateConsistencyFile(consistencyFile, true);
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
    layoutData.widthHint = 900; // decommenter
    layoutData.heightHint = 500; // commenter
    content.setLayoutData(layoutData);

    //
    activateButton = new Button(content, SWT.CHECK);
    activateButton.setText("Activate plugin consistency");
    boolean activation = Activator.getDefault().isPluginConsistencyActivated();
    activateButton.setSelection(activation);

    //
    configureImportConsistencyFile(content);

    //
    pluginTabFolder = new PluginTabFolder(this, content, SWT.FLAT);

    return null;
  }

  /**
   * Configure Import Consistency File
   * @param content
   */
  private void configureImportConsistencyFile(Composite content)
  {
    Composite pluginConsistencyFileComposite = new Composite(content, SWT.NONE);

    GridLayout pluginConsistencyFileGridLayout = new GridLayout(4, false);
    pluginConsistencyFileGridLayout.marginWidth = pluginConsistencyFileGridLayout.marginHeight = 0;
    pluginConsistencyFileComposite.setLayout(pluginConsistencyFileGridLayout);

    //    pluginConsistencyFileComposite.setLayout(new GridLayout(3, false));
    pluginConsistencyFileComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(pluginConsistencyFileComposite, SWT.NONE).setText("Import");
    pluginConsistencyFileText = new Text(pluginConsistencyFileComposite, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
    pluginConsistencyFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    String consistency_file_path = getPreferenceStore().getString(Activator.CONSISTENCY_FILE_PATH);
    pluginConsistencyFileText.setText(consistency_file_path);

    //
    Button reloadPluginConsistencyFileButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    reloadPluginConsistencyFileButton.setText("Reload");
    reloadPluginConsistencyFileButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        String filePath = pluginConsistencyFileText.getText();
        pluginConsistency = Util.loadAndUpdateConsistencyFile(new File(filePath), true);

        pluginTabFolder.refresh();
      }
    });

    //
    Button findPluginConsistencyFileButton = new Button(pluginConsistencyFileComposite, SWT.NONE);
    findPluginConsistencyFileButton.setText("Browse...");
    findPluginConsistencyFileButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent se)
      {
        FileDialog dialog = new FileDialog(content.getShell(), SWT.OPEN);
        dialog.setText("Find plugin consistency file");
        dialog.setFilterExtensions(new String[]{"*.xml", "*.*"});
        dialog.setFilterNames(new String[]{"XML", "Any"});
        String filePath = dialog.open();
        if (filePath != null)
        {
          pluginConsistencyFileText.setText(filePath);

          //
          pluginConsistency = Util.loadAndUpdateConsistencyFile(new File(filePath), true);

          pluginTabFolder.refresh();
        }
      }
    });
  }

  Map<Object, String> elementToIdCacheMap = new HashMap<>();

  /**
   * Get plugin id from cache
   * @param o IProject or Bundle
   */
  public String getIdInCache(Object o)
  {
    String id = elementToIdCacheMap.get(o);
    if (id == null)
    {
      if (o instanceof IProject)
        id = Util.getPluginId((IProject) o);
      else
        id = ((Bundle) o).getSymbolicName();

      elementToIdCacheMap.put(o, id);
    }

    return id;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  @Override
  public boolean performOk()
  {
    String consistency_file_path = pluginConsistencyFileText.getText();
    if (consistency_file_path == null || consistency_file_path.isEmpty())
    {
      MessageDialog.openError(getShell(), "Error", "Define a path for plugin consistency informations");
      return false;
    }

    // remove useless pluginInfo
    PluginConsistency compactPluginConsistency = pluginConsistency.compact();

    try
    {
      // save
      PluginConsistencyLoader.savePluginConsistency(compactPluginConsistency, new File(consistency_file_path));
    }
    catch(Exception e)
    {
      String message = "Exception when saving plugin consistency informations : " + e.getLocalizedMessage();
      Activator.logError(message, e);
      MessageDialog.openError(getShell(), "Error", message);
      return false;
    }

    //
    getPreferenceStore().setValue(Activator.CONSISTENCY_ACTIVATION, activateButton.getSelection());
    getPreferenceStore().setValue(Activator.CONSISTENCY_FILE_PATH, consistency_file_path);

    //
    if (activateButton.getSelection())
    {
      Activator.getDefault().setPluginConsistency(compactPluginConsistency);
      Activator.getDefault().activate();
    }
    else
    {
      Activator.getDefault().setPluginConsistency(null);
      Activator.getDefault().desactivate();

      // launch project uncheck
      WorkspaceJob job = new WorkspaceJob("Remove Project Consistency")
      {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
        {
          IProject[] validProjects = Util.getValidProjects();
          monitor.beginTask("Removing project consistency ...", validProjects.length);
          for(IProject project : validProjects)
          {
            if (monitor.isCanceled())
              break;

            try
            {
              monitor.subTask("Removing for project " + project.getName());
              Util.removeCheckProjectConsistency(project);
              monitor.worked(1);
            }
            catch(Exception e)
            {
              Activator.logError("Error when removing consistency check on project " + project.getName(), e);
            }
          }

          return Status.OK_STATUS;
        }
      };
      job.schedule();
    }

    return super.performOk();
  }
}
