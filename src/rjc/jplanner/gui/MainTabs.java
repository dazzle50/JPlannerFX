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

package rjc.jplanner.gui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import rjc.jplanner.gui.calendars.CalendarsTab;
import rjc.jplanner.gui.days.DaysTab;
import rjc.jplanner.gui.plan.PlanTab;
import rjc.jplanner.gui.resources.ResourcesTab;
import rjc.jplanner.gui.tasks.TasksTab;

/*************************************************************************************************/
/************** TabPane showing the Plan/Tasks&Gantt/Resources/Calendars/Days tabs ***************/
/*************************************************************************************************/

public class MainTabs extends TabPane
{
  private Tab m_plan;      // tab showing overall plan controls & information
  private Tab m_tasks;     // tab showing table view of plan tasks and Gantt
  private Tab m_resources; // tab showing table view of plan resources
  private Tab m_calendars; // tab showing table view of plan calendars
  private Tab m_days;      // tab showing table view of plan day-types

  /**************************************** constructor ******************************************/
  public MainTabs()
  {
    // construct main window tabs with plan-tabs
    this( true );
  }

  /**************************************** constructor ******************************************/
  public MainTabs( boolean showPlanTab )
  {
    // construct main window tabs with optional plan-tabs
    if ( showPlanTab )
      m_plan = new PlanTab();
    m_tasks = new TasksTab();
    m_resources = new ResourcesTab();
    m_calendars = new CalendarsTab();
    m_days = new DaysTab();

    if ( showPlanTab )
      getTabs().addAll( m_plan );
    getTabs().addAll( m_tasks, m_resources, m_calendars, m_days );
  }

}
