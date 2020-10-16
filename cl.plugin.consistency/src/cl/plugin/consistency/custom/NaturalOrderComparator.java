package cl.plugin.consistency.custom;

import java.util.Comparator;

/**
 * The class <b>NaturalOrderComparator</b> allows to.<br>
 */
public class NaturalOrderComparator implements Comparator<String>
{
  public static final NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

  private NaturalOrderComparator()
  {
  }

  static int compareRight(String a, String b)
  {
    int bias = 0, ia = 0, ib = 0;

    // The longest run of digits wins. That aside, the greatest
    // value wins, but we can't know that it will until we've scanned
    // both numbers to know that they have the same magnitude, so we
    // remember it in BIAS.
    for(;; ia++, ib++)
    {
      char ca = charAt(a, ia);
      char cb = charAt(b, ib);

      if (!Character.isDigit(ca) && !Character.isDigit(cb))
        return bias;
      if (!Character.isDigit(ca))
        return -1;
      if (!Character.isDigit(cb))
        return +1;
      if (ca == 0 && cb == 0)
        return bias;

      if (bias == 0)
      {
        if (ca < cb)
          bias = -1;
        else if (ca > cb)
          bias = +1;
      }
    }
  }

  @Override
  public int compare(String a, String b)
  {
    int ia = 0, ib = 0;
    int nza = 0, nzb = 0;
    char ca, cb;

    while(true)
    {
      // Only count the number of zeroes leading the last number compared
      nza = nzb = 0;

      ca = charAt(a, ia);
      cb = charAt(b, ib);

      // skip over leading spaces or zeros
      while(Character.isSpaceChar(ca) || ca == '0')
      {
        if (ca == '0')
          nza++;
        else
        {
          // Only count consecutive zeroes
          nza = 0;
        }

        ca = charAt(a, ++ia);
      }

      while(Character.isSpaceChar(cb) || cb == '0')
      {
        if (cb == '0')
          nzb++;
        else
        {
          // Only count consecutive zeroes
          nzb = 0;
        }

        cb = charAt(b, ++ib);
      }

      // Process run of digits
      if (Character.isDigit(ca) && Character.isDigit(cb))
      {
        int bias = compareRight(a.substring(ia), b.substring(ib));
        if (bias != 0)
          return bias;
      }

      if (ca == 0 && cb == 0)
      {
        // The strings compare the same. Perhaps the caller
        // will want to call strcmp to break the tie.
        return nza - nzb;
      }
      if (ca < cb)
        return -1;
      if (ca > cb)
        return +1;

      ++ia;
      ++ib;
    }
  }

  static char charAt(String s, int i)
  {
    return i >= s.length()? 0 : s.charAt(i);
  }
}