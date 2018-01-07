package cl.plugin.consistency.preferences;

import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import cl.plugin.consistency.model.PatternInfo;

/**
 * The class <b>PatternTabItem</b> allows to.<br>
 */
public class PatternTabItem
{
  final PluginTabFolder pluginTabFolder;
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
    configurePatternTableViewer(patternTabComposite);
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
    addPatternButton.setEnabled(false);
    addPatternButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        //        Set<String> alreadyExistTypeset = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> type.name).collect(Collectors.toSet());
        //
        //        IInputValidator validator = newText -> {
        //          if (newText.isEmpty())
        //            return "Value is empty";
        //          if (alreadyExistTypeset.contains(newText))
        //            return "The type already exists";
        //          return null;
        //        };
        //        InputDialog inputDialog = new InputDialog(parent.getShell(), "Add new type", "Enter a value for new type", "", validator);
        //        if (inputDialog.open() == InputDialog.OK)
        //        {
        //          String newTypeName = inputDialog.getValue();
        //          Type type = new Type();
        //          type.name = newTypeName;
        //          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.add(type);
        //
        //          // refresh all TabFolder
        //          pluginTabFolder.refresh();
        //        }
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
  void refresh()
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
    }
  }
}
