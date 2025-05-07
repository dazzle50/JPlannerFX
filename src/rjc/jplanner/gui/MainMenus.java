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

package rjc.jplanner.gui;

import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.stage.Stage;
import rjc.jplanner.Main;
import rjc.table.Utils;
import rjc.table.undo.UndoStack;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/*********************************** Main application menu bar ***********************************/
/*************************************************************************************************/

public class MainMenus extends MenuBar
{
  private Menu            m_menuFile;
  private Menu            m_menuEdit;
  private Menu            m_menuReport;
  private Menu            m_menuView;
  private Menu            m_menuHelp;

  private MenuItem        editUndo;
  private MenuItem        editRedo;

  // modifier shortcuts
  private static Modifier SHIFT   = KeyCombination.SHIFT_DOWN;
  private static Modifier CONTROL = KeyCombination.CONTROL_DOWN;

  /**************************************** constructor ******************************************/
  public MainMenus()
  {
    // construct main window menu bar
    m_menuFile = fileMenu();
    m_menuEdit = editMenu();
    m_menuReport = reportMenu();
    m_menuView = viewMenu();
    m_menuHelp = helpMenu();

    getMenus().addAll( m_menuFile, m_menuEdit, m_menuReport, m_menuView, m_menuHelp );
  }

  /***************************************** onMenuShow ******************************************/
  private void onMenuShow()
  {
    // if any table cell editing in progress, end it
    AbstractCellEditor.endEditing();
  }

  /****************************************** fileMenu *******************************************/
  private Menu fileMenu()
  {
    // file menu
    Menu menu = new Menu( "File" );
    menu.setOnShowing( event -> onMenuShow() );

    MenuItem fileNew = new MenuItem( "New" );
    fileNew.setAccelerator( new KeyCodeCombination( KeyCode.N, CONTROL ) );
    fileNew.setOnAction( event -> Utils.trace( "NEW - NOT YET IMPLEMENTED" ) );

    MenuItem fileOpen = new MenuItem( "Open..." );
    fileOpen.setAccelerator( new KeyCodeCombination( KeyCode.O, CONTROL ) );
    fileOpen.setOnAction( event -> Utils.trace( "OPEN - NOT YET IMPLEMENTED" ) );

    MenuItem fileSave = new MenuItem( "Save" );
    fileSave.setAccelerator( new KeyCodeCombination( KeyCode.S, CONTROL ) );
    fileSave.setOnAction( event -> Utils.trace( "SAVE - NOT YET IMPLEMENTED" ) );

    MenuItem fileSaveAs = new MenuItem( "Save As..." );
    fileSaveAs.setOnAction( event -> Utils.trace( "SAVE AS - NOT YET IMPLEMENTED" ) );

    MenuItem filePrintPreview = new MenuItem( "Print preview..." );
    filePrintPreview.setDisable( true );

    MenuItem filePrint = new MenuItem( "Print..." );
    filePrint.setDisable( true );

    MenuItem fileExit = new MenuItem( "Exit" );
    fileExit.setAccelerator( new KeyCodeCombination( KeyCode.Q, CONTROL ) );
    fileExit.setOnAction( event -> Utils.trace( "EXIT - NOT YET IMPLEMENTED" ) );

    menu.getItems().addAll( fileNew, fileOpen, fileSave, fileSaveAs );
    menu.getItems().addAll( new SeparatorMenuItem(), filePrintPreview, filePrint );
    menu.getItems().addAll( new SeparatorMenuItem(), fileExit );
    return menu;
  }

  /****************************************** editMenu *******************************************/
  private Menu editMenu()
  {
    // edit menu
    Menu menu = new Menu( "Edit" );
    menu.setOnShowing( event ->
    {
      onMenuShow();

      // check if possible to undo
      UndoStack stack = Main.getUndostack();
      if ( stack.getIndex() > 0 )
      {
        editUndo.setText( "Undo " + stack.getUndoText() );
        editUndo.setDisable( false );
      }
      else
      {
        editUndo.setText( "Undo" );
        editUndo.setDisable( true );
      }

      // check if possible to redo
      if ( stack.getIndex() < stack.getSize() )
      {
        editRedo.setText( "Redo " + stack.getRedoText() );
        editRedo.setDisable( false );
      }
      else
      {
        editRedo.setText( "Redo" );
        editRedo.setDisable( true );
      }
    } );

    editUndo = new MenuItem();
    editUndo.setAccelerator( new KeyCodeCombination( KeyCode.Z, CONTROL ) );
    editUndo.setOnAction( event -> Main.getUndostack().undo() );

    editRedo = new MenuItem();
    editRedo.setAccelerator( new KeyCodeCombination( KeyCode.Y, CONTROL ) );
    editRedo.setOnAction( event -> Main.getUndostack().redo() );

    MenuItem editFind = new MenuItem( "Find/Replace..." );
    editFind.setAccelerator( new KeyCodeCombination( KeyCode.F, CONTROL ) );
    editFind.setDisable( true );

    MenuItem editSchedule = new MenuItem( "Schedule" );
    editSchedule.setDisable( true );

    menu.getItems().addAll( editUndo, editRedo, new SeparatorMenuItem() );
    menu.getItems().addAll( editFind, editSchedule );
    return menu;
  }

  /***************************************** reportMenu ******************************************/
  private Menu reportMenu()
  {
    // report menu
    Menu menu = new Menu( "Report" );
    menu.setOnShowing( event -> onMenuShow() );

    MenuItem reportTBD = new MenuItem( "TBD" );
    reportTBD.setDisable( true );

    menu.getItems().addAll( reportTBD );
    return menu;
  }

  /****************************************** viewMenu *******************************************/
  private Menu viewMenu()
  {
    // view menu
    Menu menu = new Menu( "View" );
    menu.setOnShowing( event -> onMenuShow() );

    // show/hide undostack window
    var viewUndoStack = new CheckMenuItem( "Undo Stack..." );
    var undoWindow = Main.getUndoWindow();
    undoWindow.showingProperty().addListener( ( property, oldShow, newShow ) -> viewUndoStack.setSelected( newShow ) );
    viewUndoStack.setOnAction( event ->
    {
      if ( undoWindow.isShowing() )
        undoWindow.hide();
      else
      {
        undoWindow.show();
        undoWindow.toFront();
      }
    } );

    // create new window
    var viewNewWindow = new MenuItem( "New Window..." );
    viewNewWindow.setOnAction( event ->
    {
      Stage stage = new Stage();
      stage.setTitle( "JPlannerFX" );
      stage.getIcons().add( Main.getIcon() );
      stage.setScene( new Scene( new MainTabs( false ) ) );
      stage.show();
    } );

    menu.getItems().addAll( viewUndoStack, viewNewWindow );
    return menu;
  }

  /****************************************** helpMenu *******************************************/
  private Menu helpMenu()
  {
    // help menu
    Menu menu = new Menu( "Help" );
    menu.setOnShowing( event -> onMenuShow() );

    MenuItem helpAbout = new MenuItem( "About JPlannerFX " + Main.VERSION );
    helpAbout.setDisable( true );

    menu.getItems().addAll( helpAbout );
    return menu;
  }

}
