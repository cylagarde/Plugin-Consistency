package cl.plugin.consistency.preferences.pluginInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Images;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.preferences.TypeElement;
import cl.plugin.consistency.preferences.impl.ElementManagerComposite;
import cl.plugin.consistency.preferences.impl.IElementManagerDataModel;

/**
 * The class <b>ProjectDetail</b> allows to.<br>
 */
class ProjectDetail
{
  final PluginTabItem pluginTabItem;
  PluginInfo pluginInfo;
  ElementManagerComposite<TypeElement, PluginInfoData> typeComposite;
  ElementManagerComposite<TypeElement, PluginInfoData> forbiddenTypeComposite;
  ForbiddenPluginComposite forbiddenPluginComposite;
  Composite content;

  /**
   * Constructor
   *
   * @param pluginTabItem
   * @param parent
   */
  ProjectDetail(PluginTabItem pluginTabItem, Composite parent)
  {
    this.pluginTabItem = pluginTabItem;

    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    content = formToolkit.createComposite(parent);

    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = gridLayout.marginHeight = 2;
    gridLayout.marginBottom = 3;
    content.setLayout(gridLayout);

    //
    SashForm sashForm = new SashForm(content, SWT.VERTICAL | SWT.SMOOTH);
    //    sashForm.setBackground(new Color(null, 255,0,0));
    formToolkit.adapt(sashForm);

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
  }

  /**
   *
   */
  private void createTypeComposite(Composite parent)
  {
    IElementManagerDataModel<TypeElement, PluginInfoData> typeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PluginInfoData>()
    {
      @Override
      public void refreshData(PluginInfoData pluginInfoData)
      {
        pluginTabItem.refreshPluginInfo(pluginInfo);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(TypeElement::new).collect(Collectors.toList());
      }

      @Override
      public String getSectionTitle()
      {
        return "Types";
      }

      @Override
      public Image getSectionImage()
      {
        return Images.TYPE.getImage();
      }

      @Override
      public String getAddElementToolTipText()
      {
        return "Add new type";
      }
    };

    typeComposite = new ElementManagerComposite<>(typeElementManagerDataModel, parent, SWT.NONE);
  }

  /**
   *
   */
  private void createForbiddenTypeComposite(Composite parent)
  {
    IElementManagerDataModel<TypeElement, PluginInfoData> forbiddenTypeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PluginInfoData>()
    {
      @Override
      public void refreshData(PluginInfoData pluginInfoData)
      {
        pluginTabItem.refreshPluginInfo(pluginInfo);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(TypeElement::new).collect(Collectors.toList());
      }

      @Override
      public String getSectionTitle()
      {
        return "Forbidden types";
      }

      @Override
      public Image getSectionImage()
      {
        return Images.FORBIDDEN_TYPE.getImage();
      }

      @Override
      public String getAddElementToolTipText()
      {
        return "Add new forbidden type";
      }
    };

    forbiddenTypeComposite = new ElementManagerComposite<>(forbiddenTypeElementManagerDataModel, parent, SWT.NONE);
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

    if (pluginInfo != null)
    {
      // sort
      Collections.sort(pluginInfo.typeList, Comparator.comparing(type -> type.name));
      Collections.sort(pluginInfo.forbiddenTypeList, Comparator.comparing(type -> type.name));

      typeComposite.setData(new PluginInfoData(pluginInfo, pluginInfo.typeList));
      forbiddenTypeComposite.setData(new PluginInfoData(pluginInfo, pluginInfo.forbiddenTypeList));
    }
    else
    {
      typeComposite.setData(null);
      forbiddenTypeComposite.setData(null);
    }

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
