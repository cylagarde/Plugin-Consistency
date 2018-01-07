package cl.plugin.consistency.preferences.type;

import java.util.Set;
import java.util.function.Consumer;
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

import cl.plugin.consistency.model.PatternInfo;
import cl.plugin.consistency.model.PluginInfo;
import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.DefaultLabelViewerComparator;
import cl.plugin.consistency.preferences.PluginTabFolder;
import cl.plugin.consistency.preferences.pluginInfo.PluginTabItem;

/**
 * The class <b>TypeTabItem</b> allows to.<br>
 */
public class TypeTabItem
{
  final PluginTabFolder pluginTabFolder;
  TableViewer typeTableViewer;

  /**
   * Constructor
   */
  public TypeTabItem(PluginTabFolder pluginTabFolder)
  {
    this.pluginTabFolder = pluginTabFolder;

    //
    TabItem typeTabItem = new TabItem(pluginTabFolder.tabFolder, SWT.NONE);
    typeTabItem.setText("Types");

    //
    Composite typeTabComposite = new Composite(pluginTabFolder.tabFolder, SWT.NONE);
    typeTabItem.setControl(typeTabComposite);

    GridLayout typeTabCompositeLayout = new GridLayout(1, false);
    typeTabCompositeLayout.marginWidth = typeTabCompositeLayout.marginHeight = 0;
    typeTabComposite.setLayout(typeTabCompositeLayout);

    //
    configurateToobar(typeTabComposite);

    //
    configureTypeTableViewer(typeTabComposite);
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

    Button addTypeButton = new Button(toolbarComposite, SWT.FLAT);
    addTypeButton.setToolTipText("Add new type");
    addTypeButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD));
    addTypeButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        Set<String> alreadyExistTypeSet = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> type.name).collect(Collectors.toSet());

        IInputValidator validator = newText -> {
          if (newText.isEmpty())
            return "Value is empty";
          if (alreadyExistTypeSet.contains(newText))
            return "The type already exists";
          return null;
        };
        InputDialog inputDialog = new InputDialog(parent.getShell(), "Add new type", "Enter a value for new type", "", validator);
        if (inputDialog.open() == InputDialog.OK)
        {
          String newTypeName = inputDialog.getValue();
          Type type = new Type();
          type.name = newTypeName;
          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.add(type);

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
  private void configureTypeTableViewer(Composite parent)
  {
    typeTableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
    typeTableViewer.getTable().setLayout(new TableLayout());
    typeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
    typeTableViewer.getTable().setHeaderVisible(true);
    typeTableViewer.getTable().setLinesVisible(true);
    typeTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
    typeTableViewer.setComparator(new DefaultLabelViewerComparator());

    // 'Name' TableViewerColumn
    TableViewerColumn nameTableViewerColumn = new TableViewerColumn(typeTableViewer, SWT.NONE);
    nameTableViewerColumn.getColumn().setText("Name");
    nameTableViewerColumn.getColumn().setWidth(PluginTabItem.COLUMN_PREFERRED_WIDTH);
    nameTableViewerColumn.getColumn().setData(PluginTabItem.COLUMN_SPACE_KEY, PluginTabItem.COLUMN_SPACE);

    nameTableViewerColumn.setLabelProvider(new ColumnLabelProvider()
    {
      @Override
      public String getText(Object element)
      {
        Type type = (Type) element;
        return type.name;
      }
    });
    DefaultLabelViewerComparator.configureForSortingColumn(nameTableViewerColumn);

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
    Menu menu = manager.createContextMenu(typeTableViewer.getControl());
    typeTableViewer.getControl().setMenu(menu);

    manager.addMenuListener(new TypeMenuListener());
  }

  /**
   * Refresh
   */
  public void refresh()
  {
    try
    {
      typeTableViewer.getTable().setRedraw(false);

      // Update typeTableViewer
      typeTableViewer.setInput(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList);

      // pack columns
      for(TableColumn tableColumn : typeTableViewer.getTable().getColumns())
        PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);
    }
    finally
    {
      typeTableViewer.getTable().setRedraw(true);
    }
  }

  /**
   * The class <b>TypeMenuListener</b> allows to.<br>
   */
  class TypeMenuListener implements IMenuListener
  {
    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
      manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      //
      if (!typeTableViewer.getSelection().isEmpty())
      {
        createRemoveTypesMenuItem(manager);
      }
    }

    /**
     */
    private void createRemoveTypesMenuItem(IMenuManager manager)
    {
      manager.add(new Action("Remove selected types")
      {
        @Override
        public void run()
        {
          IStructuredSelection selection = (IStructuredSelection) typeTableViewer.getSelection();
          Stream<Type> selectedTypeStream = selection.toList().stream().filter(Type.class::isInstance).map(Type.class::cast);
          Set<Type> selectedTypeSet = selectedTypeStream.collect(Collectors.toSet());
          Set<String> selectedTypeNameSet = selectedTypeSet.stream().map(type -> type.name).collect(Collectors.toSet());
          String selectedTypeNames = selectedTypeNameSet.stream().collect(Collectors.joining(", "));

          Shell shell = typeTableViewer.getControl().getShell();
          String message = "Do you want to remove the selected types\n" + selectedTypeNames + " ?";
          boolean result = MessageDialog.openConfirm(shell, "Confirm", message);
          if (result)
          {
            // remove types
            pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.removeIf(selectedTypeSet::contains);

            // remove types in plugin infos
            Consumer<PluginInfo> removeTypeInPluginInfoConsumer = pluginInfo -> {
              pluginInfo.typeList.removeIf(type -> selectedTypeNameSet.contains(type.name));
              pluginInfo.forbiddenTypeList.removeIf(type -> selectedTypeNameSet.contains(type.name));
            };
            pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.pluginInfoList.forEach(removeTypeInPluginInfoConsumer);

            // remove types in pattern infos
            Consumer<PatternInfo> removeTypeInPatternInfoConsumer = patternInfo -> {
              patternInfo.typeList.removeIf(type -> selectedTypeNameSet.contains(type.name));
              patternInfo.forbiddenTypeList.removeIf(type -> selectedTypeNameSet.contains(type.name));
            };
            pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.patternList.forEach(removeTypeInPatternInfoConsumer);

            // refresh all TabFolder
            pluginTabFolder.refresh();
          }
        }
      });
    }
  }
}