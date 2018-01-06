package cl.plugin.consistency.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

/**
 *
 */
public class PluginTabFolder
{
  final PluginConsistencyPreferencePage pluginConsistencyPreferencePage;
  final TabFolder tabFolder;
  final PluginTabItem pluginTabItem;
  final TypeTabItem typeTabItem;
  final PatternTabItem patternTabItem;

  /**
   * Constructor
   */
  public PluginTabFolder(PluginConsistencyPreferencePage pluginConsistencyPreferencePage, Composite parent, int style)
  {
    this.pluginConsistencyPreferencePage = pluginConsistencyPreferencePage;

    tabFolder = new TabFolder(parent, style);
    tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

    //
    pluginTabItem = new PluginTabItem(this);

    //
    typeTabItem = new TypeTabItem(this);

    //
    patternTabItem = new PatternTabItem(this);

    //
    tabFolder.setSelection(0);
    tabFolder.setFocus();
  }

  /**
   * Refresh
   */
  public void refresh()
  {
    pluginTabItem.refresh();
    typeTabItem.refresh();
    patternTabItem.refresh();
  }
}
