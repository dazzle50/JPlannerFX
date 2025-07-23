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

  /**************************************** addException *****************************************/
  public void addException( int day, int month, int year, Day daytype )
  {
    // add exception to this calendar
    m_exceptions.put( new Date( year, month, day ), daytype );
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
    // determine which normal field is wanted
    int normal = field >= FIELD.Normal.ordinal() ? field - FIELD.Normal.ordinal() : 0;

    // return value for the different fields
    switch ( FIELD.values()[field - normal] )
    {
      case Name:
        return m_name;
      case Anchor:
        return m_cycleAnchor;
      case Cycle:
        return m_normal.size();
      case Exceptions:
        return m_exceptions.size();
      case Normal:
        return normal < m_normal.size() ? m_normal.get( normal ).getName() : null;
      default:
        throw new IllegalArgumentException( "Unrecognised field " + field );
    }
  }

  /******************************************** isBlank ******************************************/
  public boolean isBlank( int field )
  {
    // normal is blank if beyond cycle
    return field >= FIELD.Normal.ordinal() + m_normal.size();
  }

  /****************************************** setValue *******************************************/
  public String setValue( int field, Object newValue, Boolean commit )
  {
    // set/check field value and return null if successful/possible
    int normal = field >= FIELD.Normal.ordinal() ? field - FIELD.Normal.ordinal() : 0;

    switch ( FIELD.values()[field - normal] )
    {
      case Name:
        // new value can be of any type
        String newName = newValue == null ? "" : Utils.clean( newValue.toString() );
        String problem = nameValidity( newName );
        if ( problem != null )
          return problem;
        if ( commit )
          m_name = newName;
        return null;

      case Anchor:
        // any valid date can be used as cycle anchor
        if ( newValue instanceof Date date )
        {
          if ( commit )
            m_cycleAnchor = date;
          return null;
        }
        return "Not date: " + Utils.objectsString( newValue );

      case Exceptions:
        // TODO setting calendar exceptions
        return "Not implemented: " + Utils.objectsString( newValue );

      case Cycle:
        // check new value is integer and in range
        if ( newValue instanceof Integer length )
        {
          if ( length < 1 || length > 99 )
            return "Value not between 1 and 99";
          if ( commit )
            return "Cannot commit integer";
          return null;
        }

        // check new value is ArrayList<Day> and reasonable size
        try
        {
          @SuppressWarnings( "unchecked" )
          var newNormal = (ArrayList<Day>) newValue;
          if ( newNormal.isEmpty() || newNormal.size() > 99 )
            return "Array size not between 1 and 99";
          if ( commit )
            m_normal = newNormal;
          return null;
        }
        catch ( Exception exception )
        {
          return "Invalid: " + Utils.objectsString( newValue ) + " [" + exception + "]";
        }

      default:
        // setting normal day-types
        if ( normal >= m_normal.size() )
          return "Normal beyond cycle length";
        Day day = Plan.getDays().findByName( newValue );
        if ( day == null )
          return "Not day-type: " + Utils.objectsString( newValue );
        if ( commit )
          m_normal.set( normal, day );
        return null;

    }
  }

  /*************************************** nameValidity ******************************************/
  public String nameValidity( String newName )
  {
    // check name is not too short or long
    if ( newName.length() < 1 || newName.length() > 40 )
      return "Name length not between 1 and 40 characters";

    // check name is not a duplicate
    for ( int index = 0; index < Plan.getCalendars().size(); index++ )
      if ( Plan.getCalendar( index ) != this && newName.equals( Plan.getCalendar( index ).getName() ) )
        return "Name not unique (clash with calendar " + ( index + 1 ) + ")";

    // no problem so return null
    return null;
  }

}
