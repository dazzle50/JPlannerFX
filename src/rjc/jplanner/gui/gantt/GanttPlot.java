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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import rjc.jplanner.Main;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.jplanner.plan.Calendar;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/***************** GanttPlot provides a view of the plan tasks and dependencies ******************/
/*************************************************************************************************/

class GanttPlot extends Canvas
{
  private TasksView  m_view;  // associated tasks table-view for vertical scaling
  private GanttScale m_scale; // horizontal scaling for this gantt

  /**************************************** constructor ******************************************/
  public GanttPlot( TasksView view, GanttScale scale )
  {
    // construct gantt-scale
    super( 0.0, 0.0 );
    setManaged( false );
    m_view = view;
    m_scale = scale;

    widthProperty().addListener( ( observable, oldW, newW ) -> widthChange( oldW.intValue(), newW.intValue() ) );
    heightProperty().addListener( ( observable, oldH, newH ) -> heightChange( oldH.intValue(), newH.intValue() ) );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // redraw whole gantt-scale
    getGraphicsContext2D().clearRect( 0, 0, getWidth(), getHeight() );
    widthChange( 0, (int) getWidth() );
  }

  /***************************************** widthChange *****************************************/
  private void widthChange( int oldWidth, int newWidth )
  {
    GraphicsContext gc = getGraphicsContext2D();
    int h = (int) getHeight();
    int w = (int) getWidth();

    // draw only if increase in width
    if ( h <= 0 || newWidth <= oldWidth )
      return;

    // draw gantt scale between old-width and new-width
    gc.setFill( Gantt.GANTT_BACKGROUND );
    gc.fillRect( oldWidth, 0, w - oldWidth, h );

    // draw gantt plot
    shadeNonWorking( oldWidth, 0, w - oldWidth, (int) getHeight() );
    drawTasks( 0, (int) getHeight() );
    drawDependencies();
  }

  /**************************************** heightChange *****************************************/
  private void heightChange( int oldHeight, int newHeight )
  {
    GraphicsContext gc = getGraphicsContext2D();
    int h = (int) getHeight();
    int w = (int) getWidth();

    // draw only if increase in height
    if ( w <= 0 || newHeight <= oldHeight )
      return;

    // draw gantt scale between old-height and new-height
    gc.setFill( Gantt.GANTT_BACKGROUND );
    gc.fillRect( 0, oldHeight, w, h - oldHeight );

    // draw gantt plot
    shadeNonWorking( 0, oldHeight, (int) getWidth(), h - oldHeight );
    drawTasks( oldHeight, h - oldHeight );
    drawDependencies();
  }

  /*************************************** shadeNonWorking ****************************************/
  private void shadeNonWorking( int x, int y, int w, int h )
  {
    // shade non-working time on gantt-plot
    Calendar calendar = Main.getPlan().getDefaultCalendar();
    Date date = m_scale.datetime( x - 1 ).getDate();
    int endEpoch = m_scale.datetime( x + w ).getDate().getEpochday();
    int startShadeEpoch;

    GraphicsContext gc = getGraphicsContext2D();
    gc.setFill( Gantt.GANTT_NONWORKING );
    do
    {
      // find start of non-working period
      if ( !calendar.isWorking( date ) )
      {
        startShadeEpoch = date.getEpochday();

        // find end of non-working period
        do
          date.increment();
        while ( date.getEpochday() <= endEpoch && !calendar.isWorking( date ) );

        // if width at least 1 pixel shade non-working period
        long width = ( date.getEpochday() - startShadeEpoch ) * DateTime.MILLISECONDS_IN_DAY / m_scale.getMsPP();
        if ( width > 0L )
        {
          long xe = m_scale.x( new DateTime( date.getEpochday() * DateTime.MILLISECONDS_IN_DAY ) );
          gc.fillRect( xe - width, y, width, h );
        }
      }

      date.increment();
    }
    while ( date.getEpochday() <= endEpoch );
  }

  /****************************************** drawTasks ******************************************/
  private void drawTasks( int y, int h )
  {
    // draw tasks on gantt
  }

  /*************************************** drawDependencies **************************************/
  private void drawDependencies()
  {
    // draw dependencies on gantt for each task
  }
}
