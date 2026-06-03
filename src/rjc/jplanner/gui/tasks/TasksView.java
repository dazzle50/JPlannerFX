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
import javafx.scene.input.ContextMenuEvent;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.jplanner.gui.editors.EditorPredecessors;
import rjc.jplanner.gui.editors.EditorResources;
import rjc.jplanner.gui.editors.EditorTimeSpan;
import rjc.jplanner.plan.tasks.TaskType;
import rjc.table.data.TableData;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.DateTime.IntervalUnit;
import rjc.table.view.Colours;
import rjc.table.view.TableContextMenu;
import rjc.table.view.TableContextMenu.TableMenuItems;
import rjc.table.view.TableView;
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

  /**************************************** constructor ******************************************/
  public TasksView( TableData data, String name )
  {
    // construct the table view
    super( data, name );

    // set table view properties
    TableContextMenu.disable( this, TableMenuItems.ROW_SORT, TableMenuItems.ROW_FILTER_TEXT );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();
    getColumnsAxis().setNominalSize( FIELD.Title.ordinal(), 200 );
    getColumnsAxis().setNominalSize( FIELD.Duration.ordinal(), 60 );
    getColumnsAxis().setNominalSize( FIELD.Start.ordinal(), 140 );
    getColumnsAxis().setNominalSize( FIELD.End.ordinal(), 140 );
    getColumnsAxis().setNominalSize( FIELD.Comment.ordinal(), 250 );
  }

  /****************************************** relocate *******************************************/
  @Override
  public void relocate( double x, double y )
  {
    // do nothing when relocate called - to prevent jitter when using split-pane
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // if cell is disabled (e.g. textPaint is null) then can't edit
    cell.getValueVisual();
    if ( cell.visual.cellBackground == Colours.CELL_DISABLED_BACKGROUND )
      return null;

    // determine editor appropriate for cell
    switch ( FIELD.values()[cell.dataColumn] )
    {
      case Title, Comment:
        return new EditorText();

      case Duration:
        return new EditorTimeSpan();

      case Start, End:
        return new EditorDateTime();

      case Deadline:
        var deadline = new EditorDateTime();
        if ( cell.value == null )
          Platform.runLater( () -> deadline.setValue( DateTime.now().roundUp( IntervalUnit.DAY ) ) );
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
    // build context menu
    var menu = new TableContextMenu( this );

    // loop through menu items find item with text "Add row" and rename it
    menu.getItems().stream().filter( item -> item.getText().equals( "Add row" ) )
        .forEach( item -> item.setText( "Add new task" ) );

    // show the context menu if it has any items
    if ( menu != null && !menu.getItems().isEmpty() )
      menu.show( getScene().getWindow(), event.getScreenX(), event.getScreenY() );
  }

}
