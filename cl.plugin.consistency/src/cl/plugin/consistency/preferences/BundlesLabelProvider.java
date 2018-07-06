package cl.plugin.consistency.preferences;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.osgi.framework.Bundle;

import cl.plugin.consistency.Cache;

/**
 * The class <b>BundlesLabelProvider</b> allows to.<br>
 */
public class BundlesLabelProvider extends LabelProvider implements IColorProvider, IStyledLabelProvider
{
  final Cache cache;
  final Set<String> requireBundleSet;
  final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

  public BundlesLabelProvider(Cache cache, Set<String> requireBundleSet)
  {
    this.cache = cache;
    this.requireBundleSet = requireBundleSet;
  }

  public BundlesLabelProvider(Cache cache)
  {
    this(cache, null);
  }

  @Override
  public String getText(Object element)
  {
    return getStyledText(element).getString();
  }

  @Override
  public StyledString getStyledText(Object element)
  {
    StyledString styledString = new StyledString();
    String id = cache.getId(element);
    styledString.append(id);

    if (element instanceof IProject)
    {
      IProject project = (IProject) element;
      if (!id.equals(project.getName()))
        styledString.append(" (" + project.getName() + ")", StyledString.DECORATIONS_STYLER);
    }

    return styledString;
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof IProject)
      return workbenchLabelProvider.getImage(element);
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
