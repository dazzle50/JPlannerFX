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
/*********************** Rounds task finishes to configured end boundaries ***********************/
/*************************************************************************************************/

final class EndRounding
{
  private static final LocalDate REFERENCE_DATE   = LocalDate.of( 2001, 1, 1 );                        // calendar grid origin
  private static final long      REFERENCE_MILLIS = REFERENCE_DATE.toEpochDay() * Time.MILLIS_PER_DAY; // fixed grid origin

  /***************************************** constructor *****************************************/
  private EndRounding()
  {
    // use static calculations without creating an end-rounding object
  }

  /******************************************* roundUp *******************************************/
  static DateTime roundUp( DateTime normalFinish, TimeSpan rounding )
  {
    // find the first configured finish boundary at or after normal finish, retaining absent rounding unchanged
    long finishMillis = Objects.requireNonNull( normalFinish, "normalFinish" ).toSerialMilliseconds();
    if ( rounding == null )
      return normalFinish;

    Unit unit = Objects.requireNonNull( rounding.getUnit(), "rounding unit" );
    double number = rounding.getNumber();
    int scale = unit == Unit.SECONDS ? 1 : 100;
    if ( !Double.isFinite( number ) || number <= 0.0 || number > Integer.MAX_VALUE / (double) scale )
      throw new IllegalArgumentException( "End rounding must be finite, positive and within the input range" );

    // reconstruct entered seconds/hundredths once, before converting either fixed or calendar units.
    long quantity = (long) Math.rint( number * scale );
    if ( quantity == 0 )
      throw new IllegalArgumentException( "End rounding must be at least one second or one hundredth of another unit" );

    long advanceMillis;
    if ( unit == Unit.MONTHS || unit == Unit.YEARS )
      advanceMillis = calculateMonthAdvanceMillis( finishMillis, unit == Unit.YEARS ? quantity * 12 : quantity );
    else
    {
      long intervalMillis = toIntervalMillis( quantity, unit );
      boolean subDay = intervalMillis < Time.MILLIS_PER_DAY;
      // use timeline positions so stored 24:00 and the following 00:00 share the same grid.
      long position = subDay ? Math.floorMod( finishMillis, Time.MILLIS_PER_DAY ) : finishMillis - REFERENCE_MILLIS;
      long remainder = Math.floorMod( position, intervalMillis );
      advanceMillis = remainder == 0 ? 0 : intervalMillis - remainder;

      // sub-day grids restart each date; the final interval can be shorter than the configured interval.
      if ( subDay )
        advanceMillis = Math.min( advanceMillis, Time.MILLIS_PER_DAY - position );
    }

    // enabled rounding uses ending-date notation without moving an exact midnight boundary.
    // the earliest stored midnight has no representable preceding date, so retain its 00:00 notation.
    if ( advanceMillis == 0 && normalFinish.getMillisecondsOfDay() == 0
        && normalFinish.getEpochDay() > DateTime.MIN_VALUE.getEpochDay() )
      return DateTime.of( normalFinish.getDate().plusDays( -1 ), Time.MAX_VALUE );

    // positive timeline addition supplies 24:00 at midnight and checks the final stored date range.
    return normalFinish.plusMilliseconds( advanceMillis );
  }

  /*************************************** toIntervalMillis **************************************/
  private static long toIntervalMillis( long quantity, Unit unit )
  {
    // convert a validated whole-second or hundredth-unit quantity to an exact fixed-length interval
    // quantities are validated; even the largest week interval fits in a long.
    long millisPerQuantity = switch ( unit )
    {
      case SECONDS -> Time.MILLIS_PER_SECOND;
      case MINUTES -> Time.MILLIS_PER_MINUTE / 100;
      case HOURS -> Time.MILLIS_PER_HOUR / 100;
      case DAYS -> Time.MILLIS_PER_DAY / 100;
      case WEEKS -> 7L * Time.MILLIS_PER_DAY / 100;
      default -> throw new IllegalArgumentException( "Expected a fixed-length end-rounding unit" );
    };
    return quantity * millisPerQuantity;
  }

  /********************************* calculateMonthAdvanceMillis *********************************/
  private static long calculateMonthAdvanceMillis( long finishMillis, long months100ths )
  {
    // calculate the forward offset to the first month-grid boundary at or after the finish
    // use the date before the first permissible midnight, including one day below compact storage.
    // this retains exact midnight boundaries rounded forward from the preceding month.
    LocalDate date = LocalDate.ofEpochDay( Math.floorDiv( finishMillis - 1, Time.MILLIS_PER_DAY ) );
    int monthLength = date.lengthOfMonth();
    long monthOffset = ( date.getYear() - (long) REFERENCE_DATE.getYear() ) * 12 + date.getMonthValue() - 1;

    // invert round(fraction * monthLength): reaching day d requires fraction * monthLength >= d - 0.5.
    // integer ceiling gives the first sufficient hundredth; duplicate boundary dates need no search.
    long minimumMonth100ths = monthOffset * 100 + ( date.getDayOfMonth() * 100L - 50 + monthLength - 1 ) / monthLength;
    long remainder = Math.floorMod( minimumMonth100ths, months100ths );
    long boundaryMonth100ths = minimumMonth100ths + ( remainder == 0 ? 0 : months100ths - remainder );

    // signed floor division counts forward within the actual month on both sides of the reference.
    // wider temporary anchors are valid; only the final timeline result must fit compact storage.
    LocalDate monthStart = REFERENCE_DATE.plusMonths( Math.floorDiv( boundaryMonth100ths, 100 ) );
    long fraction100ths = Math.floorMod( boundaryMonth100ths, 100 );
    long boundaryDay = monthStart.toEpochDay() + ( fraction100ths * monthStart.lengthOfMonth() + 50 ) / 100;
    return boundaryDay * Time.MILLIS_PER_DAY - finishMillis;
  }
}
