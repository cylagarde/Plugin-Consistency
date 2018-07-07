package cl.plugin.consistency.preferences;

import cl.plugin.consistency.model.Type;
import cl.plugin.consistency.preferences.impl.IElement;

public class TypeElement implements IElement
{
  public final Type type;
  public final boolean isPatternType;

  public TypeElement(Type type, boolean isPatternType)
  {
    this.type = type;
    this.isPatternType = isPatternType;
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

  /*
   * @see cl.plugin.consistency.preferences.impl.IElement#isPatternType()
   */
  @Override
  public boolean isPatternType()
  {
    return isPatternType;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null)? 0 : type.hashCode());
    return result;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TypeElement other = (TypeElement) obj;
    if (type == null)
    {
      if (other.type != null)
        return false;
    }
    else if (!type.equals(other.type))
      return false;
    return true;
  }
}
