package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * The class <b>PatternInfo</b> allows to.<br>
 */
public class PatternInfo
{
  @XmlAttribute(name = "pattern", required = true)
  public String pattern;

  @XmlElementWrapper(name = "Types")
  @XmlElement(name = "Type")
  public List<Type> typeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenTypes")
  @XmlElement(name = "Type")
  public List<Type> forbiddenTypeList = new ArrayList<>();

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return "Pattern[pattern=" + pattern + "]";
  }

  /**
   */
  public boolean isModified()
  {
    if (!typeList.isEmpty() || !forbiddenTypeList.isEmpty())
      return true;
    return false;
  }
}
