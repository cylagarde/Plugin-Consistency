package cl.plugin.consistency.preferences.pluginInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;

/**
 * The class <b>TypeComposite</b> allows to.<br>
 */
class TypeComposite
{
  final ProjectDetail projectDetail;
  final Section section;
  final Composite typeListComposite;
  final ScrolledComposite scrolledComposite;
  IAction addTypeAction;
  PluginInfo pluginInfo;
  boolean fireEvent = true;

  /**
   * Constructor
   * @param parent
   * @param style
   */
  TypeComposite(ProjectDetail projectDetail, Composite parent, int style)
  {
    this.projectDetail = projectDetail;

    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    section = formToolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
    section.setText("Types");
    section.setLayout(new GridLayout());

    // Add toolbar to section
    final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    final ToolBar toolbar = toolBarManager.createControl(section);
    addTypeAction = new AddTypeAction();

    toolBarManager.add(addTypeAction);
    toolBarManager.update(true);
    section.setTextClient(toolbar);

    //
    scrolledComposite = new ScrolledComposite(section, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

    section.setClient(scrolledComposite);

    //
    typeListComposite = formToolkit.createComposite(scrolledComposite);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = gridLayout.marginHeight = 0;
    gridLayout.marginTop = gridLayout.marginLeft = gridLayout.marginRight = gridLayout.marginBottom = 0;
    typeListComposite.setLayout(gridLayout);
    scrolledComposite.setContent(typeListComposite);

    //
    scrolledComposite.addControlListener(new ControlAdapter()
    {
      @Override
      public void controlResized(ControlEvent ce)
      {
        Rectangle rect = scrolledComposite.getClientArea();
        scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(rect.width, SWT.DEFAULT));
      }
    });
  }

  /**
   * Return the not used types
   */
  private List<String> getNotUsedTypes()
  {
    List<String> types = projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> type.name).sorted().distinct().collect(Collectors.toList());
    types.add(0, "");
    List<String> forbiddenTypes = pluginInfo.typeList.stream().map(forbiddenType -> forbiddenType.name).collect(Collectors.toList());
    types.removeAll(forbiddenTypes);
    return types;
  }

  /**
   * @param pluginInfo
   */
  public void setPluginInfo(PluginInfo pluginInfo)
  {
    this.pluginInfo = pluginInfo;
    //    Util.setEnabled(section, pluginInfo != null);

    Stream.of(typeListComposite.getChildren()).forEach(Control::dispose);
    if (pluginInfo != null)
    {
      for(Type type : pluginInfo.typeList)
      {
        TypeBiConsumer typeBiConsumer = new TypeBiConsumer();
        List<String> types = getNotUsedTypes();
        types.add(type.name);
        Collections.sort(types);
        ComboViewer typeComboViewer = Util.createCombo(typeListComposite, types, type.name, typeBiConsumer);
        typeBiConsumer.typeComboViewer = typeComboViewer;

        typeComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      }
    }
    typeListComposite.layout();
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  /**
   * The class <b>TypeBiConsumer</b> allows to.<br>
   */
  class TypeBiConsumer implements BiConsumer<IStructuredSelection, IStructuredSelection>
  {
    ComboViewer typeComboViewer;

    @Override
    public void accept(IStructuredSelection oldStructuredSelection, IStructuredSelection newStructuredSelection)
    {
      if (!fireEvent)
        return;
      int forbiddenTypeIndex = Arrays.asList(typeListComposite.getChildren()).indexOf(typeComboViewer.getControl());

      String selectedTypeName = (String) newStructuredSelection.getFirstElement();
      if (selectedTypeName == null || selectedTypeName.isEmpty())
      {
        if (forbiddenTypeIndex < pluginInfo.typeList.size())
        {
          pluginInfo.typeList.remove(forbiddenTypeIndex);
        }

        // remove typeComboViewer
        typeComboViewer.getControl().dispose();
        typeListComposite.layout();
        scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
      }
      else
      {
        if (forbiddenTypeIndex < pluginInfo.typeList.size())
        {
          // same -> no change
          Type type = pluginInfo.typeList.get(forbiddenTypeIndex);
          if (type.name == selectedTypeName)
            return;
          type.name = selectedTypeName;
        }
        else
        {
          Type type = new Type();
          type.name = selectedTypeName;
          pluginInfo.typeList.add(type);
        }
      }

      // enable if last combo dont use empty selection
      Control[] children = typeListComposite.getChildren();
      Combo combo = children.length == 0? null : (Combo) children[children.length - 1];
      addTypeAction.setEnabled(combo == null || (combo.getSelectionIndex() != 0 && getNotUsedTypes().size() != 1));

      // reconstruct items for all combos
      try
      {
        fireEvent = false;
        for(Control control : typeListComposite.getChildren())
        {
          if (control instanceof Combo)
          {
            Combo child = (Combo) control;
            List<String> types = getNotUsedTypes();
            String selection = child.getText();
            types.add(selection);
            ComboViewer comboViewer = (ComboViewer) child.getData("ComboViewer");
            comboViewer.setInput(types);
            comboViewer.setSelection(new StructuredSelection(selection));
          }
        }
      }
      finally
      {
        fireEvent = true;
      }

      projectDetail.pluginTabItem.refreshPluginInfo(pluginInfo);
    }
  }

  /**
   * The class <b>AddTypeAction</b> allows to.<br>
   */
  class AddTypeAction extends Action
  {
    {
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
      setToolTipText("Add type");
    }

    @Override
    public void run()
    {
      TypeBiConsumer typeBiConsumer = new TypeBiConsumer();
      ComboViewer typeComboViewer = Util.createCombo(typeListComposite, getNotUsedTypes(), "", typeBiConsumer);
      typeBiConsumer.typeComboViewer = typeComboViewer;

      typeComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

      setEnabled(false);
      typeListComposite.layout();
      scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
  }
}
