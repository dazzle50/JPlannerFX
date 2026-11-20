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

import rjc.jplanner.plan.TimeSpan;

/*************************************************************************************************/
/******************* Measures duration and resource work over a supplied interval ****************/
/*************************************************************************************************/

final class IntervalProgress
{
  private static final long MAX_EXACT_MILLIS = 1L << 53; // largest exact whole-millisecond double value

  // work uses resource-milliseconds; entries follow the supplied allocation order.
  static final class Result
  {
    private final long     m_durationMillis;     // project working milliseconds
    private final double[] m_resourceWorkMillis; // privately owned resource contributions
    private final double   m_workMillis;         // compensated total of resource contributions

    /*************************************** constructor *****************************************/
    private Result( long durationMillis, double[] resourceWorkMillis, double workMillis )
    {
      m_durationMillis = durationMillis;
      m_resourceWorkMillis = resourceWorkMillis;
      m_workMillis = workMillis;
    }

    /************************************** durationMillis ***************************************/
    long durationMillis()
    {
      return m_durationMillis;
    }

    /*************************************** resourceCount ***************************************/
    int resourceCount()
    {
      return m_resourceWorkMillis.length;
    }

    /************************************ resourceWorkMillis *************************************/
    double resourceWorkMillis( int index )
    {
      return m_resourceWorkMillis[index];
    }

    /**************************************** workMillis *****************************************/
    double workMillis()
    {
      // defer aggregate overflow until the total is requested; individual resource work remains usable.
      if ( !Double.isFinite( m_workMillis ) )
        throw new ArithmeticException( "Total work exceeds the supported double range" );
      return m_workMillis;
    }
  }

  /***************************************** constructor *****************************************/
  private IntervalProgress()
  {
  }

  /**************************************** targetMillis *****************************************/
  // convert entered seconds/hundredths as integers; calculated work never passes through input rounding.
  static long targetMillis( TimeSpan target )
  {
    double number = Objects.requireNonNull( target, "target" ).getNumber();
    if ( !Double.isFinite( number ) || number < 0.0 )
      throw new IllegalArgumentException( "Target must be finite and non-negative" );

    int factor = switch ( Objects.requireNonNull( target.getUnit(), "target unit" ) )
    {
      case SECONDS -> 1_000;
      case MINUTES -> 60_000;
      case HOURS -> 3_600_000;
      default -> throw new IllegalArgumentException( "Target conversion supports seconds, minutes and hours only" );
    };

    int scale = target.getUnit() == TimeSpan.Unit.SECONDS ? 1 : 100;
    double units = Math.rint( number * scale );
    int millisPerUnit = factor / scale;
    if ( units > MAX_EXACT_MILLIS / millisPerUnit )
      throw new IllegalArgumentException( "Target exceeds the supported millisecond range" );
    return (long) units * millisPerUnit;
  }

  /***************************************** calculate *******************************************/
  // callers supply [start, end) with unchanged calendar state and allocations after resource restrictions.
  // this measures the whole interval; target clipping and resource-release dates are calculated separately.
  static Result calculate( long elapsedMillis, boolean projectWorking, double[] allocations )
  {
    Objects.requireNonNull( allocations, "allocations" );
    if ( elapsedMillis < 0 || elapsedMillis > MAX_EXACT_MILLIS )
      throw new IllegalArgumentException( "Elapsed milliseconds must be non-negative and exactly representable" );

    double[] work = new double[allocations.length];
    double sum = 0.0, correction = 0.0;
    for ( int resource = 0; resource < allocations.length; resource++ )
    {
      double allocation = allocations[resource];
      // validate even empty intervals and project-calendar gaps; resources have independent work clocks.
      if ( !Double.isFinite( allocation ) || allocation < 0.0 )
        throw new IllegalArgumentException( "Allocation must be finite and non-negative" );

      work[resource] = allocation * elapsedMillis;
      if ( !Double.isFinite( work[resource] ) )
        throw new ArithmeticException( "Resource work exceeds the supported double range" );

      // compensate in the calculation loop so small contributions survive without another array scan.
      double contribution = work[resource];
      double next = sum + contribution;
      correction += sum >= contribution ? ( sum - next ) + contribution : ( contribution - next ) + sum;
      sum = next;
    }

    return new Result( projectWorking ? elapsedMillis : 0, work, sum + correction );
  }
}
