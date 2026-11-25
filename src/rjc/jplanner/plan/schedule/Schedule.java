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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.TimeSpan.Unit;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.days.DayType;
import rjc.jplanner.plan.days.DayType.Direction;
import rjc.jplanner.plan.tasks.Dependency.DependencyType;
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.TaskTiming;
import rjc.jplanner.plan.tasks.TaskType;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/************************ Orders tasks and stages bounded date calculations **********************/
/*************************************************************************************************/

public final class Schedule
{
  private static final int                   MAX_DATE_TASKS    = 1_000;     // task limit
  private static final long                  MAX_CALENDAR_WORK = 1_000_000; // visit limit

  private static final Comparator<TaskState> STATE_ORDER       = Comparator
      .comparingInt( ( TaskState state ) -> state.m_priority ).reversed()
      .thenComparingInt( state -> state.m_taskNumber );                     // graph order

  // dependency links, captured inputs and staged timing for one task in this calculation.
  private static class TaskState
  {
    private final Task           m_task;           // original task identity
    private final int            m_taskNumber;     // task number captured for this run
    private final int            m_priority;       // priority captured for this run
    private int                  m_incoming;       // predecessors still awaiting graph processing
    private ArrayList<TaskState> m_successors;     // tasks released by graph processing
    private TimeSpan             m_duration;       // validated duration copied for calculation
    private TimeSpan             m_rounding;       // validated optional end rounding
    private DateTime             m_enteredStart;   // fixed start, or null for ASAP
    private long                 m_durationMillis; // working-millisecond target, or -1 for another duration basis
    private TaskTiming           m_timing;         // staged dates, never applied by date calculation

    /*************************************** constructor *****************************************/
    private TaskState( Task task, int index )
    {
      // capture task identity, list position and priority for one dependency-ordering pass
      m_task = task;
      m_taskNumber = index;
      m_priority = task.getPriority();
    }
  }

  /***************************************** constructor *****************************************/
  private Schedule()
  {
    // use static entry points for task ordering and date staging
  }

  /****************************************** schedule *******************************************/
  public static void schedule( Plan plan )
  {
    // placeholder for scheduling the specified plan
    Utils.trace( "Plan scheduling is not implemented yet" );
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
      DateTime planStart = Objects.requireNonNull( plan.getDefaultStart(), "plan default start" );
      Tasks tasks = plan.getTasks();
      if ( tasks.size() > MAX_CALENDAR_WORK )
        throw new IllegalArgumentException( "Too many plan rows for date calculation" );
      tasks.buildHierarchy();
      int taskCount = tasks.getNotNullCount();
      if ( taskCount > MAX_DATE_TASKS )
        throw new IllegalArgumentException( "Date calculation supports at most " + MAX_DATE_TASKS + " tasks" );

      int maxPeriods = validateProjectCalendar( plan );
      Calendar calendar = plan.getDefaultCalendar();
      var statesByTask = new IdentityHashMap<Task, TaskState>( taskCount );
      var ordered = orderTasks( tasks, statesByTask );
      long searches = 0;
      long edges = 0;
      long earliestMillis = limit.toSerialMilliseconds();
      for ( TaskState state : ordered )
      {
        try
        {
          searches += prepareTask( state, planStart, limit );
        }
        catch ( IllegalArgumentException failure )
        {
          throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": " + failure.getMessage(), failure );
        }
        DateTime start = state.m_enteredStart == null ? planStart : state.m_enteredStart;
        earliestMillis = Math.min( earliestMillis, start.toSerialMilliseconds() );
        var predecessors = state.m_task.getPredecessors();
        edges += predecessors == null ? 0 : predecessors.size();
      }

      // include a spare date for stored 24:00 starts and both period scans on a completion date.
      long days = Math.floorDiv( limit.toSerialMilliseconds(), Time.MILLIS_PER_DAY )
          - Math.floorDiv( earliestMillis, Time.MILLIS_PER_DAY ) + 2;
      long baseWork = (long) tasks.size() + edges + plan.getDayTypes().size() + calendar.getNormals().size()
          + calendar.getExceptions().size();
      long periodWork = 2L * maxPeriods + 1;
      // divide the remaining budget before multiplying, including extremely distant calculation limits.
      if ( baseWork > MAX_CALENDAR_WORK
          || searches > 0 && days > ( MAX_CALENDAR_WORK - baseWork ) / searches / periodWork )
        throw new IllegalArgumentException( "Date calculation workload exceeds " + MAX_CALENDAR_WORK
            + " estimated visits; choose an earlier limit or fewer tasks" );

      // stage dates independently from live timing; return nothing if any task fails.
      for ( TaskState state : ordered )
        try
        {
          state.m_timing = calculateTaskTiming( state, statesByTask, calendar, planStart, limit );
        }
        catch ( ArithmeticException failure )
        {
          throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": date exceeds the stored range",
              failure );
        }

      // keep identity lookup so edits and row moves cannot rebind the staged dates to another task.
      var timing = new IdentityHashMap<Task, TaskTiming>( taskCount );
      for ( TaskState state : ordered )
        timing.put( state.m_task, state.m_timing );
      Utils.trace( "SCHEDULE DATES complete", "tasks=", taskCount, "limit=", limit, "applied=false" );
      return Collections.unmodifiableMap( timing );
    }
    catch ( RuntimeException failure )
    {
      Utils.trace( "SCHEDULE DATES failed", failure.getClass().getSimpleName(), failure.getMessage() );
      throw failure;
    }
  }

  /*********************************** validateProjectCalendar ***********************************/
  private static int validateProjectCalendar( Plan plan )
  {
    // validate project-calendar ownership and structure, returning the largest work-period count
    Calendar calendar = plan.getDefaultCalendar();
    if ( calendar == null || !plan.getCalendars().contains( calendar ) )
      throw new IllegalArgumentException( "Project default calendar is missing or does not belong to this plan" );
    if ( calendar.getAnchor() == null || calendar.getNormals().isEmpty() || calendar.getNormals().size() > 99 )
      throw new IllegalArgumentException( "Project default calendar has an invalid anchor or cycle" );
    if ( (long) plan.getDayTypes().size() + calendar.getNormals().size()
        + calendar.getExceptions().size() > MAX_CALENDAR_WORK )
      throw new IllegalArgumentException(
          "Project calendar configuration exceeds the date calculation workload limit" );

    var ownedDays = new IdentityHashMap<DayType, Boolean>();
    for ( DayType day : plan.getDayTypes() )
      if ( day != null )
        ownedDays.put( day, Boolean.TRUE );
    int maxPeriods = 0;
    for ( DayType day : calendar.getNormals() )
    {
      if ( day == null || !ownedDays.containsKey( day ) )
        throw new IllegalArgumentException( "Project calendar cycle has a missing or foreign day type" );
      maxPeriods = Math.max( maxPeriods, day.getWorkPeriods().size() );
    }
    for ( var exception : calendar.getExceptions().entrySet() )
    {
      DayType day = exception.getValue();
      if ( exception.getKey() == null || day == null || !ownedDays.containsKey( day ) )
        throw new IllegalArgumentException( "Project calendar exception has a missing date or foreign day type" );
      maxPeriods = Math.max( maxPeriods, day.getWorkPeriods().size() );
    }
    return maxPeriods;
  }

  /***************************************** prepareTask *****************************************/
  private static int prepareTask( TaskState state, DateTime planStart, DateTime limit )
  {
    // validate supported task inputs, capture calculation values and count bounded calendar searches
    Task task = state.m_task;
    if ( task.getIndent() != 0 )
      throw new IllegalArgumentException( "Outline tasks are not supported in date calculation" );
    if ( task.getTaskType() != TaskType.ASAP_FIXED_DURATION && task.getTaskType() != TaskType.START_ON_DURATION )
      throw new IllegalArgumentException(
          "Only ASAP-duration and Start-on-duration tasks are supported in date calculation" );
    if ( task.getValue( Task.FIELD.Resources.ordinal() ) != null )
      throw new IllegalArgumentException( "Resource assignments are not supported in date calculation" );
    if ( task.getPriority() < 0 || task.getPriority() > 999 )
      throw new IllegalArgumentException( "Priority must be between 0 and 999" );

    state.m_duration = TimeSpan.parse( task.getValue( Task.FIELD.Duration.ordinal() ) );
    if ( state.m_duration == null )
      throw new IllegalArgumentException( "Duration is required" );
    if ( state.m_duration.getNumber() == 0 )
      state.m_durationMillis = 0;
    else
      state.m_durationMillis = switch ( state.m_duration.getUnit() )
      {
        case SECONDS, MINUTES, HOURS -> IntervalProgress.toTargetMillis( state.m_duration );
        case DAYS, WEEKS, MONTHS, YEARS -> -1;
      };

    state.m_rounding = TimeSpan.parse( task.getValue( Task.FIELD.Rounding.ordinal() ) );
    if ( state.m_rounding != null && state.m_rounding.getNumber() <= 0 )
      throw new IllegalArgumentException( "End rounding must be positive or absent" );
    if ( task.getTaskType() == TaskType.START_ON_DURATION )
    {
      state.m_enteredStart = (DateTime) task.getValue( Task.FIELD.Start.ordinal() );
      if ( state.m_enteredStart == null )
        throw new IllegalArgumentException( "Start-on date is required" );
    }
    DateTime start = state.m_enteredStart == null ? planStart : state.m_enteredStart;
    if ( start.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException( "Start " + start + " is after calculation limit " + limit );

    int searches = state.m_durationMillis > 0
        || state.m_duration.getUnit() == Unit.DAYS && state.m_duration.getNumber() > 0 ? 1 : 0;
    var predecessors = task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      if ( dependency.type != DependencyType.FS || dependency.lag100ths < 0 )
        throw new IllegalArgumentException( "Only nonnegative FS dependencies are supported in date calculation" );
      if ( dependency.lagUnit == null || dependency.lagUnit == Unit.DAYS && dependency.lag100ths != 0 )
        throw new IllegalArgumentException(
            "Dependency lag unit is missing or needs unsupported equivalent-day advancement" );
      if ( dependency.lag100ths > 0 && ( dependency.lagUnit == Unit.SECONDS || dependency.lagUnit == Unit.MINUTES
          || dependency.lagUnit == Unit.HOURS ) )
        searches++;
    }
    return searches;
  }

  /************************************* calculateTaskTiming *************************************/
  private static TaskTiming calculateTaskTiming( TaskState state, IdentityHashMap<Task, TaskState> statesByTask,
      Calendar calendar, DateTime planStart, DateTime limit )
  {
    // calculate start, normal finish and rounded finish from the already staged predecessor dates
    DateTime start = state.m_enteredStart == null ? planStart : state.m_enteredStart;
    var predecessors = state.m_task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      TaskState predecessor = statesByTask.get( dependency.task );
      DateTime bound = DependencyBounds.calculateRequiredDate( dependency, predecessor.m_timing.getStart(),
          predecessor.m_timing.getRoundedEnd(), calendar, limit );
      if ( bound == null )
        throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": FS bound from task "
            + predecessor.m_taskNumber + " was not reached by calculation limit " + limit );
      if ( bound.toSerialMilliseconds() > start.toSerialMilliseconds() )
      {
        if ( state.m_enteredStart != null )
          throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": entered start " + start
              + " conflicts with FS on task " + predecessor.m_taskNumber + "; required start is at or after " + bound );
        start = bound;
      }
    }

    DateTime normalFinish;
    if ( state.m_duration.getUnit() == Unit.DAYS )
    {
      // reconstruct entered hundredths once; day weights determine progress rather than a fixed hours-per-day value.
      int quantity100ths = (int) Math.rint( state.m_duration.getNumber() * 100 );
      try
      {
        normalFinish = calendar.advanceEquivalentDays( start, quantity100ths, limit );
      }
      catch ( IllegalArgumentException failure )
      {
        throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": " + failure.getMessage(), failure );
      }
    }
    else
      normalFinish = state.m_durationMillis >= 0
          ? calendar.advanceWorkingMillis( start, state.m_durationMillis, Direction.FORWARD, limit )
          : ElapsedDateShift.shift( start, state.m_duration );
    if ( normalFinish == null || normalFinish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException(
          "Task " + state.m_taskNumber + ": duration target was not reached by calculation limit " + limit );
    DateTime finish = EndRounding.roundUp( normalFinish, state.m_rounding );
    if ( finish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException(
          "Task " + state.m_taskNumber + ": rounded finish " + finish + " is after calculation limit " + limit );
    return new TaskTiming( start, normalFinish, null, null, finish );
  }

  /****************************************** orderTasks *****************************************/
  private static List<TaskState> orderTasks( Tasks tasks, IdentityHashMap<Task, TaskState> statesByTask )
  {
    // order nonblank tasks by predecessor readiness, descending priority and ascending task number
    // create scheduling states for non-blank tasks, excluding the overall-project task
    int taskCount = tasks.getNotNullCount();
    var states = new ArrayList<TaskState>( taskCount );

    for ( int index = 1; index < tasks.size(); index++ )
    {
      Task task = tasks.get( index );
      if ( !task.isBlank() )
      {
        TaskState state = new TaskState( task, index );
        if ( task == tasks.get( 0 ) || statesByTask.putIfAbsent( task, state ) != null )
          throw new IllegalStateException( "Task object is repeated at row " + index );
        states.add( state );
      }
    }

    // build incoming counts and successor links from predecessor relationships
    for ( TaskState state : states )
    {
      var predecessors = state.m_task.getPredecessors();
      if ( predecessors == null )
        continue;

      for ( int index = 0; index < predecessors.size(); index++ )
      {
        var dependency = predecessors.get( index );
        TaskState predecessor = dependency == null ? null : statesByTask.get( dependency.task );
        if ( predecessor == null )
          throw new IllegalStateException( "Invalid predecessor for task " + state.m_taskNumber );

        // allocate successor storage only for tasks that need it
        if ( predecessor.m_successors == null )
          predecessor.m_successors = new ArrayList<>( 2 );
        predecessor.m_successors.add( state );
        state.m_incoming++;
      }
    }

    // queue graph-ready tasks by descending priority and then ascending task index
    var ready = new PriorityQueue<TaskState>( Math.max( 1, taskCount ), STATE_ORDER );
    for ( TaskState state : states )
      if ( state.m_incoming == 0 )
        ready.add( state );

    // remove each best graph-ready task and release its successors
    var ordered = new ArrayList<TaskState>( taskCount );
    while ( !ready.isEmpty() )
    {
      TaskState state = ready.remove();
      ordered.add( state );

      if ( state.m_successors != null )
        for ( TaskState successor : state.m_successors )
          if ( --successor.m_incoming == 0 )
            ready.add( successor );
    }

    // any unprocessed tasks form a circular dependency
    if ( ordered.size() != taskCount )
      throw new IllegalStateException( "Circular task dependency" );

    return ordered;
  }

}
