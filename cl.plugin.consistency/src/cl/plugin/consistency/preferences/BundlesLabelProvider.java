package cl.plugin.consistency.preferences;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;

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
    Styler styler = getStylerForPluginId(id);
    styledString.append(id, styler);

    if (element instanceof IProject)
    {
      IProject project = (IProject) element;
      if (!id.equals(project.getName()))
        styledString.append(" (" + project.getName() + ")", StyledString.DECORATIONS_STYLER);
    }

    return styledString;
  }

  protected Styler getStylerForPluginId(String pluginId)
  {
    return null;
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof IProject)
      return workbenchLabelProvider.getImage(element);
    if (element instanceof IBundlePluginModelBase)
    {
      // find project
      Optional<IProject> optional = cache.getValidProjects()
        .filter(project -> cache.getId(project).equals(cache.getId(element)))
        .findAny();
      if (optional.isPresent())
        return workbenchLabelProvider.getImage(optional.get());
    }
    return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
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
