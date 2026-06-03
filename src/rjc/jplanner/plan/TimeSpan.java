/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.plan;

/*************************************************************************************************/
/********************************** Quantity of time with units **********************************/
/*************************************************************************************************/

public class TimeSpan
{
  private double m_num;  // number of unit periods (max 2 decimal places, except only whole seconds)
  private Unit   m_unit; // type of periods (first letter shown in string version)

  public enum Unit // duration units with their single-character abbreviations
  {
    SECONDS( 'S' ), MINUTES( 'M' ), HOURS( 'H' ), DAYS( 'd' ), WEEKS( 'w' ), MONTHS( 'm' ), YEARS( 'y' );

    private final char abbreviation;

    Unit( char letter )
    {
      abbreviation = letter;
    }

    public char abbreviation()
    {
      return abbreviation;
    }

    public static Unit fromChar( char ch )
    {
      for ( Unit unit : values() )
        if ( unit.abbreviation == ch )
          return unit;
      return null;
    }
  }

  /**************************************** constructor ******************************************/
  public TimeSpan()
  {
    // construct default time-span (one day)
    m_num = 1.0;
    m_unit = Unit.DAYS;
  }

  /**************************************** constructor ******************************************/
  public TimeSpan( String str )
  {
    // construct time-span from string, start with default
    this();

    // if string is null, don't do anything more
    if ( str == null )
      return;

    // if string with white-spaces removed is zero length, don't do anything more
    str = str.replaceAll( "\\s+", "" );
    if ( str.isEmpty() )
      return;

    // set units if last character matches units first character
    char lastchr = str.charAt( str.length() - 1 );
    Unit units = Unit.fromChar( lastchr );
    if ( units != null )
    {
      m_unit = units;
      str = str.substring( 0, str.length() - 1 );
    }

    // set number from remainder of string but THROWS EXCEPTION IF PARSING FAILS
    double num = Double.parseDouble( "0" + str );
    if ( m_unit == Unit.SECONDS )
      m_num = Math.rint( num );
    else
      m_num = Math.rint( num * 100.0 ) / 100.0;
  }

  /**************************************** constructor ******************************************/
  public TimeSpan( double num, Unit unit )
  {
    // construct time-span from parameters, rounding number based on units
    m_unit = unit;
    if ( unit == Unit.SECONDS )
      m_num = Math.rint( num );
    else
      m_num = Math.rint( num * 100.0 ) / 100.0;
  }

  /**************************************** toStringLong *****************************************/
  public String toStringLong()
  {
    // return string version of time-span, e.g. "1 Month" or "6 Seconds"
    var units = m_unit.name().charAt( 0 ) + m_unit.name().substring( 1 ).toLowerCase();
    if ( m_num == 1.0 )
      units = units.substring( 0, units.length() - 1 ); // singular form if number is 1
    return numberString() + " " + units;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return short string version of time-span, e.g. "1.23 d" or "1 S"
    return numberString() + " " + m_unit.abbreviation();
  }

  private String numberString()
  {
    // return number as string, rounded to 2 decimal places except for seconds
    if ( m_unit == Unit.SECONDS )
      return Integer.toString( (int) m_num ); // no decimal places for seconds

    // when not seconds, convert to fixed-point integer (e.g. 1.23 -> 123)
    int fixed = (int) Math.round( m_num * 100 );
    int whole = fixed / 100;
    int frac = fixed % 100;

    // build string manually
    StringBuilder sb = new StringBuilder( 8 );
    sb.append( whole );

    if ( frac != 0 )
    {
      sb.append( '.' );
      sb.append( frac / 10 );
      if ( frac % 10 != 0 )
        sb.append( frac % 10 );
    }

    return sb.toString();
  }

  /******************************************* getUnit *******************************************/
  public Unit getUnit()
  {
    // return time-span units
    return m_unit;
  }

  /******************************************* setUnit *******************************************/
  public void setUnit( Unit units )
  {
    // set time-span units
    m_unit = units;
    if ( units == Unit.SECONDS )
      m_num = Math.rint( m_num );
  }

  /****************************************** getNumber ******************************************/
  public double getNumber()
  {
    // return time-span number
    return m_num;
  }

  /******************************************* equals ********************************************/
  @Override
  public boolean equals( Object other )
  {
    // return true if this time-span and other time-span are same
    if ( other instanceof TimeSpan ts )
      return m_unit == ts.m_unit && Math.abs( m_num - ts.m_num ) < 0.001;

    return false;
  }

}
