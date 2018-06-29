package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.eclipse.ui.dialogs.SearchPattern;

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
    String containsPattern = getContainsPattern();
    String doNotContainsPattern = getDoNotContainsPattern();
    return "PatternInfo[contains=" + containsPattern + (doNotContainsPattern != null && !doNotContainsPattern.isEmpty()? ", not contains=" + doNotContainsPattern : "") + "]";
  }

  public String getContainsPattern()
  {
    int index = pattern.indexOf(';');
    String containsPattern = index >= 0? pattern.substring(0, index) : pattern;
    return containsPattern;
  }

  public String getDoNotContainsPattern()
  {
    int index = pattern.indexOf(';');
    String doNotContainsPattern = index >= 0? pattern.substring(index + 1) : "";
    return doNotContainsPattern;
  }

  /**
   */
  public boolean containsTypes()
  {
    if (!typeList.isEmpty() || !forbiddenTypeList.isEmpty())
      return true;
    return false;
  }

  /**
   * @param pluginId
   */
  public boolean acceptPlugin(String pluginId)
  {
    SearchPattern containsPatternSearchPattern = new SearchPattern();
    containsPatternSearchPattern.setPattern('*'+getContainsPattern());

    if (containsPatternSearchPattern.matches(pluginId))
    {
      String doNotContainsPattern = getDoNotContainsPattern();
      if (doNotContainsPattern == null || doNotContainsPattern.isEmpty())
        return true;
      SearchPattern doNotContainsPatternSearchPattern = new SearchPattern();
      doNotContainsPatternSearchPattern.setPattern('*' + doNotContainsPattern);
      if (!doNotContainsPatternSearchPattern.matches(pluginId))
        return true;
    }

    return false;
  }

  /**
   * @param containsPattern
   * @param doNotContainsPatternValue
   */
  public void setPattern(String containsPattern, String doNotContainsPattern)
  {
    pattern = doNotContainsPattern == null || doNotContainsPattern.isEmpty()? containsPattern : containsPattern + ";" + doNotContainsPattern;
  }
}
