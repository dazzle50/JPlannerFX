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

import java.util.function.DoubleConsumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.util.Duration;
import rjc.table.view.TableScrollBar;

/*************************************************************************************************/
/*************** Extended version of ScrollBar with special increment & decrement ****************/
/*************************************************************************************************/

class GanttScrollBar extends ScrollBar
{
  private static final double            SCROLL_STEP = 4.0; // pixels moved by each arrow step

  private final DoubleConsumer           m_scroll;          // scrolls and extends the owning gantt
  private final Timeline                 m_repeat;          // repeats a held arrow at the normal JavaFX rate
  private double                         m_scrollPixels;    // signed step for the currently held arrow
  private final EventHandler<MouseEvent> m_releaseListener; // stops repeating even when released outside the button
  private final ChangeListener<Boolean>  m_focusListener;   // stops repeating when the window loses focus
  private final ChangeListener<Window>   m_windowListener;  // follows the window displaying this scrollbar

  /**************************************** constructor ******************************************/
  public GanttScrollBar( DoubleConsumer scroll )
  {
    // construct gantt scroll-bar
    m_scroll = scroll;
    setPrefHeight( TableScrollBar.SIZE );
    setUnitIncrement( SCROLL_STEP );

    // intercept only arrow presses; the standard skin still handles the thumb and track
    m_repeat = new Timeline( new KeyFrame( Duration.millis( 200 ), event -> m_scroll.accept( m_scrollPixels ) ) );
    m_repeat.setCycleCount( Timeline.INDEFINITE );
    addEventFilter( MouseEvent.MOUSE_PRESSED, this::mousePressed );
    m_releaseListener = event -> stopScrolling();
    m_focusListener = ( observable, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        stopScrolling();
    };
    m_windowListener = ( observable, oldWindow, newWindow ) -> windowChanged( oldWindow, newWindow );

    // release and focus listeners belong only to the scene and window currently displaying this control
    sceneProperty().addListener( ( observable, oldScene, newScene ) ->
    {
      stopScrolling();
      if ( oldScene != null )
      {
        oldScene.removeEventFilter( MouseEvent.MOUSE_RELEASED, m_releaseListener );
        oldScene.windowProperty().removeListener( m_windowListener );
        windowChanged( oldScene.getWindow(), null );
      }
      if ( newScene != null )
      {
        newScene.addEventFilter( MouseEvent.MOUSE_RELEASED, m_releaseListener );
        newScene.windowProperty().addListener( m_windowListener );
        windowChanged( null, newScene.getWindow() );
      }
    } );
    disabledProperty().addListener( ( observable, oldDisabled, newDisabled ) ->
    {
      if ( newDisabled )
        stopScrolling();
    } );
    skinProperty().addListener( ( observable, oldSkin, newSkin ) -> stopScrolling() );
  }

  /**************************************** mousePressed *****************************************/
  private void mousePressed( MouseEvent event )
  {
    // only intercept mouse presses on the arrow buttons, not the thumb or track
    if ( event.getButton() != MouseButton.PRIMARY || isDisabled() )
      return;

    // the native button behaviour suppresses boundary presses, including a fully visible range
    Node node = event.getTarget() instanceof Node target ? target : null;
    while ( node != null && node != this )
    {
      boolean increment = node.getStyleClass().contains( "increment-button" );
      boolean decrement = node.getStyleClass().contains( "decrement-button" );
      if ( increment || decrement )
      {
        event.consume();
        stopScrolling();
        if ( isFocusTraversable() )
          requestFocus();
        m_scrollPixels = increment ? SCROLL_STEP : -SCROLL_STEP;
        m_scroll.accept( m_scrollPixels );
        m_repeat.playFromStart();
        return;
      }
      node = node.getParent();
    }
  }

  /**************************************** windowChanged ****************************************/
  private void windowChanged( Window oldWindow, Window newWindow )
  {
    // stop scrolling when the window loses focus
    stopScrolling();
    if ( oldWindow != null )
      oldWindow.focusedProperty().removeListener( m_focusListener );
    if ( newWindow != null )
      newWindow.focusedProperty().addListener( m_focusListener );
  }

  /**************************************** stopScrolling ****************************************/
  private void stopScrolling()
  {
    // stop repeating a held arrow, if any
    m_repeat.stop();
  }

  /****************************************** increment ******************************************/
  @Override
  public void increment()
  {
    // make gantt end later if scroll-bar at maximum
    m_scroll.accept( SCROLL_STEP );
  }

  /****************************************** decrement ******************************************/
  @Override
  public void decrement()
  {
    // make gantt start earlier if scroll-bar at minimum
    m_scroll.accept( -SCROLL_STEP );
  }

}
