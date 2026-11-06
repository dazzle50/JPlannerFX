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

import java.util.function.DoubleSupplier;

import rjc.jplanner.gui.PlanContext;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/********************* Coordinates horizontal Gantt navigation and its undo ***********************/
/*************************************************************************************************/

class GanttNavigation
{
  private final PlanContext    m_context;                                                                   // plan providing task bounds and undo history
  private final DoubleSupplier m_widthSupplier;                                                             // current width, including before pending layout
  private final Runnable       m_redraw;                                                                    // redraws axes and plot after navigation
  private final GanttScale     m_scale;                                                                     // date-to-pixel mapping shared with the renderers
  private final GanttScrollBar m_scrollBar;                                                                 // horizontal offset and navigation control
  private boolean              m_rangePending;                                                              // horizontal range needs refreshing
  private double               m_width;                                                                     // previously laid out viewport width
  private long                 m_rangeEnd;                                                                  // scrollable end retained across viewport changes
  private boolean              m_updatingScrollBar;                                                         // suppress redraws while updating the range

  private static final double  FIT_MARGIN_FRACTION        = 0.05;                                           // margin on each side of fitted tasks
  private static final double  FIT_MIN_MARGIN_PIXELS      = 5.0;                                            // minimum margin on each side
  private static final int     ZOOM_STEPS_PER_DOUBLING    = 16;                                             // key presses to double or halve the scale

  private static final double  ZOOM_FACTOR                = Math.pow( 2.0, 1.0 / ZOOM_STEPS_PER_DOUBLING ); // zoom step multiplier

  private static final long    INITIAL_LEAD_DAYS          = 7L;                                             // days shown before the plan default start
  private static final long    AXIS_BOUNDARY_PADDING_DAYS = 32L;                                            // allowance for month and week axis boundaries
  private static final double  SCROLL_BLOCK_FRACTION      = 0.9;                                            // viewport fraction moved by a page scroll

  /**************************************** constructor ******************************************/
  GanttNavigation( PlanContext context, DoubleSupplier widthSupplier, Runnable redraw )
  {
    // store references to plan context, width supplier and redraw callback
    m_context = context;
    m_widthSupplier = widthSupplier;
    m_redraw = redraw;
    m_scrollBar = new GanttScrollBar( this::scroll );
    m_scale = new GanttScale( DateTime.ofSerialMilliseconds( defaultStart() ), m_scrollBar.valueProperty() );
    m_rangeEnd = m_scale.getStartMilliseconds();

    // keep the timeline headers and plot on the same horizontal offset
    m_scrollBar.valueProperty().addListener( ( observable, oldValue, newValue ) ->
    {
      if ( !m_updatingScrollBar )
        m_redraw.run();
    } );
  }

  /****************************************** getScale *******************************************/
  GanttScale getScale()
  {
    // return the date-to-pixel mapping shared with the renderers
    return m_scale;
  }

  /**************************************** getScrollBar *****************************************/
  GanttScrollBar getScrollBar()
  {
    // return the horizontal offset and navigation control
    return m_scrollBar;
  }

  /******************************************* zoomIn ********************************************/
  void zoomIn()
  {
    // zoom in by a fixed factor, keeping the current left-edge of the viewport in the same position
    zoom( 1.0 / ZOOM_FACTOR );
  }

  /******************************************* zoomOut *******************************************/
  void zoomOut()
  {
    // zoom out by a fixed factor, keeping the current left-edge of the viewport in the same position
    zoom( ZOOM_FACTOR );
  }

  /******************************************* canFit ********************************************/
  boolean canFit()
  {
    // return true if there are any tasks to fit, and the current viewport width is sufficient for margins
    double width = m_widthSupplier.getAsDouble();
    return width > 2.0 * FIT_MIN_MARGIN_PIXELS && width <= Gantt.MAX_LAYOUT_SIZE;
  }

  /*************************************** invalidateRange ***************************************/
  void invalidateRange()
  {
    // mark the horizontal range as needing refreshing
    m_rangePending = true;
  }

  /******************************************* refresh *******************************************/
  void refresh()
  {
    // refresh the horizontal range if it is marked as needing it, and the viewport width is valid
    double width = m_widthSupplier.getAsDouble();
    if ( width <= 0.0 || width > Gantt.MAX_LAYOUT_SIZE )
      return;

    // ordinary layout does not need task bounds or an axis redraw
    if ( m_rangePending || m_width != width )
    {
      // preserve the visible start when resizing or rebuilding the scroll range
      updateViewportRange( visibleStart(), m_scale.getMsPP(), taskBounds(), false );
    }
  }

  /**************************************** defaultStart *****************************************/
  private long defaultStart()
  {
    // start one calendar week before the plan default, within the stored date range
    return Math.max( DateTime.MIN_VALUE.toSerialMilliseconds(),
        m_context.getPlan().getDefaultStart().toSerialMilliseconds() - INITIAL_LEAD_DAYS * Time.MILLIS_PER_DAY );
  }

  /******************************************** zoom *********************************************/
  private void zoom( double factor )
  {
    // preserve the visible start, including the scroll offset, when changing horizontal scale
    long current = m_scale.getMsPP();
    long millisecondsPP = Math.round( current * factor );

    // integer rounding must still advance the scale at very fine zoom levels
    millisecondsPP = factor < 1.0 ? Math.max( GanttScale.MIN_MS_PP, Math.min( current - 1, millisecondsPP ) )
        : Math.max( current + 1, millisecondsPP );
    changeViewport( visibleStart(), millisecondsPP, false, taskBounds() );
  }

  /********************************************* fit *********************************************/
  void fit( boolean undoable )
  {
    // fit the horizontal range to the task bounds, with a margin on each side
    double width = m_widthSupplier.getAsDouble();
    if ( !canFit() )
      return;

    long millisecondsPP = GanttScale.DEFAULT_MS_PP;
    long left = defaultStart();
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
      left = first - Math.round( ( width * millisecondsPP - span ) / 2.0 );
    }

    if ( undoable )
      changeViewport( left, millisecondsPP, true, bounds );
    else
      updateViewportRange( left, millisecondsPP, bounds, true );
  }

  /**************************************** visibleStart *****************************************/
  private long visibleStart()
  {
    // return the left edge of the visible viewport, including the scroll offset
    return m_scale.milliseconds( 0.0 );
  }

  // task dates read once for each horizontal refresh, without retaining a cached copy
  private record TaskBounds( DateTime start, DateTime end )
  {
  }

  /***************************************** taskBounds ******************************************/
  private TaskBounds taskBounds()
  {
    // return the earliest start and latest end of all tasks in the plan, or null if there are no tasks
    var plan = m_context.getPlan();
    return new TaskBounds( plan.getEarliestTaskStart(), plan.getLatestTaskEnd() );
  }

  /*************************************** changeViewport ****************************************/
  private void changeViewport( long left, long millisecondsPP, boolean fit, TaskBounds bounds )
  {
    // change the horizontal range, and record the change for undo/redo if requested
    if ( !fit && millisecondsPP == m_scale.getMsPP() && left == visibleStart() )
      return;

    // leave room for axis interval boundaries and reject zooms outside the supported dates
    if ( !validViewport( left, millisecondsPP ) )
      return;

    ViewportState oldState = snapshot();
    updateViewportRange( left, millisecondsPP, bounds, fit );
    ViewportState newState = snapshot();

    // fitting an already fitted view must not add an empty command to the history
    if ( oldState.equals( newState ) )
      return;

    var stack = m_context.getUndoStack();
    var previous = stack.getUndoCommand();
    if ( !fit && !stack.isClean() && stack.getIndex() == stack.getSize() && previous instanceof ZoomCommand command
        && command.m_navigation == this && !command.m_fit && command.m_newState.equals( oldState ) )
    {
      // merge uninterrupted zooms in this view without crossing the saved history position
      command.m_newState = newState;
      stack.commandMerged();
    }
    else
      stack.push( new ZoomCommand( oldState, newState, fit ) );
  }

  // snapshots describe undo positions; live scrolling continues to use the scale and scrollbar
  private record ViewportState( long visibleStart, long millisecondsPP, long rangeStart, long rangeEnd )
  {
  }

  /****************************************** snapshot *******************************************/
  private ViewportState snapshot()
  {
    // return a snapshot of the current horizontal range, including the scrollbar range
    return new ViewportState( visibleStart(), m_scale.getMsPP(), m_scale.getStartMilliseconds(), m_rangeEnd );
  }

  /**************************************** validViewport ****************************************/
  private boolean validViewport( long left, long millisecondsPP )
  {
    // return true if the requested horizontal range is within the supported date range, and the scale is valid
    long minimum = DateTime.MIN_VALUE.toSerialMilliseconds() + AXIS_BOUNDARY_PADDING_DAYS * Time.MILLIS_PER_DAY;
    long maximum = DateTime.MAX_VALUE.toSerialMilliseconds() - AXIS_BOUNDARY_PADDING_DAYS * Time.MILLIS_PER_DAY;
    double width = m_widthSupplier.getAsDouble();
    return width > 0.0 && width <= Gantt.MAX_LAYOUT_SIZE && millisecondsPP >= GanttScale.MIN_MS_PP && left >= minimum
        && left <= maximum && width * millisecondsPP <= maximum - left;
  }

  /******************************************* scroll ********************************************/
  private void scroll( double pixels )
  {
    // scroll the visible viewport by the requested number of pixels, preserving the current scale
    long millisecondsPP = m_scale.getMsPP();
    long left = visibleStart() + Math.round( pixels * millisecondsPP );
    if ( !validViewport( left, millisecondsPP ) )
      return;

    // ordinary scrolling changes only the offset; crossing an end extends just that boundary
    double value = m_scrollBar.getValue() + pixels;
    if ( value >= m_scrollBar.getMin() && value <= m_scrollBar.getMax() && m_width == m_widthSupplier.getAsDouble() )
      m_scrollBar.setValue( value );
    else if ( m_rangePending )
      updateViewportRange( left, millisecondsPP, taskBounds(), false );
    else
      setViewportRange( left, millisecondsPP, m_scale.getStartMilliseconds(), m_rangeEnd );
  }

  /************************************* updateViewportRange *************************************/
  private void updateViewportRange( long left, long millisecondsPP, TaskBounds bounds, boolean resetRange )
  {
    // update the horizontal range, and optionally reset the scrollbar range to fit the task bounds
    double width = m_widthSupplier.getAsDouble();
    long right = left + Math.round( width * millisecondsPP );

    // only Fit resets the range; resizing, zooming and task refreshes retain user-added space
    long first = resetRange ? left : Math.min( left, m_scale.getStartMilliseconds() );
    long last = resetRange ? right : Math.max( right, m_rangeEnd );
    long margin = Math.round( Math.max( FIT_MIN_MARGIN_PIXELS, width * FIT_MARGIN_FRACTION ) * millisecondsPP );

    if ( bounds.start() != null && ( resetRange || bounds.start().toSerialMilliseconds() < first ) )
      first = Math.min( first, bounds.start().toSerialMilliseconds() - margin );
    if ( bounds.end() != null && ( resetRange || bounds.end().toSerialMilliseconds() > last ) )
      last = Math.max( last, bounds.end().toSerialMilliseconds() + margin );

    setViewportRange( left, millisecondsPP, first, last );
  }

  /*************************************** setViewportRange **************************************/
  private void setViewportRange( long left, long millisecondsPP, long first, long last )
  {
    // include the current viewport so rebuilding the range never moves the viewed date
    double width = m_widthSupplier.getAsDouble();
    long span = Math.round( width * millisecondsPP );
    first = Math.min( first, left );
    last = Math.max( last, left + span );
    m_width = width;
    m_rangeEnd = last;
    double total = (double) ( last - first ) / millisecondsPP;
    // use the same rounded span so a fractional width still places the final step exactly at maximum
    double maximum = Math.max( 0.0, (double) ( last - first - span ) / millisecondsPP );
    m_updatingScrollBar = true;
    m_scale.setScale( first, millisecondsPP );
    m_scrollBar.setMin( 0.0 );
    m_scrollBar.setMax( maximum );

    // JavaFX expresses the thumb size as a fraction of the scrollable range
    m_scrollBar.setVisibleAmount( maximum > 0.0 ? maximum * width / total : width );
    m_scrollBar.setBlockIncrement( Math.max( 1.0, width * SCROLL_BLOCK_FRACTION ) );
    m_scrollBar.setValue( (double) ( left - first ) / millisecondsPP );
    m_updatingScrollBar = false;
    m_rangePending = false;

    // extending left can leave the scrollbar at zero, so its value listener may not redraw
    m_redraw.run();
  }

  // save the visible start, scale and range so undo also restores space removed by Fit
  private class ZoomCommand implements IUndoCommand
  {
    private final GanttNavigation m_navigation = GanttNavigation.this; // navigation whose state this command restores
    private final ViewportState   m_oldState;                          // original viewport before the first zoom
    private ViewportState         m_newState;                          // viewport after the latest zoom
    private final boolean         m_fit;                               // whether this command explicitly fits the tasks

    /************************************** constructor ******************************************/
    ZoomCommand( ViewportState oldState, ViewportState newState, boolean fit )
    {
      m_oldState = oldState;
      m_newState = newState;
      m_fit = fit;
    }

    /***************************************** redo **********************************************/
    @Override
    public void redo()
    {
      setViewportRange( m_newState.visibleStart(), m_newState.millisecondsPP(), m_newState.rangeStart(),
          m_newState.rangeEnd() );
    }

    /***************************************** undo **********************************************/
    @Override
    public void undo()
    {
      // restore the saved dates and scale rather than recalculating from possibly edited tasks
      setViewportRange( m_oldState.visibleStart(), m_oldState.millisecondsPP(), m_oldState.rangeStart(),
          m_oldState.rangeEnd() );
    }

    /***************************************** text **********************************************/
    @Override
    public String text()
    {
      return m_fit ? "Fit Gantt"
          : "Gantt zoom " + Math.round( 100.0 * GanttScale.DEFAULT_MS_PP / m_newState.millisecondsPP() ) + "%";
    }
  }
}
