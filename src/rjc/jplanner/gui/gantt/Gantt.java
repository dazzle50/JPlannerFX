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

package rjc.jplanner.gui.gantt;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.table.data.types.DateInterval;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.view.Colours;

/*************************************************************************************************/
/********************** Gantt shows tasks in a gantt plot with gantt scales **********************/
/*************************************************************************************************/

public class Gantt extends Region
{
  private ArrayList<GanttAxis> m_axes;                               // list of gantt-axes showing timeline
  private GanttNavigation      m_navigation;                         // horizontal navigation and undo
  private GanttPlot            m_plot;                               // gantt plot chart showing the tasks
  private GanttScrollBar       m_scrollBar;                          // horizontal scroll bar
  private PlanContext          m_context;                            // shared undo history for keyboard shortcuts
  private boolean              m_fitPending;                         // view still needs its first fit
  private boolean              m_fitQueued;                          // fit is waiting for layout to finish

  private ObservableInteger    m_axesHeight;                         // height in pixels of all gantt axes

  final public static int      SCROLLBAR_SIZE   = 18;                // scroll bar height (thickness)
  final public static int      GANTTAXIS_HEIGHT = 15;                // gantt axis individual height

  static final double          MAX_LAYOUT_SIZE  = 999_999_999.0;     // maximum accepted layout dimension in pixels

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
    m_context = (PlanContext) view.getData().getUserData();
    m_fitPending = true;
    m_navigation = new GanttNavigation( m_context, this::getWidth, this::redraw );
    m_scrollBar = m_navigation.getScrollBar();
    var scale = m_navigation.getScale();
    m_axes = new ArrayList<>( 2 );
    m_axes.add( new GanttAxis( scale, DateInterval.MONTH ) );
    m_axes.add( new GanttAxis( scale, DateInterval.WEEK ) );
    m_plot = new GanttPlot( view, scale );
    getChildren().addAll( m_axes.get( 0 ), m_axes.get( 1 ), m_plot, m_scrollBar );

    // allow keyboard navigation into the gantt and acquire focus from any part of it
    setFocusTraversable( true );
    addEventFilter( MouseEvent.MOUSE_PRESSED, event -> requestFocus() );
    addEventFilter( KeyEvent.KEY_PRESSED, this::keyPressed );

    // detach shared data listeners while hidden, then refresh the range when shown again
    IListener dataListener = ( sender, message ) ->
    {
      m_navigation.invalidateRange();
      requestLayout();
    };
    ISignal[] sources = { view.getData(), view.getData().rowCountProperty() };
    sceneProperty().addListener( ( observable, oldScene, newScene ) ->
    {
      for ( ISignal source : sources )
      {
        if ( oldScene != null )
          source.removeListener( dataListener );
        if ( newScene != null )
          source.addListener( dataListener );
      }
      if ( newScene != null )
      {
        m_navigation.invalidateRange();
        requestLayout();
      }
    } );

    // property containing height in pixels of all gantt axes (for 'Tasks' table-view header height)
    m_axesHeight = new ObservableInteger( (int) ( m_axes.get( 0 ).getHeight() + m_axes.get( 1 ).getHeight() ) );

    // set background colour to same as normal table-view header background colour
    setBackground( new Background( new BackgroundFill( Colours.HEADER_DEFAULT_FILL, null, null ) ) );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    // pass key event to navigation for undo/redo and horizontal scrolling
    if ( !event.isControlDown() || event.isAltDown() || event.isMetaDown() || getWidth() <= 0.0 )
      return;

    switch ( event.getCode() )
    {
      case PLUS, EQUALS, ADD -> m_navigation.zoomIn();
      case MINUS, SUBTRACT -> m_navigation.zoomOut();
      case DIGIT0, NUMPAD0 -> m_navigation.fit( true );
      case Z -> m_context.getUndoStack().undo();
      case Y -> m_context.getUndoStack().redo();
      default -> {
        return;
      }
    }
    event.consume();
  }

  /******************************************* redraw ********************************************/
  private void redraw()
  {
    // redraw the gantt plot and axes
    for ( var axis : m_axes )
      axis.redraw();
    m_plot.redraw();
  }

  /*************************************** layoutChildren ****************************************/
  @Override
  protected void layoutChildren()
  {
    // skip layout if dimensions not ready
    double w = getWidth();
    double h = getHeight();
    if ( w <= 0.0 || h <= 0.0 || w > MAX_LAYOUT_SIZE || h > MAX_LAYOUT_SIZE )
      return;

    m_navigation.refresh();

    // layout gantt axes at top
    int y = 0;
    for ( var axis : m_axes )
    {
      axis.setWidth( w );
      axis.setLayoutY( y );
      y += axis.getHeight();
    }

    // layout gantt plot below axes
    m_plot.setWidth( w );
    int plotHeight = (int) ( h - m_scrollBar.getHeight() - y );
    if ( plotHeight < 0 )
      plotHeight = 0;
    m_plot.setHeight( plotHeight );
    m_plot.setLayoutY( y );

    // layout gantt scroll-bar below plot
    m_scrollBar.setPrefWidth( w );
    m_scrollBar.setLayoutY( y + plotHeight );
    super.layoutChildren();

    // initialise each view after the split pane has finished sizing, even if the tab was initially hidden
    if ( m_fitPending && !m_fitQueued && m_navigation.canFit() && getScene() != null )
    {
      m_fitQueued = true;
      Platform.runLater( () ->
      {
        m_fitQueued = false;
        if ( getScene() != null && m_navigation.canFit() )
        {
          m_fitPending = false;
          m_navigation.fit( false );
        }
      } );
    }
  }

  /************************************* axesHeightProperty **************************************/
  public ReadOnlyInteger axesHeightProperty()
  {
    // return read-only property for axes height in pixels
    return m_axesHeight.getReadOnly();
  }

}
