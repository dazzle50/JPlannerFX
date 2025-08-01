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

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import rjc.jplanner.Main;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.table.data.types.DateTime.Interval;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.view.Colours;

/*************************************************************************************************/
/********************** Gantt shows tasks in a gantt plot with gantt scales **********************/
/*************************************************************************************************/

public class Gantt extends Region
{
  private ArrayList<GanttAxis> m_axes;                               // list of gantt-axes showing timeline
  private GanttScale           m_scale;                              // scale defines starts and scaling size
  private GanttPlot            m_plot;                               // gantt plot chart showing the tasks
  private GanttScrollBar       m_scrollBar;                          // horizontal scroll bar

  private ObservableInteger    m_axesHeight;                         // height in pixels of all gantt axes

  final public static int      SCROLLBAR_SIZE   = 18;                // scroll bar height (thickness)
  final public static int      GANTTAXIS_HEIGHT = 15;                // gantt axis individual height

  // gantt colours
  public static final Color    GANTT_BACKGROUND = Color.WHITE;
  public static final Color    GANTT_NONWORKING = Color.gray( 0.95 );
  public static final Color    GANTT_DIVIDER    = Color.SILVER;
  public static final Color    GANTT_TASK_EDGE  = Color.BLACK;
  public static final Color    GANTT_TASK_FILL  = Color.YELLOW;
  public static final Color    GANTT_SUMMARY    = Color.BLACK;
  public static final Color    GANTT_MILESTONE  = Color.BLACK;
  public static final Color    GANTT_DEPENDENCY = Color.SLATEGRAY;
  public static final Color    GANTT_DEADLINE   = Color.GREEN;

  /**************************************** constructor ******************************************/
  public Gantt( TasksView view )
  {
    // construct the gantt
    m_scrollBar = new GanttScrollBar();
    m_scale = new GanttScale( Main.getPlan().getDefaultStart(), m_scrollBar.valueProperty() );
    m_axes = new ArrayList<>( 2 );
    m_axes.add( new GanttAxis( m_scale, Interval.MONTH ) );
    m_axes.add( new GanttAxis( m_scale, Interval.WEEK ) );
    m_plot = new GanttPlot( view, m_scale );
    getChildren().addAll( m_axes.get( 0 ), m_axes.get( 1 ), m_plot, m_scrollBar );

    // property containing height in pixels of all gantt axes (for 'Tasks' table-view header height)
    m_axesHeight = new ObservableInteger( (int) ( m_axes.get( 0 ).getHeight() + m_axes.get( 1 ).getHeight() ) );

    // set background colour to same as normal table-view header background colour
    setBackground( new Background( new BackgroundFill( Colours.HEADER_DEFAULT_FILL, null, null ) ) );
  }

  /*************************************** layoutChildren ****************************************/
  @Override
  protected void layoutChildren()
  {
    // layout gantt axes at top
    int y = 0;
    for ( var axis : m_axes )
    {
      axis.setWidth( getWidth() );
      axis.setLayoutY( y );
      y += axis.getHeight();
    }

    // layout gantt plot below axes
    m_plot.setWidth( getWidth() );
    int h = (int) ( getHeight() - m_scrollBar.getHeight() - y );
    if ( h < 0 )
      h = 0;
    m_plot.setHeight( h );
    m_plot.setLayoutY( y );

    // layout gantt scroll-bar below plot
    m_scrollBar.setPrefWidth( getWidth() );
    m_scrollBar.setLayoutY( y + h );
    super.layoutChildren();
  }

  /************************************* axesHeightProperty **************************************/
  public ReadOnlyInteger axesHeightProperty()
  {
    // return read-only property for axes height in pixels
    return m_axesHeight.getReadOnly();
  }
}
