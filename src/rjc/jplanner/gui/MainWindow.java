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
import rjc.jplanner.plan.Plan;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************** Main JPlannerFX application window contents **************************/
/*************************************************************************************************/

public class MainWindow extends Scene
{
  private MenuBar   m_menus;     // menus at top of scene
  private TabPane   m_tabs;      // tabs containing application functionality
  private TextField m_statusBar; // status bar at bottom of scene

  /**************************************** constructor ******************************************/
  public MainWindow()
  {
    // main window scene based on grid layout
    super( new GridPane() );

    // prepare initial plan and table data sources
    Plan plan = new Plan();
    plan.initialise();
    PlanContext context = new PlanContext( plan );

    // prepare components
    m_menus = new MainMenus( context );
    m_tabs = new MainTabs( context );
    m_statusBar = createStatusBar( context.getStatus() );

    // arrange the grid
    GridPane grid = (GridPane) getRoot();
    grid.add( m_menus, 0, 0 );
    grid.add( m_tabs, 0, 1 );
    grid.add( m_statusBar, 0, 2 );
    GridPane.setHgrow( m_tabs, Priority.ALWAYS );
    GridPane.setVgrow( m_tabs, Priority.ALWAYS );
  }

  /**************************************** getStatusBar *****************************************/
  private TextField createStatusBar( ObservableStatus status )
  {
    // create status-bar for displaying status messages
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
}
