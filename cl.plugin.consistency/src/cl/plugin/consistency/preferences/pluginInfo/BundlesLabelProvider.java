package cl.plugin.consistency.preferences.pluginInfo;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

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
    String id = pluginConsistencyPreferencePage.getIdInCache(element);
    if (element instanceof IProject)
    {
      IProject project = (IProject) element;
      if (!id.equals(project.getName()))
        id += " (" + project.getName() + ")";
    }
    return id;
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof IProject)
      return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
    if (element instanceof Bundle)
    {
      Bundle bundle = (Bundle) element;
      if (bundle.getHeaders().get("Fragment-Host") != null)
        return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ);
      return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
    }
    return null;
  }

  @Override
  public Color getForeground(Object element)
  {
    if (requireBundleSet == null)
      return null;
    if (requireBundleSet.contains(getText(element)))
      return Display.getDefault().getSystemColor(SWT.COLOR_RED);
    return null;
  }

  @Override
  public Color getBackground(Object element)
  {
    return null;
  }
}
