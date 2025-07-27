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
import rjc.table.data.types.DateTime.Interval;

/*************************************************************************************************/
/************************* Provides visual timeline showing gantt scale **************************/
/*************************************************************************************************/

class GanttAxis extends Canvas
{
  private Interval m_interval;
  private String   m_format;

  /**************************************** constructor ******************************************/
  public GanttAxis( GanttScale m_scale )
  {
    // construct gantt-scale
    super( 0.0, Gantt.GANTTSCALE_HEIGHT );
    m_interval = Interval.MONTH;
    m_format = "mmm";

    widthProperty().addListener( ( observable, oldW, newW ) -> widthChange( oldW.intValue(), newW.intValue() ) );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // redraw whole gantt-scale
    widthChange( 0, (int) getWidth() );
  }

  /***************************************** widthChange *****************************************/
  private void widthChange( int oldW, int newW )
  {
    // draw only if increase in width
    if ( getHeight() <= 0.0 || newW <= oldW )
      return;

    // draw gantt scale between old-width and new-width
    GraphicsContext gc = getGraphicsContext2D();
  }

}
