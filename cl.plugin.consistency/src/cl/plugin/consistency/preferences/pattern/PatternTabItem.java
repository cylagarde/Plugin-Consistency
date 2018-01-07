package cl.plugin.consistency.preferences.pattern;

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
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import cl.plugin.consistency.Util;
import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.preferences.DefaultLabelViewerComparator;
import cl.plugin.consistency.preferences.PluginTabFolder;
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
    PatternTypeComposite patternTypeComposite = new PatternTypeComposite(this, scrolledComposite, SWT.NONE);
    scrolledComposite.setContent(patternTypeComposite.section);
    scrolledComposite.setMinSize(scrolledComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));

    // selection
    patternTableViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) patternTableViewer.getSelection();
      PatternInfo patternInfo = (PatternInfo) selection.getFirstElement();
      patternTypeComposite.setPatternInfo(patternInfo);
    });

    sashForm.setWeights(new int[]{2, 1});
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
        String txt = patternInfo.typeList.stream().map(type -> type.name).collect(Collectors.joining(", "));
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
        String txt = patternInfo.forbiddenTypeList.stream().map(type -> type.name).collect(Collectors.joining(", "));
        return txt;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(forbiddenTypeTableViewerColumn);

    //
    configurePopupMenuForTypeTableViewer();

    refresh();
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
        createRemovePatternsMenuItem(manager);
      }
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
  public void refreshPatternInfo(PatternInfo patternInfo)
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
