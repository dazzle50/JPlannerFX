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

import java.util.List;

import rjc.jplanner.plan.tasks.Task;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/************************ Immutable captured rows from a date-only preview ***********************/
/*************************************************************************************************/

public final class SchedulePreview
{
  private final DateTime        m_limit; // inclusive calculation limit selected for this run
  private final List<TaskDates> m_tasks; // completed rows in original plan order

  // task is retained solely for identity; all displayed values are captured independently.
  public record TaskDates( Task task, int taskNumber, String title, DateTime start, DateTime normalFinish,
      DateTime finish )
  {
  }

  /***************************************** constructor *****************************************/
  SchedulePreview( DateTime limit, List<TaskDates> tasks )
  {
    m_limit = limit;
    m_tasks = List.copyOf( tasks );
  }

  /****************************************** getLimit *******************************************/
  public DateTime getLimit()
  {
    return m_limit;
  }

  /****************************************** getTasks *******************************************/
  public List<TaskDates> getTasks()
  {
    return m_tasks;
  }
}
