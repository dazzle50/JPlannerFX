/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.jplanner.plan;

import rjc.table.Utils;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/******************************** Single task within overall plan ********************************/
/*************************************************************************************************/

public class Task
{
  private String        m_title;        // free text title
  private TimeSpan      m_duration;     // duration of task
  private DateTime      m_start;        // start date-time of task
  private DateTime      m_end;          // end date-time of task
  private TimeSpan      m_work;         // work effort for task
  private Predecessors  m_predecessors; // task predecessors
  private TaskResources m_resources;    // resources allocated to task
  private TaskType      m_type;         // task type
  private int           m_priority;     // overall task priority (0 to 999)
  private DateTime      m_deadline;     // task warning deadline
  private String        m_cost;         // calculated cost based on resource use
  private String        m_comment;      // free text comment

  private int           m_indent;       // task indent level, zero for no indent
  private int           m_summaryStart; // index of this task's summary, ultimately task 0
  private int           m_summaryEnd;   // if summary, index of summary end, otherwise -1

  public enum TaskType
  {
    ASAP_FIXED_DURATION, ASAP_FIXED_WORK, START_ON_DURATION, START_ON_WORK, FIXED_PERIOD
  }

  /**************************************** constructor ******************************************/
  public Task()
  {
    // initialise
    m_summaryEnd = -1;
  }

  /***************************************** toString ********************************************/
  @Override
  public String toString()
  {
    // convert to string
    return Utils.name( this ) + "['" + m_title + "' " + m_type + " " + m_priority + "]";
  }

  /***************************************** initialise ******************************************/
  public void initialise()
  {
    // initialise private variables
    m_duration = new TimeSpan( "1d" );
    m_predecessors = new Predecessors( "" );
    m_resources = new TaskResources();
    m_type = TaskType.ASAP_FIXED_DURATION;
    m_predecessors = new Predecessors();
    m_priority = 100;
    m_indent = 0;
    m_summaryStart = 0;
    m_summaryEnd = -1;
  }

}
