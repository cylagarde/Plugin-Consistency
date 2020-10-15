package cl.plugin.consistency.preferences.pattern;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Images;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.custom.NaturalOrderComparator;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.preferences.TypeElement;
import cl.plugin.consistency.preferences.impl.ElementManagerComposite;
import cl.plugin.consistency.preferences.impl.IElementManagerDataModel;

/**
 * The class <b>PatternInfoDetail</b> allows to.<br>
 */
class PatternInfoDetail
{
  final PatternTabItem patternTabItem;
  PatternInfo patternInfo;
  ElementManagerComposite<TypeElement, PatternInfoData> declaredPluginTypeComposite;
  ElementManagerComposite<TypeElement, PatternInfoData> forbiddenPluginTypeComposite;
  ForbiddenPluginComposite forbiddenPluginComposite;
  Composite content;

  /**
   * Constructor
   *
   * @param patternTabItem
   * @param parent
   */
  PatternInfoDetail(PatternTabItem patternTabItem, Composite parent)
  {
    this.patternTabItem = patternTabItem;

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
    createDeclaredPluginTypeComposite(sashForm);

    //
    createForbiddenPluginTypeComposite(sashForm);

    //
    createForbiddenPluginComposite(sashForm);

    sashForm.setWeights(new int[]{2, 2, 3});
  }

  /**
   *
   */
  private void createDeclaredPluginTypeComposite(Composite parent)
  {
    IElementManagerDataModel<TypeElement, PatternInfoData> declaredPluginTypeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PatternInfoData>() {
      @Override
      public void refreshData(PatternInfoData patternInfoData)
      {
        patternTabItem.refreshPatternInfo(patternInfo);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> new TypeElement(type, false)).collect(Collectors.toList());
      }

      @Override
      public String getSectionTitle()
      {
        return "Declared plugin types";
      }

      @Override
      public Image getSectionImage()
      {
        return Images.TYPE.getImage();
      }

      @Override
      public String getAddElementToolTipText()
      {
        return "Add declared plugin type";
      }
    };

    declaredPluginTypeComposite = new ElementManagerComposite<>(declaredPluginTypeElementManagerDataModel, parent, SWT.NONE);
  }

  /**
   *
   */
  private void createForbiddenPluginTypeComposite(Composite parent)
  {
    IElementManagerDataModel<TypeElement, PatternInfoData> forbiddenPluginTypeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PatternInfoData>() {
      @Override
      public void refreshData(PatternInfoData patternInfoData)
      {
        patternTabItem.refreshPatternInfo(patternInfo);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> new TypeElement(type, false)).collect(Collectors.toList());
      }

      @Override
      public String getSectionTitle()
      {
        return "Forbidden plugin types";
      }

      @Override
      public Image getSectionImage()
      {
        return Images.FORBIDDEN_TYPE.getImage();
      }

      @Override
      public String getAddElementToolTipText()
      {
        return "Add forbidden plugin type";
      }
    };

    forbiddenPluginTypeComposite = new ElementManagerComposite<>(forbiddenPluginTypeElementManagerDataModel, parent, SWT.NONE);
  }

  /**
   *
   */
  private void createForbiddenPluginComposite(Composite parent)
  {
    forbiddenPluginComposite = new ForbiddenPluginComposite(this, parent, SWT.NONE);
  }

  /**
   * Set PatternInfo
   *
   * @param patternInfo
   */
  public void setPatternInfo(PatternInfo patternInfo)
  {
    this.patternInfo = patternInfo;

    if (patternInfo != null)
    {
      // sort
      Collections.sort(patternInfo.declaredPluginTypeList, Comparator.comparing(type -> type.name, NaturalOrderComparator.INSTANCE));
      Collections.sort(patternInfo.forbiddenPluginTypeList, Comparator.comparing(type -> type.name, NaturalOrderComparator.INSTANCE));

      //      Set<Type> declaredPluginTypeFromPatternInfoSet = patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream()
      //        .filter(patternInfo -> patternInfo.acceptPlugin(patternInfo.id))
      //        .flatMap(patternInfo -> patternInfo.declaredPluginTypeList.stream())
      //        .collect(Collectors.toSet());
      declaredPluginTypeComposite.setData(new PatternInfoData(Util.duplicatePatternInfo(patternInfo), patternInfo.declaredPluginTypeList, false));

      //      Set<Type> forbiddenPluginTypeFromPatternInfoSet = patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream()
      //        .filter(patternInfo -> patternInfo.acceptPlugin(patternInfo.id))
      //        .flatMap(patternInfo -> patternInfo.forbiddenPluginTypeList.stream())
      //        .collect(Collectors.toSet());
      forbiddenPluginTypeComposite.setData(new PatternInfoData(Util.duplicatePatternInfo(patternInfo), patternInfo.forbiddenPluginTypeList, true));
    }
    else
    {
      declaredPluginTypeComposite.setData(null);
      forbiddenPluginTypeComposite.setData(null);
    }

    forbiddenPluginComposite.setPatternInfo(patternInfo);

    //
    boolean validPlugin = false;
    //    if (patternInfo != null)
    //    {
    //      IProject project = Util.getProject(patternInfo);
    //      validPlugin = patternTabItem.cache.isValidProject(project);
    //    }

    declaredPluginTypeComposite.setEnabled(validPlugin);
    forbiddenPluginTypeComposite.setEnabled(validPlugin);
    forbiddenPluginComposite.setEnabled(validPlugin);
  }

  /**
   * Refresh
   */
  public void refresh()
  {
    setPatternInfo(patternInfo);
  }
}
