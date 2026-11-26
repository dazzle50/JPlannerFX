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

import java.util.Map;
import java.util.Objects;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.TaskTiming;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/******************** Entry points for preflight and bounded task-date staging *******************/
/*************************************************************************************************/

public final class Schedule
{
  /***************************************** constructor *****************************************/
  private Schedule()
  {
    // use static entry points without retaining calculation state
  }

  /****************************************** schedule *******************************************/
  // synchronous preflight only; callers must serialize model edits throughout preparation.
  public static void schedule( Plan plan )
  {
    // validate supported inputs without resetting retained results
    Utils.trace( "SCHEDULE PREFLIGHT begin" );
    try
    {
      var calculation = new TaskDateCalculation( plan );
      Utils.trace( "SCHEDULE PREFLIGHT complete", "tasks=", calculation.getTaskCount(), "applied=false",
          "calculation=not implemented" );
    }
    catch ( RuntimeException failure )
    {
      Utils.trace( "SCHEDULE PREFLIGHT failed", failure.getClass().getSimpleName(), failure.getMessage() );
      throw failure;
    }
  }

  /**************************************** stageTaskDates ***************************************/
  // internal date staging only; callers must serialize model edits throughout this calculation.
  static Map<Task, TaskTiming> stageTaskDates( Plan plan, DateTime limit )
  {
    // stage bounded task timing without changing live dates, statuses or allocations
    Utils.trace( "SCHEDULE DATES begin", "limit=", limit );
    try
    {
      Objects.requireNonNull( plan, "plan" );
      Objects.requireNonNull( limit, "limit" );
      var calculation = new TaskDateCalculation( plan );
      IllegalArgumentException failure = calculation.calculateTaskDates( limit );
      // retain the strict contract after considering independent work; expose no map on failure.
      if ( failure != null )
        throw failure;

      Map<Task, TaskTiming> timing = calculation.collectTaskDates();
      Utils.trace( "SCHEDULE DATES complete", "tasks=", timing.size(), "limit=", limit, "applied=false" );
      return timing;
    }
    catch ( RuntimeException failure )
    {
      Utils.trace( "SCHEDULE DATES failed", failure.getClass().getSimpleName(), failure.getMessage() );
      throw failure;
    }
  }

  /************************************ stagePartialTaskDates ************************************/
  // internal date staging only; callers must serialize model edits throughout this calculation.
  static Map<Task, TaskTiming> stagePartialTaskDates( Plan plan, DateTime limit )
  {
    // return reliable available dates and explanations without changing live results
    Utils.trace( "SCHEDULE PARTIAL DATES begin", "limit=", limit );
    try
    {
      Objects.requireNonNull( plan, "plan" );
      Objects.requireNonNull( limit, "limit" );
      var calculation = new TaskDateCalculation( plan );
      IllegalArgumentException failure = calculation.calculateTaskDates( limit );
      Map<Task, TaskTiming> timing = calculation.collectTaskDates();
      Utils.trace( failure == null ? "SCHEDULE PARTIAL DATES complete" : "SCHEDULE PARTIAL DATES partial", "tasks=",
          timing.size(), "limit=", limit, "applied=false" );
      return timing;
    }
    catch ( RuntimeException failure )
    {
      Utils.trace( "SCHEDULE PARTIAL DATES failed", failure.getClass().getSimpleName(), failure.getMessage() );
      throw failure;
    }
  }

}
