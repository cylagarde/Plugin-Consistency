package cl.plugin.consistency.model;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * The class <b>PatternInfo</b> allows to.<br>
 */
public class PatternInfo
{
  @XmlAttribute(name = "pattern", required = true)
  public String pattern;

  @XmlAttribute(name = "typeReference")
  public String typeReference;

  @XmlAttribute(name = "forbiddenTypeReference")
  public String forbiddenTypeReference;

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return "Pattern[pattern=" + pattern + "]";
  }
}
