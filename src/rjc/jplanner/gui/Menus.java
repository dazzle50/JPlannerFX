/**************************************************************************
 *  Copyright (C) 2018 by Richard Crook                                   *
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

import java.util.Set;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import rjc.jplanner.JPlanner;
import rjc.jplanner.command.UndoStack;
import rjc.jplanner.gui.table.AbstractCellEditor;
import rjc.jplanner.gui.table.Table;

/*************************************************************************************************/
/****************************** Main JPlanner application menu bar *******************************/
/*************************************************************************************************/

class Menus extends MenuBar
{
  // public variables
  public CheckMenuItem viewUndoStack;
  public MenuItem      editUndo;
  public MenuItem      editRedo;
  public Menu          menuTasks;
  public MenuItem      tasksIndent;
  public MenuItem      tasksOutdent;

  // modifier shortcuts
  private Modifier     SHIFT   = KeyCombination.SHIFT_DOWN;
  private Modifier     CONTROL = KeyCombination.CONTROL_DOWN;

  /**************************************** constructor ******************************************/
  public Menus()
  {
    // construct main window menu bar
    Menu menuFile = fileMenu();
    Menu menuEdit = editMenu();
    Menu menuReport = reportMenu();
    menuTasks = tasksMenu();
    menuTasks.setDisable( true );
    Menu menuView = viewMenu();
    Menu menuHelp = helpMenu();

    getMenus().addAll( menuFile, menuEdit, menuReport, menuTasks, menuView, menuHelp );
  }

  /***************************************** onMenuShow ******************************************/
  private void onMenuShow()
  {
    // clear status bar message
    JPlanner.gui.message();

    // if any table cell editing in progress, end it
    AbstractCellEditor.endEditing();

    // ensure plan is up-to-date
    JPlanner.gui.checkPlanUpToDate();
  }

  /*************************************** onTaskMenuShow ****************************************/
  private void onTaskMenuShow()
  {
    // perform normal onMenuShow functionality
    onMenuShow();

    // enable or disable tasks indent + outdent menu items
    Table table = JPlanner.gui.getTabs().get( 0 ).getTasksTab().getTable();
    Set<Integer> rows = table.getSelected().getRowsWithSelection();
    tasksIndent.setDisable( JPlanner.plan.tasks.canIndent( rows ).isEmpty() );
    tasksOutdent.setDisable( JPlanner.plan.tasks.canOutdent( rows ).isEmpty() );
  }

  /****************************************** fileMenu *******************************************/
  private Menu fileMenu()
  {
    // file menu
    Menu menu = new Menu( "File" );
    menu.setOnShowing( event -> onMenuShow() );

    MenuItem fileNew = new MenuItem( "New" );
    fileNew.setAccelerator( new KeyCodeCombination( KeyCode.N, CONTROL ) );
    fileNew.setOnAction( event -> JPlanner.gui.newPlan() );

    MenuItem fileOpen = new MenuItem( "Open..." );
    fileOpen.setAccelerator( new KeyCodeCombination( KeyCode.O, CONTROL ) );
    fileOpen.setOnAction( event -> JPlanner.gui.load() );

    MenuItem fileSave = new MenuItem( "Save" );
    fileSave.setAccelerator( new KeyCodeCombination( KeyCode.S, CONTROL ) );
    fileSave.setOnAction( event -> JPlanner.gui.save() );

    MenuItem fileSaveAs = new MenuItem( "Save As..." );
    fileSaveAs.setOnAction( event -> JPlanner.gui.saveAs() );

    MenuItem filePrintPreview = new MenuItem( "Print preview..." );
    filePrintPreview.setDisable( true );

    MenuItem filePrint = new MenuItem( "Print..." );
    filePrint.setDisable( true );

    MenuItem fileExit = new MenuItem( "Exit" );
    fileExit.setAccelerator( new KeyCodeCombination( KeyCode.Q, CONTROL ) );
    fileExit.setOnAction( event -> JPlanner.gui.quit() );

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
      UndoStack stack = JPlanner.plan.getUndostack();
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
      if ( stack.getIndex() < stack.size() )
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
    editUndo.setOnAction( event -> JPlanner.plan.getUndostack().undo() );

    editRedo = new MenuItem();
    editRedo.setAccelerator( new KeyCodeCombination( KeyCode.Y, CONTROL ) );
    editRedo.setOnAction( event -> JPlanner.plan.getUndostack().redo() );

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

  /****************************************** tasksMenu ******************************************/
  private Menu tasksMenu()
  {
    // tasks menu (only enabled when Tasks tab selected)
    Menu menu = new Menu( "Tasks" );
    menu.setOnShowing( event -> onTaskMenuShow() );

    tasksIndent = new MenuItem( "Indent" );
    tasksIndent.setAccelerator( new KeyCodeCombination( KeyCode.GREATER, CONTROL ) );
    tasksIndent.setDisable( true );
    tasksIndent.setOnAction( event ->
    {
      Table table = JPlanner.gui.getTabs().get( 0 ).getTasksTab().getTable();
      Set<Integer> rows = table.getSelected().getRowsWithSelection();
      table.getData().indentRows( rows );
    } );

    tasksOutdent = new MenuItem( "Outdent" );
    tasksOutdent.setAccelerator( new KeyCodeCombination( KeyCode.LESS, CONTROL ) );
    tasksOutdent.setDisable( true );
    tasksOutdent.setOnAction( event ->
    {
      Table table = JPlanner.gui.getTabs().get( 0 ).getTasksTab().getTable();
      Set<Integer> rows = table.getSelected().getRowsWithSelection();
      table.getData().outdentRows( rows );
    } );

    menu.getItems().addAll( tasksIndent, tasksOutdent );
    return menu;
  }

  /****************************************** viewMenu *******************************************/
  private Menu viewMenu()
  {
    // view menu
    Menu menu = new Menu( "View" );
    menu.setOnShowing( event -> onMenuShow() );

    viewUndoStack = new CheckMenuItem( "Undo Stack..." );
    viewUndoStack.setOnAction( event -> JPlanner.gui.showUndoStackWindow( viewUndoStack.isSelected() ) );

    MenuItem viewNewWindow = new MenuItem( "New Window..." );
    viewNewWindow.setOnAction( event -> JPlanner.gui.newWindow() );

    menu.getItems().addAll( viewUndoStack, viewNewWindow );
    return menu;
  }

  /****************************************** helpMenu *******************************************/
  private Menu helpMenu()
  {
    // help menu
    Menu menu = new Menu( "Help" );
    menu.setOnShowing( event -> onMenuShow() );

    MenuItem helpAbout = new MenuItem( "About JPlannerFX " + JPlanner.VERSION );
    helpAbout.setDisable( true );

    menu.getItems().addAll( helpAbout );

    // benchmarking
    addBenchmark( menu, "Null", () ->
    {
    }, 1000 );
    addBenchmark( menu, "Trace Here", () -> JPlanner.trace( "Here" ), 100 );
    addBenchmark( menu, "Load reference.xml",
        () -> JPlanner.gui.load( new java.io.File( "C:\\Users\\Richard\\Desktop\\tests\\reference.xml" ) ), 1000 );

    return menu;
  }

  /**************************************** addBenchmark *****************************************/
  private void addBenchmark( Menu menu, String name, Runnable test, int count )
  {
    // add benchmark test to menu
    MenuItem benchmark = new MenuItem( "BenchMark - " + count + " " + name );
    menu.getItems().addAll( benchmark );

    benchmark.setOnAction( event ->
    {
      // run benchmark requested number of times
      long[] nanos = new long[count + 1];
      JPlanner.trace( "######### BENCHMARK START - " + name + " " + count + " times" );
      nanos[0] = System.nanoTime();
      for ( int num = 1; num <= count; num++ )
      {
        test.run();
        nanos[num] = System.nanoTime();
      }

      // report each run duration
      for ( int num = 0; num < count; num++ )
        trace( "Run " + ( num + 1 ) + " duration =", nanos[num + 1] - nanos[num] );

      // report total & average duration
      long total = nanos[count] - nanos[0];
      trace( "  Total duration =", total );
      trace( "Average duration =", total / count );
    } );

  }

  /******************************************** trace ********************************************/
  private void trace( String text, long nanos )
  {
    // generate trace output with nano-seconds in human readable format
    String units = " ns";
    double div = 1.0;

    if ( nanos > 1000L )
    {
      units = " \u00B5s";
      div = 1000.0;
    }

    if ( nanos > 1000000L )
    {
      units = " ms";
      div = 1000000.0;
    }

    if ( nanos > 1000000000L )
    {
      units = " s";
      div = 1000000000.0;
    }

    JPlanner.trace( "BENCHMARK " + text + String.format( "%8.3f", nanos / div ) + units );
  }

}
