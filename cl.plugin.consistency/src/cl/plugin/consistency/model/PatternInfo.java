package cl.plugin.consistency.model;

import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlAttribute;

import org.eclipse.ui.dialogs.SearchPattern;

/**
 * The class <b>PatternInfo</b> allows to.<br>
 */
public class PatternInfo extends AbstractData
{
  @XmlAttribute(name = "activate")
  public boolean activate = true;

  @XmlAttribute(name = "pattern", required = true)
  public String pattern;

  @XmlAttribute(name = "description")
  public String description;

  private static final String TYPE_PATTERN_SEPARATOR = "#";
  public static final String PATTERN_SEPARATOR = ";";

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
    return containsPattern + (doNotContainsPattern != null && !doNotContainsPattern.isEmpty()? TYPE_PATTERN_SEPARATOR + doNotContainsPattern : "");
  }

  public String getContainsPattern()
  {
    int index = pattern.indexOf(TYPE_PATTERN_SEPARATOR);
    String containsPattern = index >= 0? pattern.substring(0, index) : pattern;
    return containsPattern;
  }

  public String getDoNotContainsPattern()
  {
    int index = pattern.indexOf(TYPE_PATTERN_SEPARATOR);
    String doNotContainsPattern = index >= 0? pattern.substring(index + 1) : "";
    return doNotContainsPattern;
  }

  public boolean containsTypes()
  {
    if (!declaredPluginTypeList.isEmpty() || !forbiddenPluginTypeList.isEmpty())
      return true;
    return false;
  }

  public boolean acceptPlugin(String pluginId)
  {
    if (!activate)
      return false;

    //
    String containsPatterns = getContainsPattern();
    if (containsPatterns != null && !containsPatterns.isEmpty())
    {
      SearchPattern containsPatternSearchPattern = new SearchPattern();

      StringTokenizer stringTokenizer = new StringTokenizer(containsPatterns, PATTERN_SEPARATOR);
      boolean acceptPlugin = false;
      while(stringTokenizer.hasMoreTokens())
      {
        String containsPattern = stringTokenizer.nextToken();
        containsPatternSearchPattern.setPattern('*' + containsPattern);
        if (containsPatternSearchPattern.matches(pluginId))
        {
          acceptPlugin = true;
          break;
        }
      }
      if (!acceptPlugin)
        return false;
    }

    //
    String doNotContainsPatterns = getDoNotContainsPattern();
    if (doNotContainsPatterns != null && !doNotContainsPatterns.isEmpty())
    {
      SearchPattern doNotContainsPatternSearchPattern = new SearchPattern();
      StringTokenizer stringTokenizer = new StringTokenizer(doNotContainsPatterns, PATTERN_SEPARATOR);
      while(stringTokenizer.hasMoreTokens())
      {
        String doNotContainsPattern = stringTokenizer.nextToken();
        doNotContainsPatternSearchPattern.setPattern('*' + doNotContainsPattern);
        if (doNotContainsPatternSearchPattern.matches(pluginId))
          return false;
      }
    }

    return true;
  }

  /**
   * Set pattern
   *
   * @param containsPattern
   * @param doNotContainsPattern
   */
  public void setPattern(String containsPattern, String doNotContainsPattern)
  {
    pattern = doNotContainsPattern == null || doNotContainsPattern.isEmpty()? containsPattern : containsPattern + TYPE_PATTERN_SEPARATOR + doNotContainsPattern;
  }
}
