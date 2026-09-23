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

import java.io.File;

import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import rjc.jplanner.storage.FileLoad;
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
  private HBox        m_statusBar; // status bar at bottom of scene
  private TextField   m_jobField;  // job status field in status bar

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
    loadDefault();
    rebuildTabs();

    m_context.getStatus().update( Level.INFO, "JPlannerFX has started" );
    m_context.getStatus().clearAfterMillisecs( 2500 );

  }

  /**************************************** loadDefault ******************************************/
  private void loadDefault()
  {
    // attempt to load default plan from current directory, or if not found, from parent directory
    new FileLoad( m_context, new File( "!default.xml" ) );
    if ( m_context.getStatus().isError() )
      new FileLoad( m_context, new File( "../!default.xml" ) );
  }

  /************************************* createStatusBar *****************************************/
  private HBox createStatusBar( ObservableStatus status )
  {
    // create two selectable text fields side by side
    TextField statusField = new TextField();
    m_jobField = new TextField();
    for ( TextField field : new TextField[] { statusField, m_jobField } )
    {
      field.setFocusTraversable( false );
      field.setEditable( false );
      field.setBackground( new Background( new BackgroundFill( Color.gray( 0.96 ), null, null ) ) );
      field.setMinWidth( 0.0 );
      field.setMaxWidth( Double.MAX_VALUE );
    }

    // size the job field to its text, including insets and room for the caret
    Text text = new Text();
    text.textProperty().bind( m_jobField.textProperty() );
    text.fontProperty().bind( m_jobField.fontProperty() );
    m_jobField.prefWidthProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> Math.ceil( text.getLayoutBounds().getWidth() ) + m_jobField.getInsets().getLeft()
                    + m_jobField.getInsets().getRight() + 2.0,
                text.layoutBoundsProperty(), m_jobField.insetsProperty() ) );
    m_jobField.setMinWidth( Region.USE_PREF_SIZE );
    m_jobField.setMaxWidth( Region.USE_PREF_SIZE );
    HBox.setHgrow( statusField, Priority.ALWAYS );

    // display status changes on status-bar using runLater so can handle signals from other threads
    status.addLaterListener( ( sender, msg ) ->
    {
      statusField.setText( status.getMessage() );
      statusField.setStyle( status.getStyle() );
    } );

    return new HBox( statusField, m_jobField );
  }

  /*************************************** getJobTextField ***************************************/
  public TextField getJobTextField()
  {
    // return the job status text field in the status bar
    return m_jobField;
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