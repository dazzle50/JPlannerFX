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

package rjc.jplanner.plan.schedule;

import java.time.LocalDate;
import java.util.Objects;

import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.TimeSpan.Unit;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************** Shifts elapsed calendar duration and dependency boundaries *****************/
/*************************************************************************************************/

final class CalendarShift
{
  /***************************************** constructor *****************************************/
  private CalendarShift()
  {
  }

  /******************************************** shift ********************************************/
  // reconstruct entered hundredths once; signed dependency quantities use the integer overload directly.
  static DateTime shift( DateTime from, TimeSpan duration )
  {
    double number = Objects.requireNonNull( duration, "duration" ).getNumber();
    if ( !Double.isFinite( number ) || number < 0.0 || number > Integer.MAX_VALUE / 100.0 )
      throw new IllegalArgumentException( "Calendar duration must be finite, non-negative and within the input range" );

    return shift( from, (int) Math.rint( number * 100.0 ), duration.getUnit() );
  }

  /******************************************** shift ********************************************/
  // only elapsed weeks/months/years belong here; project equivalent days and optional end rounding are separate.
  static DateTime shift( DateTime from, int quantity100ths, Unit unit )
  {
    Objects.requireNonNull( from, "from" );
    // validate units even for zero quantities; promote before scaling so signed integer extrema cannot wrap.
    return switch ( Objects.requireNonNull( unit, "unit" ) )
    {
      case WEEKS -> {
        long days = roundDays( (long) quantity100ths * 7 );
        yield from.plusMilliseconds( Math.multiplyExact( days, Time.MILLIS_PER_DAY ) );
      }
      case MONTHS -> shiftMonths( from, quantity100ths );
      case YEARS -> shiftMonths( from, (long) quantity100ths * 12 );
      default -> throw new IllegalArgumentException( "Calendar shifting supports weeks, months and years only" );
    };
  }

  /***************************************** shiftMonths *****************************************/
  private static DateTime shiftMonths( DateTime from, long months100ths )
  {
    if ( months100ths == 0 )
      return from;

    // anchor equivalent 24:00/00:00 boundaries to the same date, including the one-past-maximum midnight.
    long originalDay = Math.floorDiv( from.toSerialMilliseconds(), Time.MILLIS_PER_DAY );
    LocalDate original = LocalDate.ofEpochDay( originalDay );
    long wholeMonths = months100ths / 100;
    long fraction100ths = months100ths % 100;
    long baseDay = original.plusMonths( wholeMonths ).toEpochDay();
    long resultDay = baseDay;

    if ( fraction100ths != 0 )
    {
      // shift both boundaries from the original date; negative fractions travel toward the preceding month.
      // wider temporary dates avoid rejecting a valid result because its interpolation boundary is out of storage.
      long adjacentDay = original.plusMonths( wholeMonths + Long.signum( fraction100ths ) ).toEpochDay();
      resultDay += roundDays( Math.abs( fraction100ths ) * ( adjacentDay - baseDay ) );
    }

    // apply the final whole-day displacement on the timeline, retaining milliseconds and checked storage limits.
    return from.plusMilliseconds( Math.multiplyExact( resultDay - originalDay, Time.MILLIS_PER_DAY ) );
  }

  /****************************************** roundDays ******************************************/
  private static long roundDays( long days100ths )
  {
    // numerators are bounded by signed integer inputs; truncation with a signed half rounds ties away from zero.
    return ( days100ths + ( days100ths < 0 ? -50 : 50 ) ) / 100;
  }
}
