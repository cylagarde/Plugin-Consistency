package cl.plugin.consistency;

import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The class <b>PatternPredicate</b> allows to predicate text with multiple regexp ('*' and '?' are supported).<br>
 * PatternPredicate patternPredicate = new PatternPredicate("*.java ; *.class", ";", false);
 * patternPredicate.test("file.java") -> true
 * patternPredicate.test("file.class") -> true
 */
public class PatternPredicate implements Predicate<String>
{
  final String patterns;
  Predicate<String> delegatePredicate = txt -> false;

  /**
   * Constructor
   * @param patterns
   * @param patternSeparator
   * @param andPredicate
   */
  public PatternPredicate(String patterns, String patternSeparator, boolean andPredicate)
  {
    this.patterns = patterns;

    StringTokenizer patternsStringTokenizer = new StringTokenizer(patterns, patternSeparator);
    while(patternsStringTokenizer.hasMoreTokens())
    {
      Predicate<String> predicate;
      String text = patternsStringTokenizer.nextToken().trim();
      if (text.contains("*") || text.contains("?"))
      {
        Pattern pattern = createRegexPattern(text);
        predicate = txt -> pattern.matcher(txt).matches();
      }
      else
      {
        predicate = text::equals;
      }

      delegatePredicate = delegatePredicate == null? predicate : andPredicate? delegatePredicate.and(predicate) : delegatePredicate.or(predicate);
    }
  }

  public String getPatterns()
  {
    return patterns;
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

  @Override
  public boolean test(String txt)
  {
    return delegatePredicate.test(txt);
  }
}
