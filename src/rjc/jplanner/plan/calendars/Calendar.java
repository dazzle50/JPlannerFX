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

package rjc.jplanner.plan.calendars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import rjc.jplanner.plan.days.DayType;
import rjc.jplanner.plan.days.DayType.Direction;
import rjc.table.Utils;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/********************************* Single calendar for planning **********************************/
/*************************************************************************************************/

public class Calendar
{
  private String                 m_name;        // name of calendar
  private Date                   m_cycleAnchor; // anchor date of calendar cycle
  private ArrayList<DayType>     m_normal;      // normal basic cycle days
  private HashMap<Date, DayType> m_exceptions;  // exceptions override normal days

  public enum FIELD
  {
    Name, Anchor, Exceptions, Cycle, Normal
  }

  /**************************************** constructor ******************************************/
  public Calendar( DayType day )
  {
    // construct simple new calendar
    m_name = "New";
    m_cycleAnchor = Date.of( 2000, 1, 1 );
    m_normal = new ArrayList<>();
    m_normal.add( day );
    m_exceptions = new HashMap<>();
  }

  /**************************************** constructor ******************************************/
  public Calendar( String name, Date date, DayType... normal )
  {
    // construct new calendar
    m_name = name;
    m_cycleAnchor = date;
    m_normal = new ArrayList<>( Arrays.asList( normal ) );
    m_exceptions = new HashMap<>();
  }

  /**************************************** addException *****************************************/
  public void addException( int day, int month, int year, DayType daytype )
  {
    // add exception to this calendar
    m_exceptions.put( Date.of( year, month, day ), daytype );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return m_name;
  }

  public String toStringLong()
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
  public ArrayList<DayType> getNormals()
  {
    return m_normal;
  }

  /**************************************** getExceptions ****************************************/
  public HashMap<Date, DayType> getExceptions()
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
        return normal < m_normal.size() ? m_normal.get( normal ) : null;
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
  public String setValue( int field, Object newValue, boolean commit )
  {
    // set/check field value and return null if successful/possible
    int normal = field >= FIELD.Normal.ordinal() ? field - FIELD.Normal.ordinal() : 0;

    switch ( FIELD.values()[field - normal] )
    {
      case Name:
        // new value can be of any type
        String newName = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newName.length() < 1 || newName.length() > 40 )
          return "Name length not between 1 and 40 characters";
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

        // check new value is ArrayList<DayType> and reasonable size
        try
        {
          @SuppressWarnings( "unchecked" )
          var newNormal = (ArrayList<DayType>) newValue;
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
        if ( newValue instanceof DayType day )
        {
          if ( commit )
            m_normal.set( normal, day );
          return null;
        }
        return "Not day-type: " + Utils.objectsString( newValue );

    }
  }

  /***************************************** isWorking *******************************************/
  public boolean isWorking( Date date )
  {
    // return whether date is working or not
    return getDayType( date ).hasWorkingTime();
  }

  /***************************************** getDayType ******************************************/
  public DayType getDayType( Date date )
  {
    // if exception exists return it, otherwise return normal cycle day
    DayType day = m_exceptions.get( date );
    if ( day != null )
      return day;

    int normal = ( date.getEpochDay() - m_cycleAnchor.getEpochDay() ) % m_normal.size();
    if ( normal < 0 )
      normal += m_normal.size();

    return m_normal.get( normal );
  }

  /************************************** nextWorkingStart ***************************************/
  // find the earliest working start in [from, limit]; null means none within that interval.
  public DateTime nextWorkingStart( DateTime from, DateTime limit )
  {
    validateSearch( from, limit, Direction.FORWARD );
    long last = limit.toSerialMilliseconds();
    Date date = from.getDate();
    Time time = from.getTime();

    while ( true )
    {
      Time start = getDayType( date ).nextWorkingStart( time );
      if ( start != null )
      {
        DateTime result = DateTime.of( date, start );
        return result.toSerialMilliseconds() <= last ? result : null;
      }

      // allow a start at the next midnight when it is exactly the inclusive limit.
      long nextMidnight = ( (long) date.getEpochDay() + 1 ) * Time.MILLIS_PER_DAY;
      if ( date.equals( Date.MAX_VALUE ) || nextMidnight > last )
        return null;
      date = date.plusDays( 1 );
      time = Time.MIN_VALUE;
    }
  }

  /************************************ previousWorkingFinish ************************************/
  // find the latest working finish in [limit, from]; null does not prove global impossibility.
  public DateTime previousWorkingFinish( DateTime from, DateTime limit )
  {
    validateSearch( from, limit, Direction.BACKWARD );
    long first = limit.toSerialMilliseconds();
    Date date = from.getDate();
    Time time = from.getTime();

    while ( true )
    {
      Time finish = getDayType( date ).previousWorkingFinish( time );
      if ( finish != null )
      {
        DateTime result = DateTime.of( date, finish );
        return result.toSerialMilliseconds() >= first ? result : null;
      }

      // the previous date's 24:00 is the same instant as this date's 00:00.
      long midnight = (long) date.getEpochDay() * Time.MILLIS_PER_DAY;
      if ( date.equals( Date.MIN_VALUE ) || midnight < first )
        return null;
      date = date.plusDays( -1 );
      time = Time.MAX_VALUE;
    }
  }

  /************************************ workingMillisBetween *************************************/
  // sum working time across dates; reversing the endpoints negates the result.
  public long workingMillisBetween( DateTime from, DateTime to )
  {
    long first = Objects.requireNonNull( from, "from" ).toSerialMilliseconds();
    long last = Objects.requireNonNull( to, "to" ).toSerialMilliseconds();
    if ( first > last )
      return -workingMillisBetween( to, from );

    long total = 0;
    while ( first < last )
    {
      DateTime position = DateTime.ofSerialMilliseconds( first );
      long midnight = (long) position.getEpochDay() * Time.MILLIS_PER_DAY;
      long end = Math.min( last, midnight + Time.MILLIS_PER_DAY );
      Time finish = Time.ofMilliseconds( (int) ( end - midnight ) );
      total += getDayType( position.getDate() ).workingMillisBetween( position.getTime(), finish );
      first = end;
    }
    return total;
  }

  /************************************ equivalentDaysBetween ************************************/
  // each date supplies its own weight, including exceptions and partial working days.
  public double equivalentDaysBetween( DateTime from, DateTime to )
  {
    long first = Objects.requireNonNull( from, "from" ).toSerialMilliseconds();
    long last = Objects.requireNonNull( to, "to" ).toSerialMilliseconds();
    if ( first > last )
      return -equivalentDaysBetween( to, from );

    double total = 0.0;
    double correction = 0.0;
    while ( first < last )
    {
      DateTime position = DateTime.ofSerialMilliseconds( first );
      long midnight = (long) position.getEpochDay() * Time.MILLIS_PER_DAY;
      long end = Math.min( last, midnight + Time.MILLIS_PER_DAY );
      Time finish = Time.ofMilliseconds( (int) ( end - midnight ) );
      double days = getDayType( position.getDate() ).equivalentDaysBetween( position.getTime(), finish );

      // compensate summation so small daily contributions are not repeatedly lost to rounding.
      double adjusted = days - correction;
      double next = total + adjusted;
      correction = ( next - total ) - adjusted;
      total = next;
      first = end;
    }
    return total;
  }

  /************************************ advanceWorkingMillis *************************************/
  // advance a non-negative amount in the requested direction, including completion at limit.
  // zero preserves from; null means insufficient work before the limit, not global impossibility.
  public DateTime advanceWorkingMillis( DateTime from, long amount, Direction direction, DateTime limit )
  {
    validateSearch( from, limit, direction );
    if ( amount < 0 )
      throw new IllegalArgumentException( "Working amount must not be negative" );
    if ( amount == 0 )
      return from;

    boolean forward = direction == Direction.FORWARD;
    long last = limit.toSerialMilliseconds();
    Date date = from.getDate();
    Time time = from.getTime();
    while ( true )
    {
      long midnight = (long) date.getEpochDay() * Time.MILLIS_PER_DAY;
      int boundary = (int) ( forward ? Math.min( Time.MILLIS_PER_DAY, last - midnight )
          : Math.max( 0, last - midnight ) );
      DayType day = getDayType( date );
      int capacity = Math.abs( day.workingMillisBetween( time, Time.ofMilliseconds( boundary ) ) );
      if ( amount <= capacity )
        return DateTime.of( date, day.advanceWorkingMillis( time, (int) amount, direction ) );
      amount -= capacity;

      // check the limit before stepping, including the minimum/maximum representable dates.
      if ( midnight + boundary == last || date.equals( forward ? Date.MAX_VALUE : Date.MIN_VALUE ) )
        return null;
      date = date.plusDays( forward ? 1 : -1 );
      time = forward ? Time.MIN_VALUE : Time.MAX_VALUE;
    }
  }

  /*************************************** validateSearch ****************************************/
  // compare timeline positions so 24:00 and the following 00:00 share the same bound.
  private static void validateSearch( DateTime from, DateTime limit, Direction direction )
  {
    long start = Objects.requireNonNull( from, "from" ).toSerialMilliseconds();
    long end = Objects.requireNonNull( limit, "limit" ).toSerialMilliseconds();
    Objects.requireNonNull( direction, "direction" );
    if ( direction == Direction.FORWARD ? end < start : end > start )
      throw new IllegalArgumentException( "Search limit is opposite to the requested direction" );
  }
}
