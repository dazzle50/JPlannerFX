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

package rjc.jplanner.gui.menu;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import rjc.jplanner.Main;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/**************************** Edit menu for main application menu bar ****************************/
/*************************************************************************************************/

public class EditMenu extends Menu
{
  private MenuItem        m_editUndo;
  private MenuItem        m_editRedo;

  private static Modifier CONTROL = KeyCombination.CONTROL_DOWN;

  /***************************************** constructor *****************************************/
  public EditMenu()
  {
    // construct edit menu for main window menu bar
    setText( "Edit" );

    getItems().add( undo() );
    getItems().add( redo() );
    getItems().add( new SeparatorMenuItem() );
    getItems().add( findReplace() );
    getItems().add( schedule() );

    // just before menu is show update the undo/redo menu items
    setOnShowing( event ->
    {
      // check if possible to undo
      UndoStack stack = Main.getUndostack();
      boolean undo = stack.getIndex() > 0;
      m_editUndo.setText( "Undo " + ( undo ? stack.getUndoText() : "" ) );
      m_editUndo.setDisable( !undo );

      // check if possible to redo
      boolean redo = stack.getIndex() < stack.getSize();
      m_editRedo.setText( "Redo " + ( redo ? stack.getRedoText() : "" ) );
      m_editRedo.setDisable( !redo );
    } );
  }

  /******************************************** undo *********************************************/
  private MenuItem undo()
  {
    // undo last command on undo-stack
    m_editUndo = new MenuItem();
    m_editUndo.setAccelerator( new KeyCodeCombination( KeyCode.Z, CONTROL ) );
    m_editUndo.setOnAction( event -> Main.getUndostack().undo() );
    return m_editUndo;
  }

  /******************************************** redo *********************************************/
  private MenuItem redo()
  {
    // redo next command on undo-stack
    m_editRedo = new MenuItem();
    m_editRedo.setAccelerator( new KeyCodeCombination( KeyCode.Y, CONTROL ) );
    m_editRedo.setOnAction( event -> Main.getUndostack().redo() );

    return m_editRedo;
  }

  /***************************************** findReplace *****************************************/
  private MenuItem findReplace()
  {
    // find replace functionality
    MenuItem editFind = new MenuItem( "Find/Replace..." );
    editFind.setAccelerator( new KeyCodeCombination( KeyCode.F, CONTROL ) );
    editFind.setDisable( true );
    return editFind;
  }

  /****************************************** schedule *******************************************/
  private MenuItem schedule()
  {
    // trigger plan scheduling
    MenuItem editSchedule = new MenuItem( "Schedule" );
    editSchedule.setDisable( true );
    return editSchedule;
  }

}
