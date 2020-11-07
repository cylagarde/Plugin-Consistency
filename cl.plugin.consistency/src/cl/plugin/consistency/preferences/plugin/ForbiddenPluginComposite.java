package cl.plugin.consistency.preferences.plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.Images;
import cl.plugin.consistency.PatternPredicate;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.custom.StyledToolTip;
import cl.plugin.consistency.custom.StylerUtilities;
import cl.plugin.consistency.model.ForbiddenPlugin;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.preferences.ArrayTreeContentProvider;
import cl.plugin.consistency.preferences.BundlesLabelProvider;
import cl.plugin.consistency.preferences.SectionPane;

/**
 * The class <b>ForbiddenPluginComposite</b> allows to.<br>
 */
class ForbiddenPluginComposite
{
  private static final String PATTERN_SEPARATOR = ";";
  private final static String FILTER_MESSAGE = "Filter: ('*' and '?' are supported) (multiple patterns must be separated by " + PATTERN_SEPARATOR + ")";
  private final static String SELECT_FORBIDDEN_MESSAGE = "Select the forbidden plugins:";

  final PluginInfoDetail pluginInfoDetail;
  final TableViewer forbiddenPluginTableViewer;
  final ToolBar toolBar;
  final IAction addPluginAction;

  PluginInfo pluginInfo;
  CompletableFuture<Set<String>> requireBundleSetCompletableFuture;
  final Cache cache;
  final Set<Object> checkedObjects;

  /**
   * Constructor
   *
   * @param pluginInfoDetail
   * @param parent
   * @param style
   */
  ForbiddenPluginComposite(PluginInfoDetail pluginInfoDetail, Composite parent, int style)
  {
    this.pluginInfoDetail = pluginInfoDetail;

    cache = pluginInfoDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getCache();
    checkedObjects = new TreeSet<>(cache.getPluginIdComparator());

    //
    SectionPane sectionPane = new SectionPane(parent, SWT.NONE);
    sectionPane.getHeaderSection().setText("Forbidden plugins/projects");
    sectionPane.getHeaderSection().setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ));

    // Add toolbar to section
    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBar = sectionPane.createToolBar(toolBarManager);

    addPluginAction = new AddPluginAction();
    toolBarManager.add(addPluginAction);
    toolBarManager.update(true);

    //
    forbiddenPluginTableViewer = new TableViewer(sectionPane, SWT.BORDER);
    forbiddenPluginTableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new BundlesLabelProvider(cache) {
      @Override
      protected Styler getStylerForPluginId(String pluginId)
      {
        boolean isForbiddenPluginFromPattern = pluginInfoDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream()
          .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
          .anyMatch(patternInfo -> patternInfo.forbiddenPluginList.stream().anyMatch(forbiddenPlugin -> pluginId.equals(forbiddenPlugin.id)));
        return isForbiddenPluginFromPattern? StyledString.COUNTER_STYLER : null;
      }
    }));
    forbiddenPluginTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    forbiddenPluginTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

    Table table = forbiddenPluginTableViewer.getTable();
    BiFunction<Integer, Integer, StyledString> styledStringFunction = (row, column) -> {
      if (row < 0 || column < 0)
        return null;

      // search all patternInfos accepting pluginInfo
      List<PatternInfo> patternList = pluginInfoDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList;
      List<PatternInfo> patternInfos = patternList.stream()
        .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
        .collect(Collectors.toList());

      if (!patternInfos.isEmpty())
      {
        IPluginModelBase pluginModelBase = (IPluginModelBase) table.getItem(row).getData();
        String forbiddenPluginId = cache.getId(pluginModelBase);

        // search all patternInfos where forbiddenPlugin is in forbiddenPluginList
        List<PatternInfo> collectPatternInfos = patternInfos.stream()
          .filter(patternInfo -> patternInfo.forbiddenPluginList.stream().map(forbiddenPlugin -> forbiddenPlugin.id).anyMatch(forbiddenPluginId::equals))
          .collect(Collectors.toList());

        if (!collectPatternInfos.isEmpty())
        {
          StyledString styledString = new StyledString();
          Styler regexStyler = StylerUtilities.createStyler(new Color(null, 0, 128, 0));

          //
          collectPatternInfos.forEach(patternInfo -> {
            if (styledString.length() != 0)
              styledString.append('\n');

            // patternInfo
            styledString.append("#" + (patternList.indexOf(patternInfo) + 1), StylerUtilities.BOLD_STYLER);
            if (patternInfo.description != null && !patternInfo.description.isEmpty())
              styledString.append(" " + patternInfo.description, StylerUtilities.BOLD_STYLER);

            // regex
            styledString.append("  pattern[");
            String containsPattern = patternInfo.getAcceptPattern();
            String doNotContainsPattern = patternInfo.getDoNotAcceptPattern();
            if (containsPattern != null && !containsPattern.isEmpty())
            {
              styledString.append("verify=").append("\"" + containsPattern + "\"", regexStyler);

              if (doNotContainsPattern != null && !doNotContainsPattern.isEmpty())
                styledString.append(", not verify=").append("\"" + doNotContainsPattern + "\"", regexStyler);
            }
            else if (doNotContainsPattern != null && !doNotContainsPattern.isEmpty())
              styledString.append("not verify=").append("\"" + doNotContainsPattern + "\"", regexStyler);
            styledString.append(']');
          });

          return styledString;
        }
      }

      return null;
    };
    new StyledToolTip(table, styledStringFunction);

  }

  /**
   * Set PluginInfo
   *
   * @param pluginInfo
   */
  public void setPluginInfo(PluginInfo pluginInfo)
  {
    this.pluginInfo = pluginInfo;
    // Util.setEnabled(section, pluginInfo != null);

    // select forbidden plugins for TableViewer
    checkedObjects.clear();
    if (pluginInfo != null)
    {
      List<IPluginModelBase> checkedElements = pluginInfo.forbiddenPluginList.stream()
        .map(forbiddenPlugin -> forbiddenPlugin.id)
        .map(id -> {
          IPluginModelBase pluginModelBase = cache.getPluginModelBases().get(id);
          if (pluginModelBase == null)
            PluginConsistencyActivator.logInfo("Plugin not found for id " + id);
          return pluginModelBase;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
      checkedObjects.addAll(checkedElements);
    }

    // toArray: create copy
    forbiddenPluginTableViewer.setInput(checkedObjects.toArray());

    //
    if (pluginInfo == null)
      return;

    IProject workspaceProject = Util.getProject(pluginInfo);
    addPluginAction.setEnabled(workspaceProject != null && workspaceProject.isOpen());

    //
    if (addPluginAction.isEnabled())
    {
      Supplier<Set<String>> supplier = () -> {
        // find project with id
        Optional<IProject> optional = cache.getValidProjects()
          .filter(project -> cache.getId(project).equals(pluginInfo.id))
          .findFirst();

        IProject project = optional.get();
        Set<String> requireBundleSet = null;
        try
        {
          IBundleProjectService bundleProjectService = PluginConsistencyActivator.getBundleProjectService();
          IBundleProjectDescription bundleProjectDescription = bundleProjectService.getDescription(project);

          IRequiredBundleDescription[] requiredBundles = bundleProjectDescription.getRequiredBundles();
          if (requiredBundles == null)
            requireBundleSet = Collections.emptySet();
          else
            requireBundleSet = Stream.of(requiredBundles)
              .map(bundleDescription -> bundleDescription.getName())
              .collect(Collectors.toSet());
        }
        catch(CoreException e)
        {
          PluginConsistencyActivator.logError("Error: " + e, e);
        }

        return requireBundleSet;
      };

      requireBundleSetCompletableFuture = CompletableFuture.supplyAsync(supplier);
    }
  }

  public void setEnabled(boolean enabled)
  {
    toolBar.setEnabled(enabled);
  }

  /**
   * The class <b>AddPluginAction</b> allows to.<br>
   */
  class AddPluginAction extends Action
  {
    {
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
      setToolTipText("Add forbidden plugins/projects");
    }

    @Override
    public void run()
    {
      Object[] forbiddenPlugins = ((IStructuredContentProvider) forbiddenPluginTableViewer.getContentProvider()).getElements(forbiddenPluginTableViewer.getInput());
      checkedObjects.clear();

      // retrieve checkedElements
      List<IPluginModelBase> checkedElements = Stream.of(forbiddenPlugins)
        .map(cache::getId)
        .map(id -> {
          IPluginModelBase pluginModelBase = cache.getPluginModelBases().get(id);
          if (pluginModelBase == null)
            PluginConsistencyActivator.logInfo("Plugin not found for id " + id);
          return pluginModelBase;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
      checkedObjects.addAll(checkedElements);

      Set<String> requireBundleSet = new HashSet<>();
      try
      {
        requireBundleSet.addAll(requireBundleSetCompletableFuture.get());
      }
      catch(InterruptedException | ExecutionException e)
      {
        PluginConsistencyActivator.logError("Error: " + e, e);
      }

      //
      BundlesLabelProvider bundlesLabelProvider = new BundlesLabelProvider(cache, requireBundleSet);
      CheckedTreeSelectionDialog checkedTreeDialog = new CheckedTreeSelectionDialog(forbiddenPluginTableViewer.getControl().getShell(), bundlesLabelProvider, new ArrayTreeContentProvider()) {
        Text searchPluginText;

        @Override
        protected CheckboxTreeViewer createTreeViewer(Composite parent)
        {
          CheckboxTreeViewer checkboxTreeViewer = super.createTreeViewer(parent);
          checkboxTreeViewer.addCheckStateListener(event -> {
            if (event.getChecked())
              checkedObjects.add(event.getElement());
            else
              checkedObjects.remove(event.getElement());
          });
          return checkboxTreeViewer;
        }

        @Override
        protected Label createMessageArea(Composite parent)
        {
          Composite composite = new Composite(parent, SWT.NONE);
          GridLayout layout = new GridLayout(2, false);
          layout.marginWidth = layout.marginHeight = 0;
          layout.horizontalSpacing = 50;
          layout.marginBottom = 10;
          composite.setLayout(layout);

          Button seeRequirePluginButton = new Button(composite, SWT.CHECK);
          seeRequirePluginButton.setText("See require plugins");
          Button seeCheckedPluginButton = new Button(composite, SWT.CHECK);
          seeCheckedPluginButton.setText("See checked plugins");

          //
          seeRequirePluginButton.addSelectionListener(new SelectionAdapter() {
            ViewerFilter seeRequirePluginViewerFilter = new ViewerFilter() {
              @Override
              public boolean select(Viewer viewer, Object parentElement, Object element)
              {
                return requireBundleSet.contains(cache.getId(element));
              }
            };

            @Override
            public void widgetSelected(SelectionEvent e)
            {
              // init searchPluginText
              searchPluginText.setText("");

              getTreeViewer().getTree().setRedraw(false);
              seeCheckedPluginButton.setEnabled(!seeRequirePluginButton.getSelection());
              if (seeRequirePluginButton.getSelection())
                getTreeViewer().addFilter(seeRequirePluginViewerFilter);
              else
                getTreeViewer().removeFilter(seeRequirePluginViewerFilter);
              getTreeViewer().getTree().setRedraw(true);
            }
          });

          //
          seeCheckedPluginButton.addSelectionListener(new SelectionAdapter() {
            ViewerFilter seeOnlyCheckedPluginViewerFilter = new ViewerFilter() {
              @Override
              public boolean select(Viewer viewer, Object parentElement, Object element)
              {
                // boolean result = getTreeViewer().getChecked(element); // slow
                boolean result = checkedObjects.contains(element);
                return result;
              }
            };

            @Override
            public void widgetSelected(SelectionEvent e)
            {
              checkedObjects.clear();
              for(TreeItem treeItem : getTreeViewer().getTree().getItems())
              {
                if (treeItem.getChecked())
                  checkedObjects.add(treeItem.getData());
              }

              // init searchPluginText
              searchPluginText.setText("");

              getTreeViewer().getTree().setRedraw(false);
              seeRequirePluginButton.setEnabled(!seeCheckedPluginButton.getSelection());
              if (seeCheckedPluginButton.getSelection())
                getTreeViewer().addFilter(seeOnlyCheckedPluginViewerFilter);
              else
                getTreeViewer().removeFilter(seeOnlyCheckedPluginViewerFilter);
              getTreeViewer().getTree().setRedraw(true);
            }
          });

          // create label
          Label label = super.createMessageArea(parent);

          Composite searchPluginComposite = new Composite(parent, SWT.BORDER);
          GridLayout withoutMarginLayout = new GridLayout(2, false);
          withoutMarginLayout.marginWidth = withoutMarginLayout.marginHeight = withoutMarginLayout.horizontalSpacing = withoutMarginLayout.verticalSpacing = 0;
          searchPluginComposite.setLayout(withoutMarginLayout);
          searchPluginComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

          //
          searchPluginText = new Text(searchPluginComposite, SWT.SINGLE);
          searchPluginText.setLayoutData(new GridData(GridData.FILL_BOTH));
          searchPluginText.addModifyListener(new ModifyListener() {
            ViewerFilter searchPluginViewerFilter = null;
            PatternPredicate patternPredicate;

            @Override
            public void modifyText(ModifyEvent e)
            {
              String patterns = searchPluginText.getText();
              if (patterns.isEmpty())
              {
                if (searchPluginViewerFilter != null)
                {
                  getTreeViewer().removeFilter(searchPluginViewerFilter);
                  searchPluginViewerFilter = null;
                }
              }
              else
              {
                patternPredicate = new PatternPredicate(patterns, PATTERN_SEPARATOR, false);
                if (searchPluginViewerFilter == null)
                {
                  searchPluginViewerFilter = new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, Object parentElement, Object element)
                    {
                      String text = bundlesLabelProvider.getText(element);
                      return patternPredicate.test(text);
                    }
                  };
                  getTreeViewer().addFilter(searchPluginViewerFilter);
                }
              }

              getTreeViewer().refresh();
            }
          });

          Label clearLabel = new Label(searchPluginComposite, SWT.NONE);
          clearLabel.setImage(Images.CLEAR.getImage());
          clearLabel.setToolTipText("Clear");
          clearLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
          clearLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e)
            {
              // init searchPluginText
              searchPluginText.setText("");
            }
          });

          new Label(parent, SWT.NONE).setText(SELECT_FORBIDDEN_MESSAGE);

          return label;
        }
      };
      checkedTreeDialog.setTitle("Bundles and worskspace projects");
      checkedTreeDialog.setMessage(FILTER_MESSAGE);
      checkedTreeDialog.setContainerMode(true);

      //
      TreeSet<Object> treeSet = new TreeSet<>(cache.getPluginIdComparator());
      treeSet.addAll(cache.getPluginModelBases().values());

      // remove current plugin/project
      treeSet.removeIf(o -> cache.getId(o).equals(pluginInfo.id));

      // remove plugin/project from pattern
      Set<String> forbiddenPluginsFromPattern = pluginInfoDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.stream()
        .filter(patternInfo -> patternInfo.acceptPlugin(pluginInfo.id))
        .flatMap(patternInfo -> patternInfo.forbiddenPluginList.stream())
        .map(forbiddenPlugin -> forbiddenPlugin.id)
        .collect(Collectors.toSet());
      treeSet.removeIf(o -> forbiddenPluginsFromPattern.contains(cache.getId(o)));

      //
      checkedTreeDialog.setInput(treeSet.toArray());
      checkedTreeDialog.setInitialSelections(checkedObjects.toArray());

      //
      if (checkedTreeDialog.open() == IDialogConstants.OK_ID)
      {
        pluginInfo.forbiddenPluginList.clear();

        TreeSet<Object> newInput = new TreeSet<>(cache.getPluginIdComparator());

        for(String id : forbiddenPluginsFromPattern)
        {
          ForbiddenPlugin forbiddenPluginInfo = new ForbiddenPlugin();
          forbiddenPluginInfo.id = id;
          pluginInfo.forbiddenPluginList.add(forbiddenPluginInfo);

          IPluginModelBase pluginModelBase = cache.getPluginModelBases().get(id);
          if (pluginModelBase != null)
            newInput.add(pluginModelBase);
        }

        for(Object o : checkedTreeDialog.getResult())
        {
          ForbiddenPlugin forbiddenPluginInfo = new ForbiddenPlugin();
          forbiddenPluginInfo.id = cache.getId(o);
          pluginInfo.forbiddenPluginList.add(forbiddenPluginInfo);
          newInput.add(o);
        }

        forbiddenPluginTableViewer.setInput(newInput);

        pluginInfoDetail.pluginTabItem.refreshPluginInfo(pluginInfo);
      }

      checkedObjects.clear();
    }
  }
}
