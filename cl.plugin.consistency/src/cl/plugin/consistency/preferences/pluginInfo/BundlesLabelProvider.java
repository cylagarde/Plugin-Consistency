package cl.plugin.consistency.preferences.pluginInfo;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import cl.plugin.consistency.Images;
import cl.plugin.consistency.preferences.PluginConsistencyPreferencePage;

/**
 * The class <b>BundlesLabelProvider</b> allows to.<br>
 */
class BundlesLabelProvider extends LabelProvider implements IColorProvider
{
  final PluginConsistencyPreferencePage pluginConsistencyPreferencePage;
  final Set<String> requireBundleSet;

  BundlesLabelProvider(PluginConsistencyPreferencePage pluginConsistencyPreferencePage, Set<String> requireBundleSet)
  {
    this.pluginConsistencyPreferencePage = pluginConsistencyPreferencePage;
    this.requireBundleSet = requireBundleSet;
  }

  BundlesLabelProvider(PluginConsistencyPreferencePage pluginConsistencyPreferencePage)
  {
    this(pluginConsistencyPreferencePage, null);
  }

  @Override
  public String getText(Object element)
  {
    return pluginConsistencyPreferencePage.getIdInCache(element);
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof IProject)
    {
      return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
    }
    return Images.PLUGIN.getImage();
  }

  /*
   * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
   */
  @Override
  public Color getForeground(Object element)
  {
    if (requireBundleSet == null)
    {
      return null;
    }
    if (requireBundleSet.contains(getText(element)))
    {
      return Display.getDefault().getSystemColor(SWT.COLOR_RED);
    }
    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
   */
  @Override
  public Color getBackground(Object element)
  {
    return null;
  }
}
