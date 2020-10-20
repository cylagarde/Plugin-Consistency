package cl.plugin.consistency.model;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;

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

  public boolean acceptPlugin(String pluginId)
  {
    if (!activate)
      return false;

    //
    String acceptPatterns = getAcceptPattern();
    StringTokenizer acceptPatternStringTokenizer = new StringTokenizer(acceptPatterns, PATTERN_SEPARATOR);
    boolean acceptPlugin = false;
    while(acceptPatternStringTokenizer.hasMoreTokens())
    {
      String acceptPattern = acceptPatternStringTokenizer.nextToken();
      if (acceptPattern.contains("*") || acceptPattern.contains("?"))
      {
        Pattern pattern = createRegexPattern(acceptPattern);
        if (pattern.matcher(pluginId).matches())
        {
          acceptPlugin = true;
          break;
        }
      }
      else if (acceptPattern.equals(pluginId))
      {
        acceptPlugin = true;
        break;
      }
    }
    if (!acceptPlugin)
      return false;

    //
    String doNotAcceptPatterns = getDoNotAcceptPattern();
    StringTokenizer doNotAcceptPatternsStringTokenizer = new StringTokenizer(doNotAcceptPatterns, PATTERN_SEPARATOR);
    while(doNotAcceptPatternsStringTokenizer.hasMoreTokens())
    {
      String doNotAcceptPattern = doNotAcceptPatternsStringTokenizer.nextToken();
      if (doNotAcceptPattern.contains("*") || doNotAcceptPattern.contains("?"))
      {
        Pattern pattern = createRegexPattern(doNotAcceptPattern);
        if (pattern.matcher(pluginId).matches())
          return false;
      }
      else if (doNotAcceptPattern.equals(pluginId))
      {
        return false;
      }
    }

    return true;
  }

  private static Pattern createRegexPattern(String text)
  {
    // Ajoute de \Q \E autour de la chaine
    String regexpPattern = Pattern.quote(text);
    // On remplace toutes les occurences de '*' afin de les interpréter
    regexpPattern = regexpPattern.replaceAll("\\*", "\\\\E.*\\\\Q");
    // On remplace toutes les occurences de '?' afin de les interpréter
    regexpPattern = regexpPattern.replaceAll("\\?", "\\\\E.\\\\Q");
    // On supprime tous les \Q \E inutiles
    regexpPattern = regexpPattern.replaceAll("\\\\Q\\\\E", "");

    //
    return Pattern.compile(regexpPattern);
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
