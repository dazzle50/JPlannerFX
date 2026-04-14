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

package rjc.jplanner.gui;

import rjc.jplanner.gui.calendars.CalendarsData;
import rjc.jplanner.gui.days.DaysData;
import rjc.jplanner.gui.resources.ResourcesData;
import rjc.jplanner.gui.tasks.TasksData;
import rjc.jplanner.plan.Plan;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/************** Container for Plan, Status, UndoStack, and TableView data sources  ***************/
/*************************************************************************************************/

/**
 * Structure holding Plan, Status, UndoStack, and TableView data sources.
 * Acts as the central data provider for the JPlannerFX GUI components.
 */
public class PlanContext
{
  // core plan and status - marked final for safety
  private final Plan             m_plan;
  private final ObservableStatus m_status;
  private final UndoStack        m_undoStack;

  // data wrappers for TableView data sources
  private final DaysData         m_dayData;
  private final CalendarsData    m_calendarData;
  private final ResourcesData    m_resourceData;
  private final TasksData        m_taskData;

  /**************************************** constructor ******************************************/
  public PlanContext( Plan plan )
  {
    m_plan = plan;
    m_status = new ObservableStatus();
    m_undoStack = new UndoStack();

    // initialise TableView data sources using plan sub-collections
    m_dayData = new DaysData( m_plan.getDays() );
    m_calendarData = new CalendarsData( m_plan.getCalendars() );
    m_resourceData = new ResourcesData( m_plan.getResources() );
    m_taskData = new TasksData( m_plan.getTasks() );

    // set user-data for tables to allow them to access the wider context
    m_dayData.setUserData( this );
    m_calendarData.setUserData( this );
    m_resourceData.setUserData( this );
    m_taskData.setUserData( this );
  }

  /******************************************* getters *******************************************/
  public Plan getPlan()
  {
    return m_plan;
  }

  public ObservableStatus getStatus()
  {
    return m_status;
  }

  public UndoStack getUndoStack()
  {
    return m_undoStack;
  }

  public DaysData getDaysTableData()
  {
    return m_dayData;
  }

  public CalendarsData getCalendarsTableData()
  {
    return m_calendarData;
  }

  public ResourcesData getResourcesTableData()
  {
    return m_resourceData;
  }

  public TasksData getTasksTableData()
  {
    return m_taskData;
  }
}