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
import rjc.table.data.types.Time;

/*************************************************************************************************/
/***************************** Single day type as used by calendars ******************************/
/*************************************************************************************************/

public class Day
{
  private String                   m_name;    // name of day type
  private double                   m_work;    // equivalent days worked (typically 1.0 or 0.0)
  private ArrayList<DayWorkPeriod> m_periods; // list of work periods

  private int                      m_workMS;  // pre-calculated number of worked milliseconds in day-type

  public enum FIELD
  {
    Name, Work, Periods, Start, End
  }

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

  /******************************************* getWork *******************************************/
  public double getWork()
  {
    return m_work;
  }

  /**************************************** getWorkPeriods ***************************************/
  public ArrayList<DayWorkPeriod> getWorkPeriods()
  {
    return m_periods;
  }

  /******************************************* getValue ******************************************/
  public Object getValue( int field )
  {
    // adjust field for period start & end counter
    int period = ( field - FIELD.Start.ordinal() ) / 2;
    if ( period > 0 )
      field -= period * 2;

    // return value for the different fields
    switch ( FIELD.values()[field] )
    {
      case Name:
        return m_name;
      case Work:
        return m_work;
      case Periods:
        return m_periods.size();
      case Start:
        return period < m_periods.size() ? m_periods.get( period ).m_start : null;
      case End:
        return period < m_periods.size() ? m_periods.get( period ).m_end : null;
      default:
        throw new IllegalArgumentException( "Unrecognised field " + field );
    }
  }

  /******************************************** isBlank ******************************************/
  public boolean isBlank( int field )
  {
    // work field is blank if no periods, and start/end fields are blank if beyond periods
    if ( field == FIELD.Work.ordinal() && m_periods.size() == 0 )
      return true;

    return field >= FIELD.Start.ordinal() + ( 2 * m_periods.size() );
  }

  /****************************************** setValue *******************************************/
  public String setValue( int field, Object newValue, Boolean commit )
  {
    // set/check field value and return null if successful/possible
    int period = ( field - FIELD.Start.ordinal() ) / 2;
    if ( period > 0 )
      field -= period * 2;

    switch ( FIELD.values()[field] )
    {
      case Name:
        // new value can be of any type
        String newName = newValue == null ? null : Utils.clean( newValue.toString() );
        String problem = nameValidity( newName );
        if ( problem != null )
          return problem;
        if ( commit )
          m_name = newName;
        return null;

      case Work:
        // check new value is double and in range
        if ( newValue instanceof Double newWork )
        {
          if ( newWork < 0 || newWork > 9.99 )
            return "Value not between 0 and 9.99";
          if ( commit )
            m_work = newWork;
          return null;
        }
        return "Not double: " + Utils.objectsString( newValue );

      case Periods:
        // check new value is integer and in range
        if ( newValue instanceof Integer newPeriods )
        {
          if ( newPeriods < 0 || newPeriods > 8 )
            return "Value not between 0 and 8";
          if ( commit )
            Utils.trace( "SETTING PERIODS NOT IMPLEMENTED !!!" );
          return null;
        }
        return "Not integer: " + Utils.objectsString( newValue );

      default:
        // either a period start or end time
        if ( newValue instanceof Time time )
        {
          boolean isStart = ( ( field - FIELD.Start.ordinal() ) & 1 ) == 0;
          problem = timeValidity( period, field, time );
          if ( problem != null )
            return problem;
          if ( commit )
          {
            // set period start or end time;
            if ( isStart )
              m_periods.get( period ).m_start = time;
            else
              m_periods.get( period ).m_end = time;
          }
          return null;
        }
        return "Not time: " + Utils.objectsString( newValue );
    }
  }

  /*************************************** nameValidity ******************************************/
  public String nameValidity( String newName )
  {
    // check name is not too long
    if ( newName.length() > 40 )
      return "Name too long (max 40 characters)";

    // check name is not a duplicate
    for ( int index = 0; index < Plan.getDays().size(); index++ )
      if ( Plan.getDay( index ) != this && Plan.getDay( index ).getName().equals( newName ) )
        return "Name not unique (clash with day-type " + ( index + 1 ) + ")";

    // no problem so return null
    return null;
  }

  /*************************************** timeValidity  ******************************************/
  private String timeValidity( int period, int field, Time time )
  {
    // check is valid period
    if ( period < 0 || period >= m_periods.size() )
      return "Invalid work period (" + period + ")";

    // check time is between previous and next
    Object previousTime = getValue( field + period * 2 - 1 );
    int previousMS = ( previousTime instanceof Time t ) ? t.getDayMilliseconds() : Integer.MIN_VALUE;
    Object nextTime = getValue( field + period * 2 + 1 );
    int nextMS = ( nextTime instanceof Time t ) ? t.getDayMilliseconds() : Integer.MAX_VALUE;
    int ms = time.getDayMilliseconds();

    if ( previousMS >= ms || ms >= nextMS )
      return "Start/End times not in ascending order";

    // no problem found so return null
    return null;
  }

}
