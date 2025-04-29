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

package rjc.jplanner.gui.tasks;

import javafx.scene.control.Tab;
import rjc.jplanner.Main;
import rjc.jplanner.gui.XSplitPane;
import rjc.jplanner.plan.Plan;

/*************************************************************************************************/
/******************** Tab showing table of the plan tasks alongside the gantt ********************/
/*************************************************************************************************/

public class TasksTab extends Tab
{
  private TasksView  m_view;
  private Gantt      m_gantt;
  private XSplitPane m_split;

  /**************************************** constructor ******************************************/
  public TasksTab()
  {
    // construct tab
    super( "Tasks" );
    setClosable( false );

    // showing table of available plan resources
    m_view = new TasksView( new TasksData( Plan.getTasks() ), getText() );
    m_view.setUndostack( Main.getUndostack() );
    m_view.setStatus( Main.getStatus() );

    // alongside the gantt
    m_gantt = new Gantt( m_view );
    m_split = new XSplitPane( m_view, m_gantt );

    // only have tab contents set if tab selected
    selectedProperty().addListener( ( observable, oldValue, newValue ) ->
    {
      if ( newValue )
        setContent( m_split );
      else
        setContent( null );
    } );

  }
}
