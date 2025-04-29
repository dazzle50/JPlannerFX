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

package rjc.jplanner;

import java.util.TreeSet;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import rjc.jplanner.gui.MainWindow;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
// Aims to be a project planner similar to M$Project with table entry of tasks & Gantt chart
// Also aims to have automatic resource levelling and scheduling based on task priority
// Also aims to have resource levels variable within single task
// Also aims to have Gantt chart task bar thickness showing this variable resource usage
/*************************************************************************************************/

public class Main extends Application
{
  public static final String      VERSION = "v0.1.0 WIP";

  private static Image            m_icon;                // icon for application
  private static Plan             m_plan;                // current active plan
  private static ObservableStatus m_status;              // current application status
  private static UndoStack        m_undostack;           // current active undostack

  /******************************************** main *********************************************/
  public static void main( String[] args )
  {
    // main entry point for application startup
    Utils.trace( "############################### Java properties ###############################" );
    for ( Object property : new TreeSet<>( System.getProperties().keySet() ) )
      Utils.trace( property + " = '" + System.getProperty( property.toString() ) + "'" );

    Utils.trace( "JTableFX    VERSION = '" + Utils.VERSION + "'", args );
    Utils.trace( "JPlannerFX  VERSION = '" + VERSION + "'", args );
    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JPlannerFX started ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

    // prepare application
    m_icon = new Image( Main.class.getResourceAsStream( "JPlannerFX-icon64.png" ) );
    m_status = new ObservableStatus();
    m_undostack = new UndoStack();
    m_plan = new Plan();
    m_plan.initialise();
    Utils.trace( "PLAN INITIALISED : ", m_plan );

    // launch application display
    launch( args );

    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JPlannerFX ended ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
  }

  /******************************************** start ********************************************/
  @Override
  public void start( Stage primaryStage ) throws Exception
  {
    // prepare application display
    primaryStage.setTitle( "JPlannerFX  " + VERSION );
    primaryStage.getIcons().add( m_icon );
    primaryStage.setScene( new MainWindow() );

    // close app when main window is closed (in case other windows are open)
    primaryStage.setOnHidden( event -> Platform.exit() );

    // open app window
    primaryStage.setWidth( 1000 );
    primaryStage.setHeight( 600 );
    primaryStage.show();
  }

  /******************************************* getPlan *******************************************/
  public static Plan getPlan()
  {
    // return application current active plan
    return m_plan;
  }

  /****************************************** getStatus ******************************************/
  public static ObservableStatus getStatus()
  {
    // return application current status property
    return m_status;
  }

  /***************************************** getUndostack ****************************************/
  public static UndoStack getUndostack()
  {
    // return application current active undo-stack
    return m_undostack;
  }
}
