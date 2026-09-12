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

import java.util.Objects;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/************************** Immutable working period within a day type ***************************/
/*************************************************************************************************/

/**
 * Represents a single immutable, contiguous working period within a day type,
 * defined by an inclusive start time and an exclusive end time.
 */
public final class WorkPeriod
{
  private final Time m_start; // inclusive work period start time
  private final Time m_end;   // exclusive work period end time

  /***************************************** constructor *****************************************/
  /**
   * Constructs a work period from start and end time values.
   *
   * @param start start time of the work period
   * @param end   end time of the work period
   * @throws NullPointerException if either endpoint is null
   * @throws IllegalArgumentException if the start is at or after the end
   */
  public WorkPeriod( Time start, Time end )
  {
    // Time already enforces 00:00 through 24:00; the interval must also have positive duration.
    m_start = Objects.requireNonNull( start, "start" );
    m_end = Objects.requireNonNull( end, "end" );
    if ( start.compareTo( end ) >= 0 )
      throw new IllegalArgumentException( "Work period start must precede end" );
  }

  /******************************************* ofHours *******************************************/
  /**
   * Constructs a work period from start and end hour values.
   *
   * @param start start of the work period in hours
   * @param end   end of the work period in hours
   * @throws java.time.DateTimeException if either hour value is non-finite or outside 0 through 24
   * @throws IllegalArgumentException if the rounded start is at or after the rounded end
   */
  public static WorkPeriod ofHours( double start, double end )
  {
    // validate after millisecond rounding, which can collapse distinct hour values to one endpoint.
    return new WorkPeriod( Time.ofHours( start ), Time.ofHours( end ) );
  }

  /****************************************** getStart *******************************************/
  public Time getStart()
  {
    return m_start;
  }

  /******************************************* getEnd ********************************************/
  public Time getEnd()
  {
    return m_end;
  }

  /************************************** getDurationMillis **************************************/
  public int getDurationMillis()
  {
    return m_end.toMillisecondsOfDay() - m_start.toMillisecondsOfDay();
  }

  /******************************************* equals ********************************************/
  @Override
  public boolean equals( Object other )
  {
    // value equality lets replacement lists detect no-op edits without sharing period instances.
    return other instanceof WorkPeriod p && m_start.equals( p.m_start ) && m_end.equals( p.m_end );
  }

  /****************************************** hashCode *******************************************/
  @Override
  public int hashCode()
  {
    return Objects.hash( m_start, m_end );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return "[" + m_start + ", " + m_end + "]";
  }
}
