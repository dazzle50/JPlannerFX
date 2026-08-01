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

package rjc.jplanner.gui;

import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************** Main JPlannerFX application window contents **************************/
/*************************************************************************************************/

public class MainWindow extends Scene
{
  private GridPane    m_grid;      // root grid layout container
  private PlanContext m_context;   // plan context shared across main window components
  private MenuBar     m_menus;     // menus at top of scene
  private TabPane     m_tabs;      // tabs containing application functionality
  private TextField   m_statusBar; // status bar at bottom of scene

  /**************************************** constructor ******************************************/
  public MainWindow()
  {
    // main window scene based on grid layout
    super( new GridPane() );
    m_grid = (GridPane) getRoot();

    // create the plan context, menus, and status bar
    m_context = new PlanContext( this );
    m_menus = new MainMenus( m_context );
    m_statusBar = createStatusBar( m_context.getStatus() );

    // arrange the grid
    m_grid.add( m_menus, 0, 0 );
    m_grid.add( m_statusBar, 0, 2 );
    rebuildTabs();
  }

  /************************************* createStatusBar *****************************************/
  private TextField createStatusBar( ObservableStatus status )
  {
    // create selectable text status-bar for displaying status messages
    TextField statusBar = new TextField();
    statusBar.setFocusTraversable( false );
    statusBar.setEditable( false );
    statusBar.setBackground( new Background( new BackgroundFill( Color.gray( 0.96 ), null, null ) ) );

    // display status changes on status-bar using runLater so can handle signals from other threads
    status.addLaterListener( ( sender, msg ) ->
    {
      statusBar.setText( status.getMessage() );
      statusBar.setStyle( status.getStyle() );
    } );
    status.update( Level.INFO, "JPlannerFX has started" );
    status.clearAfterMillisecs( 2500 );

    return statusBar;
  }

  /***************************************** rebuildTabs *****************************************/
  public void rebuildTabs()
  {
    // do nothing if plan not yet available
    if ( m_context == null )
      return;

    // recreate the main window tabs using stored context
    if ( m_tabs != null )
      m_grid.getChildren().remove( m_tabs );

    m_tabs = new MainTabs( m_context );
    m_grid.add( m_tabs, 0, 1 );
    GridPane.setHgrow( m_tabs, Priority.ALWAYS );
    GridPane.setVgrow( m_tabs, Priority.ALWAYS );
  }

}