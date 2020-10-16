package cl.plugin.consistency.preferences.pattern;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.osgi.framework.Bundle;

import cl.plugin.consistency.Cache;
import cl.plugin.consistency.Images;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.custom.NaturalOrderComparator;
import cl.plugin.consistency.model.ForbiddenPlugin;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.preferences.BundlesLabelProvider;
import cl.plugin.consistency.preferences.SectionPane;

/**
 * The class <b>ForbiddenPluginComposite</b> allows to.<br>
 */
class ForbiddenPluginComposite
{
  final PatternInfoDetail projectDetail;
  final TableViewer forbiddenPluginTableViewer;
  final ToolBar toolBar;
  final Bundle[] bundles;
  final IAction addPluginAction;

  PatternInfo patternInfo;
  final Cache cache;
  final Set<Object> checkedObjects;

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

    bundles = PluginConsistencyActivator.getDefault().getBundle().getBundleContext().getBundles();
    cache = projectDetail.patternTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getCache();
    checkedObjects = new TreeSet<>(cache.getPluginIdComparator());

    //
    SectionPane sectionPane = new SectionPane(parent, SWT.NONE);
    sectionPane.getHeaderSection().setText("Forbidden plugins/projects");
    sectionPane.getHeaderSection().setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ));

    // Add toolbar to section
    final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
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
    checkedObjects.clear();
    if (patternInfo != null)
    {
      for(ForbiddenPlugin forbiddenPatternInfo : patternInfo.forbiddenPluginList)
      {
        Bundle bundle = Platform.getBundle(forbiddenPatternInfo.id);
        if (bundle != null)
          checkedObjects.add(bundle);
        else
        {
          Optional<IProject> optional = Stream.of(cache.getValidProjects())
            .filter(project -> cache.getId(project).equals(forbiddenPatternInfo.id))
            .findFirst();
          optional.ifPresent(checkedObjects::add);
        }
      }
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
      Object[] checkedElements = ((IStructuredContentProvider) forbiddenPluginTableViewer.getContentProvider()).getElements(forbiddenPluginTableViewer.getInput());
      checkedObjects.clear();
      checkedObjects.addAll(Arrays.asList(checkedElements));

      Set<String> requireBundleSet = new HashSet<>();
      //      try
      //      {
      //        requireBundleSet.addAll(requireBundleSetCompletableFuture.get());
      //      }
      //      catch(InterruptedException | ExecutionException e)
      //      {
      //        PluginConsistencyActivator.logError("Error: " + e, e);
      //      }

      //
      BundlesLabelProvider bundlesLabelProvider = new BundlesLabelProvider(cache, requireBundleSet);
      CheckedTreeSelectionDialog checkedTreeDialog = new CheckedTreeSelectionDialog(forbiddenPluginTableViewer.getControl().getShell(), bundlesLabelProvider, new BundleTreeContentProvider()) {
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

          Composite borderComposite = new Composite(parent, SWT.BORDER);
          GridLayout withoutMarginLayout = new GridLayout(2, false);
          withoutMarginLayout.marginWidth = withoutMarginLayout.marginHeight = withoutMarginLayout.horizontalSpacing = withoutMarginLayout.verticalSpacing = 0;
          borderComposite.setLayout(withoutMarginLayout);
          borderComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

          //
          searchPluginText = new Text(borderComposite, SWT.SINGLE);
          searchPluginText.setLayoutData(new GridData(GridData.FILL_BOTH));
          searchPluginText.addModifyListener(new ModifyListener() {
            ViewerFilter searchPluginViewerFilter = null;
            Pattern pattern;
            String text;
            boolean containsStar;

            @Override
            public void modifyText(ModifyEvent e)
            {
              text = searchPluginText.getText().toLowerCase();
              if (text.equals("*"))
              {
                text = "";
              }
              containsStar = text.contains("*");

              if (containsStar)
              {
                // Ajoute de \Q \E autour de la chaine
                String regexpPattern = Pattern.quote(text);
                // On remplace toutes les occurences de '*' afin de les interpréter
                regexpPattern = regexpPattern.replaceAll("\\*", "\\\\E.*\\\\Q");
                // On remplace toutes les occurences de '?' afin de les interpréter
                regexpPattern = regexpPattern.replaceAll("\\?", "\\\\E.\\\\Q");
                // On supprime tous les \Q \E inutiles
                regexpPattern = regexpPattern.replaceAll("\\\\Q\\\\E", "");

                //
                pattern = Pattern.compile(regexpPattern);
              }

              //
              if (searchPluginViewerFilter == null)
              {
                searchPluginViewerFilter = new ViewerFilter() {
                  @Override
                  public boolean select(Viewer viewer, Object parentElement, Object element)
                  {
                    if (text.length() == 0)
                      return true;

                    String lowerCaseText = bundlesLabelProvider.getText(element).toLowerCase();
                    boolean result = containsStar? pattern.matcher(lowerCaseText).find() : lowerCaseText.contains(text);
                    return result;
                  }
                };
                getTreeViewer().addFilter(searchPluginViewerFilter);
              }

              getTreeViewer().refresh();
            }
          });

          Label clearLabel = new Label(borderComposite, SWT.NONE);
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

          return label;
        }
      };
      checkedTreeDialog.setTitle("Bundles and worskspace projects");
      checkedTreeDialog.setMessage("Select the forbidden plugins:");
      checkedTreeDialog.setContainerMode(true);

      //
      TreeSet<Object> treeSet = new TreeSet<>(Comparator.comparing(cache::getId, NaturalOrderComparator.INSTANCE));
      treeSet.addAll(Arrays.asList(cache.getValidProjects()));

      // remove current plugin/project
      //      treeSet.removeIf(o -> cache.getId(o).equals(patternInfo.id));

      treeSet.addAll(Arrays.asList(bundles));

      //
      checkedTreeDialog.setInput(treeSet.toArray());
      checkedTreeDialog.setInitialSelections(checkedObjects.toArray());

      //
      if (checkedTreeDialog.open() == IDialogConstants.OK_ID)
      {
        patternInfo.forbiddenPluginList.clear();
        for(Object o : checkedTreeDialog.getResult())
        {
          ForbiddenPlugin forbiddenPatternInfo = new ForbiddenPlugin();
          forbiddenPatternInfo.id = cache.getId(o);
          patternInfo.forbiddenPluginList.add(forbiddenPatternInfo);
        }
        forbiddenPluginTableViewer.setInput(checkedTreeDialog.getResult());

        projectDetail.patternTabItem.updateAllPluginInfosWithPatternInfo(patternInfo, true);
      }

      checkedObjects.clear();
    }

    /**
     * The class <b>BundleTreeContentProvider</b> allows to.<br>
     */
    class BundleTreeContentProvider implements ITreeContentProvider
    {

      @Override
      public Object[] getElements(Object inputElement)
      {
        return (Object[]) inputElement;
      }

      @Override
      public Object[] getChildren(Object parentElement)
      {
        return null;
      }

      @Override
      public Object getParent(Object element)
      {
        return null;
      }

      @Override
      public boolean hasChildren(Object element)
      {
        return false;
      }
    }
  }
}
