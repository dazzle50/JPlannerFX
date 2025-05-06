/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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
  private double m_num;   // number of unit periods (max 2 decimal places, except only whole seconds)
  private UNITS  m_units; // type of periods (first letter shown in string version)

  public enum UNITS
  {
    SECONDS, MINUTES, HOURS, days, weeks, months, years
  }

  /**************************************** constructor ******************************************/
  public TimeSpan()
  {
    // construct default time-span (one day)
    m_num = 1;
    m_units = UNITS.days;
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
    if ( str.length() == 0 )
      return;

    // set units if last character matches units first character
    char lastchr = str.charAt( str.length() - 1 );
    for ( int index = 0; index < UNITS.values().length; index++ )
      if ( UNITS.values()[index].name().charAt( 0 ) == lastchr )
      {
        // found units first character that matches
        m_units = UNITS.values()[index];
        str = str.substring( 0, str.length() - 1 );
        break;
      }

    // set number from remainder of string but THROWS EXCEPTION IF PARSING FAILS
    double num = Double.parseDouble( str );
    if ( m_units == UNITS.SECONDS )
      m_num = Math.rint( num );
    else
      m_num = Math.rint( num * 100.0 ) / 100.0;
  }

  /**************************************** constructor ******************************************/
  public TimeSpan( double num, UNITS units )
  {
    // construct time-span from parameters, rounding number based on units
    m_units = units;
    if ( units == UNITS.SECONDS )
      m_num = Math.rint( num );
    else
      m_num = Math.rint( num * 100.0 ) / 100.0;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // if units are seconds, show integer number and 'S' (first letter of SECONDS)
    if ( m_units == UNITS.SECONDS )
      return String.format( "%.0f", m_num ) + " S";

    // otherwise, show number to 2 decimal places with trailing zeros/dot removed, and first letter of units
    String str = String.format( "%.2f", m_num );
    if ( str.contains( "." ) )
    {
      int index = str.length() - 1;
      while ( str.charAt( index ) == '0' )
        index--;
      if ( str.charAt( index ) == '.' )
        index--;
      str = str.substring( 0, index + 1 );
    }
    return str + " " + m_units.toString().charAt( 0 );
  }

  /****************************************** getUnits *******************************************/
  public UNITS getUnits()
  {
    // return time-span units
    return m_units;
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
      return m_units == ts.m_units && Math.abs( m_num - ts.m_num ) < 0.001;

    return false;
  }
}
