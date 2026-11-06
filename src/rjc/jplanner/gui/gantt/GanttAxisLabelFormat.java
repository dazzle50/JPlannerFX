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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.IsoFields;

import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/**************************** Enum for selectable axis label formats *****************************/
/*************************************************************************************************/

// selectable axis label formats with their relevant interval spacing
enum GanttAxisLabelFormat
{
  YEAR_SHORT( "Short", "uu" ),

  YEAR_FULL( "Full", "uuuu" ),

  HALF_YEAR_SHORT( "Short", (String) null ),

  HALF_YEAR_WITH_YEAR( "With year", (String) null ),

  QUARTER_SHORT( "Short", "QQQ" ),

  QUARTER_WITH_YEAR( "With year", "uuuu QQQ" ),

  MONTH_INITIAL( "Initial", "MMMMM" ),

  MONTH_TWO_DIGITS( "Two digits", "MM" ),

  MONTH_SHORT( "Short name", "MMM" ),

  MONTH_FULL( "Full name", "MMMM" ),

  MONTH_WITH_YEAR( "With year", "MMM-uu" ),

  WEEK_NUMBER( "Number", weekFormatter( false, false ) ),

  WEEK_PREFIXED( "Prefixed", weekFormatter( true, false ) ),

  WEEK_WITH_YEAR( "With year", weekFormatter( true, true ) ),

  DAY_NUMBER( "Day number", "d" ),

  DAY_TWO_DIGITS( "Two digits", "dd" ),

  DAY_WITH_MONTH( "Day with month", "d MMM" ),

  DAY_NUMERIC_DATE( "Numeric date", "dd/MM" ),

  DAY_FULL_DATE( "Full date", "dd/MM/uuuu" ),

  WEEKDAY_INITIAL( "Initial", "EEEEE" ),

  WEEKDAY_SHORT( "Short", "EEE" ),

  WEEKDAY_FULL( "Full", "EEEE" ),

  WEEKDAY_WITH_DAY( "With day", "EEE d" ),

  WEEKDAY_WITH_DATE( "With date", "EEE d MMM" ),

  TIME_HOUR( "Hour", "H" ),

  TIME_HOUR_TWO_DIGITS( "Two-digit hour", "HH" ),

  TIME_HOUR_MINUTE( "Hour and minute", "HH:mm" ),

  TIME_WITH_SECONDS( "With seconds", "HH:mm:ss" ),

  TIME_WITH_MILLISECONDS( "With milli", "HH:mm:ss.SSS" ),

  DATE_TIME_COMPACT( "Compact", "d/M HH:mm" ),

  DATE_TIME_SHORT( "Short", "d MMM HH:mm" ),

  DATE_TIME_FULL( "Full", "dd/MM/uuuu HH:mm:ss" );

  private final String            m_text;      // descriptive preset name
  private final DateTimeFormatter m_formatter; // reusable formatter, or null for half-years

  /**************************************** constructor ******************************************/
  GanttAxisLabelFormat( String text, String pattern )
  {
    this( text, pattern == null ? null : DateTimeFormatter.ofPattern( pattern ) );
  }

  /**************************************** constructor ******************************************/
  GanttAxisLabelFormat( String text, DateTimeFormatter formatter )
  {
    m_text = text;
    m_formatter = formatter;
  }

  /****************************************** getText ********************************************/
  String getText()
  {
    return m_text;
  }

  /**************************************** isRelevantTo *****************************************/
  boolean isRelevantTo( GanttAxisInterval interval )
  {
    return switch ( this )
    {
      case YEAR_SHORT, YEAR_FULL -> interval == GanttAxisInterval.YEAR;
      case HALF_YEAR_SHORT, HALF_YEAR_WITH_YEAR -> interval == GanttAxisInterval.HALF_YEAR;
      case QUARTER_SHORT, QUARTER_WITH_YEAR -> interval == GanttAxisInterval.QUARTER;
      case MONTH_INITIAL, MONTH_TWO_DIGITS, MONTH_SHORT, MONTH_FULL, MONTH_WITH_YEAR -> interval == GanttAxisInterval.MONTH;
      case WEEK_NUMBER, WEEK_PREFIXED, WEEK_WITH_YEAR -> interval == GanttAxisInterval.WEEK;
      case DAY_NUMBER, DAY_WITH_MONTH -> interval == GanttAxisInterval.DAY || interval == GanttAxisInterval.WEEK;
      case DAY_TWO_DIGITS, DAY_NUMERIC_DATE, DAY_FULL_DATE, WEEKDAY_INITIAL, WEEKDAY_SHORT, WEEKDAY_FULL, WEEKDAY_WITH_DAY, WEEKDAY_WITH_DATE -> interval == GanttAxisInterval.DAY;
      case TIME_HOUR, TIME_HOUR_TWO_DIGITS -> interval == GanttAxisInterval.HALF_DAY
          || interval == GanttAxisInterval.SIX_HOURS || interval == GanttAxisInterval.THREE_HOURS
          || interval == GanttAxisInterval.HOUR;
      case TIME_HOUR_MINUTE, DATE_TIME_COMPACT, DATE_TIME_SHORT -> interval.isTimeInterval()
          && interval != GanttAxisInterval.SECOND;
      case TIME_WITH_SECONDS, TIME_WITH_MILLISECONDS -> interval == GanttAxisInterval.SECOND;
      case DATE_TIME_FULL -> interval.isTimeInterval();
    };
  }

  /******************************************* format ********************************************/
  String format( DateTime datetime )
  {
    // half-year presets use the stored calendar half without recompiling extended patterns
    if ( this == HALF_YEAR_SHORT || this == HALF_YEAR_WITH_YEAR )
    {
      String half = "H" + datetime.getDate().getHalfYear();
      return this == HALF_YEAR_SHORT ? half : YEAR_FULL.format( datetime ) + " " + half;
    }
    return datetime.format( m_formatter );
  }

  /*************************************** weekFormatter *****************************************/
  private static DateTimeFormatter weekFormatter( boolean prefix, boolean year )
  {
    // ISO week labels must agree with Monday-based interval boundaries in every locale
    var builder = new DateTimeFormatterBuilder();
    if ( prefix )
      builder.appendLiteral( 'W' );
    builder.appendValue( IsoFields.WEEK_OF_WEEK_BASED_YEAR );
    if ( year )
      builder.appendLiteral( ' ' ).appendValue( IsoFields.WEEK_BASED_YEAR );
    return builder.toFormatter();
  }
}
