package cl.plugin.consistency.preferences.pluginInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE.SharedImages;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.BundlesLabelProvider;
import cl.plugin.consistency.preferences.DefaultLabelViewerComparator;
import cl.plugin.consistency.preferences.PluginTabFolder;
import cl.plugin.consistency.tooltip.StyledToolTip;
import cl.plugin.consistency.tooltip.StylerUtilities;

/**
 * The class <b>PluginTabItem</b> allows to.<br>
 */
public class PluginTabItem
{
  public static final int COLUMN_PREFERRED_WIDTH = 250;
  public static final String COLUMN_SPACE_KEY = "COLUMN_SPACE";
  public static final int COLUMN_SPACE = 5;

  final PluginTabFolder pluginTabFolder;
  final Cache cache;
  TableViewer projectTableViewer;
  TableViewerColumn typeTableViewerColumn;
  TableViewerColumn forbiddenTypeTableViewerColumn;
  TableViewerColumn forbiddenBundlesTableViewerColumn;
  ProjectDetail projectDetail;

  /**
   * Constructor
   */
  public PluginTabItem(PluginTabFolder pluginTabFolder)
  {
    this.pluginTabFolder = pluginTabFolder;
    cache = pluginTabFolder.pluginConsistencyPreferencePage.getCache();

    //
    TabItem pluginTabItem = new TabItem(pluginTabFolder.tabFolder, SWT.NONE);
    pluginTabItem.setText("Plugins");
    pluginTabItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT));

    //
    Composite pluginTabComposite = new Composite(pluginTabFolder.tabFolder, SWT.NONE);
    pluginTabItem.setControl(pluginTabComposite);

    GridLayout pluginTabCompositeLayout = new GridLayout(1, false);
    pluginTabCompositeLayout.marginWidth = pluginTabCompositeLayout.marginHeight = 0;
    pluginTabCompositeLayout.verticalSpacing = 10;
    pluginTabComposite.setLayout(pluginTabCompositeLayout);

    configureProjectSashForm(pluginTabComposite);

    //
    refresh();
  }

  /**
   * Configure Project SashForm
   *
   * @param parent
   */
  private void configureProjectSashForm(Composite parent)
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
    configureProjectTableViewer(sashForm);

    //
    ScrolledComposite scrolledComposite = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);

    //
    projectDetail = new ProjectDetail(this, scrolledComposite);
    scrolledComposite.setContent(projectDetail.content);
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));

    // selection
    projectTableViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
      PluginInfo pluginInfo = (PluginInfo) selection.getFirstElement();
      projectDetail.setPluginInfo(pluginInfo);
    });

    sashForm.setWeights(new int[]{2, 1});
  }

  /**
   * Configure Project TableViewer
   *
   * @param parent
   */
  private void configureProjectTableViewer(Composite parent)
  {
    //
    projectTableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
    projectTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    projectTableViewer.setComparator(new DefaultLabelViewerComparator());

    Table table = projectTableViewer.getTable();
    table.setLayout(new TableLayout());
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    // define specific tooltip
    BiFunction<Integer, Integer, StyledString> styledStringFunction = (row, column) -> {
      if (row >= 0 && column >= 0 && (table.getColumn(column) == typeTableViewerColumn.getColumn() || table.getColumn(column) == forbiddenTypeTableViewerColumn.getColumn()))
      {
        PluginInfo pluginInfo = (PluginInfo) table.getItem(row).getData();

        List<PatternInfo> patternList = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList;
        Set<PatternInfo> patternInfos = patternList.stream()
          .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
          .collect(Collectors.toSet());

        if (!patternInfos.isEmpty())
        {
          Map<String, List<PatternInfo>> typeToPatternInfoMap = new TreeMap<>();
          if (table.getColumn(column) == typeTableViewerColumn.getColumn())
            patternInfos.forEach(patternInfo -> patternInfo.typeList.forEach(type -> typeToPatternInfoMap.computeIfAbsent(type.name, k -> new ArrayList<>()).add(patternInfo)));
          else
            patternInfos.forEach(patternInfo -> patternInfo.forbiddenTypeList.forEach(type -> typeToPatternInfoMap.computeIfAbsent(type.name, k -> new ArrayList<>()).add(patternInfo)));

          StyledString buffer = new StyledString();

          boolean[] firstType = new boolean[]{true};
          typeToPatternInfoMap.forEach((type, list) -> {
            if (!firstType[0])
              buffer.append("\n\n");
            else
              firstType[0] = false;
            buffer.append(type, StylerUtilities.withBold(StyledString.COUNTER_STYLER)).append(":\n");

            // sort
            list.sort(Comparator.comparing(patternList::indexOf, Integer::compare));

            //
            boolean[] firstList = new boolean[]{true};
            list.forEach(patternInfo -> {
              if (!firstList[0])
                buffer.append("\n");
              else
                firstList[0] = false;

              buffer.append("    #" + (patternList.indexOf(patternInfo) + 1), StylerUtilities.boldStyler);
              buffer.append("  pattern[");

              String containsPattern = patternInfo.getContainsPattern();
              String doNotContainsPattern = patternInfo.getDoNotContainsPattern();
              if (containsPattern != null && !containsPattern.isEmpty())
              {
                buffer.append("contains=").append("\"" + containsPattern + "\"", StylerUtilities.createStyler(new Color(null, 0, 128, 0)));

                if (doNotContainsPattern != null && !doNotContainsPattern.isEmpty())
                  buffer.append(", not contains=").append("\"" + doNotContainsPattern + "\"", StylerUtilities.createStyler(new Color(null, 0, 128, 0)));
              }
              else if (doNotContainsPattern != null && !doNotContainsPattern.isEmpty())
                buffer.append("not contains=").append("\"" + doNotContainsPattern + "\"", StylerUtilities.createStyler(new Color(null, 0, 128, 0)));
            });
          });

          return buffer;
        }
      }
      return null;
    };
    StyledToolTip styledToolTip = new StyledToolTip(table, styledStringFunction);

    // 'Plugin id' TableViewerColumn
    TableViewerColumn pluginIdTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    pluginIdTableViewerColumn.getColumn().setText("Workspace plugin id");
    pluginIdTableViewerColumn.getColumn().setWidth(COLUMN_PREFERRED_WIDTH);
    pluginIdTableViewerColumn.getColumn().setData(COLUMN_SPACE_KEY, COLUMN_SPACE);
    pluginIdTableViewerColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new BundlesLabelProvider(cache)
    {
      @Override
      public StyledString getStyledText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        IProject project = Util.getProject(pluginInfo);
        boolean isValidPlugin = cache.isValidProject(project);
        if (isValidPlugin)
          return super.getStyledText(project);

        // invalid project
        StyledString styledString = new StyledString();
        styledString.append(pluginInfo.id, StyledString.QUALIFIER_STYLER);
        if (!pluginInfo.id.equals(pluginInfo.name))
          styledString.append(" (" + pluginInfo.name + ")", StyledString.QUALIFIER_STYLER);

        return styledString;
      }

      @Override
      public Image getImage(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        IProject project = Util.getProject(pluginInfo);
        Image projectImage = super.getImage(project);

        //
        boolean isValidPlugin = cache.isValidProject(project);
        if (isValidPlugin)
          return projectImage;

        // invalid project
        String key = SharedImages.IMG_OBJ_PROJECT + ":" + ISharedImages.IMG_DEC_FIELD_ERROR;
        Image img = PluginConsistencyActivator.getDefault().getImageRegistry().get(key);
        if (img == null)
        {
          ImageDescriptor imageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
          DecorationOverlayIcon resultIcon = new DecorationOverlayIcon(projectImage, imageDescriptor, IDecoration.BOTTOM_LEFT);
          img = resultIcon.createImage();
          PluginConsistencyActivator.getDefault().getImageRegistry().put(key, img);
        }

        return img;
      }
    }));
    DefaultLabelViewerComparator.configureForSortingColumn(pluginIdTableViewerColumn);

    // 'Type' TableViewerColumn
    typeTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    typeTableViewerColumn.getColumn().setText("Plugin type");
    typeTableViewerColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public StyledString getStyledText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;

        StyledString styledString = new StyledString();

        Set<String> typeFromPatternInfoSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream()
          .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
          .flatMap(patternInfo -> patternInfo.typeList.stream())
          .map(type -> type.name)
          .collect(Collectors.toSet());

        int[] size = {pluginInfo.typeList.size()};
        pluginInfo.typeList.stream()
          .map(type -> type.name)
          .sorted()
          .forEach(typename -> {
            if (typeFromPatternInfoSet.contains(typename))
              styledString.append(typename, StyledString.COUNTER_STYLER);
            else
              styledString.append(typename);
            if (size[0]-- != 1)
              styledString.append(", ");
          });

        return styledString;
      }
    }));
    DefaultLabelViewerComparator.configureForSortingColumn(typeTableViewerColumn);

    // 'Forbidden types' TableViewerColumn
    forbiddenTypeTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    forbiddenTypeTableViewerColumn.getColumn().setText("Forbidden plugin type");
    forbiddenTypeTableViewerColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public StyledString getStyledText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;

        StyledString styledString = new StyledString();

        Set<String> forbiddenTypeFromPatternInfoSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
          .flatMap(patternInfo -> patternInfo.forbiddenTypeList.stream()).map(type -> type.name).collect(Collectors.toSet());

        int[] size = {pluginInfo.forbiddenTypeList.size()};
        pluginInfo.forbiddenTypeList.stream().map(forbiddenType -> forbiddenType.name).sorted().forEach(forbiddenTypename -> {
          if (forbiddenTypeFromPatternInfoSet.contains(forbiddenTypename))
            styledString.append(forbiddenTypename, StyledString.COUNTER_STYLER);
          else
            styledString.append(forbiddenTypename);
          if (size[0]-- != 1)
            styledString.append(", ");
        });

        return styledString;
      }
    }));
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenTypeTableViewerColumn);

    // 'Forbidden bundles' TableViewerColumn
    forbiddenBundlesTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    forbiddenBundlesTableViewerColumn.getColumn().setText("Forbidden bundle");
    forbiddenBundlesTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        String forbiddenBundles = pluginInfo.forbiddenPluginList.stream().map(forbiddenPluginInfo -> forbiddenPluginInfo.id).sorted().collect(Collectors.joining(", "));
        return forbiddenBundles;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenBundlesTableViewerColumn);

    //
    configurePopupMenuForProjectTableViewer();
  }

  /**
   *
   */
  private void configurePopupMenuForProjectTableViewer()
  {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    Menu menu = manager.createContextMenu(projectTableViewer.getControl());
    projectTableViewer.getControl().setMenu(menu);

    manager.addMenuListener(new PluginInfoMenuListener());
  }

  /**
   * Refresh
   */
  public void refresh()
  {
    projectTableViewer.getTable().setRedraw(false);

    try
    {
      // Update projectTableViewer
      projectTableViewer.setInput(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList);

      // pack columns
      for(TableColumn tableColumn : projectTableViewer.getTable().getColumns())
        pack(tableColumn, COLUMN_PREFERRED_WIDTH);

      projectTableViewer.setSelection(projectTableViewer.getSelection());
    }
    finally
    {
      projectTableViewer.getTable().setRedraw(true);
    }

    //
    projectDetail.refresh();
  }

  /**
   * Pack table column
   *
   * @param tableColumn
   * @param maxWidth
   */
  public static void pack(TableColumn tableColumn, int maxWidth)
  {
    tableColumn.pack();
    Object columnSpace = tableColumn.getData(COLUMN_SPACE_KEY);
    if (columnSpace instanceof Integer)
    {
      int width = tableColumn.getWidth();
      width = Math.min(width + (Integer) columnSpace, maxWidth);
      tableColumn.setWidth(width);
    }
  }

  /**
   * Refresh for PluginInfo
   */
  void refreshPluginInfo(PluginInfo pluginInfo)
  {
    projectTableViewer.getTable().setRedraw(false);
    try
    {
      projectTableViewer.refresh(pluginInfo);

      pack(typeTableViewerColumn.getColumn(), COLUMN_PREFERRED_WIDTH);
      pack(forbiddenTypeTableViewerColumn.getColumn(), COLUMN_PREFERRED_WIDTH);
    }
    finally
    {
      projectTableViewer.getTable().setRedraw(true);
    }
  }

  /**
   * The class <b>PluginInfoColumnLabelProvider</b> allows to.<br>
   */
  class PluginInfoColumnLabelProvider extends ColumnLabelProvider implements IStyledLabelProvider
  {
    @Override
    public final Color getForeground(Object element)
    {
      PluginInfo pluginInfo = (PluginInfo) element;
      IProject project = Util.getProject(pluginInfo);
      Boolean isValidPlugin = cache.isValidProject(project);
      if (isValidPlugin)
        return super.getForeground(element);
      ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      return colorRegistry.get(JFacePreferences.QUALIFIER_COLOR);
    }

    @Override
    public String getText(Object element)
    {
      return getStyledText(element).getString();
    }

    @Override
    public StyledString getStyledText(Object element)
    {
      return new StyledString(super.getText(element));
    }
  }

  /**
   * The class <b>PluginInfoMenuListener</b> allows to.<br>
   */
  class PluginInfoMenuListener implements IMenuListener
  {
    Set<String> copiedTypeSet = Collections.emptySet();
    Set<String> copiedForbiddenTypeSet = Collections.emptySet();

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
      manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      if (pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList.isEmpty())
        return;

      //
      createCopyPasteTypesMenuItems(manager);
      createAddTypesFromPatternsMenuItems(manager);
      createRemoveTypesFromPatternsMenuItems(manager);
      createResetTypesMenuItems(manager);
      createRemoveInvalidPluginsMenuItem(manager);
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createCopyPasteTypesMenuItems(IMenuManager manager)
    {
      boolean separatorAdded = false;

      IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
      Stream<PluginInfo> pluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
      Set<PluginInfo> selectedPluginInfoSet = pluginInfoStream.collect(Collectors.toSet());
      if (selectedPluginInfoSet.size() == 1)
      {
        PluginInfo pluginInfo = selectedPluginInfoSet.iterator().next();
        if (pluginInfo.containsInformations())
        {
          Set<String> currentTypeSet = pluginInfo.typeList.stream().map(type -> type.name).collect(Collectors.toSet());
          Set<String> currentForbiddenTypeSet = pluginInfo.forbiddenTypeList.stream().map(type -> type.name).collect(Collectors.toSet());
          if (!currentTypeSet.equals(copiedTypeSet) || !currentForbiddenTypeSet.equals(copiedForbiddenTypeSet))
          {
            if (manager.getItems().length > 1)
              manager.add(new Separator());
            separatorAdded = true;

            manager.add(new Action("Copy types in memory")
            {
              @Override
              public void run()
              {
                copiedTypeSet = currentTypeSet;
                copiedForbiddenTypeSet = currentForbiddenTypeSet;
              }
            });
          }
        }
      }

      if (!copiedTypeSet.isEmpty() || !copiedForbiddenTypeSet.isEmpty())
      {
        if (!selectedPluginInfoSet.isEmpty())
        {
          if (!separatorAdded && manager.getItems().length > 1)
            manager.add(new Separator());

          manager.add(new Action("Paste and replace types from memory")
          {
            @Override
            public void run()
            {
              Set<String> availableTypeSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> type.name).collect(Collectors.toSet());

              for(PluginInfo pluginInfo : selectedPluginInfoSet)
              {
                // clear
                pluginInfo.typeList.clear();
                pluginInfo.forbiddenTypeList.clear();

                // copy type
                for(String typeName : copiedTypeSet)
                {
                  if (availableTypeSet.contains(typeName))
                  {
                    Type newType = new Type();
                    newType.name = typeName;
                    pluginInfo.typeList.add(newType);
                  }
                }

                // copy forbidden type
                for(String forbiddenTypeName : copiedForbiddenTypeSet)
                {
                  if (availableTypeSet.contains(forbiddenTypeName))
                  {
                    Type newForbiddentType = new Type();
                    newForbiddentType.name = forbiddenTypeName;
                    pluginInfo.forbiddenTypeList.add(newForbiddentType);
                  }
                }
              }

              // refresh tableViewer
              refresh();
            }
          });
        }
      }
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createRemoveInvalidPluginsMenuItem(IMenuManager manager)
    {
      IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
      Stream<PluginInfo> selectedPluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
      Set<PluginInfo> notExistPluginInfoSet = selectedPluginInfoStream.filter(pluginInfo -> !Util.getProject(pluginInfo).exists()).collect(Collectors.toSet());
      if (!notExistPluginInfoSet.isEmpty())
      {
        if (manager.getItems().length > 1)
          manager.add(new Separator());

        manager.add(new Action("Remove non-existent plugins")
        {
          @Override
          public void run()
          {
            String notExistPluginInfoNames = notExistPluginInfoSet.stream().map(pluginInfo -> pluginInfo.name).collect(Collectors.joining(", "));

            Shell shell = projectTableViewer.getControl().getShell();
            String message = "Do you want to remove the selected non-existent plugins\n" + notExistPluginInfoNames + " ?";
            boolean result = MessageDialog.openConfirm(shell, "Confirm", message);
            if (result)
            {
              // remove pluginInfos
              pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList.removeIf(notExistPluginInfoSet::contains);

              // refresh tableViewer
              refresh();
            }
          }
        });
      }
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createRemoveTypesFromPatternsMenuItems(IMenuManager manager)
    {
      if (!pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.isEmpty())
      {
        boolean separatorAdded = false;

        IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
        Stream<PluginInfo> selectedPluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
        Set<PluginInfo> modifiedSelectedPluginInfoSet = selectedPluginInfoStream.filter(PluginInfo::containsInformations).collect(Collectors.toSet());
        if (!modifiedSelectedPluginInfoSet.isEmpty())
        {
          if (manager.getItems().length > 1)
            manager.add(new Separator());
          separatorAdded = true;

          manager.add(new Action("Remove types from patterns on selected plugins")
          {
            @Override
            public void run()
            {
              //
              for(PluginInfo pluginInfo : modifiedSelectedPluginInfoSet)
                Util.removePatternInPluginInfo(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency, pluginInfo);

              // refresh tableViewer
              refresh();
            }
          });
        }

        Set<PluginInfo> modifiedPluginInfoSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList.stream().filter(PluginInfo::containsInformations).collect(Collectors.toSet());
        if (!modifiedPluginInfoSet.isEmpty())
        {
          if (!separatorAdded && manager.getItems().length > 1)
            manager.add(new Separator());

          manager.add(new Action("Remove types from patterns on all plugins")
          {
            @Override
            public void run()
            {
              //
              for(PluginInfo pluginInfo : modifiedPluginInfoSet)
                Util.removePatternInPluginInfo(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency, pluginInfo);

              // refresh tableViewer
              refresh();
            }
          });
        }
      }
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createAddTypesFromPatternsMenuItems(IMenuManager manager)
    {
      Set<PatternInfo> modifiedPatternInfoSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream().filter(PatternInfo::containsTypes).collect(Collectors.toSet());
      if (!modifiedPatternInfoSet.isEmpty())
      {
        if (manager.getItems().length > 1)
          manager.add(new Separator());

        //
        IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
        Stream<PluginInfo> selectedPluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
        Set<PluginInfo> modifiedPluginInfoSet = selectedPluginInfoStream.collect(Collectors.toSet());

        boolean found = false;
        loop: for(PatternInfo patternInfo : modifiedPatternInfoSet)
        {
          for(PluginInfo pluginInfo : modifiedPluginInfoSet)
          {
            if (patternInfo.acceptPlugin(pluginInfo.id))
            {
              found = true;
              break loop;
            }
          }
        }

        if (found)
        {
          manager.add(new Action("Add types from patterns on selected plugins")
          {
            @Override
            public void run()
            {
              //
              for(PluginInfo pluginInfo : modifiedPluginInfoSet)
                Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency, pluginInfo);

              // refresh tableViewer
              refresh();
            }
          });
        }

        manager.add(new Action("Add types from patterns on all plugins")
        {
          @Override
          public void run()
          {
            //
            Util.updatePluginInfoWithPattern(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency);

            // refresh tableViewer
            refresh();
          }
        });
      }
    }

    /**
     * @param manager
     */
    @SuppressWarnings("unchecked")
    private void createResetTypesMenuItems(IMenuManager manager)
    {
      boolean separatorAdded = false;

      IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
      Stream<PluginInfo> selectedPluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
      Set<PluginInfo> modifiedSelectedPluginInfoSet = selectedPluginInfoStream.filter(PluginInfo::containsInformations).collect(Collectors.toSet());
      if (!modifiedSelectedPluginInfoSet.isEmpty())
      {
        if (manager.getItems().length > 1)
          manager.add(new Separator());
        separatorAdded = true;

        manager.add(new Action("Reset types on selected plugins")
        {
          @Override
          public void run()
          {
            //
            for(PluginInfo pluginInfo : modifiedSelectedPluginInfoSet)
              Util.resetTypesInPluginInfo(pluginInfo);

            // refresh tableViewer
            refresh();
          }
        });
      }

      Set<PluginInfo> modifiedPluginInfoSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList.stream().filter(PluginInfo::containsInformations).collect(Collectors.toSet());
      if (!modifiedPluginInfoSet.isEmpty())
      {
        if (!separatorAdded && manager.getItems().length > 1)
          manager.add(new Separator());

        manager.add(new Action("Reset types on all plugins")
        {
          @Override
          public void run()
          {
            //
            for(PluginInfo pluginInfo : modifiedPluginInfoSet)
              Util.resetTypesInPluginInfo(pluginInfo);

            // refresh tableViewer
            refresh();
          }
        });
      }
    }
  }
}
