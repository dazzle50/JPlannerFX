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

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import rjc.table.data.types.DateTime;
import rjc.table.view.Colours;

/*************************************************************************************************/
/************************* Provides visual timeline showing gantt scale **************************/
/*************************************************************************************************/

class GanttAxis extends Canvas
{
  private final GanttScale     m_scale;             // scale defines starts and scaling size
  private GanttAxisInterval    m_interval;          // aligned axis interval step
  private GanttAxisLabelFormat m_labelFormat;       // selected label format
  private final Text           m_text = new Text(); // reusable label measurement
  private double               m_zoom = 1.0;        // table zoom applied to label text
  private boolean              m_redrawRequested;   // whether a redraw is already queued

  /**************************************** constructor ******************************************/
  GanttAxis( GanttScale scale, GanttAxisInterval interval )
  {
    // construct timeline axis
    super( 0.0, Gantt.GANTTAXIS_HEIGHT );
    setManaged( false );

    m_scale = scale;
    m_interval = interval;
    m_labelFormat = interval.getDefaultFormat();

    widthProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    heightProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    sceneProperty().addListener( ( observable, oldScene, newScene ) -> redraw() );
  }

  /**************************************** getInterval ******************************************/
  GanttAxisInterval getInterval()
  {
    // return current axis interval
    return m_interval;
  }

  /**************************************** setInterval ******************************************/
  void setInterval( GanttAxisInterval interval )
  {
    // set new axis interval and default label format
    m_interval = interval;
    redraw();
  }

  /*************************************** getLabelFormat ****************************************/
  GanttAxisLabelFormat getLabelFormat()
  {
    // return current axis label format
    return m_labelFormat;
  }

  /*************************************** setLabelFormat ****************************************/
  void setLabelFormat( GanttAxisLabelFormat format )
  {
    // set new axis label format
    m_labelFormat = format;
    redraw();
  }

  /****************************************** setZoom ********************************************/
  void setZoom( double zoom )
  {
    // set new zoom factor for axis label text
    if ( m_zoom == zoom )
      return;
    m_zoom = zoom;
    m_text.setFont( Font.font( Font.getDefault().getFamily(), Font.getDefault().getSize() * zoom ) );
    redraw();
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // combine geometry and navigation redraw requests on the JavaFX application thread
    if ( m_redrawRequested || getScene() == null )
      return;
    m_redrawRequested = true;
    Platform.runLater( () ->
    {
      m_redrawRequested = false;
      if ( getScene() != null && getWidth() > 0.0 && getHeight() > 0.0 )
        draw();
    } );
  }

  /******************************************** draw *********************************************/
  private void draw()
  {
    // repaint the axis using the same font for measurement and drawing
    GraphicsContext gc = getGraphicsContext2D();
    gc.clearRect( 0, 0, getWidth(), getHeight() );
    gc.setFont( m_text.getFont() );
    double y = getHeight() - 0.5;
    gc.setStroke( Colours.CELL_BORDER );
    gc.strokeLine( 0, y, getWidth(), y );

    // boundary coordinates may lie outside compact dates for a partially visible first or last period
    long minimum = DateTime.MIN_VALUE.toSerialMilliseconds();
    long maximum = DateTime.MAX_VALUE.toSerialMilliseconds();
    long start = m_interval.startOf( m_scale.milliseconds( 0.0 ) );
    double xs = m_scale.x( start );

    while ( xs < getWidth() )
    {
      long end = m_interval.next( start );
      double xe = m_scale.x( end );

      // draw interval divider
      if ( xe > 0.0 && xe <= getWidth() )
        gc.strokeLine( Math.floor( xe ) - 0.5, 0, Math.floor( xe ) - 0.5, y );

      // only format labels with visible space and a representable true period start
      double left = Math.max( 0.0, xs );
      double ww = Math.min( getWidth(), xe ) - left - 3.0;
      if ( ww > 1.0 && start >= minimum && start <= maximum )
      {
        String label = m_labelFormat.format( DateTime.ofSerialMilliseconds( start ) );
        m_text.setText( label );
        Bounds bounds = m_text.getLayoutBounds();
        double yy = ( getHeight() - bounds.getHeight() ) / 2.0 - bounds.getMinY() - 1.0;
        // keep labels visible when only part of a long calendar interval is on screen
        double xx = left + 1.0;
        if ( ww > bounds.getWidth() )
          xx += ( ww - bounds.getWidth() ) / 2.0;
        gc.fillText( label, xx, yy, ww );
      }

      if ( xe >= getWidth() )
        break;

      // jump over subpixel intervals so work is bounded by viewport width, even at second resolution
      long nextPixel = m_scale.milliseconds( Math.floor( xe ) + 1.0 );
      start = Math.max( end, m_interval.startOf( nextPixel ) );
      xs = m_scale.x( start );
    }
  }

}
