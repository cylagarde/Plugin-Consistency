package cl.plugin.consistency.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The class <b>OpenPreferenceHandler</b> allows to open preference for plugin consistency.<br>
 */
public class OpenPreferenceHandler extends AbstractHandler
{
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    Shell shell = HandlerUtil.getActiveShell(event);

    String preferencePageId = "cl.plugin.consistency.preferences.PluginConsistencyPreferencePage";
    PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(shell, preferencePageId, null, null);
    if (preferenceDialog != null)
      preferenceDialog.open();

    return null;
  }
}
