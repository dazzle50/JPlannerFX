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
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.Colours;

/*************************************************************************************************/
/********************** Gantt shows tasks in a gantt plot with gantt scales **********************/
/*************************************************************************************************/

public class Gantt extends Region
{
  private ArrayList<GanttAxis> m_axes;                                                                      // list of gantt-axes showing timeline
  private GanttScale           m_scale;                                                                     // scale defines starts and scaling size
  private GanttPlot            m_plot;                                                                      // gantt plot chart showing the tasks
  private GanttScrollBar       m_scrollBar;                                                                 // horizontal scroll bar
  private PlanContext          m_context;                                                                   // plan providing task bounds and default start
  private boolean              m_rangePending;                                                              // horizontal range needs refreshing
  private double               m_width;                                                                     // previously laid out viewport width
  private boolean              m_fitPending;                                                                // view still needs its first fit
  private boolean              m_fitQueued;                                                                 // fit is waiting for layout to finish
  private boolean              m_updatingScrollBar;                                                         // suppress redraws while updating the range

  private ObservableInteger    m_axesHeight;                                                                // height in pixels of all gantt axes

  final public static int      SCROLLBAR_SIZE             = 18;                                             // scroll bar height (thickness)
  final public static int      GANTTAXIS_HEIGHT           = 15;                                             // gantt axis individual height

  private static final double  FIT_MARGIN_FRACTION        = 0.05;                                           // margin on each side of fitted tasks
  private static final double  FIT_MIN_MARGIN_PIXELS      = 5.0;                                            // minimum margin on each side
  private static final int     ZOOM_STEPS_PER_DOUBLING    = 16;                                             // key presses to double or halve the scale

  private static final double  ZOOM_FACTOR                = Math.pow( 2.0, 1.0 / ZOOM_STEPS_PER_DOUBLING ); // zoom step multiplier

  private static final long    INITIAL_LEAD_DAYS          = 7L;                                             // days shown before the plan default start
  private static final long    AXIS_BOUNDARY_PADDING_DAYS = 32L;                                            // allowance for month and week axis boundaries
  private static final double  SCROLL_UNIT_FRACTION       = 0.05;                                           // viewport fraction moved by a scroll arrow
  private static final double  SCROLL_BLOCK_FRACTION      = 0.9;                                            // viewport fraction moved by a page scroll
  private static final double  MAX_LAYOUT_SIZE            = 999_999_999.0;                                  // maximum accepted layout dimension in pixels

  // gantt colours
  public static final Color    GANTT_BACKGROUND           = Color.WHITE;
  public static final Color    GANTT_NONWORKING           = Color.gray( 0.95 );
  public static final Color    GANTT_DIVIDER              = Color.SILVER;
  public static final Color    GANTT_TASK_EDGE            = Color.BLACK;
  public static final Color    GANTT_TASK_FILL            = Color.YELLOW;
  public static final Color    GANTT_SUMMARY              = Color.BLACK;
  public static final Color    GANTT_MILESTONE            = Color.BLACK;
  public static final Color    GANTT_DEPENDENCY           = Color.SLATEGRAY;
  public static final Color    GANTT_DEADLINE             = Color.GREEN;

  /**************************************** constructor ******************************************/
  public Gantt( TasksView view )
  {
    // construct the gantt
    m_context = (PlanContext) view.getData().getUserData();
    m_fitPending = true;
    m_scrollBar = new GanttScrollBar();
    m_scale = new GanttScale( DateTime.ofSerialMilliseconds( defaultStart() ), m_scrollBar.valueProperty() );
    m_axes = new ArrayList<>( 2 );
    m_axes.add( new GanttAxis( m_scale, DateInterval.MONTH ) );
    m_axes.add( new GanttAxis( m_scale, DateInterval.WEEK ) );
    m_plot = new GanttPlot( view, m_scale );
    getChildren().addAll( m_axes.get( 0 ), m_axes.get( 1 ), m_plot, m_scrollBar );

    // keep the timeline headers and plot on the same horizontal offset
    m_scrollBar.valueProperty().addListener( ( observable, oldValue, newValue ) ->
    {
      if ( !m_updatingScrollBar )
        redraw();
    } );

    // allow keyboard navigation into the gantt and acquire focus from any part of it
    setFocusTraversable( true );
    addEventFilter( MouseEvent.MOUSE_PRESSED, event -> requestFocus() );
    addEventFilter( KeyEvent.KEY_PRESSED, this::keyPressed );

    // detach shared data listeners while hidden, then refresh the range when shown again
    IListener dataListener = ( sender, message ) ->
    {
      m_rangePending = true;
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
        m_rangePending = true;
        requestLayout();
      }
    } );

    // property containing height in pixels of all gantt axes (for 'Tasks' table-view header height)
    m_axesHeight = new ObservableInteger( (int) ( m_axes.get( 0 ).getHeight() + m_axes.get( 1 ).getHeight() ) );

    // set background colour to same as normal table-view header background colour
    setBackground( new Background( new BackgroundFill( Colours.HEADER_DEFAULT_FILL, null, null ) ) );
  }

  /**************************************** defaultStart *****************************************/
  private long defaultStart()
  {
    // start one calendar week before the plan default, within the stored date range
    return Math.max( DateTime.MIN_VALUE.toSerialMilliseconds(),
        m_context.getPlan().getDefaultStart().toSerialMilliseconds() - INITIAL_LEAD_DAYS * Time.MILLIS_PER_DAY );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    if ( !event.isControlDown() || event.isAltDown() || event.isMetaDown() || getWidth() <= 0.0 )
      return;

    switch ( event.getCode() )
    {
      case PLUS, EQUALS, ADD -> zoom( 1.0 / ZOOM_FACTOR );
      case MINUS, SUBTRACT -> zoom( ZOOM_FACTOR );
      case DIGIT0, NUMPAD0 -> fit( true );
      case Z -> m_context.getUndoStack().undo();
      case Y -> m_context.getUndoStack().redo();
      default -> {
        return;
      }
    }
    event.consume();
  }

  /******************************************** zoom *********************************************/
  private void zoom( double factor )
  {
    // preserve the date at the viewport centre when changing horizontal scale
    long current = m_scale.getMsPP();
    long millisecondsPP = Math.round( current * factor );

    // integer rounding must still advance the scale at very fine zoom levels
    millisecondsPP = factor < 1.0 ? Math.max( GanttScale.MIN_MS_PP, Math.min( current - 1, millisecondsPP ) )
        : Math.max( current + 1, millisecondsPP );
    changeViewport( centre(), millisecondsPP, false, taskBounds() );
  }

  /********************************************* fit *********************************************/
  private void fit( boolean undoable )
  {
    double width = getWidth();
    if ( width <= 2.0 * FIT_MIN_MARGIN_PIXELS )
      return;

    long millisecondsPP = GanttScale.DEFAULT_MS_PP;
    long centre = defaultStart() + Math.round( width * millisecondsPP / 2.0 );
    TaskBounds bounds = taskBounds();
    DateTime start = bounds.start();
    DateTime end = bounds.end();
    if ( start != null && end != null && end.toSerialMilliseconds() > start.toSerialMilliseconds() )
    {
      // round up so both ends fit inside equal margins, including the minimum pixel margin
      long first = start.toSerialMilliseconds();
      long span = end.toSerialMilliseconds() - first;
      double usable = width - 2.0 * Math.max( FIT_MIN_MARGIN_PIXELS, width * FIT_MARGIN_FRACTION );
      millisecondsPP = Math.max( GanttScale.MIN_MS_PP, (long) Math.ceil( span / usable ) );
      centre = first + span / 2;
    }

    if ( undoable )
      changeViewport( centre, millisecondsPP, true, bounds );
    else
      applyViewport( centre, millisecondsPP, bounds );
  }

  /******************************************* centre ********************************************/
  private long centre()
  {
    return m_scale.milliseconds( getWidth() / 2.0 );
  }

  // task dates read once for each horizontal refresh, without retaining a cached copy
  private record TaskBounds( DateTime start, DateTime end )
  {
  }

  /***************************************** taskBounds ******************************************/
  private TaskBounds taskBounds()
  {
    var plan = m_context.getPlan();
    return new TaskBounds( plan.getEarliestTaskStart(), plan.getLatestTaskEnd() );
  }

  /*************************************** changeViewport ****************************************/
  private void changeViewport( long centre, long millisecondsPP, boolean fit, TaskBounds bounds )
  {
    if ( millisecondsPP == m_scale.getMsPP() && centre == centre() )
      return;

    // leave room for axis interval boundaries and reject zooms outside the supported dates
    double halfSpan = getWidth() * millisecondsPP / 2.0;
    if ( centre - halfSpan < DateTime.MIN_VALUE.toSerialMilliseconds()
        + AXIS_BOUNDARY_PADDING_DAYS * Time.MILLIS_PER_DAY
        || centre + halfSpan > DateTime.MAX_VALUE.toSerialMilliseconds()
            - AXIS_BOUNDARY_PADDING_DAYS * Time.MILLIS_PER_DAY )
      return;

    var stack = m_context.getUndoStack();
    var previous = stack.getUndoCommand();
    if ( !fit && !stack.isClean() && stack.getIndex() == stack.getSize() && previous instanceof ZoomCommand command
        && command.m_gantt == this && !command.m_fit && command.m_newCentre == centre()
        && command.m_newMsPP == m_scale.getMsPP() )
    {
      // merge uninterrupted zooms in this view without crossing the saved history position
      command.m_newCentre = centre;
      command.m_newMsPP = millisecondsPP;
      applyViewport( centre, millisecondsPP, bounds );
      stack.commandMerged();
    }
    else
    {
      var command = new ZoomCommand( centre, millisecondsPP, fit );
      applyViewport( centre, millisecondsPP, bounds );
      stack.push( command );
    }
  }

  /*************************************** applyViewport *****************************************/
  private void applyViewport( long centre, long millisecondsPP, TaskBounds bounds )
  {
    // this viewport already accounts for the current width, even if resize layout is still pending
    m_width = getWidth();
    long left = centre - Math.round( getWidth() * millisecondsPP / 2.0 );
    updateViewportRange( left, millisecondsPP, bounds );
  }

  /************************************* updateViewportRange *************************************/
  private void updateViewportRange( long left, long millisecondsPP, TaskBounds bounds )
  {
    double width = getWidth();
    long right = left + Math.round( width * millisecondsPP );
    long first = left;
    long last = right;
    long margin = Math.round( Math.max( FIT_MIN_MARGIN_PIXELS, width * FIT_MARGIN_FRACTION ) * millisecondsPP );
    if ( bounds.start() != null )
      first = Math.min( first, bounds.start().toSerialMilliseconds() - margin );
    if ( bounds.end() != null )
      last = Math.max( last, bounds.end().toSerialMilliseconds() + margin );

    // include the current viewport so rebuilding the range never moves the viewed date
    double total = (double) ( last - first ) / millisecondsPP;
    double maximum = Math.max( 0.0, total - width );
    m_updatingScrollBar = true;
    m_scale.setScale( first, millisecondsPP );
    m_scrollBar.setMin( 0.0 );
    m_scrollBar.setMax( maximum );

    // JavaFX expresses the thumb size as a fraction of the scrollable range
    m_scrollBar.setVisibleAmount( maximum > 0.0 ? maximum * width / total : width );
    m_scrollBar.setUnitIncrement( Math.max( 1.0, width * SCROLL_UNIT_FRACTION ) );
    m_scrollBar.setBlockIncrement( Math.max( 1.0, width * SCROLL_BLOCK_FRACTION ) );
    m_scrollBar.setValue( (double) ( left - first ) / millisecondsPP );
    m_updatingScrollBar = false;
    m_rangePending = false;
    redraw();
  }

  /******************************************* redraw ********************************************/
  private void redraw()
  {
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

    // refresh only after horizontal changes; ordinary layout does not need task bounds or an axis redraw
    if ( m_rangePending || m_width != w )
    {
      // preserve the centre on resize, while retaining the initial left edge on first layout
      long left = m_scale.milliseconds( 0.0 );
      if ( m_width > 0.0 )
        left += Math.round( ( m_width - w ) * m_scale.getMsPP() / 2.0 );
      m_width = w;
      updateViewportRange( left, m_scale.getMsPP(), taskBounds() );
    }

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
    if ( m_fitPending && !m_fitQueued && w > 2.0 * FIT_MIN_MARGIN_PIXELS && getScene() != null )
    {
      m_fitQueued = true;
      Platform.runLater( () ->
      {
        m_fitQueued = false;
        if ( getScene() != null && getWidth() > 2.0 * FIT_MIN_MARGIN_PIXELS )
        {
          m_fitPending = false;
          fit( false );
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

  // save the centre as well as scale so undo remains meaningful after scrolling or resizing
  private class ZoomCommand implements IUndoCommand
  {
    private final Gantt   m_gantt = Gantt.this; // view whose state this command restores
    private final long    m_oldCentre;          // centre date before the first zoom
    private final long    m_oldMsPP;            // original horizontal scale
    private long          m_newCentre;          // centre date after the latest zoom
    private long          m_newMsPP;            // latest horizontal scale
    private final boolean m_fit;                // whether this command explicitly fits the tasks

    /************************************** constructor ******************************************/
    ZoomCommand( long centre, long millisecondsPP, boolean fit )
    {
      m_oldCentre = centre();
      m_oldMsPP = m_scale.getMsPP();
      m_newCentre = centre;
      m_newMsPP = millisecondsPP;
      m_fit = fit;
    }

    /***************************************** redo **********************************************/
    @Override
    public void redo()
    {
      applyViewport( m_newCentre, m_newMsPP, taskBounds() );
    }

    /***************************************** undo **********************************************/
    @Override
    public void undo()
    {
      // restore the saved date and scale rather than recalculating from possibly edited tasks
      applyViewport( m_oldCentre, m_oldMsPP, taskBounds() );
    }

    /***************************************** text **********************************************/
    @Override
    public String text()
    {
      return m_fit ? "Fit Gantt" : "Gantt zoom " + Math.round( 100.0 * GanttScale.DEFAULT_MS_PP / m_newMsPP ) + "%";
    }
  }
}
