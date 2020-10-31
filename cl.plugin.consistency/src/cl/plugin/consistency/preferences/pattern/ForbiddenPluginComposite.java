
package cl.plugin.consistency.preferences.pattern;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.Images;
import cl.plugin.consistency.PatternPredicate;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.ForbiddenPlugin;
import cl.plugin.consistency.model.PatternInfo;
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

  final PatternInfoDetail projectDetail;
  final TableViewer forbiddenPluginTableViewer;
  final ToolBar toolBar;
  final IAction addPluginAction;

  PatternInfo patternInfo;
  final Cache cache;

  /**
   * Constructor
   *
   * @param projectDetail
   * @param parent
   * @param style
   */
  ForbiddenPluginComposite(PatternInfoDetail projectDetail, Composite parent, int style)
  {
    this.projectDetail = projectDetail;

    cache = projectDetail.patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getCache();

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
    forbiddenPluginTableViewer.setLabelProvider(new BundlesLabelProvider(cache));
    forbiddenPluginTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    forbiddenPluginTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
  }

  /**
   * Set PatternInfo
   *
   * @param patternInfo
   */
  public void setPatternInfo(PatternInfo patternInfo)
  {
    this.patternInfo = patternInfo;
    // Util.setEnabled(section, patternInfo != null);

    // select forbidden plugins for TableViewer
    Set<Object> checkedObjects = new TreeSet<>(cache.getPluginIdComparator());
    if (patternInfo != null)
    {
      List<IPluginModelBase> checkedElements = patternInfo.forbiddenPluginList.stream()
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

      //
      BundlesLabelProvider bundlesLabelProvider = new BundlesLabelProvider(cache);
      CheckedTreeSelectionDialog checkedTreeDialog = new CheckedTreeSelectionDialog(forbiddenPluginTableViewer.getControl().getShell(), bundlesLabelProvider, new ArrayTreeContentProvider()) {
        Text searchPluginText;

        @Override
        protected Label createMessageArea(Composite parent)
        {
          Composite composite = new Composite(parent, SWT.NONE);
          GridLayout layout = new GridLayout(2, false);
          layout.marginWidth = layout.marginHeight = 0;
          layout.horizontalSpacing = 50;
          layout.marginBottom = 10;
          composite.setLayout(layout);

          Button seeCheckedPluginButton = new Button(composite, SWT.CHECK);
          seeCheckedPluginButton.setText("See checked plugins");

          //
          seeCheckedPluginButton.addSelectionListener(new SelectionAdapter() {
            ViewerFilter seeOnlyCheckedPluginViewerFilter = new ViewerFilter() {
              @Override
              public boolean select(Viewer viewer, Object parentElement, Object element)
              {
                boolean result = getTreeViewer().getChecked(element); // slow
                return result;
              }
            };

            @Override
            public void widgetSelected(SelectionEvent e)
            {
              // init searchPluginText
              searchPluginText.setText("");

              getTreeViewer().getTree().setRedraw(false);
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
      checkedTreeDialog.setInitialSelections(forbiddenPlugins);

      //
      checkedTreeDialog.setInput(cache.getPluginModelBases().values().toArray());

      //
      if (checkedTreeDialog.open() == IDialogConstants.OK_ID)
      {
        PatternInfo oldPatternInfo = Util.duplicatePatternInfo(patternInfo);

        patternInfo.forbiddenPluginList.clear();
        for(Object o : checkedTreeDialog.getResult())
        {
          ForbiddenPlugin forbiddenPatternInfo = new ForbiddenPlugin();
          forbiddenPatternInfo.id = cache.getId(o);
          patternInfo.forbiddenPluginList.add(forbiddenPatternInfo);
        }
        forbiddenPluginTableViewer.setInput(checkedTreeDialog.getResult());

        projectDetail.patternTabItem.updateAllPluginInfosWithPatternInfo(oldPatternInfo, patternInfo, true);
      }
    }
  }
}
