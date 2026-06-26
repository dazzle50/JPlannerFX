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

package rjc.jplanner.gui.menu;

import java.io.File;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.stage.FileChooser;
import rjc.jplanner.gui.PlanContext;
import rjc.table.Utils;

/*************************************************************************************************/
/**************************** File menu for main application menu bar ****************************/
/*************************************************************************************************/

public class FileMenu extends Menu
{
  private static Modifier CONTROL = KeyCombination.CONTROL_DOWN;

  private PlanContext     m_context;

  /***************************************** constructor *****************************************/
  public FileMenu( PlanContext context )
  {
    // construct file menu for main window menu bar
    setText( "File" );
    m_context = context;

    getItems().add( newPlan() );
    getItems().add( open() );
    getItems().add( save() );
    getItems().add( saveAs() );
    getItems().add( new SeparatorMenuItem() );
    getItems().add( printPreview() );
    getItems().add( print() );
    getItems().add( new SeparatorMenuItem() );
    getItems().add( exit() );
  }

  /******************************************* newPlan *******************************************/
  private MenuItem newPlan()
  {
    // start a new plan
    MenuItem fileNew = new MenuItem( "New" );
    fileNew.setAccelerator( new KeyCodeCombination( KeyCode.N, CONTROL ) );
    fileNew.setOnAction( event -> Utils.trace( "NEW - NOT YET IMPLEMENTED" ) );
    return fileNew;
  }

  /******************************************** open *********************************************/
  private MenuItem open()
  {
    // open a plan file
    MenuItem fileOpen = new MenuItem( "Open..." );
    fileOpen.setAccelerator( new KeyCodeCombination( KeyCode.O, CONTROL ) );

    fileOpen.setOnAction( event ->
    {
      // TODO check if current plan is saved, and prompt user to save if not
      // use file-chooser defaulting (if available) to current plan location
      FileChooser fc = new FileChooser();
      fc.setTitle( "Open plan" );
      File initialDirectory = new File( m_context.getPlan().getFileLocation() );
      if ( initialDirectory.isDirectory() )
        fc.setInitialDirectory( initialDirectory );
      fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "JPlannerFX files (*.xml)", "*.xml" ) );
      fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "All files (*.*)", "*.*" ) );
      File file = fc.showOpenDialog( m_context.getGui().getWindow() );

      // if user cancels file is null, so exit immediately
      if ( file == null )
        return;

      // attempt to load from user specified file
      Utils.trace( "OPEN - NOT YET IMPLEMENTED", file );
    } );

    return fileOpen;
  }

  /******************************************** save *********************************************/
  private MenuItem save()
  {
    // save plan to file
    MenuItem fileSave = new MenuItem( "Save" );
    fileSave.setAccelerator( new KeyCodeCombination( KeyCode.S, CONTROL ) );
    fileSave.setOnAction( event -> Utils.trace( "SAVE - NOT YET IMPLEMENTED" ) );
    return fileSave;
  }

  /******************************************* saveAs ********************************************/
  private MenuItem saveAs()
  {
    // save plan to a new file
    MenuItem fileSaveAs = new MenuItem( "Save As..." );
    fileSaveAs.setOnAction( event ->
    {
      // use file-chooser defaulting (if available) to current plan location and filename
      FileChooser fc = new FileChooser();
      fc.setTitle( "Save plan" );
      File initialDirectory = new File( m_context.getPlan().getFileLocation() );
      if ( initialDirectory.isDirectory() )
        fc.setInitialDirectory( initialDirectory );
      fc.setInitialFileName( m_context.getPlan().getFilename() );
      fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "JPlannerFX files (*.xml)", "*.xml" ) );
      fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "All files (*.*)", "*.*" ) );
      File file = fc.showSaveDialog( m_context.getGui().getWindow() );

      // if user cancels file is null, so exit immediately
      if ( file == null )
        return;

      // attempt to save to user specified file
      m_context.getStorageIO().save( m_context, file );
    } );

    return fileSaveAs;
  }

  /**************************************** printPreview *****************************************/
  private MenuItem printPreview()
  {
    // show print preview
    MenuItem filePrintPreview = new MenuItem( "Print preview..." );
    filePrintPreview.setDisable( true );
    return filePrintPreview;
  }

  /******************************************** print ********************************************/
  private MenuItem print()
  {
    // print
    MenuItem filePrint = new MenuItem( "Print..." );
    filePrint.setDisable( true );
    return filePrint;
  }

  /******************************************** exit *********************************************/
  private MenuItem exit()
  {
    // exit the application
    MenuItem fileExit = new MenuItem( "Exit" );
    fileExit.setAccelerator( new KeyCodeCombination( KeyCode.Q, CONTROL ) );
    fileExit.setOnAction( event -> Utils.trace( "EXIT - NOT YET IMPLEMENTED" ) );
    return fileExit;
  }

}
