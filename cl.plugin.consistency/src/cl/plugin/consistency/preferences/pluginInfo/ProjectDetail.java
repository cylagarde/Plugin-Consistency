package cl.plugin.consistency.preferences.pluginInfo;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginInfo;

/**
 * The class <b>ProjectDetail</b> allows to.<br>
 */
class ProjectDetail
{
  final PluginTabItem pluginTabItem;
  PluginInfo pluginInfo;
  TypeComposite typeComposite;
  ForbiddenTypeComposite forbiddenTypeComposite;
  ForbiddenPluginComposite forbiddenPluginComposite;
  Composite content;

  /**
   * Constructor
   *
   * @param pluginConsistencyPreferencePage
   * @param parent
   */
  ProjectDetail(PluginTabItem pluginTabItem, Composite parent)
  {
    this.pluginTabItem = pluginTabItem;

    FormToolkit toolkit = new FormToolkit(parent.getDisplay());

    content = toolkit.createComposite(parent);

    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = gridLayout.marginHeight = 2;
    gridLayout.marginBottom = 3;
    content.setLayout(gridLayout);

    //
    SashForm sashForm = new SashForm(content, SWT.VERTICAL | SWT.SMOOTH);
    //    sashForm.setBackground(new Color(null, 255,0,0));
    toolkit.adapt(sashForm);

    GridData sashFormLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    sashFormLayoutData.heightHint = 0;
    sashForm.setLayoutData(sashFormLayoutData);

    //
    createTypeComposite(sashForm);

    //
    createForbiddenTypeComposite(sashForm);

    //
    createForbiddenPluginComposite(sashForm);

    sashForm.setWeights(new int[]{2, 2, 3});

    Util.setEnabled(content, false);
  }

  /**
   *
   */
  private void createTypeComposite(Composite parent)
  {
    typeComposite = new TypeComposite(this, parent, SWT.NONE);
  }

  /**
   *
   */
  private void createForbiddenTypeComposite(Composite parent)
  {
    forbiddenTypeComposite = new ForbiddenTypeComposite(this, parent, SWT.NONE);
  }

  /**
   *
   */
  private void createForbiddenPluginComposite(Composite parent)
  {
    forbiddenPluginComposite = new ForbiddenPluginComposite(this, parent, SWT.NONE);
  }

  /**
   * Set PluginInfo
   *
   * @param pluginInfo
   */
  public void setPluginInfo(PluginInfo pluginInfo)
  {
    this.pluginInfo = pluginInfo;

    // recreate
    typeComposite.setPluginInfo(pluginInfo);
    forbiddenTypeComposite.setPluginInfo(pluginInfo);
    forbiddenPluginComposite.setPluginInfo(pluginInfo);

    //
    boolean validPlugin = false;
    if (pluginInfo != null)
    {
      IProject project = Util.getProject(pluginInfo);
      validPlugin = Util.isValidPlugin(project);
    }
    Util.setEnabled(content, validPlugin);
  }

  /**
   * Refresh
   */
  public void refresh()
  {
    setPluginInfo(pluginInfo);
  }
}
