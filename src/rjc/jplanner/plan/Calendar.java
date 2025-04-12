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
import java.util.Arrays;
import java.util.HashMap;

import rjc.table.Utils;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/********************************* Single calendar for planning **********************************/
/*************************************************************************************************/

public class Calendar
{
  private String             m_name;        // name of calendar
  private Date               m_cycleAnchor; // anchor date of calendar cycle
  private ArrayList<Day>     m_normal;      // normal basic cycle days
  private HashMap<Date, Day> m_exceptions;  // exceptions override normal days

  public enum FIELD
  {
    Name, Anchor, Exceptions, Cycle, Normal
  }

  /**************************************** constructor ******************************************/
  public Calendar()
  {
    // construct empty but usable calendar
    m_name = "Null";
    m_cycleAnchor = new Date( 2000, 1, 1 );
    m_normal = new ArrayList<>();
    m_exceptions = new HashMap<>();
  }

  /**************************************** constructor ******************************************/
  public Calendar( String name, Date date, Day... normal )
  {
    // construct new calendar
    m_name = name;
    m_cycleAnchor = date;
    m_normal = new ArrayList<>( Arrays.asList( normal ) );
    m_exceptions = new HashMap<>();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return Utils.name( this ) + "[" + m_name + ", " + m_cycleAnchor + ", " + m_normal + "]";
  }

  /******************************************* getName *******************************************/
  public String getName()
  {
    return m_name;
  }

  /****************************************** getAnchor ******************************************/
  public Date getAnchor()
  {
    return m_cycleAnchor;
  }

  /***************************************** getNormals ******************************************/
  public ArrayList<Day> getNormals()
  {
    return m_normal;
  }

  /**************************************** getExceptions ****************************************/
  public HashMap<Date, Day> getExceptions()
  {
    return m_exceptions;
  }

  /******************************************* getValue ******************************************/
  public Object getValue( int field )
  {
    // return value for the different fields
    if ( field == FIELD.Name.ordinal() )
      return m_name;

    if ( field == FIELD.Anchor.ordinal() )
      return m_cycleAnchor;

    if ( field == FIELD.Exceptions.ordinal() )
      return m_exceptions.size();

    if ( field == FIELD.Cycle.ordinal() )
      return m_normal.size();

    try
    {
      field -= FIELD.Normal.ordinal();
      return m_normal.get( field ).getName();
    }
    catch ( IndexOutOfBoundsException e )
    {
      // beyond cycle, return blank
      return null;
    }
  }

}
