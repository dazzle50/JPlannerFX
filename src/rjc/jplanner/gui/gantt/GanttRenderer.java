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

import javafx.scene.canvas.GraphicsContext;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/********************* Draws calendar shading and dated tasks on a Gantt plot ********************/
/*************************************************************************************************/

class GanttRenderer
{
  private final TasksView  m_view;  // task data and visible table row geometry
  private final GanttScale m_scale; // horizontal date-time scaling

  /**************************************** constructor ******************************************/
  public GanttRenderer( TasksView view, GanttScale scale )
  {
    m_view = view;
    m_scale = scale;
  }

  /********************************************* draw ********************************************/
  public void draw( GraphicsContext gc, double width, double height, double plotTop )
  {
    // repaint the background before bars so edits and scrolling leave no old shapes
    gc.save();
    gc.setFill( Gantt.GANTT_BACKGROUND );
    gc.fillRect( 0, 0, width, height );
    shadeNonWorking( gc, (int) Math.ceil( width ), height );
    drawTasks( gc, width, height, plotTop );
    gc.restore();
  }

  /*************************************** shadeNonWorking ***************************************/
  private void shadeNonWorking( GraphicsContext gc, int width, double height )
  {
    // shade whole non-working days using the current project calendar and its exceptions
    Calendar calendar = ( (PlanContext) m_view.getData().getUserData() ).getPlan().getDefaultCalendar();
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

  /****************************************** drawTasks ******************************************/
  private void drawTasks( GraphicsContext gc, double width, double height, double plotTop )
  {
    var rows = m_view.getRowsAxis();
    var data = m_view.getData();

    // table headers may be resized or zoomed independently of the timeline headers
    double top = Math.max( 0.0, m_view.getHeaderHeight() - plotTop );
    double bottom = Math.min( height, m_view.getCanvas().getHeight() - plotTop );
    if ( bottom <= top )
      return;

    // clip partially visible rows at the table body boundaries
    gc.beginPath();
    gc.rect( 0, top, width, bottom - top );
    gc.clip();
    gc.setFill( Gantt.GANTT_TASK_FILL );
    gc.setStroke( Gantt.GANTT_TASK_EDGE );
    gc.setLineWidth( 1.0 );

    int firstRow = m_view.getRowIndex( (int) Math.ceil( plotTop + top ) );
    for ( int row = firstRow; row >= 0 && row < rows.getCount(); row++ )
    {
      double y = m_view.getRowStartY( row ) - plotTop;
      if ( y >= bottom )
        break;

      int rowHeight = rows.getPixelSize( row );

      // resolve the current row mapping on each draw so bars follow reordered tasks
      int dataRow = rows.getDataIndex( row );
      if ( rowHeight <= 4 || dataRow == 0 || data.getValue( FIELD.Title.ordinal(), dataRow ) == null )
        continue;

      // use only existing dates; do not infer a schedule or treat equal dates as a milestone
      Object startValue = data.getValue( FIELD.Start.ordinal(), dataRow );
      Object endValue = data.getValue( FIELD.End.ordinal(), dataRow );
      if ( !( startValue instanceof DateTime start ) || !( endValue instanceof DateTime end )
          || start.toSerialMilliseconds() >= end.toSerialMilliseconds() )
        continue;

      // skip bars outside the horizontal viewport
      int xs = m_scale.x( start );
      int xe = m_scale.x( end );
      if ( xs >= width || xe < 0 )
        continue;

      // retain a visible mark for positive intervals shorter than one pixel
      double barWidth = Math.max( 1.0, (double) xe - xs );
      double barHeight = Math.min( 10, rowHeight - 4 );
      double barY = Math.floor( y + ( rowHeight - barHeight ) / 2.0 );
      gc.fillRect( xs, barY, barWidth, barHeight );
      gc.strokeRect( xs + 0.5, barY + 0.5, barWidth - 1.0, barHeight - 1.0 );
    }
  }
}
