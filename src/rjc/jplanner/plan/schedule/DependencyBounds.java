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

import java.util.Objects;

import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.days.DayType.Direction;
import rjc.jplanner.plan.tasks.Dependency;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************** Calculates a dependency bound from supplied predecessor dates **************/
/*************************************************************************************************/

final class DependencyBounds
{
  /***************************************** constructor *****************************************/
  private DependencyBounds()
  {
    // use static calculations without creating a dependency-bound object
  }

  /************************************ calculateRequiredDate ************************************/
  static DateTime calculateRequiredDate( Dependency dependency, DateTime predecessorStart, DateTime predecessorFinish,
      Calendar projectCalendar, DateTime limit )
  {
    // calculate the successor start or finish bound from the required predecessor boundary and lag
    Objects.requireNonNull( dependency, "dependency" );
    var type = Objects.requireNonNull( dependency.type, "dependency type" );
    var unit = Objects.requireNonNull( dependency.lagUnit, "lag unit" );
    Objects.requireNonNull( projectCalendar, "projectCalendar" );
    long limitMillis = Objects.requireNonNull( limit, "limit" ).toSerialMilliseconds();

    // use only the required supplied date; start-based links need no calculated predecessor finish.
    // finish-based links use scheduled F, including end rounding, rather than resource-release F0.
    DateTime from = Objects.requireNonNull( switch ( type )
    {
      case FS, FF -> predecessorFinish;
      case SS, SF -> predecessorStart;
    }, "predecessor boundary" );
    int lag100ths = dependency.lag100ths;
    if ( lag100ths == 0 )
      return from;

    boolean forward = lag100ths > 0;
    if ( forward ? limitMillis < from.toSerialMilliseconds() : limitMillis > from.toSerialMilliseconds() )
      throw new IllegalArgumentException( "Limit is opposite to the dependency lag direction" );

    return switch ( unit )
    {
      case SECONDS, MINUTES, HOURS -> {
        int millisPerHundredth = switch ( unit )
        {
          case SECONDS -> Time.MILLIS_PER_SECOND / 100;
          case MINUTES -> Time.MILLIS_PER_MINUTE / 100;
          case HOURS -> Time.MILLIS_PER_HOUR / 100;
          default -> throw new IllegalArgumentException( "Expected a working-time lag unit" );
        };
        // promote before taking the magnitude so Integer.MIN_VALUE cannot overflow.
        long amount = Math.abs( (long) lag100ths ) * millisPerHundredth;
        yield projectCalendar.advanceWorkingMillis( from, amount, forward ? Direction.FORWARD : Direction.BACKWARD,
            limit );
      }
      case WEEKS, MONTHS, YEARS -> {
        DateTime bound = ElapsedDateShift.shift( from, lag100ths, unit );
        // compare timeline positions so equivalent 24:00/00:00 limits are inclusive.
        boolean beyondLimit = forward ? bound.toSerialMilliseconds() > limitMillis
            : bound.toSerialMilliseconds() < limitMillis;
        yield beyondLimit ? null : bound;
      }
      case DAYS -> throw new IllegalArgumentException( "Nonzero equivalent-day dependency lag is not implemented" );
    };
  }
}
