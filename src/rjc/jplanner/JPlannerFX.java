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
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import rjc.table.Utils;

/*************************************************************************************************/
// Aims to be a project planner similar to M$Project with table entry of tasks & Gantt chart
// Also aims to have automatic resource levelling and scheduling based on task priority
// Also aims to have resource levels variable within single task
// Also aims to have Gantt chart task bar thickness showing this variable resource usage
/*************************************************************************************************/

public class JPlannerFX extends Application
{
  public static Image        JPLANNER_ICON;
  public static final String VERSION = "v0.0.1-alpha WIP";

  /******************************************** main *********************************************/
  public static void main( String[] args )
  {
    // main entry point for application startup
    Utils.trace( "################################# Java properties #################################" );
    for ( Object property : new TreeSet<Object>( System.getProperties().keySet() ) )
      Utils.trace( property + " = '" + System.getProperty( property.toString() ) + "'" );

    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JPlannerFX started ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
    Utils.trace( "JTableFX    VERSION = '" + Utils.VERSION + "'", args );
    Utils.trace( "JPlannerFX  VERSION = '" + VERSION + "'", args );

    // launch demo application display
    launch( args );
    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JPlannerFX ended ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
  }

  /******************************************** start ********************************************/
  @Override
  public void start( Stage primaryStage ) throws Exception
  {
    // create demo application window
    JPLANNER_ICON = new Image( getClass().getResourceAsStream( "JPlannerFX-icon64.png" ) );
    Scene scene = new Scene( new GridPane() );
    primaryStage.setScene( scene );
    primaryStage.setTitle( "JPlannerFX  " + VERSION );
    primaryStage.getIcons().add( JPLANNER_ICON );

    // close demo app when main window is closed (in case other windows are open)
    primaryStage.setOnHidden( event -> Platform.exit() );

    // open demo app window
    primaryStage.setWidth( 1000 );
    primaryStage.setHeight( 600 );
    primaryStage.show();
  }
}
