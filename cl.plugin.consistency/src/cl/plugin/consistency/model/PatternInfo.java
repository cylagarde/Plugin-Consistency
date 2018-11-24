package cl.plugin.consistency.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.eclipse.ui.dialogs.SearchPattern;

/**
 * The class <b>PatternInfo</b> allows to.<br>
 */
public class PatternInfo
{
  @XmlAttribute(name = "activate")
  public boolean activate = true;

  @XmlAttribute(name = "pattern", required = true)
  public String pattern;

  @XmlAttribute(name = "description")
  public String description;

  @XmlElementWrapper(name = "Types")
  @XmlElement(name = "Type")
  public List<Type> typeList = new ArrayList<>();

  @XmlElementWrapper(name = "ForbiddenTypes")
  @XmlElement(name = "Type")
  public List<Type> forbiddenTypeList = new ArrayList<>();

  private static final String SEPARATOR = ";";

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    String containsPattern = getContainsPattern();
    String doNotContainsPattern = getDoNotContainsPattern();
    return "PatternInfo[activate=" + activate + ", contains=" + containsPattern + (doNotContainsPattern != null && !doNotContainsPattern.isEmpty()? ", not contains=" + doNotContainsPattern : "") + "]";
  }

  public String getContainsAndNotContainsPattern()
  {
    String containsPattern = getContainsPattern();
    String doNotContainsPattern = getDoNotContainsPattern();
    return containsPattern + (doNotContainsPattern != null && !doNotContainsPattern.isEmpty()? SEPARATOR + doNotContainsPattern : "");
  }

  public String getContainsPattern()
  {
    int index = pattern.indexOf(SEPARATOR);
    String containsPattern = index >= 0? pattern.substring(0, index) : pattern;
    return containsPattern;
  }

  public String getDoNotContainsPattern()
  {
    int index = pattern.indexOf(SEPARATOR);
    String doNotContainsPattern = index >= 0? pattern.substring(index + 1) : "";
    return doNotContainsPattern;
  }

  public String forToolTip()
  {
    String containsPattern = getContainsPattern();
    String doNotContainsPattern = getDoNotContainsPattern();
    return "pattern[contains=" + containsPattern + (doNotContainsPattern != null && !doNotContainsPattern.isEmpty()? ", not contains=" + doNotContainsPattern : "") + "]";
  }

  public boolean containsTypes()
  {
    if (!typeList.isEmpty() || !forbiddenTypeList.isEmpty())
      return true;
    return false;
  }

  public boolean acceptPlugin(String pluginId)
  {
    if (!activate)
      return false;

    SearchPattern containsPatternSearchPattern = new SearchPattern();
    containsPatternSearchPattern.setPattern('*'+getContainsPattern());

    if (containsPatternSearchPattern.matches(pluginId))
    {
      String doNotContainsPatterns = getDoNotContainsPattern();
      if (doNotContainsPatterns == null || doNotContainsPatterns.isEmpty())
        return true;
      SearchPattern doNotContainsPatternSearchPattern = new SearchPattern();
      StringTokenizer stringTokenizer = new StringTokenizer(doNotContainsPatterns, ";");
      while (stringTokenizer.hasMoreTokens()) {
        String doNotContainsPattern = stringTokenizer.nextToken();
        doNotContainsPatternSearchPattern.setPattern('*' + doNotContainsPattern);
        if (doNotContainsPatternSearchPattern.matches(pluginId))
          return false;
      }
      return true;
    }

    return false;
  }

  /**
   * Set pattern
   *
   * @param containsPattern
   * @param doNotContainsPatternValue
   */
  public void setPattern(String containsPattern, String doNotContainsPattern)
  {
    pattern = doNotContainsPattern == null || doNotContainsPattern.isEmpty()? containsPattern : containsPattern + SEPARATOR + doNotContainsPattern;
  }
}
