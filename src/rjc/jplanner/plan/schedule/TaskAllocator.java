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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*************************************************************************************************/
/******************** Allocates eligible tasks in priority and task-number order *****************/
/*************************************************************************************************/

final class TaskAllocator
{
  private static final Comparator<Request> REQUEST_ORDER = Comparator.comparingInt( Request::priority ).reversed()
      .thenComparingInt( Request::taskNumber ); // allocation precedence within this calculation

  // one assignment per task, with supplied eligibility and distinct indexes into the capacity array.
  record Request( int taskNumber, int priority, boolean eligible, boolean allAvailable, double maxQuantity,
      List<Integer> resourceIndexes )
  {
    /*************************************** constructor *****************************************/
    Request
    {
      if ( taskNumber <= 0 )
        throw new IllegalArgumentException( "Task number must be positive" );
      if ( priority < 0 || priority > 999 )
        throw new IllegalArgumentException( "Task priority must be between 0 and 999" );
      if ( !Double.isFinite( maxQuantity ) || maxQuantity < 0.0 )
        throw new IllegalArgumentException( "Assignment maximum must be finite and non-negative" );

      resourceIndexes = List.copyOf( resourceIndexes );
      if ( new HashSet<>( resourceIndexes ).size() != resourceIndexes.size() )
        throw new IllegalArgumentException( "An assignment cannot match the same resource twice" );
    }
  }

  /***************************************** constructor *****************************************/
  private TaskAllocator()
  {
  }

  /****************************************** allocate *******************************************/
  // each result row follows the supplied request order, with quantities in that request's match order.
  // task numbers determine precedence for this calculation; they are not persistent task identity.
  static double[][] allocate( double[] capacities, List<Request> requests )
  {
    double[] remainingCapacities = AssignmentAllocator.allocateAll( capacities );
    Request[] orderedRequests = List.copyOf( requests ).toArray( new Request[0] );
    var indexesByTaskNumber = new HashMap<Integer, Integer>();

    // validate every reference before allocating, including requests that are currently ineligible.
    for ( int index = 0; index < orderedRequests.length; index++ )
    {
      Request request = orderedRequests[index];
      if ( indexesByTaskNumber.putIfAbsent( request.taskNumber(), index ) != null )
        throw new IllegalArgumentException( "Duplicate task number: " + request.taskNumber() );
      for ( int resourceIndex : request.resourceIndexes() )
        if ( resourceIndex < 0 || resourceIndex >= remainingCapacities.length )
          throw new IllegalArgumentException(
              "Invalid resource index for task " + request.taskNumber() + ": " + resourceIndex );
    }

    Arrays.sort( orderedRequests, REQUEST_ORDER );
    double[][] allocations = new double[orderedRequests.length][];
    for ( Request request : orderedRequests )
    {
      int requestIndex = indexesByTaskNumber.get( request.taskNumber() );
      var resourceIndexes = request.resourceIndexes();
      double[] matchedCapacities = new double[resourceIndexes.size()];
      if ( !request.eligible() )
      {
        allocations[requestIndex] = matchedCapacities;
        continue;
      }

      for ( int match = 0; match < resourceIndexes.size(); match++ )
        matchedCapacities[match] = remainingCapacities[resourceIndexes.get( match )];

      // capacities and maxima are already validated; this fresh array belongs only to this result row.
      double[] taskAllocation = request.allAvailable() ? matchedCapacities
          : AssignmentAllocator.allocateUpToInPlace( matchedCapacities, request.maxQuantity() );
      allocations[requestIndex] = taskAllocation;

      // subtract each distinct match conservatively; later requests cannot revise an earlier allocation.
      for ( int match = 0; match < resourceIndexes.size(); match++ )
      {
        int resourceIndex = resourceIndexes.get( match );
        remainingCapacities[resourceIndex] = AssignmentAllocator.subtractDown( remainingCapacities[resourceIndex],
            taskAllocation[match] );
      }
    }
    return allocations;
  }
}
