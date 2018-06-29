package cl.plugin.consistency.model;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * The class <b>Type</b> allows to.<br>
 */
public class Type
{
  @XmlAttribute(name = "name", required = true)
  public String name;

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null)? 0 : name.hashCode());
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
    Type other = (Type) obj;
    if (name == null)
    {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    return true;
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return "Type[name=" + name + "]";
  }
}
