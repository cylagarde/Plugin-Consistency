package cl.plugin.consistency.preferences.pluginInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.osgi.framework.Bundle;

import cl.plugin.consistency.Images;
import cl.plugin.consistency.PluginConsistencyActivator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.ForbiddenPlugin;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.preferences.SectionPane;

/**
 * The class <b>ForbiddenPluginComposite</b> allows to.<br>
 */
class ForbiddenPluginComposite
{
  final ProjectDetail projectDetail;
  final TableViewer forbiddenPluginTableViewer;
  final Bundle[] bundles;
  final IProject[] validProjects;
  final IAction addPluginAction;

  PluginInfo pluginInfo;
  CompletableFuture<Set<String>> requireBundleSetCompletableFuture;

  /**
   * Constructor
   * @param projectDetail
   * @param parent
   * @param style
   */
  ForbiddenPluginComposite(ProjectDetail projectDetail, Composite parent, int style)
  {
    this.projectDetail = projectDetail;

    bundles = PluginConsistencyActivator.getDefault().getBundle().getBundleContext().getBundles();
    validProjects = Util.getValidProjects();

    //
    SectionPane sectionPane = new SectionPane(parent, SWT.NONE);
    sectionPane.getHeaderSection().setText("Forbidden bundles/projects");
    sectionPane.getHeaderSection().setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ));

    // Add toolbar to section
    final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    sectionPane.createToolBar(toolBarManager);

    addPluginAction = new AddPluginAction();
    toolBarManager.add(addPluginAction);
    toolBarManager.update(true);

    //
    forbiddenPluginTableViewer = new TableViewer(sectionPane, SWT.BORDER);
    forbiddenPluginTableViewer.getTable().setLinesVisible(true);
    forbiddenPluginTableViewer.setLabelProvider(new BundlesLabelProvider(projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage));
    forbiddenPluginTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    forbiddenPluginTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
  }

  /**
   * Set PluginInfo
   * @param pluginInfo
   */
  public void setPluginInfo(PluginInfo pluginInfo)
  {
    this.pluginInfo = pluginInfo;
    //    Util.setEnabled(section, pluginInfo != null);

    // select forbidden plugins for TableViewer
    List<Object> bundleList = new ArrayList<>();
    if (pluginInfo != null)
    {
      for(ForbiddenPlugin forbiddenPluginInfo : pluginInfo.forbiddenPluginList)
      {
        Bundle bundle = Platform.getBundle(forbiddenPluginInfo.id);
        if (bundle != null)
          bundleList.add(bundle);
        else
        {
          Optional<IProject> optional = Stream.of(validProjects).filter(project -> projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(project).equals(forbiddenPluginInfo.id)).findFirst();
          optional.ifPresent(bundleList::add);
        }
      }
    }

    forbiddenPluginTableViewer.setInput(bundleList);

    //
    if (pluginInfo == null)
      return;

    IProject workspaceProject = Util.getProject(pluginInfo);
    addPluginAction.setEnabled(workspaceProject != null && workspaceProject.isOpen());

    //
    if (addPluginAction.isEnabled())
    {
      Supplier<Set<String>> supplier = () -> {
        Optional<IProject> optional = Stream.of(validProjects).filter(project -> projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(project).equals(pluginInfo.id)).findFirst();

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
            requireBundleSet = Stream.of(requiredBundles).map(bundleDescription -> bundleDescription.getName()).collect(Collectors.toSet());
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

  /**
   * The class <b>AddPluginAction</b> allows to.<br>
   */
  class AddPluginAction extends Action
  {
    {
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
      setToolTipText("Select forbidden plugins");
    }

    @Override
    public void run()
    {
      Set<String> tmpSet = null;
      try
      {
        tmpSet = requireBundleSetCompletableFuture.get();
      }
      catch(InterruptedException | ExecutionException e)
      {
        PluginConsistencyActivator.logError("Error: " + e, e);
        tmpSet = Collections.emptySet();
      }
      Set<String> requireBundleSet = tmpSet;

      //
      BundlesLabelProvider bundlesLabelProvider = new BundlesLabelProvider(projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage, requireBundleSet);
      CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(forbiddenPluginTableViewer.getControl().getShell(), bundlesLabelProvider, new BundleTreeContentProvider())
      {
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

          Button seeRequirePluginButton = new Button(composite, SWT.CHECK);
          seeRequirePluginButton.setText("See require plugins");
          Button seeCheckedPluginButton = new Button(composite, SWT.CHECK);
          seeCheckedPluginButton.setText("See checked plugins");

          //
          seeRequirePluginButton.addSelectionListener(new SelectionAdapter()
          {
            ViewerFilter seeRequirePluginViewerFilter = new ViewerFilter()
            {
              @Override
              public boolean select(Viewer viewer, Object parentElement, Object element)
              {
                return requireBundleSet.contains(projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(element));
              }
            };

            @Override
            public void widgetSelected(SelectionEvent e)
            {
              // init searchPluginText
              searchPluginText.setText("");

              seeCheckedPluginButton.setEnabled(!seeRequirePluginButton.getSelection());
              if (seeRequirePluginButton.getSelection())
                getTreeViewer().addFilter(seeRequirePluginViewerFilter);
              else
                getTreeViewer().removeFilter(seeRequirePluginViewerFilter);
            }
          });

          //
          seeCheckedPluginButton.addSelectionListener(new SelectionAdapter()
          {
            ViewerFilter seeOnlyCheckedPluginViewerFilter = new ViewerFilter()
            {
              @Override
              public boolean select(Viewer viewer, Object parentElement, Object element)
              {
                return getTreeViewer().getChecked(element);
              }
            };

            @Override
            public void widgetSelected(SelectionEvent e)
            {
              // init searchPluginText
              searchPluginText.setText("");

              seeRequirePluginButton.setEnabled(!seeCheckedPluginButton.getSelection());
              if (seeCheckedPluginButton.getSelection())
                getTreeViewer().addFilter(seeOnlyCheckedPluginViewerFilter);
              else
                getTreeViewer().removeFilter(seeOnlyCheckedPluginViewerFilter);
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
          searchPluginText.addModifyListener(new ModifyListener()
          {
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
                searchPluginViewerFilter = new ViewerFilter()
                {
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
          clearLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
          clearLabel.addMouseListener(new MouseAdapter()
          {
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
      dialog.setTitle("Bundles and worskspace projects");
      dialog.setMessage("Select the forbidden plugins:");

      //
      Comparator<Object> bundleProjectComparator = (o1, o2) -> {
        String id1 = projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(o1);
        String id2 = projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(o2);
        return String.CASE_INSENSITIVE_ORDER.compare(id1, id2);
      };
      TreeSet<Object> set = new TreeSet<>(bundleProjectComparator);
      set.addAll(Arrays.asList(validProjects));
      set.addAll(Arrays.asList(bundles));

      //
      dialog.setInput(set.toArray());
      dialog.setInitialSelections(ArrayContentProvider.getInstance().getElements(forbiddenPluginTableViewer.getInput()));

      //
      if (dialog.open() == IDialogConstants.OK_ID)
      {
        pluginInfo.forbiddenPluginList.clear();
        for(Object o : dialog.getResult())
        {
          ForbiddenPlugin forbiddenPluginInfo = new ForbiddenPlugin();
          forbiddenPluginInfo.id = projectDetail.pluginTabItem.pluginTabFolder.pluginConsistencyPreferencePage.getIdInCache(o);
          pluginInfo.forbiddenPluginList.add(forbiddenPluginInfo);
        }
        forbiddenPluginTableViewer.setInput(dialog.getResult());

        projectDetail.pluginTabItem.refreshPluginInfo(pluginInfo);
      }
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
