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

import java.util.Arrays;
import java.util.Objects;

/*************************************************************************************************/
/******************* Allocates one resolved assignment against supplied capacity *****************/
/*************************************************************************************************/

final class AssignmentAllocator
{
  /***************************************** constructor *****************************************/
  private AssignmentAllocator()
  {
    // use static calculations without creating an assignment allocator
  }

  /**************************************** allocateUpTo *****************************************/
  // each slot represents one distinct resolved resource; return allocations in the same order.
  // maxQuantity is the aggregate maximum across all matching resources.
  static double[] allocateUpTo( double[] capacities, double maxQuantity )
  {
    // allocate one capped assignment fairly across matching capacities without changing the supplied array
    if ( !Double.isFinite( maxQuantity ) || maxQuantity < 0.0 )
      throw new IllegalArgumentException( "Assignment maximum must be finite and non-negative" );

    return allocateUpToInPlace( copyCapacities( capacities ), maxQuantity );
  }

  /************************************* allocateUpToInPlace *************************************/
  // callers supply a privately owned, validated capacity array and a finite, non-negative maximum.
  static double[] allocateUpToInPlace( double[] allocations, double maxQuantity )
  {
    // apply capped equal-share allocation to a privately owned, already validated capacity array
    if ( maxQuantity == 0.0 || allocations.length == 0 )
    {
      Arrays.fill( allocations, 0.0 );
      return allocations;
    }

    // one match needs neither a sorting copy nor equal-share arithmetic.
    if ( allocations.length == 1 )
    {
      allocations[0] = Math.min( allocations[0], maxQuantity );
      return allocations;
    }

    // sorting a separate copy makes the threshold independent of resource traversal order.
    double[] sortedCapacities = allocations.clone();
    Arrays.sort( sortedCapacities );

    // keep a conservative remaining maximum so floating-point rounding cannot invent capacity.
    double remainingQuantity = maxQuantity;
    for ( int index = 0; index < sortedCapacities.length; index++ )
    {
      int remainingMatchCount = sortedCapacities.length - index;
      double capacity = sortedCapacities[index];
      // integer match counts keep a non-zero exact difference at least one subnormal unit in size.
      // fused multiplication therefore preserves its sign without rounding a separate product.
      if ( Math.fma( capacity, remainingMatchCount, -remainingQuantity ) <= 0.0 )
      {
        // satisfy this capped resource and redistribute its shortfall among the remaining matches.
        // each pass removes one resource, so redistribution takes at most the number of matches.
        remainingQuantity = subtractDown( remainingQuantity, capacity );
        continue;
      }

      // divide only when uncapped matches need a common share, correcting an upward-rounded quotient.
      double share = remainingQuantity / remainingMatchCount;
      if ( Math.fma( share, remainingMatchCount, -remainingQuantity ) > 0.0 )
        share = Math.nextDown( share );

      // equal shares round down together; no resource gets a traversal-dependent rounding remainder.
      for ( int match = 0; match < allocations.length; match++ )
        allocations[match] = Math.min( allocations[match], share );
      return allocations;
    }

    // every capacity fits within the maximum, so the copied capacities are already the result.
    return allocations;
  }

  /**************************************** allocateAll ******************************************/
  static double[] allocateAll( double[] capacities )
  {
    // return an independent validated allocation of all supplied capacity
    // all-available has no aggregate maximum and still returns an independent result.
    return copyCapacities( capacities );
  }

  /**************************************** subtractDown *****************************************/
  // callers supply 0 <= allocation <= available; discard an upward-rounded residual, never create capacity.
  static double subtractDown( double available, double allocation )
  {
    // subtract allocation conservatively so an upward-rounded residual cannot create physical capacity
    double remaining = available - allocation;

    // recover the subtraction error with FastTwoSum because available is at least allocation.
    if ( ( available - remaining ) - allocation < 0.0 )
      remaining = Math.nextDown( remaining );
    return remaining;
  }

  /*************************************** copyCapacities ****************************************/
  private static double[] copyCapacities( double[] capacities )
  {
    // copy and validate every capacity before allocation can change its private working array
    // validate the complete copy, including when a zero maximum would otherwise skip calculation.
    double[] copy = Objects.requireNonNull( capacities, "capacities" ).clone();
    for ( double capacity : copy )
      if ( !Double.isFinite( capacity ) || capacity < 0.0 )
        throw new IllegalArgumentException( "Resource capacity must be finite and non-negative" );
    return copy;
  }
}
