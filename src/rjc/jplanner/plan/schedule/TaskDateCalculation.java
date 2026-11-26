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
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************** Prepares supported tasks and calculates bounded task dates *****************/
/*************************************************************************************************/

final class TaskDateCalculation
{
  private static final int                       MAX_DATE_TASKS    = 1_000;     // task limit
  private static final long                      MAX_CALENDAR_WORK = 1_000_000; // visit limit

  private static final Comparator<TaskState>     STATE_ORDER       = Comparator
      .comparingInt( ( TaskState state ) -> state.m_priority ).reversed()
      .thenComparingInt( state -> state.m_taskNumber );                         // graph order

  private final DateTime                         m_planStart;                   // plan default start captured for this invocation
  private final Calendar                         m_calendar;                    // validated project calendar
  private final IdentityHashMap<Task, TaskState> m_statesByTask;                // task identity lookup for graph and dates
  private final List<TaskState>                  m_orderedTasks;                // dependency-ready priority order
  private final int                              m_maxPeriods;                  // largest referenced day-type period count
  private final long                             m_calendarSearches;            // bounded searches required by supported inputs
  private final long                             m_baseWork;                    // row, dependency and calendar configuration visits

  // dependency links, captured inputs and staged timing for one task in this invocation.
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
    private boolean              m_startValidated; // start satisfies this task's supported dependency bounds
    private long                 m_durationMillis; // working-millisecond target, or -1 for another duration basis
    private TaskTiming           m_timing;         // staged dates, never applied by date calculation
    private TaskFailure          m_failure;        // expected calculation failure or unavailable required bound

    /*************************************** constructor *****************************************/
    private TaskState( Task task, int index )
    {
      // capture task identity, list position and priority for one dependency-ordering pass
      m_task = task;
      m_taskNumber = index;
      m_priority = task.getPriority();
    }
  }

  // expected bounded calculation failures remain distinct from unexpected implementation errors.
  private static class TaskFailure extends IllegalArgumentException
  {
    private static final long serialVersionUID = 1L; // exception serialization version

    /*************************************** constructor *****************************************/
    private TaskFailure( String message )
    {
      super( message );
    }

    /*************************************** constructor *****************************************/
    private TaskFailure( String message, RuntimeException cause )
    {
      super( message, cause );
    }
  }

  /***************************************** constructor *****************************************/
  // one calculation per instance; callers must serialize model edits throughout preparation and calculation.
  TaskDateCalculation( Plan plan )
  {
    // prepare private working data without changing inputs or retained calculated results
    Objects.requireNonNull( plan, "plan" );

    // validate plan structure before allocating task states
    m_planStart = plan.getDefaultStart();
    if ( m_planStart == null )
      throw new IllegalArgumentException( "Plan default start is required" );
    Tasks tasks = plan.getTasks();
    if ( tasks.size() > MAX_CALENDAR_WORK )
      throw new IllegalArgumentException( "Too many plan rows for scheduling preflight" );
    tasks.buildHierarchy();
    int taskCount = tasks.getNotNullCount();
    if ( taskCount > MAX_DATE_TASKS )
      throw new IllegalArgumentException( "Scheduling preflight supports at most " + MAX_DATE_TASKS + " tasks" );

    // validate calendar ownership and build the dependency order once
    m_maxPeriods = validateProjectCalendar( plan );
    m_calendar = plan.getDefaultCalendar();
    m_statesByTask = new IdentityHashMap<>( taskCount );
    m_orderedTasks = orderTasks( tasks, taskCount, m_statesByTask );

    // capture supported inputs and count bounded calendar searches
    long searches = 0;
    long edges = 0;
    for ( TaskState state : m_orderedTasks )
    {
      try
      {
        searches += prepareTask( state );
      }
      catch ( IllegalArgumentException failure )
      {
        throw new IllegalArgumentException( "Task " + state.m_taskNumber + ": " + failure.getMessage(), failure );
      }
      var predecessors = state.m_task.getPredecessors();
      edges += predecessors == null ? 0 : predecessors.size();
    }

    // admit graph and calendar configuration work before any date searches
    m_calendarSearches = searches;
    m_baseWork = (long) tasks.size() + edges + plan.getDayTypes().size() + m_calendar.getNormals().size()
        + m_calendar.getExceptions().size();
    if ( m_baseWork > MAX_CALENDAR_WORK )
      throw new IllegalArgumentException(
          "Scheduling preflight workload exceeds " + MAX_CALENDAR_WORK + " estimated visits" );
  }

  /**************************************** getTaskCount *****************************************/
  int getTaskCount()
  {
    return m_orderedTasks.size();
  }

  /************************************* calculateTaskDates **************************************/
  // return the first expected failure, or null when every task completes.
  IllegalArgumentException calculateTaskDates( DateTime limit )
  {
    validateDateLimit( limit );
    TaskFailure firstFailure = null;
    for ( TaskState state : m_orderedTasks )
    {
      calculateTaskTiming( state, limit );
      // remember the first deterministic failure for strict callers without aborting independent tasks
      if ( firstFailure == null )
        firstFailure = state.m_failure;
    }
    return firstFailure;
  }

  /*************************************** collectTaskDates **************************************/
  // collect only after calculateTaskDates has considered every task without an unexpected error.
  Map<Task, TaskTiming> collectTaskDates()
  {
    // keep identity lookup so edits and row moves cannot rebind the staged dates to another task
    var timing = new IdentityHashMap<Task, TaskTiming>( m_orderedTasks.size() );
    for ( TaskState state : m_orderedTasks )
      timing.put( state.m_task, state.m_timing );
    return Collections.unmodifiableMap( timing );
  }

  /************************************** validateDateLimit **************************************/
  private void validateDateLimit( DateTime limit )
  {
    // keep horizon admission separate from input preflight; neither phase searches calendar dates
    long limitMillis = limit.toSerialMilliseconds();
    long earliestMillis = limitMillis;
    for ( TaskState state : m_orderedTasks )
    {
      DateTime start = state.m_enteredStart == null ? m_planStart : state.m_enteredStart;
      long startMillis = start.toSerialMilliseconds();
      if ( startMillis > limitMillis )
        throw new IllegalArgumentException(
            "Task " + state.m_taskNumber + ": Start " + start + " is after calculation limit " + limit );
      earliestMillis = Math.min( earliestMillis, startMillis );
    }

    // include a spare date for stored 24:00 starts and both period scans on a completion date.
    long days = Math.floorDiv( limitMillis, Time.MILLIS_PER_DAY ) - Math.floorDiv( earliestMillis, Time.MILLIS_PER_DAY )
        + 2;
    long periodWork = 2L * m_maxPeriods + 1;
    // divide the remaining budget before multiplying, including extremely distant calculation limits.
    if ( m_calendarSearches > 0 && days > ( MAX_CALENDAR_WORK - m_baseWork ) / m_calendarSearches / periodWork )
      throw new IllegalArgumentException( "Date calculation workload exceeds " + MAX_CALENDAR_WORK
          + " estimated visits; choose an earlier limit or fewer tasks" );
  }

  /*********************************** validateProjectCalendar ***********************************/
  private static int validateProjectCalendar( Plan plan )
  {
    // validate project-calendar ownership and structure, returning the largest work-period count
    Calendar calendar = plan.getDefaultCalendar();
    var calendars = plan.getCalendars();
    boolean ownedCalendar = false;
    // value equality cannot make a distinct calendar belong to this plan
    for ( int index = 0; calendar != null && index < calendars.size(); index++ )
      if ( calendars.get( index ) == calendar )
      {
        ownedCalendar = true;
        break;
      }
    if ( !ownedCalendar )
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
  private static int prepareTask( TaskState state )
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

    int searches = state.m_durationMillis > 0
        || state.m_duration.getUnit() == Unit.DAYS && state.m_duration.getNumber() > 0 ? 1 : 0;
    return searches + prepareDependencies( task );
  }

  /************************************* prepareDependencies *************************************/
  private static int prepareDependencies( Task task )
  {
    // validate supported dependencies and count their bounded calendar searches
    int searches = 0;
    var predecessors = task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      if ( ( dependency.type != DependencyType.FS && dependency.type != DependencyType.SS )
          || dependency.lag100ths < 0 )
        throw new IllegalArgumentException(
            "Only nonnegative FS and SS dependencies are supported in date calculation" );
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
  private void calculateTaskTiming( TaskState state, DateTime limit )
  {
    // preserve fixed inputs even if dependency validation fails
    // retain calculated boundaries only after their constraints validate
    DateTime start = state.m_enteredStart;
    DateTime normalFinish = null;
    DateTime finish = null;
    try
    {
      // assign each calculated boundary only after its complete phase validates
      start = calculateTaskStart( state, limit );
      state.m_startValidated = true;
      normalFinish = calculateNormalFinish( state, start, limit );
      finish = calculateRoundedFinish( state, normalFinish, limit );
    }
    catch ( TaskFailure failure )
    {
      state.m_failure = failure;
    }
    catch ( ArithmeticException failure )
    {
      state.m_failure = new TaskFailure( "Task " + state.m_taskNumber + ": date exceeds the stored range", failure );
    }

    // construct one immutable timing after the task stops; display text never controls scheduling
    String problem = state.m_failure == null ? null : state.m_failure.getMessage();
    state.m_timing = new TaskTiming( start, normalFinish, state.m_startValidated ? null : problem, problem, finish );
  }

  /************************************* calculateTaskStart **************************************/
  private DateTime calculateTaskStart( TaskState state, DateTime limit )
  {
    // keep an ASAP candidate private until every supported predecessor bound validates
    DateTime candidate = state.m_enteredStart == null ? m_planStart : state.m_enteredStart;
    var predecessors = state.m_task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      TaskState predecessor = m_statesByTask.get( dependency.task );
      if ( dependency.type == DependencyType.SS )
      {
        // an entered start may be invalid; later duration or rounding failures leave a validated start usable
        if ( !predecessor.m_startValidated )
          throw new TaskFailure( "Task " + state.m_taskNumber + ": predecessor start not validated for SS on task "
              + predecessor.m_taskNumber );
      }
      else
      {
        // FS needs scheduled F; a known start or normal finish cannot replace a missing rounded finish
        if ( predecessor.m_timing.getRoundedEnd() == null )
          throw new TaskFailure( "Task " + state.m_taskNumber + ": predecessor rounded end unavailable for FS on task "
              + predecessor.m_taskNumber );
      }
      DateTime bound = DependencyBounds.calculateRequiredDate( dependency, predecessor.m_timing.getStart(),
          predecessor.m_timing.getRoundedEnd(), m_calendar, limit );
      if ( bound == null )
        throw new TaskFailure( "Task " + state.m_taskNumber + ": " + dependency.type + " bound from task "
            + predecessor.m_taskNumber + " was not reached by calculation limit " + limit );
      if ( bound.toSerialMilliseconds() > candidate.toSerialMilliseconds() )
      {
        if ( state.m_enteredStart != null )
          throw new TaskFailure( "Task " + state.m_taskNumber + ": entered start " + state.m_enteredStart
              + " conflicts with " + dependency.type + " on task " + predecessor.m_taskNumber
              + "; required start is at or after " + bound );
        candidate = bound;
      }
    }
    return candidate;
  }

  /************************************ calculateNormalFinish ************************************/
  private DateTime calculateNormalFinish( TaskState state, DateTime start, DateTime limit )
  {
    // retain normal completion independently from the later rounding phase
    DateTime candidateFinish;
    if ( state.m_duration.getUnit() == Unit.DAYS )
    {
      // reconstruct entered hundredths once; day weights determine progress rather than a fixed hours-per-day value.
      int quantity100ths = (int) Math.rint( state.m_duration.getNumber() * 100 );
      try
      {
        candidateFinish = m_calendar.advanceEquivalentDays( start, quantity100ths, limit );
      }
      catch ( Calendar.EquivalentDayPrecisionException failure )
      {
        // keep this documented precision limit local without hiding invalid arguments or other implementation errors
        throw new TaskFailure( "Task " + state.m_taskNumber + ": " + failure.getMessage(), failure );
      }
    }
    else
      candidateFinish = state.m_durationMillis >= 0
          ? m_calendar.advanceWorkingMillis( start, state.m_durationMillis, Direction.FORWARD, limit )
          : ElapsedDateShift.shift( start, state.m_duration );
    if ( candidateFinish == null || candidateFinish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new TaskFailure(
          "Task " + state.m_taskNumber + ": duration target was not reached by calculation limit " + limit );
    return candidateFinish;
  }

  /*********************************** calculateRoundedFinish ************************************/
  private static DateTime calculateRoundedFinish( TaskState state, DateTime normalFinish, DateTime limit )
  {
    // a rounded finish becomes available to FS successors only after its horizon check succeeds
    DateTime roundedFinish = EndRounding.roundUp( normalFinish, state.m_rounding );
    if ( roundedFinish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new TaskFailure(
          "Task " + state.m_taskNumber + ": rounded finish " + roundedFinish + " is after calculation limit " + limit );
    return roundedFinish;
  }

  /****************************************** orderTasks *****************************************/
  private static List<TaskState> orderTasks( Tasks tasks, int taskCount, IdentityHashMap<Task, TaskState> statesByTask )
  {
    // order nonblank tasks by predecessor readiness, descending priority and ascending task number
    // graph readiness does not establish that the predecessor dates are available
    List<TaskState> states = createTaskStates( tasks, taskCount, statesByTask );
    linkTaskStates( states, statesByTask );

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

  /************************************** createTaskStates ***************************************/
  private static List<TaskState> createTaskStates( Tasks tasks, int taskCount,
      IdentityHashMap<Task, TaskState> statesByTask )
  {
    // create scheduling states for non-blank tasks, excluding the overall-project task
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

    return states;
  }

  /*************************************** linkTaskStates ****************************************/
  private static void linkTaskStates( List<TaskState> states, IdentityHashMap<Task, TaskState> statesByTask )
  {
    // build incoming counts and successor links from predecessor relationships
    // all states must exist before linking so predecessors can appear later in the task list
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
  }

}
