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
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
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
import rjc.jplanner.plan.tasks.TaskType;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/************************ Orders tasks and calculates bounded date previews **********************/
/*************************************************************************************************/

public class Schedule
{
  private static final int              MAX_PREVIEW_TASKS = 1_000;                                               // nonblank tasks admitted to a date preview
  private static final long             MAX_CALENDAR_WORK = 1_000_000;                                           // estimated graph, date and period visits
  private static final Comparator<Node> NODE_ORDER        = Comparator.comparingLong( node -> node.m_orderKey ); // graph order

  private static class Node
  {
    private final Task                m_task;           // original task identity
    private final int                 m_index;          // task number captured for this run
    private final long                m_orderKey;       // captured priority and task number
    private int                       m_incoming;       // predecessors still awaiting graph processing
    private ArrayList<Node>           m_successors;     // tasks released by graph processing
    private TimeSpan                  m_duration;       // validated duration copied for the preview
    private TimeSpan                  m_rounding;       // validated optional end rounding
    private DateTime                  m_enteredStart;   // fixed start, or null for ASAP
    private long                      m_durationMillis; // working target, or -1 for elapsed calendar duration
    private SchedulePreview.TaskDates m_dates;          // completed preview row

    /*************************************** constructor *****************************************/
    private Node( Task task, int index )
    {
      m_task = task;
      m_index = index;
      m_orderKey = ( (long) ( 999 - task.getPriority() ) << 32 ) | index;
    }

    /*************************************** addSuccessor ****************************************/
    private void addSuccessor( Node successor )
    {
      // allocate successor storage only for tasks that need it
      if ( m_successors == null )
        m_successors = new ArrayList<>( 2 );

      m_successors.add( successor );
    }
  }

  /****************************************** schedule *******************************************/
  public static void schedule( Plan plan )
  {
    // trace graph order; dated previews require an explicit calculation limit.
    Utils.trace( "~~~~~~~~~~~~~~~~ SCHEDULE ~~~~~~~~~~~~~~~~" );
    var list = getOrderedTasks( plan.getTasks(), new IdentityHashMap<>() );

    Utils.trace( list.stream().map( node -> node.m_task ).toList() );
  }

  /****************************************** schedule *******************************************/
  public static SchedulePreview schedule( Plan plan, DateTime limit )
  {
    Utils.trace( "SCHEDULE PREVIEW begin", "limit=", limit );
    try
    {
      Objects.requireNonNull( plan, "plan" );
      Objects.requireNonNull( limit, "limit" );
      DateTime planStart = Objects.requireNonNull( plan.getDefaultStart(), "plan default start" );
      Tasks tasks = plan.getTasks();
      if ( tasks.size() > MAX_CALENDAR_WORK )
        throw new IllegalArgumentException( "Too many plan rows for this preview" );
      tasks.buildHierarchy();
      int taskCount = tasks.getNotNullCount();
      Utils.trace( "SCHEDULE PREVIEW inputs", "tasks=", taskCount, "planStart=", planStart );
      if ( taskCount > MAX_PREVIEW_TASKS )
        throw new IllegalArgumentException( "Preview supports at most " + MAX_PREVIEW_TASKS + " tasks" );

      int maxPeriods = validateCalendar( plan );
      Calendar calendar = plan.getDefaultCalendar();
      var nodesByTask = new IdentityHashMap<Task, Node>( taskCount );
      var ordered = getOrderedTasks( tasks, nodesByTask );
      long searches = 0;
      long edges = 0;
      long earliestMillis = limit.toSerialMilliseconds();
      for ( Node node : ordered )
      {
        try
        {
          searches += validateTask( node, planStart, limit );
        }
        catch ( IllegalArgumentException failure )
        {
          throw new IllegalArgumentException( "Task " + node.m_index + ": " + failure.getMessage(), failure );
        }
        DateTime start = node.m_enteredStart == null ? planStart : node.m_enteredStart;
        earliestMillis = Math.min( earliestMillis, start.toSerialMilliseconds() );
        var predecessors = node.m_task.getPredecessors();
        edges += predecessors == null ? 0 : predecessors.size();
        Utils.trace( "SCHEDULE PREVIEW task", node.m_index, "title=",
            node.m_task.getValue( Task.FIELD.Title.ordinal() ), "type=", node.m_task.getTaskType(), "duration=",
            node.m_duration, "enteredStart=", node.m_enteredStart, "rounding=", node.m_rounding );
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
      {
        Utils.trace( "SCHEDULE PREVIEW workload rejected", "searches=", searches, "days=", days, "maxPeriods=",
            maxPeriods, "baseVisits=", baseWork, "budget=", MAX_CALENDAR_WORK );
        throw new IllegalArgumentException( "Preview calendar workload exceeds " + MAX_CALENDAR_WORK
            + " estimated visits; choose an earlier limit or fewer tasks" );
      }
      long estimatedWork = baseWork + searches * days * periodWork;
      Utils.trace( "SCHEDULE PREVIEW workload", "searches=", searches, "days=", days, "estimatedVisits=",
          estimatedWork );

      // calculate independently from the live task fields; publish no partial preview if a task fails.
      for ( Node node : ordered )
        try
        {
          node.m_dates = calculateTask( node, nodesByTask, calendar, planStart, limit );
        }
        catch ( ArithmeticException failure )
        {
          throw new IllegalArgumentException( "Task " + node.m_index + ": date exceeds the stored range", failure );
        }

      // return original plan order, retaining task identity and captured values rather than live getters.
      var rows = new ArrayList<SchedulePreview.TaskDates>( taskCount );
      for ( Task task : tasks )
      {
        Node node = nodesByTask.get( task );
        if ( node != null )
          rows.add( node.m_dates );
      }
      var preview = new SchedulePreview( limit, rows );
      Utils.trace( "SCHEDULE PREVIEW complete", "tasks=", rows.size(), "limit=", limit, "applied=false" );
      return preview;
    }
    catch ( RuntimeException failure )
    {
      Utils.trace( "SCHEDULE PREVIEW failed", failure.getClass().getSimpleName(), failure.getMessage() );
      throw failure;
    }
  }

  /************************************** validateCalendar ***************************************/
  private static int validateCalendar( Plan plan )
  {
    Calendar calendar = plan.getDefaultCalendar();
    if ( calendar == null || !plan.getCalendars().contains( calendar ) )
      throw new IllegalArgumentException( "Project default calendar is missing or does not belong to this plan" );
    if ( calendar.getAnchor() == null || calendar.getNormals().isEmpty() || calendar.getNormals().size() > 99 )
      throw new IllegalArgumentException( "Project default calendar has an invalid anchor or cycle" );
    if ( (long) plan.getDayTypes().size() + calendar.getNormals().size()
        + calendar.getExceptions().size() > MAX_CALENDAR_WORK )
      throw new IllegalArgumentException( "Project calendar configuration exceeds the preview workload limit" );

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

  /**************************************** validateTask *****************************************/
  private static int validateTask( Node node, DateTime planStart, DateTime limit )
  {
    Task task = node.m_task;
    if ( task.getIndent() != 0 )
      throw new IllegalArgumentException( "Outline tasks are not supported in this preview" );
    if ( task.getTaskType() != TaskType.ASAP_FIXED_DURATION && task.getTaskType() != TaskType.START_ON_DURATION )
      throw new IllegalArgumentException(
          "Only ASAP-duration and Start-on-duration tasks are supported in this preview" );
    if ( task.getValue( Task.FIELD.Resources.ordinal() ) != null )
      throw new IllegalArgumentException( "Resource assignments are not supported in this preview" );
    if ( task.getPriority() < 0 || task.getPriority() > 999 )
      throw new IllegalArgumentException( "Priority must be between 0 and 999" );

    node.m_duration = TimeSpan.parse( task.getValue( Task.FIELD.Duration.ordinal() ) );
    if ( node.m_duration == null )
      throw new IllegalArgumentException( "Duration is required" );
    if ( node.m_duration.getNumber() == 0 )
      node.m_durationMillis = 0;
    else
      node.m_durationMillis = switch ( node.m_duration.getUnit() )
      {
        case SECONDS, MINUTES, HOURS -> IntervalProgress.targetMillis( node.m_duration );
        case WEEKS, MONTHS, YEARS -> -1;
        case DAYS -> throw new IllegalArgumentException(
            "Positive day-based duration is not supported in this preview" );
      };

    node.m_rounding = TimeSpan.parse( task.getValue( Task.FIELD.Rounding.ordinal() ) );
    if ( node.m_rounding != null && node.m_rounding.getNumber() <= 0 )
      throw new IllegalArgumentException( "End rounding must be positive or absent" );
    if ( task.getTaskType() == TaskType.START_ON_DURATION )
    {
      node.m_enteredStart = (DateTime) task.getValue( Task.FIELD.Start.ordinal() );
      if ( node.m_enteredStart == null )
        throw new IllegalArgumentException( "Start-on date is required" );
    }
    DateTime start = node.m_enteredStart == null ? planStart : node.m_enteredStart;
    if ( start.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException( "Start " + start + " is after preview limit " + limit );

    int searches = node.m_durationMillis > 0 ? 1 : 0;
    var predecessors = task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      if ( dependency.type != DependencyType.FS || dependency.lag100ths < 0 )
        throw new IllegalArgumentException( "Only nonnegative FS dependencies are supported in this preview" );
      if ( dependency.lagUnit == null || dependency.lagUnit == Unit.DAYS && dependency.lag100ths != 0 )
        throw new IllegalArgumentException(
            "Dependency lag unit is missing or needs unsupported equivalent-day advancement" );
      if ( dependency.lag100ths > 0 && ( dependency.lagUnit == Unit.SECONDS || dependency.lagUnit == Unit.MINUTES
          || dependency.lagUnit == Unit.HOURS ) )
        searches++;
    }
    return searches;
  }

  /**************************************** calculateTask ****************************************/
  private static SchedulePreview.TaskDates calculateTask( Node node, IdentityHashMap<Task, Node> nodesByTask,
      Calendar calendar, DateTime planStart, DateTime limit )
  {
    DateTime start = node.m_enteredStart == null ? planStart : node.m_enteredStart;
    var predecessors = node.m_task.getPredecessors();
    for ( int index = 0; predecessors != null && index < predecessors.size(); index++ )
    {
      var dependency = predecessors.get( index );
      Node predecessor = nodesByTask.get( dependency.task );
      DateTime bound = DependencyBounds.calculate( dependency, predecessor.m_dates.start(),
          predecessor.m_dates.finish(), calendar, limit );
      Utils.trace( "SCHEDULE PREVIEW dependency", "task=", node.m_index, "predecessor=", predecessor.m_index,
          "FS lag100ths=", dependency.lag100ths, "unit=", dependency.lagUnit, "bound=", bound );
      if ( bound == null )
        throw new IllegalArgumentException( "Task " + node.m_index + ": FS bound from task " + predecessor.m_index
            + " was not reached by preview limit " + limit );
      if ( bound.toSerialMilliseconds() > start.toSerialMilliseconds() )
      {
        if ( node.m_enteredStart != null )
          throw new IllegalArgumentException( "Task " + node.m_index + ": entered start " + start
              + " conflicts with FS on task " + predecessor.m_index + "; required start is at or after " + bound );
        start = bound;
      }
    }

    Utils.trace( "SCHEDULE PREVIEW start", "task=", node.m_index, "start=", start );
    DateTime normalFinish = node.m_durationMillis >= 0
        ? calendar.advanceWorkingMillis( start, node.m_durationMillis, Direction.FORWARD, limit )
        : CalendarShift.shift( start, node.m_duration );
    if ( normalFinish == null || normalFinish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException(
          "Task " + node.m_index + ": duration target was not reached by preview limit " + limit );
    DateTime finish = EndRounding.roundUp( normalFinish, node.m_rounding );
    if ( finish.toSerialMilliseconds() > limit.toSerialMilliseconds() )
      throw new IllegalArgumentException(
          "Task " + node.m_index + ": rounded finish " + finish + " is after preview limit " + limit );
    Utils.trace( "SCHEDULE PREVIEW dates", "task=", node.m_index, "start=", start, "normalFinish=", normalFinish,
        "finish=", finish );
    return new SchedulePreview.TaskDates( node.m_task, node.m_index,
        (String) node.m_task.getValue( Task.FIELD.Title.ordinal() ), start, normalFinish, finish );
  }

  /*************************************** getOrderedTasks ***************************************/
  private static List<Node> getOrderedTasks( Tasks tasks, IdentityHashMap<Task, Node> nodesByTask )
  {
    // create scheduling nodes for non-blank tasks, excluding the overall-project task
    int taskCount = tasks.getNotNullCount();
    var nodes = new ArrayList<Node>( taskCount );

    for ( int index = 1; index < tasks.size(); index++ )
    {
      Task task = tasks.get( index );
      if ( !task.isBlank() )
      {
        Node node = new Node( task, index );
        if ( task == tasks.get( 0 ) || nodesByTask.putIfAbsent( task, node ) != null )
          throw new IllegalStateException( "Task object is repeated at row " + index );
        nodes.add( node );
      }
    }

    // build incoming counts and successor links from predecessor relationships
    for ( Node node : nodes )
    {
      var predecessors = node.m_task.getPredecessors();
      if ( predecessors == null )
        continue;

      for ( int index = 0; index < predecessors.size(); index++ )
      {
        var dependency = predecessors.get( index );
        Node predecessor = dependency == null ? null : nodesByTask.get( dependency.task );
        if ( predecessor == null )
          throw new IllegalStateException( "Invalid predecessor for task " + node.m_index );

        node.m_incoming++;
        predecessor.addSuccessor( node );
      }
    }

    // queue graph-ready tasks by descending priority and then ascending task index
    var ready = new PriorityQueue<Node>( Math.max( 1, taskCount ), NODE_ORDER );
    for ( Node node : nodes )
      if ( node.m_incoming == 0 )
        ready.add( node );

    // remove each best graph-ready task and release its successors
    var ordered = new ArrayList<Node>( taskCount );
    while ( !ready.isEmpty() )
    {
      Node node = ready.remove();
      ordered.add( node );

      if ( node.m_successors != null )
        for ( Node successor : node.m_successors )
          if ( --successor.m_incoming == 0 )
            ready.add( successor );
    }

    // any unprocessed tasks form a circular dependency
    if ( ordered.size() != taskCount )
      throw new IllegalStateException( "Circular task dependency" );

    return ordered;
  }

}
