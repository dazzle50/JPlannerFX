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

import java.util.ArrayList;

import rjc.table.Utils;

/*************************************************************************************************/
/***************************** Single day type as used by calendars ******************************/
/*************************************************************************************************/

public class Day
{
  private String                   m_name;    // name of day type
  private double                   m_work;    // equivalent days worked (typically 1.0 or 0.0)
  private ArrayList<DayWorkPeriod> m_periods; // list of work periods

  private int                      m_workMS;  // pre-calculated number of worked milliseconds in day-type

  /**************************************** constructor ******************************************/
  public Day()
  {
    // construct empty but usable day type
    m_name = "Null";
    m_work = 0.0;
    m_periods = new ArrayList<>();
    m_workMS = 0;
  }

  /**************************************** constructor ******************************************/
  public Day( String name, double work, double... periods )
  {
    // construct specified day type
    if ( name == null )
      throw new NullPointerException( "Name must not be null" );
    m_name = name;

    if ( work < 0.0 )
      throw new IllegalArgumentException( "Work must not be negative (" + work + ")" );
    m_work = work;

    if ( periods.length % 2 == 1 )
      throw new IllegalArgumentException( "Period times must be in pairs (" + periods.length + ")" );

    int count = periods.length / 2;
    double last = -0.0001;
    m_periods = new ArrayList<>( count );
    for ( int p = 0; p < count; p++ )
    {
      double start = periods[p * 2];
      double end = periods[p * 2 + 1];
      if ( start <= last || end <= start )
        throw new IllegalArgumentException(
            "Period times must be in assending order " + Utils.objectsString( periods ) );

      m_periods.add( new DayWorkPeriod( start, end ) );
      last = end;
    }
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return Utils.name( this ) + "[" + m_name + ", " + m_work + ", " + m_periods + "]";
  }

  /******************************************* getName *******************************************/
  public String getName()
  {
    return m_name;
  }
}
