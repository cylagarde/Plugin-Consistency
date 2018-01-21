package cl.plugin.consistency;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

/**
 * The enum <b>Images</b> allows to.<br>
 */
public enum Images {

  PLUGIN("icons/plugin.gif"),
  CLEAR(PlatformUI.PLUGIN_ID, "$nl$/icons/full/etool16/clear_co.png"),
  ;

  public final String pluginId;
  public final String path;

  private Images(String path) {
    this(PluginConsistencyActivator.PLUGIN_ID, path);
  }

  private Images(String pluginId, String path) {
    this.pluginId = pluginId;
    this.path = path;
  }

  public Image getImage() {
    return PluginConsistencyActivator.getDefault().getImageRegistry().get(getKey());
  }

  String getKey() {
    return pluginId+":"+path;
  }
}