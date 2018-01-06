package cl.plugin.consistency.preferences;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE.SharedImages;

import cl.plugin.consistency.Activator;
import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PluginInfo;

/**
 *
 */
public class PluginTabItem
{
  static final int COLUMN_PREFERRED_WIDTH = 200;
  static final String COLUMN_SPACE_KEY = "COLUMN_SPACE";
  static final int COLUMN_SPACE = 5;

  final PluginTabFolder pluginTabFolder;
  TableViewer projectTableViewer;
  TableViewerColumn typeTableViewerColumn;
  TableViewerColumn forbiddenTypesTableViewerColumn;
  TableViewerColumn forbiddenBundlesTableViewerColumn;
  ProjectDetail projectDetail;

  /**
   * Constructor
   */
  public PluginTabItem(PluginTabFolder pluginTabFolder)
  {
    this.pluginTabFolder = pluginTabFolder;

    //
    TabItem pluginTabItem = new TabItem(pluginTabFolder.tabFolder, SWT.NONE);
    pluginTabItem.setText("Plugins");

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
   * @param parent
   */
  private void configureProjectSashForm(Composite parent)
  {
    FormToolkit formToolkit = new FormToolkit(parent.getDisplay());

    //
    SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
    formToolkit.adapt(sashForm);
    //    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridData layoutData = new GridData(GridData.FILL_BOTH);
    layoutData.widthHint = 1000;
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
   * @param parent
   */
  private void configureProjectTableViewer(Composite parent)
  {
    //
    projectTableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
    projectTableViewer.getTable().setLayout(new TableLayout());
    projectTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    projectTableViewer.getTable().setHeaderVisible(true);
    projectTableViewer.getTable().setLinesVisible(true);
    projectTableViewer.setComparator(new DefaultLabelViewerComparator());

    // 'Project' TableViewerColumn
    TableViewerColumn projectTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    projectTableViewerColumn.getColumn().setText("Project");
    projectTableViewerColumn.getColumn().setWidth(COLUMN_PREFERRED_WIDTH);
    projectTableViewerColumn.getColumn().setData(COLUMN_SPACE_KEY, COLUMN_SPACE);

    projectTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        return pluginInfo.name;
      }

      @Override
      public Image getImage(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        IProject project = Util.getProject(pluginInfo);
        Image projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

        //
        if (Util.isValidPlugin(project))
          return projectImage;

        // invalid project
        String key = SharedImages.IMG_OBJ_PROJECT + ":" + ISharedImages.IMG_DEC_FIELD_ERROR;
        Image img = Activator.getDefault().getImageRegistry().get(key);
        if (img == null)
        {
          ImageDescriptor imageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
          DecorationOverlayIcon resultIcon = new DecorationOverlayIcon(projectImage, imageDescriptor, IDecoration.BOTTOM_LEFT);
          img = resultIcon.createImage();
          Activator.getDefault().getImageRegistry().put(key, img);
        }

        return img;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(projectTableViewerColumn);

    // 'Plugin Id' TableViewerColumn
    TableViewerColumn pluginIdTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    pluginIdTableViewerColumn.getColumn().setText("Plugin Id");
    pluginIdTableViewerColumn.getColumn().setWidth(COLUMN_PREFERRED_WIDTH);
    pluginIdTableViewerColumn.getColumn().setData(COLUMN_SPACE_KEY, COLUMN_SPACE);
    pluginIdTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        return pluginInfo.id;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(pluginIdTableViewerColumn);

    // 'Type' TableViewerColumn
    typeTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    typeTableViewerColumn.getColumn().setText("Type");
    typeTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        String types = pluginInfo.typeReferenceList.stream().map(type -> type.name).collect(Collectors.joining(", "));
        return types;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(typeTableViewerColumn);

    // 'Forbidden types' TableViewerColumn
    forbiddenTypesTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    forbiddenTypesTableViewerColumn.getColumn().setText("Forbidden types");
    forbiddenTypesTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        String forbiddenTypes = pluginInfo.forbiddenTypeList.stream().map(forbiddenType -> forbiddenType.name).collect(Collectors.joining(", "));
        return forbiddenTypes;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenTypesTableViewerColumn);

    // 'Forbidden bundles' TableViewerColumn
    forbiddenBundlesTableViewerColumn = new TableViewerColumn(projectTableViewer, SWT.NONE);
    forbiddenBundlesTableViewerColumn.getColumn().setText("Forbidden bundles");
    //    forbiddenBundlesTableViewerColumn.getColumn().setWidth(200);
    forbiddenBundlesTableViewerColumn.setLabelProvider(new PluginInfoColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        PluginInfo pluginInfo = (PluginInfo) element;
        String forbiddenBundles = pluginInfo.forbiddenPluginList.stream().map(forbiddenPluginInfo -> forbiddenPluginInfo.id).collect(Collectors.joining(", "));
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

    manager.addMenuListener(new IMenuListener()
    {
      @Override
      public void menuAboutToShow(IMenuManager manager)
      {
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        //
        if (!projectTableViewer.getSelection().isEmpty())
        {
          createRemoveInvalidPluginsMenuItem(manager);
        }
      }

      private void createRemoveInvalidPluginsMenuItem(IMenuManager manager)
      {
        IStructuredSelection selection = (IStructuredSelection) projectTableViewer.getSelection();
        Stream<PluginInfo> selectedPluginInfoStream = selection.toList().stream().filter(PluginInfo.class::isInstance).map(PluginInfo.class::cast);
        Set<PluginInfo> notExistPluginInfoSet = selectedPluginInfoStream.filter(pluginInfo -> !Util.getProject(pluginInfo).exists()).collect(Collectors.toSet());
        if (!notExistPluginInfoSet.isEmpty())
        {
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
    });

    projectTableViewer.getControl().setMenu(menu);
  }

  /**
   * Refresh
   */
  void refresh()
  {
    // Update projectTableViewer
    projectTableViewer.setInput(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList);

    // pack columns
    for(TableColumn tableColumn : projectTableViewer.getTable().getColumns())
      pack(tableColumn, COLUMN_PREFERRED_WIDTH);

    //
    projectDetail.refresh();
  }

  /**
   * Pack table column
   * @param tableColumn
   * @param maxWidth
   */
  static void pack(TableColumn tableColumn, int maxWidth)
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
    projectTableViewer.refresh(pluginInfo);
    pack(typeTableViewerColumn.getColumn(), COLUMN_PREFERRED_WIDTH);
    pack(forbiddenTypesTableViewerColumn.getColumn(), COLUMN_PREFERRED_WIDTH);
  }

  /**
   */
  static class PluginInfoColumnLabelProvider extends ColumnLabelProvider
  {
    @Override
    public final Color getForeground(Object element)
    {
      PluginInfo pluginInfo = (PluginInfo) element;
      IProject project = Util.getProject(pluginInfo);
      if (Util.isValidPlugin(project))
        return super.getForeground(element);
      return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    }
  }
}
