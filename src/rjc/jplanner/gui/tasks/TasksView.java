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

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import rjc.jplanner.commands.CommandTaskSetTitle;
import rjc.jplanner.gui.editors.EditorPredecessors;
import rjc.jplanner.gui.editors.EditorResources;
import rjc.jplanner.gui.editors.EditorTimeSpan;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.jplanner.plan.tasks.TaskType;
import rjc.table.data.TableData;
import rjc.table.data.types.DateInterval;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.UndoStack;
import rjc.table.view.Colours;
import rjc.table.view.TableContextMenu;
import rjc.table.view.TableContextMenu.TableMenuItems;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorChoose;
import rjc.table.view.editor.EditorDateTime;
import rjc.table.view.editor.EditorInteger;
import rjc.table.view.editor.EditorText;

/*************************************************************************************************/
/********************* Customised table-view for interacting with plan tasks *********************/
/*************************************************************************************************/

public class TasksView extends TableView
{
  private final TasksViewInput     m_input;         // input safeguards and selection targeting for this view
  private final TasksRowVisibility m_rowVisibility; // independent collapse and manual hiding state

  /**************************************** constructor ******************************************/
  public TasksView( TableData data, String name )
  {
    // construct the table view before wrapping the library's input handlers
    super( data, name );

    // keep unsupported row reordering, sorting, and filtering unavailable
    TableContextMenu.disable( this, TableMenuItems.COLUMN_SORT, TableMenuItems.ROW_SORT,
        TableMenuItems.ROW_FILTER_TEXT );
    m_input = new TasksViewInput( this );
    m_rowVisibility = new TasksRowVisibility( this, m_input );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();
    getColumnsAxis().setNominalSize( FIELD.Title.ordinal(), 200 );
    getColumnsAxis().setNominalSize( FIELD.Duration.ordinal(), 60 );
    getColumnsAxis().setNominalSize( FIELD.Work.ordinal(), 60 );
    getColumnsAxis().setNominalSize( FIELD.Start.ordinal(), 140 );
    getColumnsAxis().setNominalSize( FIELD.End.ordinal(), 140 );
    getColumnsAxis().setNominalSize( FIELD.Rounding.ordinal(), 70 );
    getColumnsAxis().setNominalSize( FIELD.Comment.ordinal(), 250 );
    // reset also runs during superclass construction, before task view components exist
    if ( m_rowVisibility != null )
      m_rowVisibility.reset();
  }

  /************************************** getRowVisibility ***************************************/
  TasksRowVisibility getRowVisibility()
  {
    return m_rowVisibility;
  }

  /**************************************** setUndostack *****************************************/
  @Override
  public void setUndostack( UndoStack undostack )
  {
    super.setUndostack( undostack );
    if ( m_rowVisibility != null )
      m_rowVisibility.observeUndoStack( getUndoStack() );
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    return new TasksCellDrawer();
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // if cell is disabled or read-only then can't edit
    cell.getValueVisual();
    if ( cell.visual.cellBackground == Colours.CELL_DISABLED_BACKGROUND )
      return null;

    // determine editor appropriate for cell
    switch ( FIELD.values()[cell.dataColumn] )
    {
      case Title:
        // capture the edited model row for the title-and-indentation command
        final int dataRow = cell.dataRow;
        return new EditorText()
        {
          /*************************************** commit ****************************************/
          @Override
          protected boolean commit()
          {
            try
            {
              return getUndoStack().push( new CommandTaskSetTitle( (TasksData) getData(), dataRow, getValue() ) );
            }
            catch ( IllegalArgumentException | IllegalStateException exception )
            {
              getStatus().update( Level.ERROR, exception.getMessage() );
              return false;
            }
          }
        };

      case Comment:
        return new EditorText();

      case Duration, Work, Rounding:
        return new EditorTimeSpan();

      case Start, End:
        return new EditorDateTime();

      case Deadline:
        var deadline = new EditorDateTime();
        if ( cell.value == null )
          Platform.runLater( () -> deadline.setValue( DateTime.now().endOf( DateInterval.DAY ) ) );
        return deadline;

      case Priority:
        var priority = new EditorInteger();
        priority.setRange( 0, 999 );
        priority.setStepPage( 10, 100 );
        return priority;

      case Predecessors:
        return new EditorPredecessors();

      case Resources:
        return new EditorResources();

      case Type:
        return new EditorChoose( TaskType.values() );

      default:
        return null;
    }
  }

  /*************************************** openContextMenu ***************************************/
  @Override
  public void openContextMenu( ContextMenuEvent event )
  {
    // focus positions locate keyboard menus, but never supply a fallback indentation target
    boolean keyboardTrigger = event.isKeyboardTrigger();
    var position = keyboardTrigger ? getFocusCell() : getMouseCell();
    if ( position.getRow() < TableAxis.HEADER || position.getColumn() < TableAxis.HEADER )
      return;
    var menu = buildContextMenu( position.getRow(), position.getColumn(), keyboardTrigger );

    // show the context menu if it has any items
    if ( !menu.getItems().isEmpty() )
      menu.show( getScene().getWindow(), event.getScreenX(), event.getScreenY() );
  }

  /*************************************** buildContextMenu **************************************/
  protected ContextMenu buildContextMenu( int viewRow, int viewColumn, boolean keyboardTrigger )
  {
    // build a fresh menu so its handlers retain this request's trigger coordinates
    return new TasksContextMenu( this, m_input, viewRow, viewColumn, keyboardTrigger );
  }

  /************************************** getIndentDataRows **************************************/
  protected int[] getIndentDataRows( int viewRow, int viewColumn, boolean keyboardTrigger )
  {
    // keep the view's target-resolution hook available to subclasses and menu actions
    return m_input.getIndentDataRows( viewRow, viewColumn, keyboardTrigger );
  }
}
