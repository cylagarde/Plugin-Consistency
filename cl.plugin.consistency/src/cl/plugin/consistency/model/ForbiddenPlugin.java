package cl.plugin.consistency.model;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * The class <b>ForbiddenPluginInfo</b> define some forbidden plugin informations.<br>
 */
public class ForbiddenPlugin
{
  @XmlAttribute(name = "id", required = true)
  public String id;

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (id == null? 0 : id.hashCode());
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
    ForbiddenPlugin other = (ForbiddenPlugin) obj;
    if (id == null)
    {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    return true;
  }

  @Override
  public String toString()
  {
    return "ForbiddenPlugin[id=" + id + "]";
  }
}
