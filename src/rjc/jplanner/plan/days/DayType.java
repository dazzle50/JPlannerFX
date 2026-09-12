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

package rjc.jplanner.plan.days;

import java.util.List;
import java.util.Objects;
import rjc.table.Utils;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/*************************** Editable day definition used by calendars ***************************/
/*************************************************************************************************/

public final class DayType
{
  public enum Direction
  {
    FORWARD, BACKWARD
  }

  // day-type instances keep their identity throughout editing and undo
  private String           m_name;           // name of day type
  // configured weight is independent of clock duration, including for an empty day.
  private double           m_equivalentDays; // configured equivalent days worked
  private List<WorkPeriod> m_periods;        // immutable list of work periods
  private int              m_workingMillis;  // cached working duration in milliseconds

  /***************************************** constructor *****************************************/
  public DayType( String name, double equivalentDays, List<WorkPeriod> periods )
  {
    updateName( name );
    setEquivalentDays( equivalentDays );
    setWorkPeriods( periods );
  }

  /**************************************** validatedName ****************************************/
  public static String validatedName( String name )
  {
    // constructors, rename previews and committed renames must use the same cleaned spelling.
    String cleaned = Utils.clean( Objects.requireNonNull( name, "name" ) );
    if ( cleaned.isEmpty() || cleaned.length() > 40 )
      throw new IllegalArgumentException( "Name length not between 1 and 40 characters" );
    return cleaned;
  }

  /*********************************** validateEquivalentDays ************************************/
  public static void validateEquivalentDays( double value )
  {
    // range comparisons alone would let NaN through because both comparisons would be false.
    if ( !Double.isFinite( value ) || value < 0 || value > 9.99 )
      throw new IllegalArgumentException( "Work must be finite and between 0 and 9.99" );
  }

  /************************************** validatedPeriods ***************************************/
  // returns an immutable validated copy, suitable for preview as well as commit.
  public static List<WorkPeriod> validatedPeriods( List<WorkPeriod> proposed )
  {
    // snapshot the input before checking it; later caller edits cannot invalidate this list.
    List<WorkPeriod> copy = List.copyOf( proposed );
    if ( copy.size() > 8 )
      throw new IllegalArgumentException( "Number of work periods must be between 0 and 8" );

    int previousEnd = 0;
    for ( int index = 0; index < copy.size(); index++ )
    {
      WorkPeriod period = copy.get( index );
      // equality permits adjacency; do not sort, merge or otherwise change the supplied periods.
      if ( period.getStart().toMillisecondsOfDay() < previousEnd )
        throw new IllegalArgumentException(
            "Work period " + ( index + 1 ) + " is out of order or overlaps its predecessor" );
      previousEnd = period.getEnd().toMillisecondsOfDay();
    }
    return copy;
  }

  /***************************************** updateName ******************************************/
  // collection-controlled renames enforce uniqueness before reaching this package-private setter.
  void updateName( String value )
  {
    m_name = validatedName( value );
  }

  /******************************************* getName *******************************************/
  public String getName()
  {
    return m_name;
  }

  /****************************************** toString *******************************************/
  // calendar choice controls use toString() for their visible labels.
  @Override
  public String toString()
  {
    return m_name;
  }

  /************************************** getEquivalentDays **************************************/
  public double getEquivalentDays()
  {
    return m_equivalentDays;
  }

  /************************************** setEquivalentDays **************************************/
  public void setEquivalentDays( double value )
  {
    validateEquivalentDays( value );
    m_equivalentDays = value;
  }

  /*************************************** getWorkPeriods ****************************************/
  public List<WorkPeriod> getWorkPeriods()
  {
    return m_periods;
  }

  /*************************************** setWorkPeriods ****************************************/
  public void setWorkPeriods( List<WorkPeriod> proposed )
  {
    // finish all validation and calculation locally so a failed edit preserves both stored fields.
    List<WorkPeriod> copy = validatedPeriods( proposed );
    int total = 0;
    for ( WorkPeriod period : copy )
      total += period.getDurationMillis();

    // these assignments maintain the cache but do not provide a concurrent-edit snapshot.
    m_periods = copy;
    m_workingMillis = total;
  }

  /************************************** getWorkingMillis ***************************************/
  public int getWorkingMillis()
  {
    return m_workingMillis;
  }

  /*************************************** hasWorkingTime ****************************************/
  public boolean hasWorkingTime()
  {
    return !m_periods.isEmpty();
  }

  /**************************************** isWorkingTime ****************************************/
  public boolean isWorkingTime( Time time )
  {
    // starts are inclusive and ends exclusive, so 24:00 can never be a working start.
    int ms = Objects.requireNonNull( time, "time" ).toMillisecondsOfDay();
    for ( WorkPeriod p : m_periods )
      if ( ms >= p.getStart().toMillisecondsOfDay() && ms < p.getEnd().toMillisecondsOfDay() )
        return true;
    return false;
  }

  /************************************** nextWorkingStart ***************************************/
  // earliest point in [start, end) at or after from; null if there is none.
  public Time nextWorkingStart( Time from )
  {
    Objects.requireNonNull( from, "from" );

    // skip completed periods; a point already inside work is itself the next eligible start.
    for ( WorkPeriod p : m_periods )
      if ( from.compareTo( p.getEnd() ) < 0 )
        return from.compareTo( p.getStart() ) < 0 ? p.getStart() : from;
    return null;
  }

  /************************************ previousWorkingFinish ************************************/
  // latest point in (start, end] at or before from; null if there is none.
  public Time previousWorkingFinish( Time from )
  {
    Objects.requireNonNull( from, "from" );

    // backward work uses (start, end], allowing completion at an end but not at an isolated start.
    for ( int i = m_periods.size() - 1; i >= 0; i-- )
    {
      WorkPeriod p = m_periods.get( i );
      if ( from.compareTo( p.getStart() ) > 0 )
        return from.compareTo( p.getEnd() ) > 0 ? p.getEnd() : from;
    }
    return null;
  }

  /************************************ workingMillisBetween *************************************/
  // working duration clipped to the interval; reversing the arguments negates it.
  public int workingMillisBetween( Time from, Time to )
  {
    int a = Objects.requireNonNull( from, "from" ).toMillisecondsOfDay();
    int b = Objects.requireNonNull( to, "to" ).toMillisecondsOfDay();

    // measure intersections in ascending order, then restore the requested sign once at the end.
    int low = Math.min( a, b ), high = Math.max( a, b ), total = 0;
    for ( WorkPeriod p : m_periods )
      total += Math.max( 0,
          Math.min( high, p.getEnd().toMillisecondsOfDay() ) - Math.max( low, p.getStart().toMillisecondsOfDay() ) );
    return a <= b ? total : -total;
  }

  /************************************ advanceWorkingMillis *************************************/
  // skip breaks, returning the reached boundary for exact completion without crossing dates
  // zero returns from; negative amounts or insufficient capacity are argument errors
  public Time advanceWorkingMillis( Time from, int amount, Direction direction )
  {
    // check required arguments even when zero work would otherwise return immediately.
    int position = Objects.requireNonNull( from, "from" ).toMillisecondsOfDay();
    Objects.requireNonNull( direction, "direction" );
    if ( amount < 0 )
      throw new IllegalArgumentException( "Working amount must not be negative" );
    if ( amount == 0 )
      return from;

    boolean forward = direction == Direction.FORWARD;
    for ( int i = forward ? 0 : m_periods.size() - 1; forward ? i < m_periods.size() : i >= 0; i += forward ? 1 : -1 )
    {
      WorkPeriod p = m_periods.get( i );
      int start = p.getStart().toMillisecondsOfDay(), end = p.getEnd().toMillisecondsOfDay();
      // clip each period against the original time; breaks consume no working milliseconds.
      int boundary = forward ? Math.max( position, start ) : Math.min( position, end );
      int capacity = forward ? end - boundary : boundary - start;
      if ( capacity <= 0 )
        continue;

      // exact completion stays at this boundary instead of jumping across the next break.
      if ( amount <= capacity )
        return Time.ofMilliseconds( forward ? boundary + amount : boundary - amount );

      // consume this period before scanning the next one in the requested direction.
      amount -= capacity;
    }

    throw new IllegalArgumentException( "Insufficient working time within this day" );
  }
}
