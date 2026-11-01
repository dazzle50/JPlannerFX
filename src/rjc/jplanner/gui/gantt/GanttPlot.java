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
import javafx.scene.canvas.Canvas;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/********************** Canvas and refresh coordination for the Gantt plot **********************/
/*************************************************************************************************/

class GanttPlot extends Canvas
{
  private final GanttRenderer m_renderer;        // draws the plot contents
  private final IListener     m_refreshListener; // requests redraw after model or row changes
  private final ISignal[]     m_refreshSources;  // observed sources while the plot is attached
  private boolean             m_redrawRequested; // whether a redraw is already queued

  /**************************************** constructor ******************************************/
  public GanttPlot( TasksView view, GanttScale scale )
  {
    // construct the plot canvas and its renderer
    super( 0.0, 0.0 );
    setManaged( false );
    m_renderer = new GanttRenderer( view, scale );
    m_refreshListener = ( sender, message ) -> redraw();

    var context = (PlanContext) view.getData().getUserData();
    m_refreshSources = new ISignal[] { view.getData(), view.getData().rowCountProperty(),
        view.getRowsAxis().getTotalPixelsProperty(), view.getZoom(), context.getCalendarsTableData(),
        context.getDaysTableData(), context.getUndoStack() };

    widthProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    heightProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    layoutYProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    view.getCanvas().heightProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );
    view.getVerticalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> redraw() );

    // detach shared model listeners when changing tabs or replacing a plan
    sceneProperty().addListener( ( observable, oldScene, newScene ) ->
    {
      for ( ISignal source : m_refreshSources )
      {
        if ( oldScene != null )
          source.removeListener( m_refreshListener );
        if ( newScene != null )
          source.addListener( m_refreshListener );
      }
      redraw();
    } );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // combine requests and wait until edits and row geometry changes have finished
    if ( m_redrawRequested || getScene() == null )
      return;
    m_redrawRequested = true;
    Platform.runLater( () ->
    {
      m_redrawRequested = false;
      if ( getScene() != null && getWidth() > 0.0 && getHeight() > 0.0 )
        m_renderer.draw( getGraphicsContext2D(), getWidth(), getHeight(), getLayoutY() );
    } );
  }
}
