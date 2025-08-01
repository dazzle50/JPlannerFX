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

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Text;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.DateTime.Interval;
import rjc.table.view.Colours;

/*************************************************************************************************/
/************************* Provides visual timeline showing gantt scale **************************/
/*************************************************************************************************/

class GanttAxis extends Canvas
{
  private GanttScale m_scale;    // scale defines starts and scaling size
  private Interval   m_interval; // axis interval step
  private String     m_format;   // axis interval label format

  /**************************************** constructor ******************************************/
  public GanttAxis( GanttScale scale, Interval interval )
  {
    // construct gantt-scale
    super( 0.0, Gantt.GANTTAXIS_HEIGHT );
    setManaged( false );

    m_scale = scale;
    m_interval = interval;
    m_format = interval == Interval.MONTH ? "MMM-yy" : "d";

    widthProperty().addListener( ( observable, oldW, newW ) -> widthChange( oldW.intValue(), newW.intValue() ) );
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
    // draw only if increase in width
    if ( getHeight() <= 0.0 || newWidth <= oldWidth )
      return;

    // draw gantt scale between old-width and new-width
    GraphicsContext gc = getGraphicsContext2D();
    double y = getHeight() - 0.5;
    gc.setStroke( Colours.CELL_BORDER );
    gc.strokeLine( oldWidth, y, newWidth, y );

    // determine first interval
    DateTime start = m_scale.datetime( oldWidth ).getTruncated( m_interval );
    DateTime end = start.plusInterval( m_interval );
    int xs = m_scale.x( start );
    int xe = m_scale.x( end );

    // draw intervals until past new-width
    do
    {
      // draw interval divider
      gc.clearRect( xs + 1.0, 0, xe - xs, getHeight() - 1.0 );
      gc.strokeLine( xe - 0.5, 0, xe - 0.5, y );

      // if enough width, add label
      double ww = xe - xs - 3.0;
      if ( ww > 1.0 )
      {
        String label = start.toString( m_format );
        Bounds bounds = new Text( label ).getLayoutBounds();
        double yy = ( getHeight() - bounds.getHeight() ) / 2.0 - bounds.getMinY() - 1.0;
        double xx = xs + 1.0;
        if ( ww > bounds.getWidth() )
          xx += ( ww - bounds.getWidth() ) / 2.0;
        gc.fillText( label, xx, yy, ww );
      }

      // move on to next interval
      start = end;
      xs = xe;
      end = end.plusInterval( m_interval );
      xe = m_scale.x( end );
    }
    while ( xs < newWidth );
  }

}
