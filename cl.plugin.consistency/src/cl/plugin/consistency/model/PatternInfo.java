package cl.plugin.consistency.model;

import javax.xml.bind.annotation.XmlAttribute;

import cl.plugin.consistency.PatternPredicate;

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
    String acceptPattern = getAcceptPattern();
    String doNotAcceptPattern = getDoNotAcceptPattern();
    return "PatternInfo[activate=" + activate + ", accept=" + acceptPattern + (!doNotAcceptPattern.isEmpty()? ", not accept=" + doNotAcceptPattern : "") + "]";
  }

  public String getAcceptAndNotAcceptPattern()
  {
    String containsPattern = getAcceptPattern();
    String doNotContainsPattern = getDoNotAcceptPattern();
    return containsPattern + (!doNotContainsPattern.isEmpty()? TYPE_PATTERN_SEPARATOR + doNotContainsPattern : "");
  }

  public String getAcceptPattern()
  {
    int index = pattern.indexOf(TYPE_PATTERN_SEPARATOR);
    String acceptPattern = index >= 0? pattern.substring(0, index) : pattern;
    return acceptPattern;
  }

  public String getDoNotAcceptPattern()
  {
    int index = pattern.indexOf(TYPE_PATTERN_SEPARATOR);
    String doNotAcceptPattern = index >= 0? pattern.substring(index + 1) : "";
    return doNotAcceptPattern;
  }

  public boolean containsTypes()
  {
    if (!declaredPluginTypeList.isEmpty() || !forbiddenPluginTypeList.isEmpty())
      return true;
    return false;
  }

  /**
   * Check if plugin id verifies pattern
   * @param pluginId
   */
  public boolean acceptPlugin(String pluginId)
  {
    if (!activate)
      return false;

    //
    String acceptPatterns = getAcceptPattern();
    PatternPredicate acceptPatternPredicate = new PatternPredicate(acceptPatterns, PATTERN_SEPARATOR, false);
    if (!acceptPatternPredicate.test(pluginId))
      return false;

    //
    String doNotAcceptPatterns = getDoNotAcceptPattern();
    PatternPredicate doNotAcceptPatternPredicate = new PatternPredicate(doNotAcceptPatterns, PATTERN_SEPARATOR, false);
    if (doNotAcceptPatternPredicate.test(pluginId))
      return false;

    return true;
  }

  /**
   * Set pattern
   *
   * @param acceptPattern
   * @param doNotAcceptPattern
   */
  public void setPattern(String acceptPattern, String doNotAcceptPattern)
  {
    pattern = doNotAcceptPattern == null || doNotAcceptPattern.isEmpty()? acceptPattern : acceptPattern + TYPE_PATTERN_SEPARATOR + doNotAcceptPattern;
  }
}
