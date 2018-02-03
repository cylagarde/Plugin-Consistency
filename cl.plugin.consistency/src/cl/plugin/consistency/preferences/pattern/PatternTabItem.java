package cl.plugin.consistency.preferences.pattern;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Images;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.DefaultLabelViewerComparator;
import cl.plugin.consistency.preferences.PluginTabFolder;
import cl.plugin.consistency.preferences.TypeElement;
import cl.plugin.consistency.preferences.impl.ElementManagerComposite;
import cl.plugin.consistency.preferences.impl.IElementManagerDataModel;
import cl.plugin.consistency.preferences.pluginInfo.PluginTabItem;

/**
 * The class <b>PatternTabItem</b> allows to.<br>
 */
public class PatternTabItem
{
  public final PluginTabFolder pluginTabFolder;
  TableViewer patternTableViewer;

  /**
   * Constructor
   */
  public PatternTabItem(PluginTabFolder pluginTabFolder)
  {
    this.pluginTabFolder = pluginTabFolder;

    //
    TabItem patternTabItem = new TabItem(pluginTabFolder.tabFolder, SWT.NONE);
    patternTabItem.setText("Patterns");
    patternTabItem.setImage(Images.PATTERN.getImage());

    //
    Composite patternTabComposite = new Composite(pluginTabFolder.tabFolder, SWT.NONE);
    patternTabItem.setControl(patternTabComposite);

    GridLayout patternTabCompositeLayout = new GridLayout(1, false);
    patternTabCompositeLayout.marginWidth = patternTabCompositeLayout.marginHeight = 0;
    patternTabComposite.setLayout(patternTabCompositeLayout);

    //
    configurateToobar(patternTabComposite);

    //
    configurePatternSashForm(patternTabComposite);

    refresh();
  }

  /**
   * @param parent
   */
  private void configurePatternSashForm(Composite parent)
  {
    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    //
    SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
    formToolkit.adapt(sashForm);
    //    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridData layoutData = new GridData(GridData.FILL_BOTH);
    //    layoutData.widthHint = 1000;
    //    layoutData.heightHint = 1;
    sashForm.setLayoutData(layoutData);

    //
    configurePatternTableViewer(sashForm);

    //
    ScrolledComposite scrolledComposite = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);

    //
    createPatternDetailSashForm(scrolledComposite);
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));

    sashForm.setWeights(new int[]{2, 1});
  }

  private void createPatternDetailSashForm(ScrolledComposite scrolledComposite)
  {
    FormToolkit formToolkit = new FormToolkit(scrolledComposite.getDisplay());

    //
    Composite content = formToolkit.createComposite(scrolledComposite);

    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = gridLayout.marginHeight = 2;
    gridLayout.marginBottom = 3;
    content.setLayout(gridLayout);
    scrolledComposite.setContent(content);

    //
    SashForm sashForm = new SashForm(content, SWT.VERTICAL | SWT.SMOOTH);
    formToolkit.adapt(sashForm);
    //    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridData layoutData = new GridData(GridData.FILL_BOTH);
    //    layoutData.widthHint = 1000;
    //    layoutData.heightHint = 1;
    sashForm.setLayoutData(layoutData);

    //
    ElementManagerComposite<TypeElement, PatternInfoData> patternTypeComposite = createTypeComposite(sashForm);

    //
    ElementManagerComposite<TypeElement, PatternInfoData> patternForbiddenTypeComposite = createForbiddenTypeComposite(sashForm);

    // selection
    patternTableViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) patternTableViewer.getSelection();
      PatternInfo patternInfo = (PatternInfo) selection.getFirstElement();

      Collections.sort(patternInfo.typeList, Comparator.comparing(type -> type.name));
      patternTypeComposite.setData(patternInfo == null? null : new PatternInfoData(patternInfo, patternInfo.typeList));

      Collections.sort(patternInfo.forbiddenTypeList, Comparator.comparing(type -> type.name));
      patternForbiddenTypeComposite.setData(patternInfo == null? null : new PatternInfoData(patternInfo, patternInfo.forbiddenTypeList));
    });

    sashForm.setWeights(new int[]{1, 1});
  }

  /**
   * @param patternInfoData
   */
  void updateAllPluginInfosWithPatternInfo(PatternInfoData patternInfoData)
  {
    String pattern = patternInfoData.patternInfo.pattern;

    for(PluginInfo pluginInfo : pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList)
    {
      if (pluginInfo.name.contains(pattern))
      {
        // types
        if (patternInfoData.typeList == patternInfoData.patternInfo.typeList)
        {
          pluginInfo.typeList.removeIf(type -> patternInfoData.oldTypeSet.contains(type.name));
          patternInfoData.typeList.forEach(type -> {
            Type newType = new Type();
            newType.name = type.name;
            pluginInfo.typeList.add(newType);
          });
        }
        // forbiddenTypes
        else
        {
          pluginInfo.forbiddenTypeList.removeIf(type -> patternInfoData.oldTypeSet.contains(type.name));
          patternInfoData.typeList.forEach(type -> {
            Type newType = new Type();
            newType.name = type.name;
            pluginInfo.forbiddenTypeList.add(newType);
          });
        }
      }
    }

    // refresh all TabFolder
    pluginTabFolder.refresh();
    //    refreshPatternInfo(patternInfoData.patternInfo);
  }

  /**
   * @param parent
   */
  private ElementManagerComposite<TypeElement, PatternInfoData> createTypeComposite(Composite parent)
  {
    //
    IElementManagerDataModel<TypeElement, PatternInfoData> typeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PatternInfoData>()
    {
      @Override
      public void refreshData(PatternInfoData patternInfoData)
      {
        updateAllPluginInfosWithPatternInfo(patternInfoData);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(TypeElement::new).collect(Collectors.toList());
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

    ElementManagerComposite<TypeElement, PatternInfoData> patternTypeComposite = new ElementManagerComposite<>(typeElementManagerDataModel, parent, SWT.NONE);
    return patternTypeComposite;
  }

  /**
   * @param parent
   */
  private ElementManagerComposite<TypeElement, PatternInfoData> createForbiddenTypeComposite(Composite parent)
  {
    IElementManagerDataModel<TypeElement, PatternInfoData> forbiddenTypeElementManagerDataModel = new IElementManagerDataModel<TypeElement, PatternInfoData>()
    {
      @Override
      public void refreshData(PatternInfoData patternInfoData)
      {
        updateAllPluginInfosWithPatternInfo(patternInfoData);
      }

      @Override
      public Collection<TypeElement> getElements()
      {
        return pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(TypeElement::new).collect(Collectors.toList());
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

    ElementManagerComposite<TypeElement, PatternInfoData> patternForbiddenTypeComposite = new ElementManagerComposite<>(forbiddenTypeElementManagerDataModel, parent, SWT.NONE);
    return patternForbiddenTypeComposite;
  }

  /**
   *
   * @param parent
   */
  private void configurateToobar(Composite parent)
  {
    Composite toolbarComposite = new Composite(parent, SWT.NONE);
    GridLayout toolbarLayout = new GridLayout(1, false);
    toolbarLayout.marginWidth = toolbarLayout.marginHeight = 0;
    toolbarComposite.setLayout(toolbarLayout);

    Button addPatternButton = new Button(toolbarComposite, SWT.FLAT);
    addPatternButton.setToolTipText("Add new pattern");
    addPatternButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
    addPatternButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        Set<String> alreadyExistpatternSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().map(patterInfo -> patterInfo.pattern).collect(Collectors.toSet());

        IInputValidator validator = newText -> {
          if (newText.isEmpty())
            return "Value is empty";
          if (alreadyExistpatternSet.contains(newText))
            return "The pattern already exists";
          return null;
        };
        InputDialog inputDialog = new InputDialog(parent.getShell(), "Add new pattern", "Enter a value for new pattern", "", validator);
        if (inputDialog.open() == InputDialog.OK)
        {
          String newPattern = inputDialog.getValue();
          PatternInfo patternInfo = new PatternInfo();
          patternInfo.pattern = newPattern;
          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.add(patternInfo);

          // refresh all TabFolder
          pluginTabFolder.refresh();

          patternTableViewer.setSelection(new StructuredSelection(patternInfo));
        }
      }
    });
  }

  /**
   *
   * @param parent
   */
  private void configurePatternTableViewer(Composite parent)
  {
    patternTableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
    patternTableViewer.getTable().setLayout(new TableLayout());
    patternTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    patternTableViewer.getTable().setHeaderVisible(true);
    patternTableViewer.getTable().setLinesVisible(true);
    patternTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
    patternTableViewer.setComparator(new DefaultLabelViewerComparator());

    // 'Pattern' TableViewerColumn
    TableViewerColumn patternTableViewerColumn = new TableViewerColumn(patternTableViewer, SWT.NONE);
    patternTableViewerColumn.getColumn().setText("Pattern");
    patternTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    patternTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);

    patternTableViewerColumn.setLabelProvider(new ColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PatternInfo patternInfo = (PatternInfo) element;
        return patternInfo.pattern;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(patternTableViewerColumn);

    // 'Type' TableViewerColumn
    TableViewerColumn typeTableViewerColumn = new TableViewerColumn(patternTableViewer, SWT.NONE);
    typeTableViewerColumn.getColumn().setText("Type");
    typeTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    typeTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);

    typeTableViewerColumn.setLabelProvider(new ColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PatternInfo patternInfo = (PatternInfo) element;
        String txt = patternInfo.typeList.stream().map(type -> type.name).sorted().collect(Collectors.joining(", "));
        return txt;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(typeTableViewerColumn);

    // 'Forbidden type' TableViewerColumn
    TableViewerColumn forbiddenTypeTableViewerColumn = new TableViewerColumn(patternTableViewer, SWT.NONE);
    forbiddenTypeTableViewerColumn.getColumn().setText("Forbidden type");
    forbiddenTypeTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    forbiddenTypeTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);

    forbiddenTypeTableViewerColumn.setLabelProvider(new ColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PatternInfo patternInfo = (PatternInfo) element;
        String txt = patternInfo.forbiddenTypeList.stream().map(type -> type.name).sorted().collect(Collectors.joining(", "));
        return txt;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenTypeTableViewerColumn);

    //
    configurePopupMenuForTypeTableViewer();
  }

  /**
   *
   */
  private void configurePopupMenuForTypeTableViewer()
  {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    Menu menu = manager.createContextMenu(patternTableViewer.getControl());
    patternTableViewer.getControl().setMenu(menu);

    manager.addMenuListener(new PatternMenuListener());
  }

  /**
   *
   */
  public void refresh()
  {
    try
    {
      patternTableViewer.getTable().setRedraw(false);

      // Update patternTableViewer
      patternTableViewer.setInput(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList);

      // pack columns
      for(TableColumn tableColumn : patternTableViewer.getTable().getColumns())
        PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);
    }
    finally
    {
      patternTableViewer.getTable().setRedraw(true);
    }
  }

  /**
   * The class <b>PatternMenuListener</b> allows to.<br>
   */
  class PatternMenuListener implements IMenuListener
  {
    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
      manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      //
      if (!patternTableViewer.getSelection().isEmpty())
      {
        createRenamePatternMenuItem(manager);
        createRemovePatternsMenuItem(manager);
      }
    }

    /**
     */
    private void createRenamePatternMenuItem(IMenuManager manager)
    {
      IStructuredSelection selection = (IStructuredSelection) patternTableViewer.getSelection();
      if (selection.toList().size() != 1)
        return;
      PatternInfo selectedPatternInfo = (PatternInfo) selection.getFirstElement();
      String selectedPattern = selectedPatternInfo.pattern;

      manager.add(new Action("Rename pattern")
      {
        @Override
        public void run()
        {
          Set<String> alreadyExistPatternSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().map(patternInfo -> patternInfo.pattern).collect(Collectors.toSet());

          Shell shell = patternTableViewer.getControl().getShell();

          IInputValidator validator = newText -> {
            if (newText.isEmpty())
              return "Value is empty";
            if (alreadyExistPatternSet.contains(newText))
              return "The pattern already exists";
            return null;
          };
          InputDialog inputDialog = new InputDialog(shell, "Rename pattern", "Enter a new name", selectedPattern, validator);
          if (inputDialog.open() == InputDialog.OK)
          {
            String newPattern = inputDialog.getValue();

            Util.removePatternInAllPluginInfos(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

            // rename type
            selectedPatternInfo.pattern = newPattern;

            Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

            // refresh all TabFolder
            pluginTabFolder.refresh();
          }
        }
      });
    }

    /**
     */
    private void createRemovePatternsMenuItem(IMenuManager manager)
    {
      manager.add(new Action("Remove selected patterns")
      {
        @Override
        public void run()
        {
          IStructuredSelection selection = (IStructuredSelection) patternTableViewer.getSelection();
          Stream<PatternInfo> selectedPatternInfoStream = selection.toList().stream().filter(PatternInfo.class::isInstance).map(PatternInfo.class::cast);
          Set<PatternInfo> selectedPatternInfoSet = selectedPatternInfoStream.collect(Collectors.toSet());
          Set<String> selectedPatternSet = selectedPatternInfoSet.stream().map(patternInfo -> patternInfo.pattern).collect(Collectors.toSet());
          String selectedPatterns = selectedPatternSet.stream().collect(Collectors.joining(", "));

          Shell shell = patternTableViewer.getControl().getShell();
          String message = "Do you want to remove the selected pattern\n" + selectedPatterns + " ?";
          boolean result = MessageDialog.openConfirm(shell, "Confirm", message);
          if (result)
          {
            Util.removePatternInAllPluginInfos(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

            // remove patterns
            pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.removeIf(selectedPatternInfoSet::contains);

            Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

            // refresh all TabFolder
            pluginTabFolder.refresh();
          }
        }
      });
    }
  }

  /**
   * Refresh for PatternInfo
   */
  void refreshPatternInfo(PatternInfo patternInfo)
  {
    patternTableViewer.getTable().setRedraw(false);
    try
    {
      patternTableViewer.refresh(patternInfo);

      // pack columns
      for(TableColumn tableColumn : patternTableViewer.getTable().getColumns())
        PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);
    }
    finally
    {
      patternTableViewer.getTable().setRedraw(true);
    }
  }
}
