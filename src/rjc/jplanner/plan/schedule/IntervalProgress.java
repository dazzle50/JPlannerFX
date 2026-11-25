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
  static final class Measurement
  {
    private final long     m_durationMillis;     // project working milliseconds
    private final double[] m_resourceWorkMillis; // privately owned resource contributions
    private final double   m_workMillis;         // compensated total of resource contributions

    /*************************************** constructor *****************************************/
    private Measurement( long durationMillis, double[] resourceWorkMillis, double workMillis )
    {
      // retain measured duration, private per-resource credits and their compensated total
      m_durationMillis = durationMillis;
      m_resourceWorkMillis = resourceWorkMillis;
      m_workMillis = workMillis;
    }

    /************************************* getDurationMillis *************************************/
    long getDurationMillis()
    {
      // return measured project-working milliseconds, counted once independently of resource quantity
      return m_durationMillis;
    }

    /************************************** getResourceCount *************************************/
    int getResourceCount()
    {
      // return the number of resource credits in the original allocation order
      return m_resourceWorkMillis.length;
    }

    /*********************************** getResourceWorkMillis ***********************************/
    double getResourceWorkMillis( int index )
    {
      // return the measured resource-millisecond credit at this allocation position
      return m_resourceWorkMillis[index];
    }

    /*************************************** getWorkMillis ***************************************/
    double getWorkMillis()
    {
      // return credited resource-milliseconds without passing calculated work through input rounding
      // defer aggregate overflow until the total is requested; individual resource work remains usable.
      if ( !Double.isFinite( m_workMillis ) )
        throw new ArithmeticException( "Total work exceeds the supported double range" );
      return m_workMillis;
    }
  }

  // completion is an offset from the supplied interval start; work is for the sole resource.
  static final class WorkCompletion
  {
    private final long   m_completionOffsetMillis; // represented completion offset, or -1 when not reached
    private final double m_workMillis;             // credited resource-milliseconds, clipped at completion

    /*************************************** constructor *****************************************/
    private WorkCompletion( long completionMillis, double workMillis )
    {
      // retain the completion offset or -1 together with the credited work for the sole resource
      m_completionOffsetMillis = completionMillis;
      m_workMillis = workMillis;
    }

    /********************************* getCompletionOffsetMillis *********************************/
    long getCompletionOffsetMillis()
    {
      // return the completion offset from interval start, or -1 when the target was not reached
      return m_completionOffsetMillis;
    }

    /*************************************** getWorkMillis ***************************************/
    double getWorkMillis()
    {
      // return credited resource-milliseconds without passing calculated work through input rounding
      return m_workMillis;
    }
  }

  /***************************************** constructor *****************************************/
  private IntervalProgress()
  {
    // use static measurements without retaining progress between supplied intervals
  }

  /**************************************** toTargetMillis ***************************************/
  // convert entered seconds/hundredths as integers; calculated work never passes through input rounding.
  static long toTargetMillis( TimeSpan target )
  {
    // convert an entered target in seconds, minutes or hours to exact whole-millisecond units
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

  /*************************************** measureInterval ***************************************/
  // callers supply [start, end) with unchanged calendar state and allocations after resource restrictions.
  // this measures the whole interval; target clipping and resource-release dates are calculated separately.
  static Measurement measureInterval( long elapsedMillis, boolean projectWorking, double[] allocations )
  {
    // measure duration and per-resource work across the entire supplied interval with constant allocations
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

    return new Measurement( projectWorking ? elapsedMillis : 0, work, sum + correction );
  }

  /*********************************** measureToDurationTarget ***********************************/
  // targetDurationMillis is the remaining project working duration at the supplied interval start.
  // exact durationMillis equality with that target establishes completion at the same offset, including zero.
  static Measurement measureToDurationTarget( long elapsedMillis, long targetDurationMillis, boolean projectWorking,
      double[] allocations )
  {
    // measure interval progress clipped at the remaining project-working-duration target
    // validate the original interval before clipping so a short target cannot hide an invalid elapsed value.
    if ( elapsedMillis < 0 || elapsedMillis > MAX_EXACT_MILLIS )
      throw new IllegalArgumentException( "Elapsed milliseconds must be non-negative and exactly representable" );
    if ( targetDurationMillis < 0 || targetDurationMillis > MAX_EXACT_MILLIS )
      throw new IllegalArgumentException( "Duration target must be non-negative and exactly representable" );

    // instantaneous tasks receive no work; positive targets allow resource work throughout project-calendar gaps.
    long productiveMillis = elapsedMillis;
    if ( targetDurationMillis == 0 )
      productiveMillis = 0;
    else if ( projectWorking )
      productiveMillis = Math.min( elapsedMillis, targetDurationMillis );

    // calculation validates every allocation even when clipping leaves no productive time.
    return measureInterval( productiveMillis, projectWorking, allocations );
  }

  /************************************* measureToWorkTarget *************************************/
  // one constant allocation and an integer work target at the interval start; no accumulated remainder.
  // completionMillis determines completion; rounded incomplete work can already equal the target.
  static WorkCompletion measureToWorkTarget( long elapsedMillis, long targetWorkMillis, double allocation )
  {
    // measure one resource up to its remaining work target and locate the first sufficient millisecond
    // validate all inputs before handling zero targets or intervals.
    if ( elapsedMillis < 0 || elapsedMillis > MAX_EXACT_MILLIS )
      throw new IllegalArgumentException( "Elapsed milliseconds must be non-negative and exactly representable" );
    if ( targetWorkMillis < 0 || targetWorkMillis > MAX_EXACT_MILLIS )
      throw new IllegalArgumentException( "Work target must be non-negative and exactly representable" );
    if ( !Double.isFinite( allocation ) || allocation < 0.0 )
      throw new IllegalArgumentException( "Allocation must be finite and non-negative" );

    if ( targetWorkMillis == 0 )
      return new WorkCompletion( 0, 0.0 );

    // integer milliseconds make a non-zero exact residual at least one subnormal unit in magnitude.
    // fused rounding preserves its sign even when a separately rounded product equals the target.
    // check the interval end first so division cannot overflow or produce an out-of-range candidate.
    if ( Math.fma( allocation, elapsedMillis, -targetWorkMillis ) < 0.0 )
      return new WorkCompletion( -1, allocation * elapsedMillis );

    long completionMillis = (long) Math.ceil( targetWorkMillis / allocation );
    // all integer boundaries through 2^53 are exact doubles; quotient rounding can be at most one millisecond early.
    if ( Math.fma( allocation, completionMillis, -targetWorkMillis ) < 0.0 )
      completionMillis++;

    // credit the exact target; callers retain allocation until F0 without adding productive work.
    return new WorkCompletion( completionMillis, targetWorkMillis );
  }
}
