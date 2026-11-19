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
import java.util.IdentityHashMap;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksData;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.tasks.Dependency.DependencyType;
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/**************** Draws calendar shading, dated tasks, and their dependency arrows ***************/
/*************************************************************************************************/

class GanttRenderer
{
  private static final double SUMMARY_BAND_HEIGHT          = 3.0; // height of a summary's horizontal band
  private static final double DEPENDENCY_CLEARANCE         = 6.0; // minimum space outside each attachment
  private static final double DEPENDENCY_ARROW_LENGTH      = 4.0; // arrowhead length outside the target bar
  private static final double DEPENDENCY_ARROW_HALF_HEIGHT = 2.0; // maximum arrowhead half-height

  private final Plan          m_plan;                             // plan owning this view's task rows and calendar
  private final TasksView     m_view;                             // task data and visible table row geometry
  private final GanttScale    m_scale;                            // horizontal date-time scaling

  // retain off-screen geometry so a dependency can cross the viewport without either bar on screen
  record TaskBar( Task task, double x, double y, double width, double height, double rowTop, double rowBottom,
      boolean summary )
  {
    /*************************************** attachmentY *****************************************/
    double attachmentY()
    {
      double bandHeight = summary ? summaryBandHeight( width, height ) : height;
      return Math.floor( y + bandHeight / 2.0 ) + 0.5;
    }
  }

  /**************************************** constructor ******************************************/
  public GanttRenderer( TasksView view, GanttScale scale )
  {
    // keep the table and plan together because queued draws can outlive context replacement
    m_plan = ( (PlanContext) view.getData().getUserData() ).getPlan();
    m_view = view;
    m_scale = scale;
  }

  /********************************************* draw ********************************************/
  public void draw( GraphicsContext gc, double width, double height, double plotTop )
  {
    // repaint the background before bars so edits and scrolling leave no old shapes
    gc.save();
    try
    {
      gc.setFill( Gantt.GANTT_BACKGROUND );
      gc.fillRect( 0, 0, width, height );
      shadeNonWorking( gc, (int) Math.ceil( width ), height );

      // table headers may be resized or zoomed independently of the timeline headers
      double top = Math.max( 0.0, m_view.getHeaderHeight() - plotTop );
      double bottom = Math.min( height, m_view.getCanvas().getHeight() - plotTop );
      if ( bottom > top )
      {
        // clip arrows and partially visible bars to the same table body boundaries
        gc.beginPath();
        gc.rect( 0, top, width, bottom - top );
        gc.clip();

        var bars = layoutTasks( plotTop );
        drawDependencies( gc, bars, width, top, bottom );
        drawTasks( gc, bars, width, top, bottom );
      }
    }
    finally
    {
      gc.restore();
    }
  }

  /*************************************** shadeNonWorking ***************************************/
  private void shadeNonWorking( GraphicsContext gc, int width, double height )
  {
    // shade whole non-working days using the current project calendar and its exceptions
    Calendar calendar = m_plan.getDefaultCalendar();
    Date date = m_scale.datetime( -1 ).getDate();
    int endEpoch = m_scale.datetime( width ).getDate().getEpochDay();

    gc.setFill( Gantt.GANTT_NONWORKING );
    do
    {
      // find start of non-working period
      if ( !calendar.isWorking( date ) )
      {
        int startShadeEpoch = date.getEpochDay();

        // find end of non-working period
        do
          date = date.plusDays( 1 );
        while ( date.getEpochDay() <= endEpoch && !calendar.isWorking( date ) );

        // if width at least 1 pixel shade non-working period
        long shadeWidth = ( date.getEpochDay() - startShadeEpoch ) * Time.MILLIS_PER_DAY / m_scale.getMsPP();
        if ( shadeWidth > 0L )
        {
          int xe = m_scale.x( DateTime.of( date, Time.MIN_VALUE ) );
          gc.fillRect( xe - shadeWidth, 0, shadeWidth, height );
        }
      }

      date = date.plusDays( 1 );
    }
    while ( date.getEpochDay() <= endEpoch );
  }

  /***************************************** layoutTasks *****************************************/
  private List<TaskBar> layoutTasks( double plotTop )
  {
    var rows = m_view.getRowsAxis();
    var data = (TasksData) m_view.getData();
    var tasks = m_plan.getTasks();
    var bars = new ArrayList<TaskBar>( rows.getCount() );

    for ( int row = 0; row < rows.getCount(); row++ )
    {
      int rowHeight = rows.getPixelSize( row );

      // resolve the current row mapping on each draw so bars follow reordered tasks
      int dataRow = rows.getDataIndex( row );
      if ( rowHeight <= 4 || dataRow <= 0 || data.getValue( FIELD.Title.ordinal(), dataRow ) == null )
        continue;

      // use only existing dates; do not infer a schedule or treat equal dates as a milestone
      Object startValue = data.getValue( FIELD.Start.ordinal(), dataRow );
      Object endValue = data.getValue( FIELD.End.ordinal(), dataRow );
      if ( !( startValue instanceof DateTime start ) || !( endValue instanceof DateTime end )
          || start.toSerialMilliseconds() >= end.toSerialMilliseconds() )
        continue;

      // retain off-screen endpoints; painting rejects bars and routes outside the viewport
      int xs = m_scale.x( start );
      int xe = m_scale.x( end );

      // retain a visible mark for positive intervals shorter than one pixel
      double y = m_view.getRowStartY( row ) - plotTop;
      double barWidth = Math.max( 1.0, (double) xe - xs );
      double barHeight = Math.min( 10, rowHeight - 4 );
      double barY = Math.floor( y + ( rowHeight - barHeight ) / 2.0 );
      bars.add( new TaskBar( tasks.get( dataRow ), xs, barY, barWidth, barHeight, y, y + rowHeight,
          data.isSummary( dataRow ) ) );
    }
    return bars;
  }

  /*************************************** drawDependencies **************************************/
  private void drawDependencies( GraphicsContext gc, List<TaskBar> bars, double width, double top, double bottom )
  {
    // dependencies retain task objects, so resolve identity without searching model or view indexes per arrow
    var byTask = new IdentityHashMap<Task, TaskBar>( bars.size() );
    for ( var bar : bars )
      byTask.put( bar.task(), bar );

    gc.setStroke( Gantt.GANTT_DEPENDENCY );
    gc.setFill( Gantt.GANTT_DEPENDENCY );
    gc.setLineWidth( 1.0 );
    gc.setLineCap( StrokeLineCap.BUTT );
    gc.setLineJoin( StrokeLineJoin.MITER );

    // reuse route coordinates rather than allocating a path object for each dependency
    double[] xs = new double[6];
    double[] ys = new double[6];
    for ( var target : bars )
    {
      var predecessors = target.task().getPredecessors();
      if ( predecessors == null )
        continue;

      for ( int index = 0; index < predecessors.size(); index++ )
      {
        var dependency = predecessors.get( index );
        var source = byTask.get( dependency.task );

        // hidden or undated endpoints have no bar; never transfer a child's link to its summary
        if ( source == null || source == target )
          continue;

        int count = routeDependency( source, target, dependency.type, xs, ys );
        double minX = xs[0];
        double maxX = xs[0];
        double minY = ys[0];
        double maxY = ys[0];
        for ( int point = 1; point < count; point++ )
        {
          minX = Math.min( minX, xs[point] );
          maxX = Math.max( maxX, xs[point] );
          minY = Math.min( minY, ys[point] );
          maxY = Math.max( maxY, ys[point] );
        }
        // include stroke and arrowhead height when rejecting routes, including off-screen endpoints
        if ( maxX < -0.5 || minX > width + 0.5 || maxY < top - DEPENDENCY_ARROW_HALF_HEIGHT
            || minY > bottom + DEPENDENCY_ARROW_HALF_HEIGHT )
          continue;

        gc.beginPath();
        gc.moveTo( xs[0], ys[0] );
        for ( int point = 1; point < count; point++ )
          gc.lineTo( xs[point], ys[point] );
        gc.stroke();

        // clipping must not create an arrowhead at a viewport edge in place of the real target
        double x = xs[count - 1];
        double y = ys[count - 1];
        if ( x >= 0.0 && x < width && y >= top && y < bottom )
          drawArrowhead( gc, x, y, Math.signum( x - xs[count - 2] ), target.rowBottom() - target.rowTop() );
      }
    }
  }

  /*************************************** routeDependency ***************************************/
  static int routeDependency( TaskBar source, TaskBar target, DependencyType type, double[] xs, double[] ys )
  {
    boolean sourceStart = type == DependencyType.SS || type == DependencyType.SF;
    boolean targetStart = type == DependencyType.FS || type == DependencyType.SS;
    double sourceX = source.x() + ( sourceStart ? 0.0 : source.width() );
    double targetX = target.x() + ( targetStart ? 0.0 : target.width() );
    double sourceY = source.attachmentY();
    double targetY = target.attachmentY();

    // half-pixel lanes keep one-pixel strokes sharp while preserving at least the requested clearance
    double sourceOutside = sourceX + ( sourceStart ? -1.0 : 1.0 ) * ( DEPENDENCY_CLEARANCE + 0.5 );
    double targetOutside = targetX + ( targetStart ? -1.0 : 1.0 ) * ( DEPENDENCY_CLEARANCE + 0.5 );
    xs[0] = sourceX;
    ys[0] = sourceY;

    double lane;
    if ( sourceStart == targetStart )
      lane = sourceStart ? Math.min( sourceOutside, targetOutside ) : Math.max( sourceOutside, targetOutside );
    else if ( sourceStart ? targetOutside <= sourceOutside : sourceOutside <= targetOutside )
      lane = Math.floor( ( sourceOutside + targetOutside ) / 2.0 ) + 0.5;
    else
    {
      // overlapping or reversed bars need a horizontal crossing outside the source row's bar
      double gutter = Math.floor( target.rowTop() > source.rowTop() ? source.rowBottom() : source.rowTop() ) + 0.5;
      xs[1] = sourceOutside;
      ys[1] = sourceY;
      xs[2] = sourceOutside;
      ys[2] = gutter;
      xs[3] = targetOutside;
      ys[3] = gutter;
      xs[4] = targetOutside;
      ys[4] = targetY;
      xs[5] = targetX;
      ys[5] = targetY;
      return 6;
    }

    xs[1] = lane;
    ys[1] = sourceY;
    xs[2] = lane;
    ys[2] = targetY;
    xs[3] = targetX;
    ys[3] = targetY;
    return 4;
  }

  /**************************************** drawArrowhead ****************************************/
  private void drawArrowhead( GraphicsContext gc, double x, double y, double direction, double rowHeight )
  {
    double halfHeight = Math.min( DEPENDENCY_ARROW_HALF_HEIGHT, ( rowHeight - 4.0 ) / 2.0 );
    double base = x - direction * DEPENDENCY_ARROW_LENGTH;
    gc.beginPath();
    gc.moveTo( x, y );
    gc.lineTo( base, y - halfHeight );
    gc.lineTo( base, y + halfHeight );
    gc.closePath();
    gc.fill();
  }

  /****************************************** drawTasks ******************************************/
  private void drawTasks( GraphicsContext gc, List<TaskBar> bars, double width, double top, double bottom )
  {
    for ( var bar : bars )
    {
      if ( bar.rowTop() >= bottom )
        break;
      if ( bar.rowBottom() <= top || bar.x() >= width || bar.x() + bar.width() < 0.0 )
        continue;

      if ( bar.summary() )
        drawSummaryBar( gc, bar.x(), bar.y(), bar.width(), bar.height() );
      else
        drawTaskBar( gc, bar.x(), bar.y(), bar.width(), bar.height() );
    }
  }

  /**************************************** drawTaskBar ******************************************/
  private void drawTaskBar( GraphicsContext gc, double x, double y, double width, double height )
  {
    gc.setFill( Gantt.GANTT_TASK_FILL );
    gc.setStroke( Gantt.GANTT_TASK_EDGE );
    gc.setLineWidth( 1.0 );
    gc.fillRect( x, y, width, height );
    gc.strokeRect( x + 0.5, y + 0.5, width - 1.0, height - 1.0 );
  }

  /*************************************** drawSummaryBar ****************************************/
  private void drawSummaryBar( GraphicsContext gc, double x, double y, double width, double height )
  {
    gc.setFill( Gantt.GANTT_SUMMARY );
    double bandHeight = summaryBandHeight( width, height );

    // keep compact intervals solid when there is no room for distinct end markers
    if ( bandHeight == height )
    {
      gc.fillRect( x, y, width, height );
      return;
    }

    double capWidth = Math.min( 5.0, width / 2.0 );

    // keep tips at the true interval boundaries so viewport clipping cannot create false ends
    gc.beginPath();
    gc.moveTo( x, y );
    gc.lineTo( x + width, y );
    gc.lineTo( x + width, y + height );
    gc.lineTo( x + width - capWidth, y + bandHeight );
    gc.lineTo( x + capWidth, y + bandHeight );
    gc.lineTo( x, y + height );
    gc.closePath();
    gc.fill();
  }

  /************************************** summaryBandHeight **************************************/
  private static double summaryBandHeight( double width, double height )
  {
    // use the same band for drawing and attachment, including the compact rectangle fallback
    return width < 4.0 || height < 5.0 ? height : SUMMARY_BAND_HEIGHT;
  }
}
