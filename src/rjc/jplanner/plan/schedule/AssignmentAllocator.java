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

import java.math.BigDecimal;
import java.math.MathContext;
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
  }

  /**************************************** allocateUpTo *****************************************/
  // each slot represents one distinct resolved resource; return allocations in the same order.
  // maxQuantity is the aggregate maximum across all matching resources.
  static double[] allocateUpTo( double[] capacities, double maxQuantity )
  {
    if ( !Double.isFinite( maxQuantity ) || maxQuantity < 0.0 )
      throw new IllegalArgumentException( "Assignment maximum must be finite and non-negative" );

    double[] allocations = copyCapacities( capacities );
    if ( maxQuantity == 0.0 || allocations.length == 0 )
    {
      Arrays.fill( allocations, 0.0 );
      return allocations;
    }

    // sorting a separate copy makes the threshold independent of resource traversal order.
    double[] sortedCapacities = allocations.clone();
    Arrays.sort( sortedCapacities );

    // exact binary-double values preserve small capacities when subtracting from a large maximum.
    BigDecimal remainingQuantity = new BigDecimal( maxQuantity );
    for ( int index = 0; index < sortedCapacities.length; index++ )
    {
      BigDecimal remainingMatchCount = BigDecimal.valueOf( sortedCapacities.length - index );
      BigDecimal capacity = new BigDecimal( sortedCapacities[index] );
      if ( capacity.multiply( remainingMatchCount ).compareTo( remainingQuantity ) <= 0 )
      {
        // satisfy this capped resource and redistribute its shortfall among the remaining matches.
        // each pass removes one resource, so redistribution takes at most the number of matches.
        remainingQuantity = remainingQuantity.subtract( capacity );
        continue;
      }

      // estimate the common share, then correct upward binary rounding using an exact comparison.
      // equal shares round down together; no resource gets a traversal-dependent rounding remainder.
      double share = remainingQuantity.divide( remainingMatchCount, MathContext.DECIMAL128 ).doubleValue();
      if ( new BigDecimal( share ).multiply( remainingMatchCount ).compareTo( remainingQuantity ) > 0 )
        share = Math.nextDown( share );

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
    // all-available has no aggregate maximum and still returns an independent result.
    return copyCapacities( capacities );
  }

  /*************************************** copyCapacities ****************************************/
  private static double[] copyCapacities( double[] capacities )
  {
    // validate the complete copy, including when a zero maximum would otherwise skip calculation.
    double[] copy = Objects.requireNonNull( capacities, "capacities" ).clone();
    for ( double capacity : copy )
      if ( !Double.isFinite( capacity ) || capacity < 0.0 )
        throw new IllegalArgumentException( "Resource capacity must be finite and non-negative" );
    return copy;
  }
}
