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

package rjc.jplanner.plan.tasks;

import java.util.Objects;

import rjc.jplanner.plan.TimeSpan;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/******************************** Single task within overall plan ********************************/
/*************************************************************************************************/

public class Task
{
  private String        m_title;        // free text title
  private TimeSpan      m_duration;     // duration of task
  private TaskTiming    m_timing;       // task dates and explanations for unavailable dates
  private TimeSpan      m_work;         // work effort for task
  private Predecessors  m_predecessors; // task predecessors
  private TaskResources m_resources;    // resources allocated to task
  private TaskType      m_type;         // task type
  private int           m_priority;     // overall task priority (0 to 999)
  private DateTime      m_deadline;     // task warning deadline
  private String        m_cost;         // calculated cost based on resource use TODO
  private TimeSpan      m_endRounding;  // individual end rounding, null for none
  private String        m_comment;      // free text comment

  private int           m_indent;       // task indent level, zero for no indent

  public enum FIELD
  {
    Title, Duration, Start, End, Work, Predecessors, Resources, Type, Priority, Deadline, Cost, Rounding, Comment, MAX
  }

  /**************************************** constructor ******************************************/
  public Task()
  {
    // initialise
    m_duration = new TimeSpan( "1d" );
    m_timing = new TaskTiming();
    m_type = TaskType.ASAP_FIXED_DURATION;
    m_priority = 100;
    m_indent = 0;
  }

  /***************************************** toString ********************************************/
  @Override
  public String toString()
  {
    // convert to string
    return Utils.name( this ) + "['" + m_title + "' " + m_type + " " + m_priority + "]";
  }

  /**************************************** getTaskType ******************************************/
  public TaskType getTaskType()
  {
    // return task-type
    return m_type;
  }

  /****************************************** getTiming ******************************************/
  public TaskTiming getTiming()
  {
    return m_timing;
  }

  /****************************************** setTiming ******************************************/
  public void setTiming( TaskTiming timing )
  {
    Objects.requireNonNull( timing, "timing" );

    // scheduling must preserve entered dates; user edits and undo use setValue instead
    boolean fixedStart = m_type == TaskType.START_ON_DURATION || m_type == TaskType.START_ON_WORK
        || m_type == TaskType.FIXED_PERIOD;
    if ( fixedStart && !Objects.equals( m_timing.getStart(), timing.getStart() ) )
      throw new IllegalArgumentException( "Scheduling cannot change an entered start" );
    if ( m_type == TaskType.FIXED_PERIOD && !Objects.equals( m_timing.getEnd(), timing.getEnd() ) )
      throw new IllegalArgumentException( "Scheduling cannot change an entered end" );

    m_timing = timing;
  }

  /************************************ restoreTypeAndTiming *************************************/
  public void restoreTypeAndTiming( TaskType type, TaskTiming timing )
  {
    // validate both values before restoring history without repeating type-transition status clearing
    Objects.requireNonNull( type, "type" );
    Objects.requireNonNull( timing, "timing" );
    m_type = type;
    m_timing = timing;
  }

  /***************************************** getPriority *****************************************/
  public int getPriority()
  {
    // return the task scheduling priority
    return m_priority;
  }

  /****************************************** getIndent ******************************************/
  public int getIndent()
  {
    return m_indent;
  }

  /****************************************** setIndent ******************************************/
  void setIndent( int indent )
  {
    // validate the individual value; the complete outline is checked when building its hierarchy
    if ( indent < 0 )
      throw new IllegalArgumentException( "Task indentation cannot be negative: " + indent );

    m_indent = indent;
  }

  /****************************************** getValue *******************************************/
  public Object getValue( int field )
  {
    // return value for the different fields
    switch ( FIELD.values()[field] )
    {
      case Comment:
        return m_comment;
      case Cost:
        return m_cost;
      case Deadline:
        return m_deadline;
      case Duration:
        return m_duration;
      case End:
        return m_timing.getEnd();
      case Rounding:
        return TimeSpan.copyOf( m_endRounding );
      case Predecessors:
        return m_predecessors;
      case Priority:
        return m_priority;
      case Resources:
        return m_resources;
      case Start:
        return m_timing.getStart();
      case Title:
        return m_title;
      case Type:
        return m_type;
      case Work:
        return m_work;
      default:
        throw new IllegalArgumentException( "Unrecognised field " + field );
    }
  }

  /******************************************* isBlank *******************************************/
  public boolean isBlank()
  {
    // task record is blank if title is null
    return m_title == null;
  }

  /****************************************** setValue *******************************************/
  public String setValue( int field, Object newValue, boolean commit )
  {
    // set/check field value and return null if successful/possible
    switch ( FIELD.values()[field] )
    {
      case Title:
        // new value can be of any type
        if ( commit )
          m_title = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Start:
        // user edits and undo may clear a date; only calculated timing updates protect fixed dates
        if ( newValue == null )
        {
          if ( commit )
            m_timing = m_timing.withStart( null );
          return null;
        }
        if ( newValue instanceof DateTime dt )
        {
          if ( commit )
            m_timing = m_timing.withStart( dt );
          return null;
        }
        return "Not Date-time: " + Utils.objectsString( newValue );

      case End:
        // clearing an end also clears its derived rounded end
        if ( newValue == null )
        {
          if ( commit )
            m_timing = m_timing.withEnd( null );
          return null;
        }
        if ( newValue instanceof DateTime dt )
        {
          if ( commit )
            m_timing = m_timing.withEnd( dt );
          return null;
        }
        return "Not Date-time: " + Utils.objectsString( newValue );

      case Priority:
        // check new value is integer and in range
        if ( newValue instanceof Integer newInt )
        {
          if ( newInt < 0 || newInt > 999 )
            return "Value not between 0 and 999";
          if ( commit )
            m_priority = newInt;
          return null;
        }
        return "Not integer: " + Utils.objectsString( newValue );

      case Comment:
        // new value can be of any type
        if ( commit )
          m_comment = newValue == null ? null : newValue.toString();
        return null;

      case Type:
        // check new value is a task-type
        if ( newValue instanceof TaskType type )
        {
          if ( commit )
          {
            // discard scheduling explanations only for dates that become fixed
            boolean wasFixedStart = m_type == TaskType.START_ON_DURATION || m_type == TaskType.START_ON_WORK
                || m_type == TaskType.FIXED_PERIOD;
            boolean fixedStart = type == TaskType.START_ON_DURATION || type == TaskType.START_ON_WORK
                || type == TaskType.FIXED_PERIOD;
            boolean clearStart = fixedStart && !wasFixedStart;
            boolean clearEnd = type == TaskType.FIXED_PERIOD && m_type != TaskType.FIXED_PERIOD;
            if ( clearStart || clearEnd )
              m_timing = new TaskTiming( m_timing.getStart(), m_timing.getEnd(),
                  clearStart ? null : m_timing.getStartStatus(), clearEnd ? null : m_timing.getEndStatus(),
                  m_timing.getRoundedEnd() );
            m_type = type;
          }
          return null;
        }
        return "Not task-type: " + Utils.objectsString( newValue );

      case Duration:
        // check new value is a time-span
        if ( newValue instanceof TimeSpan ts )
        {
          if ( commit )
            m_duration = ts;
          return null;
        }
        return "Not time-span: " + Utils.objectsString( newValue );

      case Deadline:
        // check new value is date-time, or null
        if ( newValue == null )
        {
          if ( commit )
            m_deadline = null;
          return null;
        }
        if ( newValue instanceof DateTime dt )
        {
          if ( commit )
            m_deadline = dt;
          return null;
        }
        return "Not Date-time: " + Utils.objectsString( newValue );

      case Predecessors:
        // check new value is predecessors, or null
        if ( newValue == null )
        {
          if ( commit )
            m_predecessors = null;
          return null;
        }
        if ( newValue instanceof Predecessors preds )
        {
          if ( preds.hasCircularReference( this ) )
            return "Circular reference to self";
          if ( commit )
            m_predecessors = preds;
          return null;
        }
        return "Not Predecessors: " + Utils.objectsString( newValue );

      case Resources:
        // check new value is task-resources, or null
        if ( newValue == null )
        {
          if ( commit )
            m_resources = null;
          return null;
        }
        if ( newValue instanceof TaskResources res )
        {
          if ( commit )
            m_resources = res;
          return null;
        }
        return "Not Task-Resources: " + Utils.objectsString( newValue );

      case Work:
        // check new value is a time-span
        if ( newValue instanceof TimeSpan ts )
        {
          if ( commit )
            m_work = ts;
          return null;
        }
        return "Not time-span: " + Utils.objectsString( newValue );

      case Rounding:
        // check new value is time-span, or null
        if ( newValue == null )
        {
          if ( commit )
            m_endRounding = null;
          return null;
        }
        if ( newValue instanceof TimeSpan ts && ts.getNumber() > 0.0 )
        {
          if ( commit )
            m_endRounding = ts;
          return null;
        }
        return "Not positive time-span: " + Utils.objectsString( newValue );

      case Cost:

      default:
        return "Not implemented";
    }
  }

  /*************************************** getPredecessors ***************************************/
  public Predecessors getPredecessors()
  {
    return m_predecessors;
  }

}
