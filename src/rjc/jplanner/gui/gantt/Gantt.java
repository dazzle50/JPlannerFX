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

package rjc.jplanner.gui.gantt;

import java.util.ArrayList;

import javafx.scene.layout.VBox;
import rjc.jplanner.Main;
import rjc.jplanner.gui.tasks.TasksView;

/*************************************************************************************************/
/********************** Gantt shows tasks in a gantt plot with gantt scales **********************/
/*************************************************************************************************/

public class Gantt extends VBox
{
  private ArrayList<GanttAxis> m_axes;                // list of gantt-axes showing timeline
  private GanttScale           m_scale;               // scale defines starts and scaling size
  private GanttPlot            m_plot;                // gantt plot chart showing the tasks
  private GanttScrollBar       m_scrollBar;           // horizontal scroll bar

  final public static int      SCROLLBAR_SIZE    = 18;
  final public static int      GANTTSCALE_HEIGHT = 15;

  /**************************************** constructor ******************************************/
  public Gantt( TasksView view )
  {
    // construct the gantt
    m_scrollBar = new GanttScrollBar();
    m_scale = new GanttScale( Main.getPlan().getDefaultStart(), m_scrollBar.valueProperty() );
    m_axes = new ArrayList<>( 2 );
    m_axes.add( new GanttAxis( m_scale ) );
    m_axes.add( new GanttAxis( m_scale ) );
    m_plot = new GanttPlot( view, m_scale );

    getChildren().addAll( m_axes.get( 0 ), m_axes.get( 1 ), m_plot, m_scrollBar );
  }
}
