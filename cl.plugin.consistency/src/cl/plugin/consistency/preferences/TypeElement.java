package cl.plugin.consistency.preferences;

import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.impl.IElement;

public class TypeElement implements IElement
{
  public final Type type;

  public TypeElement(Type type)
  {
    this.type = type;
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IElement#getName()
   */
  @Override
  public String getName()
  {
    return type.name;
  }

  /*
   * @see cl.plugin.consistency.preferences.impl.IElement#setName(java.lang.String)
   */
  @Override
  public void setName(String name)
  {
    type.name = name;
  }
}
