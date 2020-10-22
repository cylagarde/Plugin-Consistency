package cl.plugin.consistency.preferences;

import org.eclipse.jface.viewers.ITreeContentProvider;

/**
     * The class <b>ArrayTreeContentProvider</b> allows to.<br>
     */
public class ArrayTreeContentProvider implements ITreeContentProvider
{

  @Override
  public Object[] getElements(Object inputElement)
  {
    return (Object[]) inputElement;
  }

  @Override
  public Object[] getChildren(Object parentElement)
  {
    return null;
  }

  @Override
  public Object getParent(Object element)
  {
    return null;
  }

  @Override
  public boolean hasChildren(Object element)
  {
    return false;
  }
}