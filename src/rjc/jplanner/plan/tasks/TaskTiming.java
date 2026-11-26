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

package rjc.jplanner.plan.tasks;

import java.util.Objects;

import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/********************* Immutable task dates and explanations for missing dates *******************/
/*************************************************************************************************/

public final class TaskTiming
{
  private static final String NOT_YET_SCHEDULED = "Not yet scheduled"; // initial explanation for unavailable dates

  private final DateTime      m_start;                                 // entered or calculated start, null if unavailable
  private final DateTime      m_end;                                   // entered or calculated normal end, null if unavailable
  private final String        m_startStatus;                           // explanation for an unavailable start or a date conflict
  private final String        m_endStatus;                             // explanation for an unavailable end or a date conflict
  private final DateTime      m_roundedEnd;                            // scheduled end after rounding, null until calculated

  /***************************************** constructor *****************************************/
  public TaskTiming()
  {
    this( null, null, NOT_YET_SCHEDULED, NOT_YET_SCHEDULED, null );
  }

  /***************************************** constructor *****************************************/
  public TaskTiming( DateTime start, DateTime end, String startStatus, String endStatus, DateTime roundedEnd )
  {
    // fixed dates awaiting user input may have neither a value nor a status
    if ( start == null && startStatus != null && startStatus.isBlank() )
      throw new IllegalArgumentException( "An unavailable start status must not be blank" );
    if ( end == null && endStatus != null && endStatus.isBlank() )
      throw new IllegalArgumentException( "An unavailable end status must not be blank" );
    if ( roundedEnd != null && ( end == null || roundedEnd.toSerialMilliseconds() < end.toSerialMilliseconds() ) )
      throw new IllegalArgumentException( "A rounded end needs a normal end at or before it" );

    m_start = start;
    m_end = end;
    m_startStatus = startStatus;
    m_endStatus = endStatus;
    m_roundedEnd = roundedEnd;
  }

  /****************************************** getStart *******************************************/
  public DateTime getStart()
  {
    return m_start;
  }

  /******************************************* getEnd ********************************************/
  public DateTime getEnd()
  {
    return m_end;
  }

  /*************************************** getStartStatus ****************************************/
  public String getStartStatus()
  {
    return m_startStatus;
  }

  /**************************************** getEndStatus *****************************************/
  public String getEndStatus()
  {
    return m_endStatus;
  }

  /*************************************** getRoundedEnd *****************************************/
  public DateTime getRoundedEnd()
  {
    return m_roundedEnd;
  }

  /************************************ withoutCalculatedDates ***********************************/
  TaskTiming withoutCalculatedDates( boolean fixedStart, boolean fixedEnd )
  {
    // retain fixed inputs exactly, including missing values, but remove previous calculation explanations
    DateTime start = fixedStart ? m_start : null;
    DateTime end = fixedEnd ? m_end : null;
    String startStatus = fixedStart ? null : NOT_YET_SCHEDULED;
    String endStatus = fixedEnd ? null : NOT_YET_SCHEDULED;
    if ( start == m_start && end == m_end && Objects.equals( startStatus, m_startStatus )
        && Objects.equals( endStatus, m_endStatus ) && m_roundedEnd == null )
      return this;

    return new TaskTiming( start, end, startStatus, endStatus, null );
  }

  /****************************************** withStart ******************************************/
  TaskTiming withStart( DateTime start )
  {
    if ( Objects.equals( m_start, start ) )
      return this;

    return new TaskTiming( start, m_end, start == null ? NOT_YET_SCHEDULED : null, m_endStatus, m_roundedEnd );
  }

  /******************************************* withEnd *******************************************/
  TaskTiming withEnd( DateTime end )
  {
    if ( Objects.equals( m_end, end ) )
      return this;

    // changing the normal end invalidates its previously calculated rounded end
    return new TaskTiming( m_start, end, m_startStatus, end == null ? NOT_YET_SCHEDULED : null, null );
  }
}
