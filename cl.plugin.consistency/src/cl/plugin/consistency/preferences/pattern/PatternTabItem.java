package cl.plugin.consistency.preferences.pattern;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.Images;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.DefaultLabelViewerComparator;
import cl.plugin.consistency.preferences.PluginTabFolder;
import cl.plugin.consistency.preferences.pattern.InputPatternDialog.IPatternValidator;
import cl.plugin.consistency.preferences.plugin.PluginTabItem;

/**
 * The class <b>PatternTabItem</b> allows to.<br>
 */
public class PatternTabItem
{
  public final PluginTabFolder pluginTabFolder;
  private CheckboxTableViewer patternCheckTableViewer;
  private Button selectAllButton;
  private PatternInfoDetail patternInfoDetail;

  private final static String ACCEPT_PLUGIN_MESSAGE = "Enter a pattern to accept plugin ('*' and '?' are supported) (multiple patterns must be separated by " + PatternInfo.PATTERN_SEPARATOR + ")";
  private final static String NOT_ACCEPT_PLUGIN_MESSAGE = "Enter a pattern not to accept plugin ('*' and '?' are supported) (multiple patterns must be separated by " + PatternInfo.PATTERN_SEPARATOR + ")";

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
    configurePatternInfoSashForm(patternTabComposite);

    refresh();
  }

  /**
   * Configure patternInfo SashForm
   * @param parent
   */
  private void configurePatternInfoSashForm(Composite parent)
  {
    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    //
    SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
    formToolkit.adapt(sashForm);
    // sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridData layoutData = new GridData(GridData.FILL_BOTH);
    // layoutData.widthHint = 800;
    layoutData.heightHint = 1;
    sashForm.setLayoutData(layoutData);

    //
    configurePatternInfoTableViewer(sashForm);

    //
    ScrolledComposite scrolledComposite = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);

    //
    patternInfoDetail = new PatternInfoDetail(this, scrolledComposite);
    scrolledComposite.setContent(patternInfoDetail.content);
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));

    // selection
    patternCheckTableViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      PatternInfo patternInfo = (PatternInfo) selection.getFirstElement();
      patternInfoDetail.setPatternInfo(patternInfo);
    });

    sashForm.setWeights(new int[]{2, 1});
  }

  /**
   * Update all pluginInfos with patternInfo (add new types and/or remove old types)
   *
   * @param patternInfoData
   */
  void updateAllPluginInfosWithPatternInfo(PatternInfoData patternInfoData, boolean refreshPluginTabFolder)
  {
    for(PluginInfo pluginInfo : pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList)
    {
      if (patternInfoData.patternInfo.acceptPlugin(pluginInfo.id))
      {
        if (!patternInfoData.isForbiddenPluginTypeList)
          pluginInfo.declaredPluginTypeList.removeAll(patternInfoData.patternInfo.declaredPluginTypeList);
        else
          pluginInfo.forbiddenPluginTypeList.removeAll(patternInfoData.patternInfo.forbiddenPluginTypeList);

        Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency, pluginInfo, !patternInfoData.isForbiddenPluginTypeList, patternInfoData.isForbiddenPluginTypeList, true);
      }
    }

    // refresh all TabFolder
    if (refreshPluginTabFolder)
      pluginTabFolder.refresh();
  }

  /**
   * Update all pluginInfos with patternInfo (add new types and/or remove old types)
   *
   * @param patternInfo
   */
  void updateAllPluginInfosWithPatternInfo(PatternInfo oldPatternInfo, PatternInfo patternInfo, boolean refreshPluginTabFolder)
  {
    for(PluginInfo pluginInfo : pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList)
    {
      if (patternInfo.acceptPlugin(pluginInfo.id))
      {
        // faux
        pluginInfo.declaredPluginTypeList.removeAll(oldPatternInfo.declaredPluginTypeList);
        pluginInfo.forbiddenPluginTypeList.removeAll(oldPatternInfo.forbiddenPluginTypeList);
        pluginInfo.forbiddenPluginList.removeAll(oldPatternInfo.forbiddenPluginList);

        Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency, pluginInfo, true, true, true);
      }
    }

    // refresh all TabFolder
    if (refreshPluginTabFolder)
      pluginTabFolder.refresh();
  }

  /**
   * @param parent
   */
  private void configurateToobar(Composite parent)
  {
    Composite toolbarComposite = new Composite(parent, SWT.NONE);
    GridLayout toolbarLayout = new GridLayout(2, false);
    toolbarLayout.horizontalSpacing = 10;
    toolbarLayout.marginWidth = toolbarLayout.marginHeight = 0;
    toolbarComposite.setLayout(toolbarLayout);

    Button addPatternButton = new Button(toolbarComposite, SWT.FLAT);
    addPatternButton.setToolTipText("Add new pattern");
    addPatternButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
    addPatternButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        IPatternValidator patternValidator = (description, acceptPattern, doNotAcceptPattern) -> {
          if (acceptPattern.isEmpty() && doNotAcceptPattern.isEmpty())
            return "No pattern defined";
          Predicate<PatternInfo> acceptPredicate = patternInfo -> patternInfo.getAcceptPattern().equals(acceptPattern);
          Predicate<PatternInfo> doNotAcceptPredicate = patternInfo -> patternInfo.getDoNotAcceptPattern().equals(doNotAcceptPattern);
          Predicate<PatternInfo> predicate = acceptPredicate.and(doNotAcceptPredicate);
          if (pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().anyMatch(predicate))
            return "The pattern already exists";
          return null;
        };

        Cache cache = pluginTabFolder.pluginConsistencyPreferencePage.getCache();

        InputPatternDialog inputPatternDialog = new InputPatternDialog(parent.getShell(), "Add new pattern", "",
          ACCEPT_PLUGIN_MESSAGE, "",
          NOT_ACCEPT_PLUGIN_MESSAGE, "",
          cache, patternValidator);
        if (inputPatternDialog.open() == Window.OK)
        {
          PatternInfo patternInfo = new PatternInfo();
          patternInfo.setPattern(inputPatternDialog.getAcceptPattern(), inputPatternDialog.getDoNotAcceptPattern());
          patternInfo.description = inputPatternDialog.getDescription();
          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.add(patternInfo);

          // refresh all TabFolder
          pluginTabFolder.refresh();

          patternCheckTableViewer.setSelection(new StructuredSelection(patternInfo));
        }
      }
    });

    selectAllButton = new Button(toolbarComposite, SWT.CHECK);
    selectAllButton.setText("Select all");
    selectAllButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        selectAllButton.setGrayed(false);
        boolean selectAll = selectAllButton.getSelection();

        updateAfterChange(() -> {
          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.forEach(patternInfo -> patternInfo.activate = selectAll);
        });
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e)
      {
      }
    });
  }

  /**
   * @param parent
   */
  private void configurePatternInfoTableViewer(Composite parent)
  {
    patternCheckTableViewer = CheckboxTableViewer.newCheckList(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
    patternCheckTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    Table table = patternCheckTableViewer.getTable();
    table.setLayout(new TableLayout());
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    patternCheckTableViewer.addDoubleClickListener(event -> new EditPatternAction().run());

    patternCheckTableViewer.addCheckStateListener(event -> {
      PatternInfo patternInfo = (PatternInfo) event.getElement();
      boolean checked = event.getChecked();

      updateAfterChange(() -> patternInfo.activate = checked);

      // change selectAllButton state
      boolean all = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().allMatch(pi -> pi.activate == checked);
      selectAllButton.setSelection(all? checked : !all);
      selectAllButton.setGrayed(!all);
    });

    // 'Activate pattern' TableViewerColumn
    TableViewerColumn activatePatternTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    activatePatternTableViewerColumn.getColumn().setText("Activate");
    activatePatternTableViewerColumn.getColumn().setAlignment(SWT.CENTER); // dont work
    Function<PatternInfo, String> textFunction = patternInfo -> "#" + (pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.indexOf(patternInfo) + 1) + (patternInfo.description == null? "" : " " + patternInfo.description);
    activatePatternTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(textFunction));
    DefaultLabelViewerComparator.configureForSortingColumn(activatePatternTableViewerColumn);

    // 'Accept pattern' TableViewerColumn
    TableViewerColumn acceptPatternTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    acceptPatternTableViewerColumn.getColumn().setText("Accept pattern");
    acceptPatternTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    acceptPatternTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);
    acceptPatternTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(PatternInfo::getAcceptPattern));
    DefaultLabelViewerComparator.configureForSortingColumn(acceptPatternTableViewerColumn);

    // 'Do not accept pattern' TableViewerColumn
    TableViewerColumn doNotAcceptPatternTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    doNotAcceptPatternTableViewerColumn.getColumn().setText("Do not accept pattern");
    doNotAcceptPatternTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    doNotAcceptPatternTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);
    doNotAcceptPatternTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(PatternInfo::getDoNotAcceptPattern));
    DefaultLabelViewerComparator.configureForSortingColumn(doNotAcceptPatternTableViewerColumn);

    // 'Declared plugin types' TableViewerColumn
    TableViewerColumn declaredPluginTypeTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    declaredPluginTypeTableViewerColumn.getColumn().setText("Declared plugin types");
    declaredPluginTypeTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    declaredPluginTypeTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);
    declaredPluginTypeTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(patternInfo -> patternInfo.declaredPluginTypeList.stream().map(type -> type.name).sorted().collect(Collectors.joining(", "))));
    DefaultLabelViewerComparator.configureForSortingColumn(declaredPluginTypeTableViewerColumn);

    // 'Forbidden plugin types' TableViewerColumn
    TableViewerColumn forbiddenPluginTypeTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    forbiddenPluginTypeTableViewerColumn.getColumn().setText("Forbidden plugin types");
    forbiddenPluginTypeTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    forbiddenPluginTypeTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);
    forbiddenPluginTypeTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(patternInfo -> patternInfo.forbiddenPluginTypeList.stream().map(type -> type.name).sorted().collect(Collectors.joining(", "))));
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenPluginTypeTableViewerColumn);

    // 'Forbidden plugins/projects' TableViewerColumn
    TableViewerColumn forbiddenBundlesTableViewerColumn = new TableViewerColumn(patternCheckTableViewer, SWT.NONE);
    forbiddenBundlesTableViewerColumn.getColumn().setText("Forbidden plugins/projects");
    forbiddenBundlesTableViewerColumn.setLabelProvider(new PatternInfoColumnLabelProvider(patternInfo -> patternInfo.forbiddenPluginList.stream().map(forbiddenPlugin -> forbiddenPlugin.id).sorted().collect(Collectors.joining(", "))) {
      @Override
      public String getText(Object element)
      {
        String text = super.getText(element);
        // limit length
        if (text.length() > 128)
          text = text.substring(0, text.lastIndexOf(',', 128) + 1) + "  etc...";
        return text;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenBundlesTableViewerColumn);

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
    Menu menu = manager.createContextMenu(patternCheckTableViewer.getControl());
    patternCheckTableViewer.getControl().setMenu(menu);

    manager.addMenuListener(new PatternMenuListener());
  }

  /**
   *
   */
  public void refresh()
  {
    try
    {
      patternCheckTableViewer.getTable().setRedraw(false);

      // Update patternTableViewer
      patternCheckTableViewer.setComparator(null);
      List<PatternInfo> patternList = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList;
      patternCheckTableViewer.setInput(patternList);
      boolean[] first = new boolean[]{true};
      patternCheckTableViewer.setComparator(new DefaultLabelViewerComparator() {
        @Override
        protected String getTextForComparaison(Viewer viewer, Object elt, int columnIndex)
        {
          if (columnIndex == 0)
          {
            PatternInfo patternInfo = (PatternInfo) elt;
            String index = String.valueOf(patternList.indexOf(patternInfo));
            if (first[0])
              return index;
            return patternInfo.activate + " " + index;
          }
          return super.getTextForComparaison(viewer, elt, columnIndex);
        }
      });
      first[0] = false;

      // select activated
      Object[] activated = patternList.stream()
        .filter(patternInfo -> patternInfo.activate)
        .toArray();
      patternCheckTableViewer.setCheckedElements(activated);

      // change selectAllButton state
      if (!patternList.isEmpty())
      {
        boolean checked = patternList.get(0).activate;
        boolean all = patternList.stream().allMatch(pi -> pi.activate == checked);
        selectAllButton.setSelection(all? checked : !all);
        selectAllButton.setGrayed(!all);
      }

      // pack columns
      for(TableColumn tableColumn : patternCheckTableViewer.getTable().getColumns())
        PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);

      patternCheckTableViewer.setSelection(patternCheckTableViewer.getSelection());
    }
    finally
    {
      patternCheckTableViewer.getTable().setRedraw(true);
    }
  }

  /**
   * The class <b>PatternMenuListener</b> allows to.<br>
   */
  class PatternMenuListener implements IMenuListener
  {
    List<Type> copiedDeclaredPluginTypeList = Collections.emptyList();
    List<Type> copiedForbiddenPluginTypeList = Collections.emptyList();

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
      manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      //
      createCopyPasteTypesMenuItems(manager);
      createEditPatternMenuItem(manager);
      createDuplicatePatternMenuItem(manager);
      createRemovePatternsMenuItem(manager);
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createCopyPasteTypesMenuItems(IMenuManager manager)
    {
      boolean separatorAdded = false;

      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      Stream<PatternInfo> pluginInfoStream = selection.toList().stream().filter(PatternInfo.class::isInstance).map(PatternInfo.class::cast);
      Set<PatternInfo> selectedPluginInfoSet = pluginInfoStream.collect(Collectors.toSet());
      if (selectedPluginInfoSet.size() == 1)
      {
        PatternInfo patternInfo = selectedPluginInfoSet.iterator().next();
        if (patternInfo.containsTypes())
        {
          List<Type> currentDeclaredPluginTypeList = patternInfo.declaredPluginTypeList.stream().collect(Collectors.toList());
          List<Type> currentForbiddenPluginTypeList = patternInfo.forbiddenPluginTypeList.stream().collect(Collectors.toList());
          if (!currentDeclaredPluginTypeList.equals(copiedDeclaredPluginTypeList) || !currentForbiddenPluginTypeList.equals(copiedForbiddenPluginTypeList))
          {
            if (manager.getItems().length > 1)
              manager.add(new Separator());
            separatorAdded = true;

            manager.add(new Action("Copy types in memory") {
              @Override
              public void run()
              {
                copiedDeclaredPluginTypeList = currentDeclaredPluginTypeList;
                copiedForbiddenPluginTypeList = currentForbiddenPluginTypeList;
              }
            });
          }
        }
      }

      if (!copiedDeclaredPluginTypeList.isEmpty() || !copiedForbiddenPluginTypeList.isEmpty())
      {
        if (!selectedPluginInfoSet.isEmpty())
        {
          if (!separatorAdded && manager.getItems().length > 1)
            manager.add(new Separator());

          manager.add(new Action("Paste and replace types from memory") {
            @Override
            public void run()
            {
              Set<Type> availableTypeSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().collect(Collectors.toSet());

              for(PatternInfo patternInfo : selectedPluginInfoSet)
              {
                PatternInfo oldPatternInfo = Util.duplicatePatternInfo(patternInfo);

                // clear
                patternInfo.declaredPluginTypeList.clear();
                patternInfo.forbiddenPluginTypeList.clear();

                // copy type
                copiedDeclaredPluginTypeList.stream().filter(availableTypeSet::contains).map(Util::duplicateType).forEach(patternInfo.declaredPluginTypeList::add);
                copiedForbiddenPluginTypeList.stream().filter(availableTypeSet::contains).map(Util::duplicateType).forEach(patternInfo.forbiddenPluginTypeList::add);

                if (!copiedDeclaredPluginTypeList.isEmpty())
                {
                  PatternInfoData patternInfoData = new PatternInfoData(oldPatternInfo, copiedDeclaredPluginTypeList, false);
                  updateAllPluginInfosWithPatternInfo(patternInfoData, false);
                }
                if (!copiedForbiddenPluginTypeList.isEmpty())
                {
                  PatternInfoData patternInfoData = new PatternInfoData(oldPatternInfo, copiedForbiddenPluginTypeList, true);
                  updateAllPluginInfosWithPatternInfo(patternInfoData, false);
                }
              }

              //
              pluginTabFolder.refresh();
            }
          });
        }
      }
    }

    /**
     */
    private void createEditPatternMenuItem(IMenuManager manager)
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      if (selection.toList().size() != 1)
        return;

      if (manager.getItems().length > 1)
        manager.add(new Separator());

      manager.add(new EditPatternAction());
    }

    /**
     */
    private void createDuplicatePatternMenuItem(IMenuManager manager)
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      if (selection.toList().size() != 1)
        return;

      manager.add(new DuplicatePatternAction());
    }

    /**
     */
    private void createRemovePatternsMenuItem(IMenuManager manager)
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      if (selection.toList().isEmpty())
        return;

      manager.add(new RemoveSelectedPatterns());
    }
  }

  /**
   * The class <b>EditPatternAction</b> allows to.<br>
   */
  private final class EditPatternAction extends Action
  {
    /**
     * Constructor
     *
     * @param text
     * @param selection
     */
    private EditPatternAction()
    {
      super("Edit pattern");
    }

    @Override
    public void run()
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      PatternInfo selectedPatternInfo = (PatternInfo) selection.getFirstElement();
      String selectedDescription = selectedPatternInfo.description == null? "" : selectedPatternInfo.description;
      String selectedAcceptPattern = selectedPatternInfo.getAcceptPattern();
      String selectedDoNotAcceptPattern = selectedPatternInfo.getDoNotAcceptPattern();

      IPatternValidator patternValidator = (description, acceptPattern, doNotAcceptPattern) -> {
        if (acceptPattern.isEmpty() && doNotAcceptPattern.isEmpty())
          return "No entry";
        if (acceptPattern.equals(selectedAcceptPattern) && doNotAcceptPattern.equals(selectedDoNotAcceptPattern))
        {
          if (!description.equals(selectedDescription))
            return null;
          return "";
        }
        Predicate<PatternInfo> acceptPredicate = patternInfo -> patternInfo.getAcceptPattern().equals(acceptPattern);
        Predicate<PatternInfo> doNotAcceptPredicate = patternInfo -> patternInfo.getDoNotAcceptPattern().equals(doNotAcceptPattern);
        Predicate<PatternInfo> predicate = acceptPredicate.and(doNotAcceptPredicate);
        if (pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().anyMatch(predicate))
          return "The pattern already exists";
        return null;
      };

      Cache cache = pluginTabFolder.pluginConsistencyPreferencePage.getCache();

      InputPatternDialog inputPatternDialog = new InputPatternDialog(patternCheckTableViewer.getControl().getShell(), "Edit pattern", selectedDescription,
        ACCEPT_PLUGIN_MESSAGE, selectedAcceptPattern,
        NOT_ACCEPT_PLUGIN_MESSAGE, selectedDoNotAcceptPattern, cache, patternValidator);
      if (inputPatternDialog.open() == Window.OK)
      {
        updateAfterChange(() -> {
          selectedPatternInfo.description = inputPatternDialog.getDescription();
          selectedPatternInfo.setPattern(inputPatternDialog.getAcceptPattern(), inputPatternDialog.getDoNotAcceptPattern());
        });
      }
    }
  }

  /**
   * The class <b>DuplicatePatternAction</b> allows to.<br>
   */
  private final class DuplicatePatternAction extends Action
  {
    /**
     * Constructor
     */
    private DuplicatePatternAction()
    {
      super("Duplicate pattern");
    }

    @Override
    public void run()
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      PatternInfo selectedPatternInfo = (PatternInfo) selection.getFirstElement();
      String selectedDescription = selectedPatternInfo.description;
      String selectedAcceptPattern = selectedPatternInfo.getAcceptPattern();
      String selectedDoNotAcceptPattern = selectedPatternInfo.getDoNotAcceptPattern();

      IPatternValidator patternValidator = (description, acceptPattern, doNotAcceptPattern) -> {
        if (acceptPattern.isEmpty() && doNotAcceptPattern.isEmpty())
          return "No entry";
        if (acceptPattern.equals(selectedAcceptPattern) && doNotAcceptPattern.equals(selectedDoNotAcceptPattern))
          return "";
        Predicate<PatternInfo> acceptPredicate = patternInfo -> patternInfo.getAcceptPattern().equals(acceptPattern);
        Predicate<PatternInfo> doNotAcceptPredicate = patternInfo -> patternInfo.getDoNotAcceptPattern().equals(doNotAcceptPattern);
        Predicate<PatternInfo> predicate = acceptPredicate.and(doNotAcceptPredicate);
        Optional<PatternInfo> optional = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().filter(predicate).findAny();
        String message = optional.map(patternInfo -> "The pattern already exists " + patternInfo.description).orElse(null);
        return message;
      };

      Cache cache = pluginTabFolder.pluginConsistencyPreferencePage.getCache();

      InputPatternDialog inputPatternDialog = new InputPatternDialog(patternCheckTableViewer.getControl().getShell(), "Duplicate pattern", selectedDescription, ACCEPT_PLUGIN_MESSAGE, selectedAcceptPattern,
        NOT_ACCEPT_PLUGIN_MESSAGE, selectedDoNotAcceptPattern, cache, patternValidator);
      if (inputPatternDialog.open() == Window.OK)
      {
        PatternInfo patternInfo = Util.duplicatePatternInfo(selectedPatternInfo);
        patternInfo.setPattern(inputPatternDialog.getAcceptPattern(), inputPatternDialog.getDoNotAcceptPattern());
        patternInfo.description = inputPatternDialog.getDescription();
        pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.add(patternInfo);

        // refresh all TabFolder
        pluginTabFolder.refresh();

        patternCheckTableViewer.setSelection(new StructuredSelection(patternInfo));
      }
    }
  }

  /**
   * The class <b>RemoveSelectedPatterns</b> allows to.<br>
   */
  private final class RemoveSelectedPatterns extends Action
  {
    /**
     * Constructor
     */
    private RemoveSelectedPatterns()
    {
      super("Remove selected patterns");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
      IStructuredSelection selection = (IStructuredSelection) patternCheckTableViewer.getSelection();
      Stream<PatternInfo> selectedPatternInfoStream = selection.toList().stream().filter(PatternInfo.class::isInstance).map(PatternInfo.class::cast);
      Set<PatternInfo> selectedPatternInfoSet = selectedPatternInfoStream.collect(Collectors.toSet());
      Set<String> selectedAcceptPatternSet = selectedPatternInfoSet.stream().map(patternInfo -> patternInfo.getAcceptAndNotAcceptPattern()).collect(Collectors.toSet());
      String selectedPatterns = selectedAcceptPatternSet.stream().collect(Collectors.joining("\n"));

      Shell shell = patternCheckTableViewer.getControl().getShell();
      String message = "Do you want to remove the selected pattern:\n" + selectedPatterns + " ?";
      boolean result = MessageDialog.openConfirm(shell, "Confirm", message);
      if (result)
      {
        // remove patterns
        updateAfterChange(() -> pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.removeIf(selectedPatternInfoSet::contains));
      }
    }
  }

  private void updateAfterChange(Runnable runnable)
  {
    Util.removePatternInAllPluginInfos(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

    runnable.run();

    Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

    // refresh all TabFolder
    pluginTabFolder.refresh();
  }

  /**
   * Refresh for PatternInfo
   */
  void refreshPatternInfo(PatternInfo patternInfo)
  {
    patternCheckTableViewer.getTable().setRedraw(false);
    try
    {
      patternCheckTableViewer.refresh(patternInfo);

      // pack columns
      for(TableColumn tableColumn : patternCheckTableViewer.getTable().getColumns())
        PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);
    }
    finally
    {
      patternCheckTableViewer.getTable().setRedraw(true);
    }
  }

  /**
   * The class <b>PatternInfoColumnLabelProvider</b> allows to.<br>
   */
  class PatternInfoColumnLabelProvider extends ColumnLabelProvider
  {
    Function<PatternInfo, String> textFunction;

    PatternInfoColumnLabelProvider(Function<PatternInfo, String> textFunction)
    {
      this.textFunction = textFunction;
    }

    @Override
    public String getText(Object element)
    {
      PatternInfo patternInfo = (PatternInfo) element;
      return textFunction.apply(patternInfo);
    }

    @Override
    public Color getForeground(Object element)
    {
      PatternInfo patternInfo = (PatternInfo) element;
      return patternInfo.activate? super.getForeground(element) : Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    }

    @Override
    public Color getBackground(Object element)
    {
      PatternInfo patternInfo = (PatternInfo) element;
      return patternInfo.activate? super.getBackground(element) : Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    }
  }
}
