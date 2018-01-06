package cl.plugin.consistency.preferences;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import cl.plugin.consistency.model.Type;

/**
 *
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
        Set<String> alreadyExistTypeset = pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.stream().map(type -> type.name).collect(Collectors.toSet());

        IInputValidator validator = new IInputValidator()
        {
          @Override
          public String isValid(String newText)
          {
            if (newText.isEmpty())
              return "Value is empty";
            if (alreadyExistTypeset.contains(newText))
              return "The type already exists";
            return null;
          }
        };
        InputDialog inputDialog = new InputDialog(parent.getShell(), "Add new type", "Enter a value for new type", "", validator);
        if (inputDialog.open() == InputDialog.OK)
        {
          String newTypeName = inputDialog.getValue();
          Type type = new Type();
          type.name = newTypeName;
          pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList.add(type);

          refresh();
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
    typeTableViewer = new TableViewer(parent);
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

    refresh();
  }

  void refresh()
  {
    // Update typeTableViewer
    typeTableViewer.setInput(pluginTabFolder.pluginConsistencyPreferencePage.pluginConsistency.typeList);

    // pack columns
    for(TableColumn tableColumn : typeTableViewer.getTable().getColumns())
      PluginTabItem.pack(tableColumn, PluginTabItem.COLUMN_PREFERRED_WIDTH);
  }
}
