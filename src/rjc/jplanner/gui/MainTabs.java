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

/*************************************************************************************************/
/************** TabPane showing the Plan/Tasks&Gantt/Resources/Calendars/Days tabs ***************/
/*************************************************************************************************/

public class MainTabs extends TabPane
{
  private Tab m_plan;
  private Tab m_tasks;
  private Tab m_resources;
  private Tab m_calendars;
  private Tab m_days;

  /**************************************** constructor ******************************************/
  public MainTabs()
  {
    // construct main window tabs
    m_plan = new Tab( "Plan" );
    m_tasks = new Tab( "Tasks" );
    m_resources = new Tab( "Resources" );
    m_calendars = new Tab( "Calendars" );
    m_days = new Tab( "Days" );

    getTabs().addAll( m_plan, m_tasks, m_resources, m_calendars, m_days );
  }
}
