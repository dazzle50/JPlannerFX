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

package rjc.jplanner.gui.gantt;

import java.time.LocalDate;

import rjc.table.data.types.Time;

/*************************************************************************************************/
/********************** Enum for selectable calendar periods and time steps **********************/
/*************************************************************************************************/

// selectable calendar periods and midnight-aligned time steps
enum GanttAxisInterval
{
  YEAR( "Year", 12, 0, GanttAxisLabelFormat.YEAR_FULL ),

  HALF_YEAR( "Half-year", 6, 0, GanttAxisLabelFormat.HALF_YEAR_WITH_YEAR ),

  QUARTER( "Quarter", 3, 0, GanttAxisLabelFormat.QUARTER_WITH_YEAR ),

  MONTH( "Month", 1, 0, GanttAxisLabelFormat.MONTH_WITH_YEAR ),

  WEEK( "Week", 0, 7L * Time.MILLIS_PER_DAY, GanttAxisLabelFormat.DAY_NUMBER ),

  DAY( "Day", 0, Time.MILLIS_PER_DAY, GanttAxisLabelFormat.DAY_NUMBER ),

  HALF_DAY( "Half-day", 0, 12L * Time.MILLIS_PER_HOUR, GanttAxisLabelFormat.TIME_HOUR ),

  SIX_HOURS( "6 hours", 0, 6L * Time.MILLIS_PER_HOUR, GanttAxisLabelFormat.TIME_HOUR ),

  THREE_HOURS( "3 hours", 0, 3L * Time.MILLIS_PER_HOUR, GanttAxisLabelFormat.TIME_HOUR ),

  HOUR( "Hour", 0, Time.MILLIS_PER_HOUR, GanttAxisLabelFormat.TIME_HOUR ),

  TEN_MINUTES( "10 minutes", 0, 10L * Time.MILLIS_PER_MINUTE, GanttAxisLabelFormat.TIME_HOUR_MINUTE ),

  MINUTE( "Minute", 0, Time.MILLIS_PER_MINUTE, GanttAxisLabelFormat.TIME_HOUR_MINUTE ),

  SECOND( "Second", 0, Time.MILLIS_PER_SECOND, GanttAxisLabelFormat.TIME_WITH_SECONDS );

  private final String               m_text;          // interval name in menus
  private final int                  m_months;        // calendar months per period, or zero for fixed durations
  private final long                 m_milliseconds;  // fixed duration, or zero for calendar months
  private final GanttAxisLabelFormat m_defaultFormat; // initial label format for a new axis

  /**************************************** constructor ******************************************/
  GanttAxisInterval( String text, int months, long milliseconds, GanttAxisLabelFormat defaultFormat )
  {
    m_text = text;
    m_months = months;
    m_milliseconds = milliseconds;
    m_defaultFormat = defaultFormat;
  }

  /****************************************** getText ********************************************/
  String getText()
  {
    return m_text;
  }

  /************************************** getDefaultFormat ***************************************/
  GanttAxisLabelFormat getDefaultFormat()
  {
    return m_defaultFormat;
  }

  /*************************************** isTimeInterval ****************************************/
  boolean isTimeInterval()
  {
    return m_months == 0 && m_milliseconds < Time.MILLIS_PER_DAY;
  }

  /****************************************** startOf ********************************************/
  long startOf( long milliseconds )
  {
    if ( m_months == 0 )
    {
      // the epoch is a Thursday; offset weeks to the preceding ISO Monday
      long offset = this == WEEK ? -3L * Time.MILLIS_PER_DAY : 0L;
      return Math.floorDiv( milliseconds - offset, m_milliseconds ) * m_milliseconds + offset;
    }

    // use calendar arithmetic beyond compact storage so partial end periods remain drawable
    LocalDate date = LocalDate.ofEpochDay( Math.floorDiv( milliseconds, Time.MILLIS_PER_DAY ) );
    int month = ( date.getMonthValue() - 1 ) / m_months * m_months + 1;
    return LocalDate.of( date.getYear(), month, 1 ).toEpochDay() * Time.MILLIS_PER_DAY;
  }

  /******************************************** next *********************************************/
  long next( long start )
  {
    if ( m_months == 0 )
      return start + m_milliseconds;
    return LocalDate.ofEpochDay( Math.floorDiv( start, Time.MILLIS_PER_DAY ) ).plusMonths( m_months ).toEpochDay()
        * Time.MILLIS_PER_DAY;
  }
}
