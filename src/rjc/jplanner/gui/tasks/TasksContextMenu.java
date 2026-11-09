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

package rjc.jplanner.gui.tasks;

import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rjc.jplanner.commands.CommandTasksDelete;
import rjc.jplanner.commands.CommandTasksIndent;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.commands.CommandInsertIndexes;
import rjc.table.view.TableContextMenu;
import rjc.table.view.TableContextMenu.TableMenuItems;
import rjc.table.view.TableContextMenuBuilder;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/********************** Task context menu and undoable row and outline actions *******************/
/*************************************************************************************************/

final class TasksContextMenu extends ContextMenu
{
  private final TasksView      m_view;            // view providing data, history, and status
  private final TasksViewInput m_input;           // row-action targeting with the view's selection rules
  private final boolean        m_keyboardTrigger; // keyboard actions use selection without a clicked-row fallback
  private final int            m_triggerRow;      // triggering view row, resolved again when the action runs
  private final int            m_triggerColumn;   // triggering view column, including the row-header marker

  /**************************************** constructor ******************************************/
  TasksContextMenu( TasksView view, TasksViewInput input, int viewRow, int viewColumn, boolean keyboardTrigger )
  {
    m_view = view;
    m_input = input;
    m_triggerRow = viewRow;
    m_triggerColumn = viewColumn;
    m_keyboardTrigger = keyboardTrigger;

    // keep the builder local; the library's static registry would retain this view through it
    new MenuBuilder().buildMenu( this, view, viewRow, viewColumn );
  }

  /***************************************** indentTasks *****************************************/
  private void indentTasks( int direction )
  {
    // finish editing before resolving targets so the command sees the committed task and selection
    AbstractCellEditor.endEditing();
    try
    {
      int[] dataRows = m_view.getIndentDataRows( m_triggerRow, m_triggerColumn, m_keyboardTrigger );
      if ( dataRows.length > 0 )
        m_view.getUndoStack().push( new CommandTasksIndent( (TasksData) m_view.getData(), dataRows, direction ) );
    }
    catch ( IllegalArgumentException | IllegalStateException exception )
    {
      m_view.getStatus().update( Level.ERROR, exception.getMessage() );
    }
  }

  /****************************************** editRows *******************************************/
  private void editRows( int viewRow, boolean delete )
  {
    // resolve row targets after the active editor has attempted to commit
    AbstractCellEditor.endEditing();
    try
    {
      var viewRows = m_input.getRowActionViewRows( viewRow );
      int[] dataRows = viewRows.toArray();
      for ( int index = 0; index < dataRows.length; index++ )
      {
        int row = dataRows[index];
        if ( row < 0 || row > m_view.getRowsAxis().getCount() )
          throw new IllegalArgumentException( "Invalid task row" );
        dataRows[index] = row == m_view.getRowsAxis().getCount() ? row : m_view.getRowsAxis().getDataIndex( row );
      }
      var data = (TasksData) m_view.getData();
      boolean changed;
      if ( delete )
        changed = m_view.getUndoStack().push( new CommandTasksDelete( data, m_view.getRowsAxis(), dataRows ) );
      else
      {
        // preflight every target before the generic command starts changing data or geometry
        data.checkInsertRows( dataRows );
        changed = m_view.getUndoStack().push( new CommandInsertIndexes( m_view, Orientation.VERTICAL, viewRows ) );
      }
      if ( changed )
        m_view.getSelection().clear();
    }
    catch ( IllegalArgumentException | IllegalStateException exception )
    {
      m_view.getStatus().update( Level.ERROR, exception.getMessage() );
    }
  }

  /************************************* addIndentActions ***************************************/
  private void addIndentActions()
  {
    var indent = new MenuItem( "Indent" );
    var outdent = new MenuItem( "Outdent" );
    indent.setOnAction( event -> indentTasks( 1 ) );
    outdent.setOnAction( event -> indentTasks( -1 ) );
    getItems().addAll( indent, outdent );
  }

  /**************************************** addRowAction *****************************************/
  private void addRowAction( String text, int viewRow, boolean delete )
  {
    var item = new MenuItem( text );
    item.setOnAction( event -> editRows( viewRow, delete ) );
    getItems().add( item );
  }

  // reuse the standard header actions while replacing task row edits with guarded commands
  private class MenuBuilder extends TableContextMenuBuilder
  {
    /************************************* buildRowHeader **************************************/
    @Override
    protected ContextMenu buildRowHeader()
    {
      super.buildRowHeader();
      if ( !getItems().isEmpty() )
        addSeparator();
      addIndentActions();
      return TasksContextMenu.this;
    }

    /**************************************** buildBody ***************************************/
    @Override
    protected ContextMenu buildBody()
    {
      addIndentActions();
      return TasksContextMenu.this;
    }

    /*************************************** addInsertRow **************************************/
    @Override
    protected TableContextMenuBuilder addInsertRow()
    {
      if ( TableContextMenu.isEnabled( TasksContextMenu.this.m_view, TableMenuItems.ROW_INSERT ) )
        addRowAction( "Insert", TasksContextMenu.this.m_triggerRow, false );
      return this;
    }

    /*************************************** addAppendRow **************************************/
    @Override
    protected TableContextMenuBuilder addAppendRow()
    {
      if ( TableContextMenu.isEnabled( TasksContextMenu.this.m_view, TableMenuItems.ROW_INSERT ) )
        addRowAction( "Add new task", TasksContextMenu.this.m_view.getRowsAxis().getCount(), false );
      return this;
    }

    /*************************************** addDeleteRow **************************************/
    @Override
    protected TableContextMenuBuilder addDeleteRow()
    {
      if ( TableContextMenu.isEnabled( TasksContextMenu.this.m_view, TableMenuItems.ROW_DELETE ) )
        addRowAction( "Delete", TasksContextMenu.this.m_triggerRow, true );
      return this;
    }
  }
}
